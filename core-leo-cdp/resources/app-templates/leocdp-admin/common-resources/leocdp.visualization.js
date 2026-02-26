/**
 * 
 * @author tantrieuf31 (Thomas)
 * 
 * this script contains all functions for common data visualization
 * 
 */


const chartColorCodes = [];
for(var i=0; i< 100; i++){
	var colorCode = Math.floor(Math.random()*16777215).toString(16);
	chartColorCodes.push("#"+colorCode);
}

const MAX_TO_LINE_CHART = chartColorCodes.length;
const journeyLabel5A = ['AWARENESS', 'ATTRACTION', 'ASK', 'ACTION','ADVOCACY'];

function randomInteger(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function getColorCodeProfileFunnel(i){
	var colorCodes = ['#7A9F8D' ,  '#5D897C' , '#40736D', '#24585C', '#225059' , '#1F4360' , '#1A2E55', '#161C4A' , '#15113E', '#17202A'];
	return ( i >= 0 && i <= 10  ) ? colorCodes[i-1] : colorCodes[0];
}

const renderFunnelChart = function(selectorHolder, stages, options, colorCodesFunction) {
	var data = [];
	stages.forEach(function(e){
		var label = e.name;
		var idx = e.orderIndex;
		data.push([label, "[Stage " + idx + "]", colorCodesFunction(idx) ]);
	});     
    var chart = new D3Funnel(selectorHolder);
    chart.draw(data, options);
}

// to visualize Data Journey Map 
const renderJourneyFlowChart = function(domSelector, defaultMetricName, journeyStages, journeyNodes, journeyLinks, touchpointHubMap) {
	var configSankey = {
		margin : {
			top : 5,
			left : 70,
			right : 50,
			bottom : 50
		},
		nodes : {
			dynamicSizeFontNode : {
				enabled : true,
				minSize : 12,
				maxSize : 22
			},
			fontSize : 15, // if dynamicSizeFontNode not enabled
			draggableX : true, // default [ false ]
			draggableY : true, // default [ true ]
			colors : d3.scaleOrdinal(d3.schemeCategory20)
		},
		links : {
			formatValue : function(targetName, val, candidateName) {
				// console.log('renderJourneyFlowChart.formatValue ', targetName, candidateName, val);
				// var touchpointHub = touchpointHubMap[candidateName ? candidateName : targetName ];
				//var totalProfile = touchpointHub.totalProfile;
				//console.log(touchpointHub)
				
				//var labelMetrics = '<span style="font-weight:bold; margin:5px; background-color:#FFF8DC; border-radius: 6px;">Total profile: </span>';
				//var valueLabel = '<b>' + d3.format(",.0f")(val) + '</b>';
				//return  labelMetrics + ' ' + valueLabel;
				return "";
			},
			unit : defaultMetricName
		// if not set formatValue function
		},
		tooltip : {
			infoDiv : true, // if false display default tooltip
			labelSource : 'Input:',
			labelTarget : 'Output:'
		}
	}

	//console.log(journeyNodes)
	//console.log(journeyLinks)
	sk.createSankey(domSelector, configSankey, {
		nodes : journeyNodes,
		links : journeyLinks
	});
	$(domSelector + " g.sk-node text").each(function(){ 
		var name = $(this).text().trim(); 
		var touchpointHub = touchpointHubMap[name];
		if(typeof touchpointHub === "object"){
			var totalProfile = touchpointHub.totalProfile.toLocaleString();
			var labelText = name + ' [Profile: '+ totalProfile +' ]'
			var label = '<tspan dy="1em" x="-2" class="tphub_text_label"> [' + touchpointHub.journeyLevel + '] ' + labelText + '</tspan>'; 
			$(this).html(label);
		} else {
			console.error("touchpointHubMap[name] is NULL for name: " + name)
		}
	});
	d3.selectAll("path.sk-link").style("stroke-width", "1px");
	//$(domSelector).css("pointer-events","none");
}

const renderSegmentSize = function(domSelector, data, totalCount, width, height, radius, percentage) {
	var outerR = Math.floor(radius*0.97);
	var arc = d3.arc().outerRadius(outerR).innerRadius(110);

	var pie = d3.pie()
	    .sort(null)
	    .value(function(d) {
	        return d.count;
	    });

	var svg = d3.select(domSelector).append("svg")
	    .attr("width", width)
	    .attr("height", height)
	    .append("g")
	    .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");

    var g = svg.selectAll(".arc")
      .data(pie(data))
      .enter().append("g");    

   	g.append("path").attr("d", arc).style("fill", function(d,i) {
      	return d.data.color;
    });

   	var match = '<tspan y="0" > Segment Size: '+totalCount.toLocaleString()+'</tspan>';
   	var total = '<tspan y="24" >'+percentage+'% of Total Profile </tspan>';
    g.append("text")
	   .attr("text-anchor", "middle")
		 .attr('font-size', '1.1em')
		 .attr('y', 20)
	   .html(match);
    g.append("text")
	   .attr("text-anchor", "middle")
		 .attr('font-size', '1.1em')
		 .attr('y', 20)
	   .html(total);
}

const loadMediaTouchpoints  = function () {
    var sets = [{
        sets: ["Email"],
        figure: 27.92,
        label: "Email",
        size: 27.92
    }, {
        sets: ["Direct Traffic"],
        figure: 55.28,
        label: "Direct Traffic",
        size: 55.28
    }, {
        sets: ["Search Engine"],
        figure: 7.62,
        label: "Search Engine",
        size: 7.62
    }, {
        sets: ["Email", "Direct Traffic"],
        figure: 3.03,
        label: "Email and Direct Traffic",
        size: 3.03
    }, {
        sets: ["Email", "Search Engine"],
        figure: 1.66,
        label: "Email and Search Engine",
        size: 1.66
    }, {
        sets: ["Direct Traffic", "Search Engine"],
        figure: 2.4,
        label: "Direct Traffic and Search Engine",
        size: 2.4
    }, {
        sets: ["Email", "Direct Traffic", "Search Engine"],
        figure: 1.08,
        label: "Email, Direct Traffic, and Search Engine",
        size: 1.08
    }];

    var chart = venn
        .VennDiagram()
        .width(500)
        .height(400);

    var div2 = d3.select("#venn_two").datum(sets).call(chart);
    div2.selectAll("text").style("fill", "white");
    div2.selectAll(".venn-circle path")
        .style("fill-opacity", 0.8)
        .style("stroke-width", 1)
        .style("stroke-opacity", 1)
        .style("stroke", "fff");

    var tooltip = d3
        .select("body")
        .append("div")
        .attr("class", "venntooltip");

    div2.selectAll("g")
        .on("mouseover", function (d, i) {
            // sort all the areas relative to the current item
            venn.sortAreas(div2, d);

            // Display a tooltip with the current size
            tooltip
                .transition()
                .duration(40)
                .style("opacity", 1);
            tooltip.text(d.size + "% media touchpoint " + d.label);

            // highlight the current path
            var selection = d3
                .select(this)
                .transition("tooltip")
                .duration(400);
            selection
                .select("path")
                .style("stroke-width", 3)
                .style("fill-opacity", d.sets.length == 1 ? 0.8 : 0)
                .style("stroke-opacity", 1);
        })

        .on("mousemove", function () {
            tooltip
                .style("left", d3.event.pageX + "px")
                .style("top", d3.event.pageY - 28 + "px");
        })

        .on("mouseout", function (d, i) {
            tooltip
                .transition()
                .duration(2500)
                .style("opacity", 0);
            var selection = d3
                .select(this)
                .transition("tooltip")
                .duration(400);
            selection
                .select("path")
                .style("stroke-width", 3)
                .style("fill-opacity", d.sets.length == 1 ? 0.8 : 0)
                .style("stroke-opacity", 1);
        });
}

const personalityDiagram = function () {
    var width = 300,
        height = 300;

    // Config for the Radar chart
    var config = {
        w: width,
        h: height,
        maxValue: 100,
        levels: 5,
        ExtraWidthX: 300
    }

    var data = [
        [{
            "area": "Openness",
            "value": 80
        }, {
            "area": "Conscientiousness",
            "value": 40
        }, {
            "area": "Extraversion",
            "value": 40
        }, {
            "area": "Agreeableness",
            "value": 90
        }, {
            "area": "Neuroticism",
            "value": 60
        }]
    ]

    // Call function to draw the Radar chart

    var svg = d3.select('chart_persornality')
        .append('svg')
        .attr("width", width)
        .attr("height", height);

    RadarChart.draw("#chart_persornality", data, config);
}

const loadProfileAttribution =  function () {
    // set the dimensions and margins of the graph
    var containerW = Math.round($("#profile_attribution").width())
    var margin = { top: 30, right: 60, bottom: 100, left: 140 },
        width = 600,
        height = 550 - margin.top - margin.bottom;

    // append the svg object to the body of the page
    var svg = d3.select("#profile_attribution")
        .append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
        .attr("transform",
            "translate(" + margin.left + "," + margin.top + ")");

    // Labels of row and columns

    var myGroups = ["Live_in_Saigon", "Visited_Web", "Installed_App", "Used_Credit_Card","Buy_Product"]
    var myVars = ["Buy_Product", "Used_Credit_Card", "Installed_App",  "Visited_Web", "Live_in_Saigon" ]

    // Build X scales and axis:
    var x = d3.scaleBand()
        .range([0, width])
        .domain(myGroups)
        .padding(0.01);
    svg.append("g")
        .attr("transform", "translate(0," + height + ")")
        .attr("class", "x_axis")
        .call(d3.axisBottom(x))
        .selectAll("text")
        .attr("transform", "translate(-10,10)rotate(-30)")
        .style("text-anchor", "end")
        .style("font-size", 15)
        .style("fill", "#E91E0D");

    // Build X scales and axis:
    var y = d3.scaleBand()
        .range([height, 0])
        .domain(myVars)
        .padding(0.01);
    svg.append("g")
        .attr("class", "y_axis")
        .call(d3.axisLeft(y))
        .selectAll("text")
        .style("text-anchor", "end")
        .style("font-size", 15)
        .style("fill", "#463CC4")
        ;

    // Build color scale
    var myColor = d3.scaleLinear()
        .range(["white", "#008748"])
        .domain([1, 100]);
    
    function select_axis_label(datum) {
        return d3.select('.x_axis').selectAll('text').filter(function(x) { return x == datum.letter; });
    }

    // Read the data
    var urlDataSource = "/public/uploaded-files/sample-profile-attribution.csv?_=" + new Date().getTime();
    d3.csv(urlDataSource, function (data) {
    	
        // Three function that change the tooltip when user hover / move / leave
		// a cell

        var onclick = function (d) {
        	var str = " [Profile attribute: " + d.profile_attr + " - Persona attribute: " + d.persona_attr + "] : Matching Score = " + d.matching_score + " %";
            $('#attribution_description').text(str);
            var myNode = svg.select("#cell_"+ d.profile_attr + '_' + d.persona_attr);
        	
            var str = " [Profile attribute: " + d.profile_attr + " - Persona attribute: " + d.persona_attr + "] : Matching Score = " + d.matching_score + " %";
            $('#attribution_description').text(str).effect("highlight", {color:"#5eeb34"}, 6000 );
        }
       
        var mouseover = function (d) {
        	console.log(d)
        	var myNode = svg.select("#cell_"+ d.profile_attr + '_' + d.persona_attr);
        	myNode.style("fill", "#5eeb34");
        	
        	var str = " [Profile attribute: " + d.profile_attr + " - Persona attribute: " + d.persona_attr + "] : Matching Score = " + d.matching_score + " %";
            $('#attribution_description').text(str);
        }
        
        var mouseout = function(d) {
       		var myNode = svg.select("#cell_"+ d.profile_attr + '_' + d.persona_attr);
           	myNode.style("fill", myColor(d.matching_score) );
        }

        // add the squares
        svg.selectAll()
            .data(data, function (d) { return d.profile_attr + ':' + d.persona_attr; })
            .enter()
            .append("rect")
            .attr("id", function (d) { return "cell_"+ d.profile_attr + '_' + d.persona_attr })
            .attr("x", function (d) { return x(d.profile_attr) } )
            .attr("y", function (d) { return y(d.persona_attr) } )
            .attr("width", x.bandwidth())
            .attr("height", y.bandwidth())
            .style("fill", function (d) { return myColor(d.matching_score)} )
            .on("click", onclick)
            .on("mouseover", mouseover)
            .on('mouseout', mouseout)
            ;
    })
}

function loadCustomerVenn() {
    var sets = [{
            sets: ["Email"],
            figure: 27.92,
            label: "Email",
            size: 27.92
        },
        {
            sets: ["Social Media"],
            figure: 55.28,
            label: "Social Media",
            size: 55.28
        },
        {
            sets: ["Search Engine"],
            figure: 7.62,
            label: "Search Engine",
            size: 7.62
        },
        {
            sets: ["Email", "Social Media"],
            figure: 3.03,
            label: "",
            size: 3.03
        },
        {
            sets: ["Email", "Search Engine"],
            figure: 2.66,
            label: "",
            size: 1.66
        },
        {
            sets: ["Social Media", "Search Engine"],
            figure: 2.40,
            label: "",
            size: 2.40
        },
        {
            sets: ["Email", "Social Media", "Search Engine"],
            figure: 1.08,
            label: "",
            size: 1.08
        }
    ];

    var chart = venn.VennDiagram()
        .width($('#customer_source').width())
        .height(360)

    var div2 = d3.select("#customer_source").datum(sets).call(chart);
    div2.selectAll("text").style("fill", "white");
    div2.selectAll(".venn-circle path")
        .style("fill-opacity", .8)
        .style("stroke-width", 1)
        .style("stroke-opacity", 1)
        .style("stroke", "fff");

    var tooltip = d3.select("body").append("div")
        .attr("class", "venntooltip");

    div2.selectAll("g")
    .on("mouseover", function (d, i) {
        // sort all the areas relative to the current item
        venn.sortAreas(div2, d);

        // Display a tooltip with the current size
        tooltip.transition().duration(40).style("opacity", 1);
        tooltip.text(d.size + "% of Segment Two saw " + d.label);

        // highlight the current path
        var selection = d3.select(this).transition("tooltip").duration(400);
        selection.select("path")
            .style("stroke-width", 3)
            .style("fill-opacity", d.sets.length == 1 ? .8 : 0)
            .style("stroke-opacity", 1);
    })

    .on("mousemove", function () {
        tooltip.style("left", (d3.event.pageX) + "px")
            .style("top", (d3.event.pageY - 28) + "px");
    })

    .on("mouseout", function (d, i) {
        tooltip.transition().duration(2500).style("opacity", 0);
        var selection = d3.select(this).transition("tooltip").duration(400);
        selection.select("path")
            .style("stroke-width", 3)
            .style("fill-opacity", d.sets.length == 1 ? .8 : 0)
            .style("stroke-opacity", 1);
    });
}

function loadChannelPerformance() {
    var data = [{
        "channelName": "Ecommerce Website",
        "lead": 181
    }, {
        "channelName": "Ecommerce App",
        "lead": 189
    }, {
        "channelName": "Social Media Groups",
        "lead": 387
    }, {
        "channelName": "Retail Store",
        "lead": 187
    }, {
        "channelName": "Book Reseller",
        "lead": 52
    }];

    // set the dimensions and margins of the graph
    var containerWidth = $('#chart').width();
    var margin = {
            top: 20,
            right: 20,
            bottom: 30,
            left: 180
        },
        width = containerWidth - margin.left - margin.right,
        height = 350 - margin.top - margin.bottom;

    // set the ranges
    var y = d3.scaleBand()
        .range([height, 0])
        .padding(0.1);

    var x = d3.scaleLinear()
        .range([0, width]);

    // append the svg object to the body of the page
    // append a 'group' element to 'svg'
    // moves the 'group' element to the top left margin
    var svg = d3.select("#chart svg").attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
        .attr("transform",
            "translate(" + margin.left + "," + margin.top + ")");;

    // format the data
    data.forEach(function (d) {
        d.lead = +d.lead;
    });

    // Scale the range of the data in the domains
    x.domain([0, d3.max(data, function (d) {
        return d.lead;
    })])
    y.domain(data.map(function (d) {
        return d.channelName;
    }));
    //y.domain([0, d3.max(data, function(d) { return d.lead; })]);

    // append the rectangles for the bar chart
    svg.selectAll(".bar")
        .data(data)
        .enter().append("rect")
        .attr("class", "bar")
        //.attr("x", function(d) { return x(d.lead); })
        .attr("width", function (d) {
            return x(d.lead);
        })
        .attr("y", function (d) {
            return y(d.channelName);
        })
        .attr("height", y.bandwidth());

    // add the x Axis
    svg.append("g")
        .attr("transform", "translate(0," + height + ")")
        .call(d3.axisBottom(x));

    // add the y Axis
    svg.append("g")
        .call(d3.axisLeft(y));
}

var renderTimeseriesChart = function(chartId, jsonData, refresh, timeseriesChartObj, showAllDataset) {
	var timeseriesData = {labels : [], datasets:[]};
	
	// 3.1 set labels 
	var formatDate = 'YYYY-MM-DD';
	var labels = [];
	var rawDateFilter = getRawDateFilterValues();
	var bDate = rawDateFilter.beginFilterDate;
	var eDate = rawDateFilter.endFilterDate;
	while (bDate.isBefore(eDate, 'day')) {
	  labels.push(bDate.format(formatDate));
	  bDate.add(1, 'days');
	}
	//console.log(labels)
	timeseriesData.labels = labels;
	
	// 3.2 set datasets
	var colorIndex = 0;
	var eventNames = Object.keys(jsonData.reportMap);
	
	// find and remove notification 
	$('#' + chartId).show().parent().find('#noti_' + chartId).remove();
	var isNoData = eventNames.length === 0; 
    if(isNoData){
		var noti = '<div id="noti_'+chartId+'" class="list-group-item" > No data available </div>';
		$('#' + chartId).hide().parent().append(noti);
	}
    
	eventNames.sort();
	
	eventNames.forEach(function(eventName) {
		// loop in dataList, set count value into dateKeyToValue
		var dateKeyToValue = {};
		var dataList = jsonData.reportMap[eventName];
		dataList.forEach(function(e){
			var dateKey = e.dateKey;
			var dailyCount = e.dailyCount;
			dateKeyToValue[dateKey] = dateKeyToValue[dateKey] ? dateKeyToValue[dateKey] : 0;
			dateKeyToValue[dateKey] = dateKeyToValue[dateKey] + dailyCount;
		});
		//console.log(dateKeyToValue)
		
		// loop in labels (timeseries) and set count into correct index in date
		var dataStats = []
		timeseriesData.labels.forEach(function(e){
			var count = dateKeyToValue[e] ? dateKeyToValue[e] : 0;
			dataStats.push(count);
		});
		
		// pick a color code
		var colorCode = chartColorCodes[colorIndex];
		colorIndex += 1;
		var showLine = true;
		if(colorIndex >= MAX_TO_LINE_CHART){
			colorIndex = 0;
			showLine = false;
		}
		
		var hideData = (eventName.indexOf("view") >= 0 || eventName.indexOf("click") >= 0 || showAllDataset === true) ? false : true;
		// push dataList 
		var dataUnit = {
   			label: eventName,
   			backgroundColor: colorCode,
   			borderColor: colorCode,
   			fill: false,
   			borderWidth: 1.6,
   			tension:  0.3,
   			showLine: showLine,
   			hidden: hideData,
   			data: dataStats
   		};
		timeseriesData.datasets.push(dataUnit);
	})
	
	if(refresh === true && typeof timeseriesChartObj === "object"){
		timeseriesChartObj.data = timeseriesData;
		timeseriesChartObj.update();
	}
	else {
		// get chart type from DOM
		var chartType = $('#' + chartId).data('chart-type');
		chartType = chartType ? chartType : "line";
		
		var contextNode = document.getElementById(chartId);
		if(contextNode == null){
			return false;
		}
		var ctx = contextNode.getContext('2d');
		var chartJsObj = new Chart(ctx, {
			type: chartType,
			data: timeseriesData,
			options: {
				tooltips: {
					mode: 'index',
					intersect: true
				},
				responsive: true,
				interaction: {
					pointHoverRadius: 20,
					intersect: true,
					mode: 'index'
    			},
				scales: {
					xAxes: {
						stacked: false,
					},
					yAxes: {
						stacked: false
					}
				}
			}
		});
		return chartJsObj;
	}
}

const PluginCenterTextChartJS = function(){
	var plugin = {
		beforeDraw: function(chart, args, options) {
			
			var centerData = chart.data.donutChartCenterData;
			if(typeof centerData === "object") {
				var value = typeof centerData.value === "number" ? centerData.value : 0;
				var marginTop = typeof centerData.marginTop === "number" ? centerData.marginTop : 30;
				var suffixStr = typeof centerData.suffix === "string" ? centerData.suffix : "";
				
			    var width = chart.width,
			        height = chart.height,
			        ctx = chart.ctx;

			    ctx.restore();
			    var fontSize = (height / 114).toFixed(2);
			    ctx.font = fontSize + "em sans-serif";
			    ctx.textBaseline = "middle";
			    var text = value,
			        textX = Math.round((width - ctx.measureText(text).width) / 2),
			        textY = (height / 2) + marginTop;
			    
			    if(typeof suffixStr === "string"){
			    	text = text + suffixStr;
			    	textX -= 10;
			    }
			    if(value >= 0){
			    	ctx.fillStyle = "#1EC900";
			    } else {
			    	ctx.fillStyle = "#E80015";
			    	textX += 4;
			    }
			    
			    ctx.fillText(text, textX, textY);
			    ctx.save();
			}
		}
	}
	return plugin;
}

const renderDonutChartJs = function(textTitle, chartId, data) {
	var ctx = document.getElementById(chartId).getContext('2d');
	var promisedChart = new Chart(ctx, {
	  type: 'doughnut',
	  data: data,
	  options: {
		title: {
        	display: false,
            text: textTitle,
            fontSize: 15
        },
	  	responsive: true,
	  	maintainAspectRatio: true,
	  	showDatapoints: true,
	    legend: {
	      display: true
	    },
	    plugins: {
	    	datalabels: {
	    		color: 'black',
	            formatter: function(value) {
	              return value + '%';
	            },
	            font: {
	                weight: 'bold',
	                size: 15,
	            }
	        }
	    }
	  },
	  plugins: [PluginCenterTextChartJS(), ChartDataLabels]
	});
	
	return promisedChart;
}

const renderStackedBarChart = function(chartId, dataModel) {
	var ctx = document.getElementById(chartId).getContext('2d');
	var config = {
		type: 'bar',
		data: dataModel,
		options: {
			tooltips: { mode: 'index', intersect: false },
			responsive: true,
			interaction: { mode: 'index' },
			scales: {
				xAxes: { stacked: true },
				yAxes: { stacked: true }
			}
		}
	};
	var chartObj = new Chart(ctx, config);
	return chartObj;
};

const showProfileEventBubbleChart = function(titleText, container, canvas, list, dataKey) {
	var maxProfile = 0, maxEvent = 0;
	if(list.length > 0){
		maxProfile = list[0].profileCount + 90;
		maxEvent = list[0].eventCount + 90;
	}
	else {
		maxProfile = 10;
		maxEvent = 10;
	}
	
	var MAX_RADIUS = 28;
	var MIN_RADIUS = 7;
	var PROFILE_STEP_SIZE = 10;
	var EVENT_STEP_SIZE = maxEvent > 10000 ? Math.ceil( maxEvent / 1000) : 10;
    
    // datasets for bubble chart
    var datasets = [];
    
    list.forEach(function(e,i) {
    	var obj = dataKey ? e[dataKey] : e;
    	var pc = e.profileCount;
    	var ec = e.eventCount;
    	
        var radius = (pc / maxProfile) * 50;
        radius = radius > MAX_RADIUS ? MAX_RADIUS : radius;
        radius = radius < MIN_RADIUS ? MIN_RADIUS : radius;
        
        var label = textTruncate( (obj.name == null || obj.name == "") ? "Unknown" : obj.name, 70);
        datasets.push({
            label: label,
           // backgroundColor: "#77C3F7",
            backgroundColor: chartColorCodes[i],
            borderColor: "#107CC5",
            data: [{
                x: ec,
                y: pc,
                r: radius
            }]
        })
    })
    
    //Chart.defaults.scales.linear.min = 0;
    var config = {
        type: 'bubble',
        data: {
            datasets: datasets
        },
        options: {
            plugins: {
                legend: {display: true, position: "bottom", labels: {font: {size: 12}, padding: 12 } },
                title: {text: titleText, display: true, font: { size: 18}, color: "#0000CD"},
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) {
                                label += ' | ';
                            }
                            if (context.parsed.y !== null) {
                                label += ("Profile: "+context.parsed.y ) ;
                            }
                            if (context.parsed.x !== null) {
                                label += (" | Event: "+context.parsed.x ) ;
                            }
                            return label;
                        }
                    }
                }
            },
            responsive: false,
            aspectRatio: 1,
            showDatapoints: false,
            scales: {
            	y: {
            		title: {text: "Profile", display: true, font: { size: 16}, color: "#0000CD", align: "end"},
            		beginAtZero: true,
            		max : maxProfile,
                    ticks: {
                    	stepSize: PROFILE_STEP_SIZE,
                        // Include a dollar sign in the ticks
                        callback: function(value, index, ticks) {
                        	//var label = value > 1 ? " profiles" : " profile";
                            return value;
                        }
                    }
                },
                x: {
                	title: {text: "Event", display: true, font: { size: 16}, color: "#0000CD", align: "end"},
                	beginAtZero: true,
                    ticks: {
                    	stepSize: EVENT_STEP_SIZE,
                        // Include a dollar sign in the ticks
                        callback: function(value, index, ticks) {
                        	//var label = value > 1 ? " events" : " event";
                            return value;
                        }
                    }
                }
            }
        }
    };
    container.empty().append(canvas);
    var ctx = canvas[0].getContext('2d');
    new Chart(ctx, config);
}

