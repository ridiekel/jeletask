<template>
  <section class="shadow-sm border rounded mb-4 p-4">
    <div class="flex-1 px-2 md:px-6 py-6">
      <div class="shadow-sm border rounded break-inside-avoid mb-4 bg-white">
        <header
            class="rounded-t flex justify-between px-4 pt-5 pb-5 border-b sm:px-6 items-center bg-white transition-all">
          <h3 class="text-lg leading-6 font-medium text-gray-900 flex-1">
            <span>Central unit</span>
          </h3>
        </header>

        <div class="p-3" v-if="loading">Loading…</div>
        <div v-else-if="error" class="p-3 text-red-600">Error: {{ error.message || error }}</div>

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
                <th class="px-3 py-2 border">Function</th>
                <th class="px-3 py-2 border">Number</th>
                <th class="px-3 py-2 border">Description</th>
                <th class="px-3 py-2 border">Additional&nbsp;config</th>
                <th class="px-3 py-2 border">State</th>
                <th class="px-3 py-2 border">HA Config</th>
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
                  <pre class="bg-gray-50 p-2 rounded overflow-auto max-h-40">{{
                      stringify(row.haPublishedConfig)
                    }}</pre>
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
  name: 'CentralUnitTable',
  props: {
    instance: {type: Object, required: true},
    // Pas aan indien je endpoint anders heet
    endpoint: {type: String, default: 'actuator/centralunit'}
  },
  data() {
    return {
      raw: null,         // volledige payload
      loading: true,
      error: null,
      searchQuery: ''    // client-side zoekterm
    };
  },
  computed: {
    rows() {
      const out = [];
      const ct = this.raw?.componentsTypes || {};
      for (const [componentType, items] of Object.entries(ct)) {
        (items || []).forEach((item) => {
          const {number, description, haPublishedConfig, state, ...rest} = item || {};
          out.push({
            _key: `${componentType}-${number ?? Math.random()}`,
            componentType,
            number,
            description,
            haPublishedConfig: haPublishedConfig ?? null,
            state: state ?? null,
            rest
          });
        });
      }
      out.sort((a, b) =>
          a.componentType.localeCompare(b.componentType) || (a.number ?? 0) - (b.number ?? 0)
      );
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
      } catch (e) {
        return String(val);
      }
    },
    safeJson(val) {
      try {
        return val == null ? '' : JSON.stringify(val);
      } catch (e) {
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
        this.error = e;
        this.raw = null;
      } finally {
        this.loading = false;
      }
    }
  },
  async mounted() {
    await this.fetchData(); // éénmalig ophalen
  },
  watch: {
    // Refetch alleen bij instance/endpoint wissel, niet bij zoeken
    'instance.id': function () {
      this.fetchData();
    },
    endpoint: function () {
      this.fetchData();
    }
  }
};
</script>
