<template>
  <section class="shadow-sm border rounded mb-4 p-4">
    <div class="flex-1 px-2 md:px-6 py-6">
      <div class="shadow-sm border rounded break-inside-avoid mb-4 bg-white">
        <header
            class="rounded-t flex justify-between px-4 pt-5 pb-5 border-b sm:px-6 items-center bg-white transition-all">
          <h3 class="text-lg leading-6 font-medium text-gray-900 flex-1">
            <span>MQTT Messages</span>
          </h3>
        </header>

        <div class="p-3" v-if="loading">Loading…</div>
        <div v-else-if="error" class="p-3 text-red-600">Error: {{ error.message || error }}</div>

        <div v-else>
          <div class="p-3 mb-3 text-sm text-gray-600" v-if="baseTopic">
            <span>Base topic: {{ baseTopic }}</span>
          </div>

          <div class="mb-3 p-3">
            <input
                type="search"
                v-model.trim="searchQuery"
                placeholder="Search by topic, payload, direction, …"
                class="border rounded px-3 py-2 w-full"
                aria-label="Search"
            />
          </div>

          <div class="p-3 overflow-auto">
            <table class="table-auto w-full text-left border-collapse">
              <thead class="bg-gray-50">
              <tr>
                <th class="px-3 py-2 border">Timestamp</th>
                <th class="px-3 py-2 border">Direction</th>
                <th class="px-3 py-2 border">Topic</th>
                <th class="px-3 py-2 border">Payload</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="row in filteredRows" :key="row._key" class="align-top">
                <td class="px-3 py-2 border whitespace-nowrap">{{ row.createdAt }}</td>
                <td class="px-3 py-2 border whitespace-nowrap">{{ row.direction }}</td>
                <td class="px-3 py-2 border whitespace-nowrap">{{ row.simpleTopic }}</td>
                <td class="px-3 py-2 border">
                  <pre class="bg-gray-50 p-2 rounded overflow-auto max-h-40">{{ row.prettyPayload }}</pre>
                </td>
              </tr>
              <tr v-if="!loading && filteredRows.length === 0">
                <td class="px-3 py-3 text-gray-500" colspan="5">No records found.</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  </section>
</template>

<script>
export default {
  name: 'TracesView',
  props: {
    instance: {type: Object, required: true},
    endpoint: {type: String, default: 'actuator/traces'},
    maxRows: {type: Number, default: 1000}
  },
  data() {
    return {
      raw: [],
      loading: true,
      error: null,
      searchQuery: '',
      es: null,
      sseRetryDelay: 1000,
      sseMaxRetryDelay: 10000,
      sseRetryTimer: null
    };
  },
  computed: {
    rows() {
      return this.raw;
    },
    baseTopic() {
      if (!this.raw.length) return null;
      return (this.raw[0].topic || '').split('/').slice(0, 2).join('/');
    },
    filteredRows() {
      const q = (this.searchQuery || '').trim().toLowerCase();
      if (!q) return this.rows;
      const tokens = q.split(/\s+/).filter(Boolean);
      return this.rows.filter((r) => {
        const haystack = [
          r.simpleTopic,
          r.description,
          this.safeJson(r.payload)
        ].map(v => (v == null ? '' : String(v))).join(' ').toLowerCase();
        return tokens.every(t => haystack.includes(t));
      });
    }
  },
  methods: {
    safeJson(val) {
      try {
        return val == null ? '' : JSON.stringify(val);
      } catch {
        return '';
      }
    },
    stringify(val) {
      try {
        return val == null ? '' : JSON.stringify(val, null, 2);
      } catch {
        return String(val);
      }
    },
    pretty(payload) {
      try {
        const obj = typeof payload === 'string' ? JSON.parse(payload) : payload;
        return JSON.stringify(obj, null, 2);
      } catch {
        return typeof payload === 'string' ? payload : this.stringify(payload);
      }
    },
    normalizeRow(tr) {
      const createdAt = tr.createdAt || new Date().toISOString();
      const topic = tr.topic || '';
      return {
        ...tr,
        createdAt,
        topic,
        direction: tr.direction || 'IN',
        simpleTopic: topic.replace(/^(?:[^/]*\/){2}/, ''),
        prettyPayload: this.pretty(tr.payload),
        _key: tr.id || `${+new Date(createdAt)}-${Math.random().toString(36).slice(2, 8)}`
      };
    },
    async fetchData() {
      this.loading = true;
      this.error = null;
      try {
        const {data} = await this.instance.axios.get(this.endpoint);
        const list = (Array.isArray(data) ? data : (data ? [data] : [])).map(this.normalizeRow);
        this.raw = list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        if (this.maxRows && this.raw.length > this.maxRows) this.raw.length = this.maxRows;
      } catch (e) {
        this.error = e;
        this.raw = [];
      } finally {
        this.loading = false;
      }
    },
    async mountedOpenSseAfterFetch() {
      const ok = await this.fetchData().then(() => true).catch(() => false);
      if (ok) {
        this.openSse();
      } else {
        this.scheduleSseRetry();
      }
    },
    openSse() {
      this.closeSse();
      const base = this.instance?.axios?.defaults?.baseURL;
      if (!base) {
        this.scheduleSseRetry();
        return;
      }

      const url = `${base}/actuator/traces/stream`;
      try {
        this.es = new EventSource(url, {withCredentials: true});
      } catch (e) {
        this.scheduleSseRetry();
        return;
      }

      this.es.onopen = () => {
        // reset backoff
        this.clearSseRetry();
        this.sseRetryDelay = 1000;
      };
      this.es.onmessage = (evt) => {
        // server kan "hello"/"keepalive" of backlog sturen; filter op event.type indien gewenst
        if (!evt.data) return;
        try {
          const item = JSON.parse(evt.data);
          const row = this.normalizeRow(item);
          this.raw.unshift(row);
          if (this.maxRows && this.raw.length > this.maxRows) this.raw.length = this.maxRows;
        } catch { /* kan een keepalive of niet-JSON zijn */
        }
      };
      // Specifieke events (zoals "mqtt") blijven ook werken:
      this.es.addEventListener('mqtt', (evt) => {
        try {
          const item = JSON.parse(evt.data);
          const row = this.normalizeRow(item);
          this.raw.unshift(row);
          if (this.maxRows && this.raw.length > this.maxRows) this.raw.length = this.maxRows;
        } catch {
        }
      });
      this.es.onerror = () => {
        // EventSource probeert zelf te reconnecten; sommige proxies/browsers doen dat niet goed → zelf backoff
        this.closeSse();
        this.scheduleSseRetry();
      };
    },
    scheduleSseRetry() {
      this.clearSseRetry();
      this.sseRetryTimer = setTimeout(() => {
        this.openSse();
        this.sseRetryDelay = Math.min(this.sseRetryDelay * 2, this.sseMaxRetryDelay);
      }, this.sseRetryDelay);
    },
    clearSseRetry() {
      if (this.sseRetryTimer) {
        clearTimeout(this.sseRetryTimer);
        this.sseRetryTimer = null;
      }
    },
    closeSse() {
      this.clearSseRetry();
      if (this.es) {
        try {
          this.es.close();
        } catch {
        }
        this.es = null;
      }
    }
  },
  async mounted() {
    await this.mountedOpenSseAfterFetch();
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
    },
    endpoint: function () {
      this.fetchData();
    }
  }
};
</script>