const renderHorizontalBarChart = function(containerId, labels, dataList) {
	var data = {
	  labels: labels,
	  datasets: [{
	    axis: 'y',
	    data: dataList,
	    fill: false,
	    backgroundColor: chartColorCodes,
	    borderColor: [ 'rgb(0, 0, 0)'],
	    borderWidth: 1
	  }]
	}
	var maxBarThickness = 22;
	var chartHeight = (maxBarThickness * dataList.length) + 10;
	var config = { data, type: 'bar', options: 
		{ 
			indexAxis: 'y', plugins: { legend: {display: false} },
			maxBarThickness: maxBarThickness,
			responsive: true,
		    maintainAspectRatio: true,
			scales: {
                x: {
                	title: {text: "Event", display: true, font: { size: 16}, color: "#0000CD", align: "end"},
                	beginAtZero: true,
                    ticks: {
                    	stepSize: 5
                    }
                }
            }
		}
	}
	var canvasNode = $('<canvas/>');
	var container = $('#' + containerId);
	container.find("canvas").remove();
	container.html(canvasNode);
	canvasNode.height(chartHeight).css("height", chartHeight + "px")
	var myChart = new Chart(canvasNode[0], config);
	return myChart;
}

const renderVerticalBarChart = function(containerId, labels, dataList) {
  const container = $('#' + containerId);
  container.find("canvas").remove();

  const maxVisibleBars = 20;        // show up to 20 bars before scrolling
  const baseBarThickness = 18;      // ideal bar height
  const padding = 40;               // top & bottom space
  const maxHeight = baseBarThickness * maxVisibleBars + padding;

  // adaptive thickness: thinner bars when too many
  const barThickness = Math.max(8, Math.min(baseBarThickness, 600 / dataList.length));

  // compute real height (limit to maxHeight)
  const chartHeight = Math.min((barThickness * dataList.length) + padding, maxHeight);

  const data = {
    labels: labels,
    datasets: [{
      axis: 'x',
      data: dataList,
      fill: false,
      backgroundColor: chartColorCodes,
      borderColor: ['rgba(0, 0, 0, 0.1)'],
      borderWidth: 1,
      barThickness: barThickness
    }]
  };

  const config = {
    type: 'bar',
    data: data,
    options: {
      indexAxis: 'x',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: { mode: 'nearest', intersect: false }
      },
      scales: {
        x: {
          title: {
            text: "Event",
            display: true,
            font: { size: 14 },
            color: "#0033cc"
          },
          ticks: {
            autoSkip: false,
            maxRotation: 45,
            minRotation: 0
          }
        },
        y: {
          beginAtZero: true,
          ticks: {
            stepSize: Math.ceil(Math.max(...dataList) / 5) || 1
          }
        }
      }
    }
  };

  // add scrollable wrapper
  const scrollWrapper = $('<div/>')
    .css({
      'max-height': maxHeight + 'px',
      'overflow-y': dataList.length > maxVisibleBars ? 'auto' : 'hidden',
      'padding': '10px'
    });

  const canvasNode = $('<canvas/>')
    .css({ height: chartHeight + 'px', width: '100%' });

  scrollWrapper.append(canvasNode);
  container.html(scrollWrapper);

  return new Chart(canvasNode[0], config);
};


