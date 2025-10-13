<template>
  <section class="shadow-sm border rounded mb-4 p-4">
    <div class="flex-1 px-2 md:px-6 py-6">
      <div class="shadow-sm border rounded break-inside-avoid mb-4 bg-white">
        <header class="rounded-t flex justify-between px-4 pt-5 pb-5 border-b sm:px-6 items-center bg-white transition-all">
          <h3 class="text-lg leading-6 font-medium text-gray-900 flex-1">
            <span>Central unit</span>
          </h3>
        </header>

        <div class="p-3" v-if="loading">Loading…</div>
        <div v-else-if="error" class="p-3 text-red-600">
          Error: {{ error.message || error }}
          <div v-if="error.status" class="text-xs text-gray-500">Status: {{ error.status }}</div>
        </div>

        <div v-else>
          <div class="mb-3 p-3">
            <input
                type="search"
                v-model.trim="searchQuery"
                placeholder="Search by type, number, description, state, …"
                class="border rounded px-3 py-2 w-full"
                aria-label="Search"
            />
          </div>

          <div class="p-3 overflow-auto">
            <table class="table-auto w-full text-left border-collapse">
              <thead class="bg-gray-50">
              <tr>
                <th class="px-3 py-2 border" scope="col">Function</th>
                <th class="px-3 py-2 border" scope="col">Number</th>
                <th class="px-3 py-2 border" scope="col">Description</th>
                <th class="px-3 py-2 border" scope="col">Additional&nbsp;config</th>
                <th class="px-3 py-2 border" scope="col">State</th>
                <th class="px-3 py-2 border" scope="col">HA Config</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="row in filteredRows" :key="row._key" class="align-top">
                <td class="px-3 py-2 border whitespace-nowrap">{{ row.componentType }}</td>
                <td class="px-3 py-2 border whitespace-nowrap">{{ row.number }}</td>
                <td class="px-3 py-2 border">{{ row.description }}</td>
                <td class="px-3 py-2 border">
                  <pre class="bg-gray-50 p-2 rounded overflow-auto max-h-40">{{ stringify(row.rest) }}</pre>
                </td>
                <td class="px-3 py-2 border">
                  <pre class="bg-gray-50 p-2 rounded overflow-auto max-h-40">{{ stringify(row.state) }}</pre>
                </td>
                <td class="px-3 py-2 border">
                  <pre class="bg-gray-50 p-2 rounded overflow-auto max-h-40">{{ stringify(row.haPublishedConfig) }}</pre>
                </td>
              </tr>
              <tr v-if="!loading && filteredRows.length === 0">
                <td class="px-3 py-3 text-gray-500" colspan="6">No records found.</td>
              </tr>
              </tbody>
            </table>
          </div>

          <div class="px-3 py-2 text-xs text-gray-500">
            SSE: {{ sseConnected ? 'connected' : 'disconnected' }}
          </div>
        </div>

      </div>
    </div>
  </section>
</template>

