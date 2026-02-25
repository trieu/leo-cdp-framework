class JourneyFlowGraph {
    constructor(containerId, config = {}) {
        this.container = document.getElementById(containerId);
        if (!this.container) throw new Error(`Container #${containerId} not found.`);

        this.config = {
            minZoom: config.minZoom || 0.5,
            maxZoom: config.maxZoom || 5,
            avgPageview: config.avgPageview || 20,
            avgPurchase: config.avgPurchase || 7,
            layout: config.layout || {
                name: 'dagre', rankDir: "LR", fit: true, directed: true, 
                padding: 50, spacingFactor: 1.9, nodeSep: 60, rankSep: 30
            }
        };

        this.cy = null;
    }

    _getStyleSheet() {
        return cytoscape.stylesheet()
            // --- Base Node Style (with dynamic icons) ---
            .selector('node')
            .css({
                'height': 80,
                'width': 80,
                'background-fit': 'cover',
                'background-color': '#ffffff', // Clean background for icons
                'background-image': 'data(image)', // Dynamic icon mapping
                'border-opacity': 1
            })
            
            // --- SCHEMA: 3rd-Party Touchpoints (External channels) ---
            .selector('node[type="touchpoint_3rdparty"]')
            .style({
                'shape': 'ellipse',
                'border-color': '#9E9E9E', // Gray to indicate external
                'border-width': 3,
                'border-style': 'dashed'   // Dashed border for 3rd-party
            })
            
            // --- SCHEMA: 1st-Party Touchpoints (Owned channels) ---
            .selector('node[type="touchpoint_1stparty"]')
            .style({
                'shape': 'hexagon',        // Distinct shape for owned assets
                'border-color': '#4CAF50', // Green to indicate owned/safe
                'border-width': 4,
                'border-style': 'solid'
            })
            
            // --- SCHEMA: CDP Profiles (Identity) ---
            .selector('node[type="cdp_profile"]')
            .style({
                'shape': 'round-rectangle',
                'padding': '6px',
                'border-color': '#F3A06D',
                'border-width': 4,
                'border-style': 'solid'
            })

            // --- Base Edge Style ---
            .selector('edge')
            .css({
                'curve-style': 'unbundled-bezier',
                'width': 4,
                'target-arrow-shape': 'triangle',
                'line-color': '#ffaaaa',
                'target-arrow-color': '#ffaaaa'
            })

            // --- Edge Weights & Thresholds ---
            .selector('edge[weight > 0]')
            .style({
                'label': 'data(weight)',
                'background-color': '#61bffc',  
                'font-size': 20,
                'width': 5,
                'line-color': '#9BB6FB',
                'target-arrow-color': '#0048FF'
            })
            .selector(`edge[weight >= ${this.config.avgPageview}]`)
            .style({
                'line-color': '#618CF9',
                'target-arrow-color': '#0048FF',
                'width': 7
            })
            .selector(`edge[weight >= ${this.config.avgPurchase}]`)
            .style({
                'line-color': '#F3A06D',
                'target-arrow-color': '#F91805',
                'width': 9
            })
            .selector('edge[weight = 0]')
            .style({
                'background-color': '#61bffc',  
                'font-size': 20,
                'line-color': '#9BB6FB',
                'target-arrow-color': '#0048FF'
            });
    }

    _initHtmlLabels() {
        if (!this.cy.nodeHtmlLabel) return;

        this.cy.nodeHtmlLabel([{
            query: 'node',
            halign: 'center',
            valign: 'top',
            halignBox: 'center',
            valignBox: 'top',
            cssClass: '',
            tpl: (data) => {
                const name = data.label ? data.label : data.id.toUpperCase();
                
                // Assign CSS classes based on the schema for targeted CSS styling (e.g., text color)
                let cssClass = "cy_node_default";
                if (data.type === "cdp_profile") cssClass = "cy_node_profile";
                else if (data.type === "touchpoint_1stparty") cssClass = "cy_node_1stparty";
                else if (data.type === "touchpoint_3rdparty") cssClass = "cy_node_3rdparty";

                return `<h4 class="${cssClass}">${name}</h4>`;
            }
        }]);
    }

    render(graphData) {
        this.cy = cytoscape({
            container: this.container,
            boxSelectionEnabled: false,
            autounselectify: true,
            minZoom: this.config.minZoom,
            maxZoom: this.config.maxZoom,
            style: this._getStyleSheet(),
            elements: graphData,
            layout: this.config.layout
        });

        this._initHtmlLabels();
        
        if (typeof initPanZoomController === 'function') {
            initPanZoomController(this.config.minZoom, this.config.maxZoom, this.cy);
        }

        this.cy.fit(50);
    }
}