const initPanZoomController = function(MIN_ZOOM, MAX_ZOOM, cyObj, rootNodeSelector ){
	var panzoomConfigs = {
	  zoomFactor: 0.02, // zoom factor per zoom tick
	  zoomDelay: 45, // how many ms between zoom ticks
	  minZoom: MIN_ZOOM, // min zoom level
	  maxZoom: MAX_ZOOM, // max zoom level
	  fitPadding: 100, // padding when fitting
	  panSpeed: 2, // how many ms in between pan ticks
	  panDistance: 4, // max pan distance per tick
	  panDragAreaSize: 75, // the length of the pan drag box in which the vector for panning is calculated (bigger = finer control of pan speed and direction)
	  panMinPercentSpeed: 0.25, // the slowest speed we can pan by (as a percent of panSpeed)
	  panInactiveArea: 8, // radius of inactive area in pan drag box
	  panIndicatorMinOpacity: 0.5, // min opacity of pan indicator (the draggable nib); scales from this to 1.0
	  zoomOnly: false, // a minimal version of the ui only with zooming (useful on systems with bad mousewheel resolution)
	  fitSelector: rootNodeSelector, // selector of elements to fit
	  animateOnFit: function(){ // whether to animate on fit
	    return true;
	  },
	  fitAnimationDuration: 1000, // duration of animation on fit

	  // icon class names
	  sliderHandleIcon: 'fa fa-minus',
	  zoomInIcon: 'fa fa-plus',
	  zoomOutIcon: 'fa fa-minus',
	  resetIcon: 'fa fa-expand'
	};

	// add the panzoom control
	cyObj.panzoom(panzoomConfigs);
}