<script>
export default {
  name: 'CentralUnitTable',
  props: {
    instance: {type: Object, required: true},
    endpoint: {type: String, default: 'actuator/centralunit'}
  },
  data() {
    return {
      raw: null,
      loading: true,
      error: null,
      searchQuery: '',
      sse: null,
      sseConnected: false,
      reconnectTimer: null
    };
  },
  computed: {
    sseEndpoint() {
      return this.endpoint.replace(/\/?$/, '/stream');
    },
    rows() {
      const out = [];
      const ct = this.raw?.componentsTypes || {};
      for (const [componentType, items] of Object.entries(ct)) {
        (items || []).forEach((item) => {
          const {number, description, haPublishedConfig, state, ...rest} = item || {};
          const pk = `${componentType}::${String(number)}`;
          out.push({
            _key: pk,
            componentType,
            number,
            description,
            haPublishedConfig: haPublishedConfig ?? null,
            state: state ?? null,
            rest
          });
        });
      }
      out.sort((a, b) => {
        const typeCmp = a.componentType.localeCompare(b.componentType);
        if (typeCmp !== 0) return typeCmp;
        const an = Number(a.number);
        const bn = Number(b.number);
        if (!Number.isNaN(an) && !Number.isNaN(bn)) return an - bn;
        return String(a.number).localeCompare(String(b.number));
      });
      return out;
    },
    filteredRows() {
      const q = (this.searchQuery || '').trim().toLowerCase();
      if (!q) return this.rows;
      const tokens = q.split(/\s+/).filter(Boolean);
      return this.rows.filter((r) => {
        const haystack = [
          r.componentType,
          r.number,
          r.description,
          this.safeJson(r.haPublishedConfig),
          this.safeJson(r.state),
          this.safeJson(r.rest)
        ]
            .map(v => (v == null ? '' : String(v)))
            .join(' ')
            .toLowerCase();
        return tokens.every(t => haystack.includes(t));
      });
    }
  },
  methods: {
    stringify(val) {
      try {
        return val == null ? '' : JSON.stringify(val, null, 2);
      } catch {
        return String(val);
      }
    },
    safeJson(val) {
      try {
        return val == null ? '' : JSON.stringify(val);
      } catch {
        return '';
      }
    },
    async fetchData() {
      this.loading = true;
      this.error = null;
      try {
        const {data} = await this.instance.axios.get(this.endpoint);
        this.raw = data;
      } catch (e) {
        this.error = {
          message: e?.message || 'Request failed',
          status: e?.response?.status,
          url: this.endpoint
        };
        this.raw = null;
      } finally {
        this.loading = false;
      }
    },
    buildSseUrl() {
      const base = this.instance?.axios?.defaults?.baseURL || '';
      const path = this.sseEndpoint;

      if (/^https?:\/\//i.test(path)) return path;

      if (/^https?:\/\//i.test(base)) {
        return base.replace(/\/$/, '') + '/' + path.replace(/^\//, '');
      }

      const prefix = (base || '').replace(/\/$/, '');
      const suffix = path.replace(/^\//, '');
      const url = [prefix, suffix].filter(Boolean).join('/');
      return url.startsWith('/') ? url : '/' + url;
    },
    connectSse() {
      this.closeSse();
      const url = this.buildSseUrl();
      console.info('[CentralUnitTable] Opening SSE:', url);
      try {
        const es = new EventSource(url, {withCredentials: true});
        this.sse = es;

        es.onopen = () => {
          this.sseConnected = true;
          console.info('[CentralUnitTable] SSE connected');
        };

        es.onerror = (e) => {
          this.sseConnected = false;
          console.warn('[CentralUnitTable] SSE error, will reconnect in 5s', e);
          this.closeSse();
          if (!this.reconnectTimer) {
            this.reconnectTimer = setTimeout(() => {
              this.reconnectTimer = null;
              this.connectSse();
            }, 5000);
          }
        };

        es.addEventListener('state', (evt) => this.handleSseMessage(evt));
        es.onmessage = (evt) => this.handleSseMessage(evt);
      } catch (e) {
        this.sseConnected = false;
        console.error('[CentralUnitTable] Failed to init SSE, retry in 5s', e);
        this.reconnectTimer = setTimeout(() => {
          this.reconnectTimer = null;
          this.connectSse();
        }, 5000);
      }
    },
    closeSse() {
      if (this.sse) {
        try {
          this.sse.close();
        } catch {
        }
        this.sse = null;
      }
      this.sseConnected = false;
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }
    },
    handleSseMessage(evt) {
      try {
        const update = JSON.parse(evt.data);
        this.applyUpdate(update);
      } catch (e) {
        console.warn('[CentralUnitTable] Invalid SSE payload', e, evt?.data);
      }
    },
    applyUpdate(update) {
      const type = update?.componentType;
      const num = update?.number;
      if (!type || typeof num === 'undefined' || num === null) return;
      if (!this.raw?.componentsTypes?.[type]) return;

      const items = this.raw.componentsTypes[type];
      const idx = items.findIndex(it => String(it?.number) === String(num));
      if (idx < 0) return;

      const existing = items[idx];
      const merged = {...existing, state: update.state};
      items.splice(idx, 1, merged);
    }
  },
  async mounted() {
    await this.fetchData();
    this.connectSse();
  },
  beforeDestroy() {
    this.closeSse();
  },
  beforeUnmount() {
    this.closeSse();
  },
  watch: {
    'instance.id': function () {
      this.fetchData();
      this.connectSse();
    },
    endpoint: function () {
      this.fetchData();
      this.connectSse();
    }
  }
};
</script>
