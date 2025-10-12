import CentralunitEndpoint from "./centralunit-endpoint.vue";
import TracesEndpoint from "./traces.vue";

;(() => {
    SBA.use({
        install({viewRegistry}) {
            viewRegistry.addView({
                name: "instances/centralunit",
                parent: "instances",
                path: "centralunit",
                component: CentralunitEndpoint,
                label: "Central unit",
                group: "centralunit",
                order: -2,
                isEnabled: ({instance}) => {
                    return instance.hasEndpoint("centralunit");
                },
            });

            viewRegistry.setGroupIcon(
                'centralunit',
                `<svg xmlns='http://www.w3.org/2000/svg' class='h-5 mr-3' viewBox='0 0 512 512' aria-hidden='true' role='img'>
     <g stroke='currentColor' stroke-width='40' stroke-linecap='round' stroke-linejoin='round'>
       <!-- Huis (dak + muren) -->
       <path d='M256 80 L80 224 H112 V416 H400 V224 H432 L256 80' fill='none'/>

       <!-- CPU / chipset binnen het huis -->
       <rect x='196' y='256' width='120' height='120' rx='16' fill='none'/>

       <!-- Pins boven -->
       <line x1='216' y1='240' x2='216' y2='256' />
       <line x1='256' y1='240' x2='256' y2='256' />
       <line x1='296' y1='240' x2='296' y2='256' />

       <!-- Pins onder -->
       <line x1='216' y1='376' x2='216' y2='392' />
       <line x1='256' y1='376' x2='256' y2='392' />
       <line x1='296' y1='376' x2='296' y2='392' />

       <!-- Pins links -->
       <line x1='180' y1='276' x2='196' y2='276' />
       <line x1='180' y1='316' x2='196' y2='316' />
       <line x1='180' y1='356' x2='196' y2='356' />

       <!-- Pins rechts -->
       <line x1='316' y1='276' x2='332' y2='276' />
       <line x1='316' y1='316' x2='332' y2='316' />
       <line x1='316' y1='356' x2='332' y2='356' />

       <!-- Core in de chip (gevuld) -->
       <circle cx='256' cy='316' r='10' fill='currentColor' stroke='none' />
     </g>
   </svg>`
            );
        }
    });

    SBA.use({
        install({viewRegistry}) {
            viewRegistry.addView({
                name: "instances/traces",
                parent: "instances",
                path: "traces",
                component: TracesEndpoint,
                label: "MQTT Messages",
                group: "traces",
                order: -1,
                isEnabled: ({instance}) => {
                    return instance.hasEndpoint("traces");
                },
            });

            viewRegistry.setGroupIcon(
                'traces',
                `<svg xmlns='http://www.w3.org/2000/svg' class='h-5 mr-3' viewBox='0 0 512 512' aria-hidden='true' role='img'>
     <g stroke='currentColor' stroke-width='40' stroke-linecap='round' stroke-linejoin='round'>
       <!-- Centrale hub -->
       <circle cx='256' cy='256' r='56' fill='none'/>

       <!-- Inkomend (links -> hub) -->
       <path d='M64 256 H200' />
       <polygon points='200,256 180,246 180,266' fill='currentColor' stroke='none'/>

       <!-- Uitgaand (hub -> rechts) -->
       <path d='M312 256 H448' />
       <polygon points='448,256 428,246 428,266' fill='currentColor' stroke='none'/>

       <!-- Inkomend (boven -> hub) -->
       <path d='M256 64 V200' />
       <polygon points='256,200 246,180 266,180' fill='currentColor' stroke='none'/>

       <!-- Uitgaand (hub -> onder) -->
       <path d='M256 312 V448' />
       <polygon points='256,448 246,428 266,428' fill='currentColor' stroke='none'/>
     </g>
   </svg>`
            );
        }
    });
})();