const renderDirectedGraph = function(containerId, graphElements, rootNodeId) {
	const NODES_LENGTH = graphElements.nodes.length;
	//var layoutConfig =  { name: 'dagre', rankDir: "LR", fit: true, directed: true,  padding: 10, spacingFactor: 2, nodeSep: 60 , rankSep: 30 }
	var rootNodeSelector = '#'+rootNodeId;
	var layoutConfig = {directed: true, fit: true, roots: rootNodeSelector, padding: 16};
	if(NODES_LENGTH > 1) {
		// set layout Config
		layoutConfig['name'] = 'avsdf';
		
		var nodeSeparation = NODES_LENGTH > 10 ? NODES_LENGTH : 10;
		if(NODES_LENGTH < 5){
			nodeSeparation *= 20;
		}
		else if(NODES_LENGTH < 10){
			nodeSeparation *= 14;
		}
		else if(NODES_LENGTH < 20){
			nodeSeparation *= 10;
		}
		else if(NODES_LENGTH < 25){
			nodeSeparation *= 8;
		}
		else if(NODES_LENGTH < 50){
			nodeSeparation *= 6;
		}
		else if(NODES_LENGTH < 75){
			nodeSeparation *= 4;
		}
		else if(NODES_LENGTH < 100){
			nodeSeparation *= 2;
		}
		layoutConfig['nodeSeparation'] = nodeSeparation;
	}
	
	var containerNode = document.getElementById(containerId);
	const MIN_ZOOM = 0.3;
	const MAX_ZOOM = 100;
	
	var graphStyle = cytoscape.stylesheet()
	    .selector('node').style({
		  'color': '#000000',
		  'background-color': '#93C4F9',
		  'background-fit': 'cover',
		  'height': 32,
	      'width': 32
	    })
	    .selector('node[weight >= 5]').style({
	    	'background-color': '#EF5B16',
	    	'shape': 'rectangle'
	    })
	    .selector('#'+rootNodeId).style({
	    	'background-color': '#9EFA06',
	    	'background-image': 'https://cdn-icons-png.flaticon.com/128/1927/1927746.png',
	    	'height': 50,
	    	'width': 50,
	    })
	    .selector('edge').style({
		  'label': 'data(weight)',
	  	  'background-color': '#61bffc',  
		  'curve-style': 'bezier',
		  'target-arrow-shape': 'triangle',
		  'width': 3,
		  'line-color': '#61bffc',
		  'target-arrow-color': '#61bffc'
	    })
	    .selector('edge[weight >= 5]').style({
	    	'line-color': '#F99633',
	    	'target-arrow-color': '#F99633',
	    	'width': 5
	    })
	    ;
	var cy = cytoscape({
	  container: containerNode,
	  boxSelectionEnabled: true,
	  autounselectify: true,
	  wheelSensitivity: 0.1,
	  style: graphStyle,
	  elements: graphElements,
	  minZoom: MIN_ZOOM,
	  maxZoom: MAX_ZOOM,
	  layout: layoutConfig
	});
	cy.zoomingEnabled(true);
	cy.userZoomingEnabled(true);
	cy.nodeHtmlLabel([
	  {
	    query: 'node', // cytoscape query selector
	    halign: 'center', // title vertical position. Can be 'left',''center, 'right'
	    valign: 'bottom', // title vertical position. Can be 'top',''center, 'bottom'
	    halignBox: 'center', // title vertical position. Can be 'left',''center, 'right'
	    valignBox: 'bottom', // title relative box vertical position. Can be 'top',''center, 'bottom'
	    cssClass: '', // any classes will be as attribute of <div> container for every title
	    tpl(data) {
	    	var name = data.id.toUpperCase();
	    	return '<b> ' + name + '</b>'; 
	    }
	  }
	]);
	initPanZoomController(MIN_ZOOM, MAX_ZOOM, cy, rootNodeSelector);
	
	
	cy.ready(function() {
	  	setTimeout(function(){
			var c = 10, len = NODES_LENGTH;
		    if (len <= 3) {
		        c = 250;
		    } else if (len <= 9) {
		        c = 180;
		    } else if (len <= 15) {
		        c = 100;
		    } else if (len <= 21) {
		        c = 50;
		    }
			cy.fit(c);
		},680);
	});
	
	return cy;
}

