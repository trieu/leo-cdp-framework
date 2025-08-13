// main dashboard UI javascript

var mainDashboardFunnelHeight = 420;
var mainDashboardWidth = 860;
var dashboardFontSize = 14;
var flowNetworkGraph = false;
var dashboardDataModel = false;

//select date
var currentJourneyMapId = ""; 

var initPrimaryDashboard = function () {
	
	window.mainDashboardFunnelHeight = 420;
	window.mainDashboardWidth = 860;
	window.dashboardFontSize = 14;
	window.flowNetworkGraph = false;
	window.dashboardDataModel = false;
	window.currentJourneyMapId = ""; 
	
	// journeyMapList
	loadJourneyMapList(false, function(id){
		currentJourneyMapId = id;
		showDashboardReport(true);
    },true);
    	
	// init tabs
	setupTabPanels();
    	
	updateProfileChartWrapper();		
	//initDateFilterComponent(false);
	initDateFilterComponent(false, null, null, 45);
 
	showDashboardReport(true);
    	
	//loadEventStatistics()
    
    //loadChannelPerformance();
  	//loadCustomerVenn();
}

var updateProfileChartWrapper = function() {
	var container = $('#profile_funnel_wrapper');
	var ws = $(window).width();
	if(ws > 1500 && ws < 1650 ){
		container.css("width","90%");
	}
	else if(ws > 1650 && ws < 1750 ){
		container.css("width","96%");
	}
	else if(ws > 1750 ){
		container.css("width","98%");
	}
	
	var wh = $(window).height();
	var ratio = ws / wh;
	
	if(ratio >= 1.7 && ratio < 2.3 && ws > 1400 && wh > 700){
		mainDashboardFunnelHeight = wh - (Math.floor(window.innerHeight/3) + 186);
		dashboardFontSize = 15;
	}
	else if(ratio >= 2.3 && ws > 1400 && wh > 700){
		mainDashboardFunnelHeight = wh - (Math.floor(window.innerHeight/3) + 116);
		dashboardFontSize = 16;
	}
	
	mainDashboardWidth = window.pageWrapperWidth > 0 ? (window.pageWrapperWidth * 0.7) : mainDashboardWidth;
}
	
var showDashboardReport = function(refresh) {
	// get date filter
	var queryFilter = getDateFilterValues();
	// format beginReportDate
	var beginReportDate = queryFilter.beginFilterDate;
	$('#beginReportDate').text(new moment(beginReportDate).format(defaultDateFormat))
	// format endReportDate
	var endReportDate = queryFilter.endFilterDate;
	$('#endReportDate').text(new moment(endReportDate).format(defaultDateFormat))
    // funnel configs
	var dataFunnelOptions = {
   		block: {
   			highlight: true,
   	   		fill: {
                type: 'gradient'
            }
   	    },
        chart: {
         	curve: { enabled: true, height: 33 },
            height: mainDashboardFunnelHeight,
            bottomWidth : 0.64,
            bottomPinch: 2,
            width: mainDashboardFunnelHeight * 4/3
         },
         tooltip: {
             enabled: true
         },
         label: {
        	fontSize: dashboardFontSize+'px'
         },
         events: {
             click: {
                 block: function (data) {
                     console.log(data.label.raw);
                 }
             }
         }
    };
    var visualizeFunnel = function (domSelector, list) {
        var data = [];
  	    for(var i=0; i < list.length; i++){
  	    	var e = list[i];
  	    	var value = new Number(e.collectorCount).toLocaleString();
  	    	var colorCode = getColorCodeProfileFunnel(i+1);
  			data.push([e.collectorKey, value, colorCode ]);
  		}
        var chart = new D3Funnel(domSelector);
        chart.draw(data, dataFunnelOptions);
    };
	
    
	var updateChartDashboard = function(){
	    // profile funnel
        var profileFunnelData = dashboardDataModel.profileFunnelData;
        visualizeFunnel('#profile_funnel', profileFunnelData);
		showProfileTotalStatistics();
		showTouchpointItemReport();
		setTimeout(function(){
			showTouchpointHubReport();
		},3000)
	}
    
	if(dashboardDataModel === false || refresh === true) {
		$("#primary_dashboard_loader").show();
    	$("#primary_dashboard").hide();
    	
    	// set journey map ID and funnel report
    	queryFilter['journeyMapId'] = currentJourneyMapId;
    	var urlStr = baseLeoAdminUrl + '/cdp/analytics360/dashboard-primary';
        LeoAdminApiUtil.getSecuredData(urlStr, queryFilter , function (json) {
        	$("#primary_dashboard_loader").hide();
        	$("#primary_dashboard").show();
        	
            if (json.httpCode === 0 && json.errorMessage === '') {
            	dashboardDataModel = json.data;
            	updateChartDashboard();
            } else {
                LeoAdminApiUtil.logErrorPayload(json);
            }
        });  
	}
	else {
		updateChartDashboard();
	}
}
    