const renderMatrixChart = function(titleText, containerId, xDataLabels, yDataLabels, dataList) {
	console.log(xDataLabels, yDataLabels, dataList)
	
  var h = ( yDataLabels.length * 36) + 150 ;
  $('#' + containerId).css('height',h+'px');

  const container = $('#' + containerId);
  const xLabelSize = xDataLabels.length;
  const yLabelSize = yDataLabels.length;
  let fontSize = 15;

  if (xLabelSize >= 24) fontSize = 15;
  else if (xLabelSize >= 18) fontSize = 17;

  if (xLabelSize === 0 || yLabelSize === 0) {
    container.html('<div class="list-group-item">No data available</div>').removeClass('event_matrix_active');
    return;
  } else {
    container.html('<div class="loader"></div>');
  }

  // --- Dynamic height calculation ---
  // Rough heuristic: each row gets ~35px, plus 120px base padding for title + labels
  const minRowHeight = 35;
  const chartHeight = Math.max(300, yLabelSize * minRowHeight + 120);
  // -----------------------------------

  const canvasNode = $('<canvas/>')
    .attr('height', chartHeight) // <<--- key line
    .css('height', chartHeight + 'px');

  container.find('canvas').remove();
  container.html(canvasNode).addClass('event_matrix_active');

  const data = {
    datasets: [{
      label: '',
      data: dataList,
      backgroundColor(context) {
        const item = context.dataset.data[context.dataIndex];
        let value = typeof item === 'object' ? item.v : 0;
        value = (value > 0 && value < 10) ? 10 : value;
        const alpha = value / 180;
        return chroma('#313866').alpha(alpha).toString();
      },
      borderColor: 'rgb(0, 0, 0)',
      borderWidth: 0,
      width: ({ chart }) => (chart.chartArea || {}).width / xLabelSize - 1,
      height: ({ chart }) => ((chart.chartArea || {}).height + 80) / yLabelSize - 1,
      datalabels: {
        formatter(value, context) {
          const v = context.chart.data.datasets[0].data[context.dataIndex].v;
          return typeof v === 'number' ? v.toLocaleString() : v;
        },
        color: '#FF533F',
        font: { weight: 'bold', size: fontSize }
      }
    }]
  };

  const config = {
    type: 'matrix',
    data,
    options: {
      maintainAspectRatio: false, // <--- crucial for responsive height
      plugins: {
        legend: false,
        title: {
          text: titleText,
          display: true,
          font: { size: 20 },
          color: "#3300ff"
        },
        tooltip: {
          callbacks: {
            title: () => '',
            label(context) {
              const obj = context.dataset.data[context.dataIndex];
              return [`${obj.x} - ${obj.y}`, `Value: ${typeof obj.v === 'number' ? obj.v.toLocaleString() : obj.v}`];
            }
          }
        }
      },
      scales: {
        x: {
          type: 'category',
          labels: xDataLabels,
          ticks: { display: true, font: { size: 13.5 } },
          grid: { display: false }
        },
        y: {
          type: 'category',
          labels: yDataLabels,
          offset: true,
          ticks: { display: true, font: { size: 12.5 } },
          grid: { display: false }
        }
      }
    },
    plugins: [ChartDataLabels]
  };

  return new Chart(canvasNode[0], config);
};