function showTouchpointHubReport() {
	var containerSelector = '#observer_report_wrapper';
	var queryFilter = getDateFilterValues();
	queryFilter['journeyMapId'] = currentJourneyMapId;
		
	var textTitle = "Touchpoint Hub Report";
	var container = $(containerSelector);
	container.html('<div class="loader"></div>');
    
    // touchpoint detail report
    queryFilter['touchpointType'] = -1;
    queryFilter['startIndex'] = 0;
    queryFilter['numberResult'] = 30;
    
    var urlStr = baseLeoAdminUrl + '/cdp/analytics360/touchpoint-hub-report';
    LeoAdminApiUtil.getSecuredData(urlStr, queryFilter , function (json) {
		if ( typeof json.data === "object" && json.errorMessage === "") {
			
       	   	var w = mainDashboardWidth;
       	   	var h = 620;
       	   	var canvas = $('<canvas/>').width(w).height(h);
       	   	showProfileEventBubbleChart(textTitle, container, canvas, json.data);
       	   	
       	   	var fieldsSchema = [
	        	{ name: "name", title : "Touchpoint Hub", align: 'center' , type: "text" },			        
	        	{ name: "profileCount", title : "Total Profile", type: "number", align: 'center', width: 60 },
	        	{ name: "eventCount", title : "Total Event", type: "number", align: 'center', width: 60 }
	    	];
       	 	$("#observer_report_table").jsGrid({ editing: false, paging: false, width: "100%", height:"auto", data: json.data, fields: fieldsSchema})
         } else {
             LeoAdminApiUtil.logErrorPayload(json);
         }
    });
}
    
function showTouchpointItemReport(){
	var containerSelector = '#touchpoint_report_wrapper';
	
	var queryFilter = getDateFilterValues();
	queryFilter['journeyMapId'] = currentJourneyMapId;
	
	var textTitle = "Touchpoint Item Report";
	var container = $(containerSelector);
   	container.html('<div class="loader"></div>');
    
    // touchpoint detail report
    queryFilter['touchpointType'] = -1;
    queryFilter['startIndex'] = 0;
    queryFilter['numberResult'] = 30;

    var urlStr = baseLeoAdminUrl + '/cdp/analytics360/touchpoint-report';
    LeoAdminApiUtil.getSecuredData(urlStr, queryFilter , function (json) {
    	if ( typeof json.data === "object" && json.errorMessage === "") {
        	var w = mainDashboardWidth;
        	var h = 720;
      	   	var canvas = $('<canvas/>').width(w).height(h);
      	    showProfileEventBubbleChart(textTitle, container, canvas, json.data, "touchpoint");
        	
        	var fieldsSchema = [
		        { name: "touchpoint.name", title : "Touchpoint Item", align: 'center' , type: "text" },			        
		        { name: "profileCount", title : "Total Profile", type: "number", align: 'center', width: 60 },
		        { name: "eventCount", title : "Total Event", type: "number", align: 'center', width: 60 }
		    ];
		    var dataModel = json.data.map(function(e) {
				var url = e.touchpoint.url.trim();
				if(url.indexOf('http')===0){
					var s = '<a target="_blank" href="' + url + '" title="'+url+'">' + e.touchpoint.name + "<br>" + textTruncate(url, 55) + '</a>';
					e.touchpoint.name = s;
				}
			    return e;
			});
      	    $("#touchpoint_report_table").jsGrid({ editing: false, paging: false, width: "100%", height:"auto", data: dataModel, fields: fieldsSchema})
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}

function showProfileTotalStatistics() {
	$("#profile_summary_stats_loader").show();
    $("#profile_summary_stats").hide();
	
	var urlStr = baseLeoAdminUrl + '/cdp/analytics360/profile-total-stats';
	LeoAdminApiUtil.getSecuredData(urlStr, {} , function (json) {
    	$("#profile_summary_stats_loader").hide();
    	$("#profile_summary_stats").show();
    	
        if (json.httpCode === 0 && json.errorMessage === '') {
        	var profileTotalStats = json.data;
        	
			// empty 4 data boxes 
			$('#profile_total_stats').empty();
			$('#marketing_metrics_holder').empty();
			$('#sales_metrics_holder').empty();
			$('#cx_metrics_holder').empty();
			
			for(var i=0; i < profileTotalStats.length; i++){
				var obj = profileTotalStats[i];
				var label = obj.collectorKey;
				var value = obj.collectorCount.toLocaleString('en-US');
				var flowType = obj.flowType;
				
				if(flowType === 0){
					$('#profile_total_stats').append('<div class="stats_item" > '+label+' : '+value+' </div>');
				}
				else if(flowType === 1) {
					$('#marketing_metrics_holder').append('<div class="stats_item" > '+label+' : '+value+' </div>');
				}
				else if(flowType === 2) {
					$('#sales_metrics_holder').append('<div class="stats_item" > '+label+' : '+value+' </div>');
				}
				else if(flowType === 3) {
					$('#cx_metrics_holder').append('<div class="stats_item" > '+label+' : '+value+' </div>');
				}
			}
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    }); 
}


function reportJourneyOfAllProfiles() {
	// profile journey
    $('#profile_journey_funnel').empty();
	var journeyStatsData = dashboardDataModel.journeyStatsData;
    reportJourneyOfAllProfiles('#profile_journey_funnel', journeyStatsData);
	
	if(journeyStatsData.length === 0) return; // end
	var dataModel = {
        labels: journeyLabel5A,
        subLabels: ['VISITOR PROFILE', 'CONTACT PROFILE'],
        colors: [            
            ['#D6EAF8', '#85C1E9', '#3498DB'],
            ['#B7F0B7', '#8CF08C', '#5CF45C']
        ],
        values: journeyStatsData
    };

	var containerWidth = $(containerId).parent().width();
	var graph = new FunnelGraph({
        container: containerId,
        gradientDirection: 'horizontal',
        data: dataModel,
        displayPercent: true,
        direction: 'horizontal',
        width: containerWidth,
        height: 350,
        subLabelValue: 'percent'
    });
	graph.draw();
};
	