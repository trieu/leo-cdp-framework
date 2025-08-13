
// segment details UI

const refreshSegmentDetails = function(node, fullDataRefresh){
	if(segmentDataModel){
		$(node).attr('disabled','disabled')
		
		var title = 'Refresh data of segment';
		var name = segmentDataModel.name;
		var segmentId = segmentDataModel.id;
		
		iziToast.info({
			timeout: 3000, icon: 'fa fa-check-square-o', 
    	    title: title,
    	    message: 'The job is started'
    	});
		
		var urlStr = baseLeoAdminUrl + '/cdp/segment/refresh';
		var action = "#calljs-leoCdpRouter('Segment_Details','"+segmentId+"','_refresh_"+new Date().getTime()+"')";
		
		if(fullDataRefresh === true) {
			 LeoAdminApiUtil.callPostAdminApi(urlStr, {'segmentId':segmentId} , function (json) {
				$(node).removeAttr('disabled')
		        iziToast.success({
					timeout: 5000, icon: 'fa fa-check-square-o', 
		    	    title: title,
		    	    message: 'New data <b>' + name + '</b> is ready',
		    	    onClosing: function(){
						location.hash  = action;	
		    	    }
		    	});
		    });	
		}
		else {
			location.hash  = action;
		}
	   
	}
}

//
const refreshAllSegments = function() {
	// call and show statistics
	var urlStr = baseLeoAdminUrl + '/cdp/segments/refresh';
    LeoAdminApiUtil.callPostAdminApi(urlStr, {} , function (json) {
        iziToast.success({
			timeout: 3000, icon: 'fa fa-check-square-o', 
    	    title: 'Segment Data Refresh',
    	    message: "Total segment: <b>" + json.data + "</b>",
    	    onClosing: function(instance, toast, closedBy){
    	    	var t = new Date().getTime();
				location.hash = "#calljs-leoCdpRouter('Segment_Management','_refresh_"+t+"')"
    	    }
    	});
    });	
} 



const loadSegmentData = function(id) {
	// Date Range Filtering Controller
	var urlStr = baseLeoAdminUrl + '/cdp/segment/load';
	
	var callback = function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
    		// only super-admin role can remove the segment 
    		var behavioralEventMap = json.data.behavioralEventMap;
    		var assetGroups = json.data.assetGroups;
    		var touchpointHubs = json.data.touchpointHubs;

			// set model    		
    		segmentDataModel = json.data.segmentData;
    		segmentDataStats = json.data.segmentStats;

            var managedBySystem = segmentDataModel.managedBySystem;
            var jsonQueryRules = JSON.parse(segmentDataModel.jsonQueryRules);
			
       	 	if(managedBySystem){
            	$('button.data-control-delete').attr('disabled','disabled');
            	$('button.data-control-delete').attr('title','Can not delete this segment, which is managed by system');
            }
            else {
            	if( json.canDeleteData){
        			$('button.data-control-delete').click(deleteSegment);
    			} else {
    				$('button.data-control-delete').attr('disabled','disabled');
    			}
            }
    		
    		if( json.canEditData ){
    			$('button.data-control-edit').click(function(){
    				location.hash = "calljs-leoCdpRouter('Segment_Builder','" + id + "')";
    			});
    		} else {
    			$('button.data-control-edit').attr('disabled','disabled');
    		}
    		// init UI after setting data into DOM 
    		
         	// show UI
 			$("#segment_details_loader").hide();
            $("#segment_details_div").show();
           
    		// 
            setupSegmentDataAndReports(segmentDataModel)
            
            //
            initSegmentActivationButtons(segmentDataModel)

			handleSegmentCustomQueryFilter(true)
            
            // Segment Query Builder
            loadSegmentBuilder(touchpointHubs, behavioralEventMap, assetGroups, jsonQueryRules, function(){
            	
        		$('#profilelist_holder').show();

				// load profile list table
                loadProfilesInSegment(false, 3000);

				setTimeout(function(){
					// disable and hide all controllers in segment builder 
	        		var node = $('#segment-builder-holder');
	        		node.find('input,select').attr('disabled','disabled');
	        		node.find('button').hide();
					node.find('.select2-container').css('width','auto')
				},2000)
            });

			
            // Activation Plan
            loadActivationRules(segmentDataModel.activationRules); 
            
            document.title = 'Segment Details - ' + segmentDataModel.name;
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    }
	
    LeoAdminApiUtil.callPostAdminApi(urlStr, { 'id': id }, callback);		
}
 
const createSegmentJsonDataConnection = function(){
	var buttonSelector =  "#btn_segment_json_url";
	var generatingUrl = function(){
		iziToast.info({ timeout : 3000, message: 'The system is processing data for the segment "'+segmentDataModel.name +'"' });
		
		var paramObj = { 'segmentId': segmentDataModel.id };
        var url = window.baseLeoAdminUrl + '/cdp/segment/export-json';
        LeoAdminApiUtil.getSecuredData(url, paramObj, function (json) {
            if (json.httpCode === 0 && json.errorMessage === '' && json.data) {
               	var securedJsonUrl = window.baseLeoAdminUrl + json.data ;
               	$('#segment_json_url').val(securedJsonUrl);
               	$(buttonSelector).data("url",securedJsonUrl);
        		
               	setTimeout(function(){  $(buttonSelector).data("init-done",true).trigger('click') }, 800);
            }
        });
	}
	
	if(mapClipboardJS[buttonSelector] == null){
		mapClipboardJS[buttonSelector] = true;
		
		generatingUrl();
		new ClipboardJS(buttonSelector, {
 		    text: function(trigger) {
 		    	if( $(buttonSelector).data("init-done") ) {
 		    		notifySuccessMessage("Successfully copied URL into clipboard!");
 		    	}
 		        return $(buttonSelector).data("url");
 		    }
 		})
 	}
}

const getSegmentExportType = function(){
	var exportType = $('#segment_export_types').find('input[name="segment_export_type"]:checked').val();
	return exportType;
}

const createSegmentCsvDataConnection = function(){
	var buttonSelector =  "#btn_segment_csv_url";
	var generatingUrl = function(){
		iziToast.info({ timeout : 2000, message: 'The system is processing data for the segment "'+segmentDataModel.name +'"' });
		
		var paramObj = { 'segmentId': segmentDataModel.id, 'csvType': 0, 'exportType': getSegmentExportType() };
        var url = window.baseLeoAdminUrl + '/cdp/segment/export-csv';
        LeoAdminApiUtil.getSecuredData(url, paramObj, function (json) {
            if (json.httpCode === 0 && json.errorMessage === '' && json.data) {
				var rs = json.data;
				if(rs === 'in_queue'){
					notifySuccessMessage("The segment is large. Please wait 5 minutes");
				}
				else {
					var securedJsonUrl = window.baseLeoAdminUrl + rs ;
	               	$('#segment_csv_url').val(securedJsonUrl);
	               	$(buttonSelector).data("url",securedJsonUrl);
	               	setTimeout(function(){  $(buttonSelector).data("init-done",true).trigger('click') }, 800);
				}
            }
        });
	}
	
	if(mapClipboardJS[buttonSelector] == null){
		mapClipboardJS[buttonSelector] = true;
		
		generatingUrl();
		new ClipboardJS(buttonSelector, {
 		    text: function(trigger) {
 		    	if( $(buttonSelector).data("init-done") ) {
 		    		notifySuccessMessage("Successfully copied URL into clipboard!");
 		    	}
 		        return $(buttonSelector).data("url");
 		    }
 		})
 	}
}

const getAllSegmentRefs = function(callback){
	if(typeof callback !== 'function'){
		return false;
	}
	var url = window.baseLeoAdminUrl + '/cdp/segments/all-refs';
    LeoAdminApiUtil.getSecuredData(url, {}, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '' && json.data) {
           	callback(json.data)
        }
		else {
			callback(false)
		}
    });
	return true;
}

const getSegmentDownloadUrl = function(node, dataFor){
	var buttonSelector = "#"+$(node).attr('id');
	var csvType = 0; var aSel = "";
	var segmentId = segmentDataModel.id;
	var segmentName = segmentDataModel.name;

	if(dataFor === "txt_exported_excel_csv") {
		csvType = 0;
		aSel = "#ahref_exported_excel_csv";
	}
	else if(dataFor === "txt_exported_facebook_csv") {
		csvType = 1;
		aSel = "#ahref_exported_facebook_csv";
	}
	
	exportSegmentDataConfirmation(buttonSelector, aSel, segmentId, segmentName, csvType, dataFor);
}

var exportSegmentDataConfirmation = function(buttonSelector, aSel, segmentId, segmentName, csvType, dataFor) {
	if( $('#' + dataFor).val().length > 0){
		$('#confirm_action_callback').val('');
		$('#actionInfoMsg').text('Do you want to export and update the download link ?');
		
		$('#confirmActionDialog').modal({ focus: true });
		var callback = "okToExportSegmentData" + segmentId + csvType;
	    window[callback] = function () {
	         okToExportSegmentData(buttonSelector, aSel, segmentId, segmentName, csvType, dataFor)
	    }
	    $('#confirm_action_callback').val(callback);
	}
	else {
		okToExportSegmentData(buttonSelector, aSel, segmentId, segmentName, csvType, dataFor)
	}
}

var okToExportSegmentData = function(buttonSelector, aSel, segmentId, segmentName, csvType, dataFor){
		
	var paramObj = { 'segmentId': segmentId, 'csvType': csvType, 'dataFor' : dataFor, 'exportType': getSegmentExportType() };
    var url = window.baseLeoAdminUrl + '/cdp/segment/export-csv';
    
	iziToast.info({ timeout : 3000, message: 'The system is processing data for the segment <b>'+ segmentName +'</b>' });
    LeoAdminApiUtil.getSecuredData(url, paramObj, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '' && json.data) {
		   var rs = json.data;
		   if(rs === 'in_queue'){
				notifySuccessMessage("The data export is large. We'll notify you when it's finished.");
		   }
		   else {
				var url = window.baseLeoAdminUrl + json.data;
           		$('#' + dataFor).val(url);

       	   		$(buttonSelector).data("url",url);
       	   		$(aSel).attr("href",url).show();
       	   		notifySuccessMessage("Successfully exporting data");
		   }
        }
    });
	
}

window.segmentEventChart = false;
const loadSegmentEventReport = function(refresh){
	// Segment Statistics
	loadSegmentStatistics(segmentDataStats);

	var formatDate = 'YYYY-MM-DD HH:mm:ss';
	var params = getDateFilterValues();
	params["segmentId"] = segmentDataModel.id;
	
	var beginReportDate = params.beginFilterDate;
	$('#eventDataFromDate').text(new moment(beginReportDate).format(formatDate))
	
	var endReportDate = params.endFilterDate;
	$('#eventDataToDate').text(new moment(endReportDate).format(formatDate))
    
	var urlStr = baseLeoAdminUrl + '/cdp/analytics360/event-report/segment';
	var chartId = 'segmentEventChart';
	$("#segment_report_loader").show();
	$("#"+chartId).css("visibility","hidden");
	
	LeoAdminApiUtil.getSecuredData(urlStr, params , function (json) {
		$("#segment_report_loader").hide();
		$("#"+chartId).css("visibility","visible");
        if (json.httpCode === 0 && json.errorMessage === '') {
    		// 3) render chart data
    		
    		if(refresh === true && segmentEventChart !== false){
    			renderTimeseriesChart(chartId, json.data , true, segmentEventChart, true);
    		}
    		else {
    			window.segmentEventChart = renderTimeseriesChart(chartId, json.data , false, null, true);
    		}		   		
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}

//
const loadSegmentStatistics = function(segmentStats) {
	var domSelector = '#segment_size_chart';
	var width = $(domSelector).empty().width(), height = width, radius = width/2;
	var totalProfilesInSystem = segmentStats.totalProfilesInSystem;
	
	if(totalProfilesInSystem == 0){
		$(domSelector).html('<br> No data available! ')
		return;
	}
	if(segmentStats === false || segmentStats == null) {
		var data = [ {
			count : 0,
			color : '#000000'
		}, {
			count : 100, 
			color : '#f8b70a'
		}, ];
		var totalCount = 0; // total manually
		var percentNum = 0;
		renderSegmentSize(domSelector, data, totalCount, width, height, radius, percentNum)
	} 
	else {
		var remain = totalProfilesInSystem - segmentStats.totalProfilesInSegment;
		var data = [ {
			count : segmentStats.totalProfilesInSegment,
			color : '#000000'
		}, {
			count : remain ,
			color : '#f8b70a'
		}, ];
		
		var totalCount = segmentStats.totalProfilesInSegment; // total profile in segment
		var ratio =  (segmentStats.totalProfilesInSegment / totalProfilesInSystem).toFixed(2);
		var percentNum = Math.floor(ratio * 100);
		renderSegmentSize(domSelector, data, totalCount, width, height, radius, percentNum)
	}
}

const initSegmentActivationButtons = function(segmentData) {
	var segmentId = segmentData.id;
	var name = segmentData.name;
	var size = segmentData.totalCount;
	
	$('button[data-purpose]').click(function(){
		var purpose = $(this).data("purpose");
		if(purpose === "forPersonalization"){
			showDataPersonalizationDialog(segmentId, name, size)
		}
		else if(purpose === "forDataEnrichment"){
			showDataEnrichmentDialog(segmentId, name, size)
		}
		else if(purpose === "forSynchronization"){
			showDataSynchronizationDialog(segmentId, name, size)
		}
		else if(purpose === "forAutomatedCampaigns"){
			showAutomatedCampaignsDialog(segmentId, name, size)
		}
	})
}

const getHtmlForAuthorizedUserLogin = function(users) {
	var html = '';
	users.forEach(function(e){
		if(currentUserProfile.role === 6) {
			var callJsViewStr = "#calljs-leoCdpRouter('User_Login_Report','" + e + "')";
		    html += ('<a title="User Login Report" href="' + callJsViewStr + '" >'+e+'</a>, ');
		}
		else {
			html += (e + ', ');
		}
	})
   	return html;
}

const setCsvExportingDataUrl = function(dataFor, url, exportedDate){
	if(url !== ''){
		var bid = $('#' + dataFor).val(url).data('for-button');
		$('#' + bid).attr("href",url).show();
		$('#exported_datetime_' + dataFor).text(toLocalDateTime(exportedDate)).parent().show();
	}
}

const setupSegmentDataAndReports = function(segmentData) {
	var name = segmentData.name;
	var size = segmentData.totalCount;
	var sizeStr = size.toLocaleString();
	var hasPermission = currentUserProfile.role >= 4;
	
	$('#sm_indexScore').text(segmentData.indexScore);
	$('#sm_name').text(name);
	$('b.segment_size').text(sizeStr);
	 
    $('#sm_description').text(segmentData.description);
    $('#sm_authorizedViewers').html(getHtmlForAuthorizedUserLogin(segmentData.authorizedViewers));
    $('#sm_authorizedEditors').html(getHtmlForAuthorizedUserLogin(segmentData.authorizedEditors));
	if(segmentData.realtimeQuery === true){
		$('#sm_realtimeQuery').show();
	}

	// CSV Export for Excel / Gooole Sheets
	setCsvExportingDataUrl('txt_exported_excel_csv', segmentData.exportedFileUrlCsvForExcel, segmentData.exportedDateForExcel);
	
	// CSV Export for Facebook/Google Ads 
	setCsvExportingDataUrl('txt_exported_facebook_csv', segmentData.exportedFileUrlCsvForAds, segmentData.exportedDateForAds);
     
     var personalizationNode = $('#segment_data_personalization');
     if(segmentData.forPersonalization && hasPermission && window.defaultPersonalizationService.length > 0){
    	 personalizationNode.show();
    	 setTimeout(loadPersonalizationTabForSegment, 1000)
     } else {
    	 personalizationNode.hide()
     }
     
     var enrichNode = $('#segment_data_enrichment');
     if( (segmentData.forDeepAnalytics || segmentData.forPredictiveAnalytics) && hasPermission ){
    	 enrichNode.show();    
     } else {
    	 enrichNode.hide()
     }

	 var synNode = $('#segment_data_synchronization');
     if( (segmentData.for3rdSynchronization || segmentData.forEmailMarketing ) && hasPermission ){
    	 synNode.show();    
     } else {
    	 synNode.hide()
     }
}

function handleSegmentPersonalizationTabs(){
	$('#personalizationPlan li.active a').click();
}

const loadPersonalizationTabForSegment = function() {
	
	var initAssetGroupSelection = function(domSelector,list){
		var pgSelector = $(domSelector);
		list.forEach(function(group,i){ 
			var id = group.id;
			var option = '<option value="'+id+'" >' + group.title + '</option>';
			pgSelector.append(option);
		})
		// set the first
		pgSelector.chosen({width: "100%", no_results_text: "Oops, nothing found!"}).trigger("chosen:updated");
	} 
	
	//  CONTENT_HUB = 1, PRODUCT_ITEM_CATALOG = 2, SHORT_URL_LINK = 15
	var paramObj = { 'assetTypes': "1,2,15,16" };
	var urlStr = window.baseLeoAdminUrl + "/cdp/asset-group/list-by-asset-types";
	
	LeoAdminApiUtil.getSecuredData(urlStr, paramObj, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
        	var map = json.data;        
        	// clear as new container
        	$("#productGroupSelector option").remove();	
        	$("#contentGroupSelector option").remove();	
			for (var assetType in map) {
				var list = map[assetType];			
				if( parseInt(assetType) === 2){		
					// PRODUCT_ITEM_CATALOG = 2			
					initAssetGroupSelection('#productGroupSelector', list)
				}	
				else {
					initAssetGroupSelection('#contentGroupSelector', list)
				}
			}
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}


const initPersonalizationInSegment = function(profileId){
	if(window.segmentFirstProfileId === false){
		window.segmentFirstProfileId = profileId;
		loadRecommendedContentsOfProfile(segmentFirstProfileId, false, function(len){
    		$('#max_recommended_contents').html(len)
    	});
		loadRecommendedProductsOfProfile(segmentFirstProfileId, false, function(len){
    		$('#max_recommended_products').html(len)
    	});
	}
}

// the handler for set content items for all profiles in segment
const setContentPersonalizationInSegment = function() {
	var assetGroupId = $('#contentGroupSelector').val();
		
	if(assetGroupId && assetGroupId !== "" && segmentDataModel){
		iziToast.info({ title: 'Information Box', color : 'green', message: 'Starting content personalization...'});
	
		var segmentId = segmentDataModel.id;    
	    var urlStr = baseLeoAdminUrl + '/cdp/segment/set-recommended-items';
	    var params = { "assetGroupId": assetGroupId, "segmentId" : segmentId, "isProductGroup": false };
	    
	    LeoAdminApiUtil.callPostAdminApi(urlStr, params, function (json) {
	         if (json.httpCode === 0 && json.errorMessage === '') {	        	
	        	var msg = 'Setting recommended contents for customer personalization is successful !';
	        	iziToast.info({ title: 'Information Box', color : 'green',  message: msg  });
	        	if(segmentFirstProfileId !== false ){
	        		// select the first profile of this segment and load recommended items
	        		loadRecommendedContentsOfProfile(segmentFirstProfileId, false, function(len){
	 		    		$('#max_recommended_contents').html(len)
	 		    	});
	 		    }
	         }
	    });
	}
}

//the handler for set product items for all profiles in segment
const setProductPersonalizationInSegment = function() {
	var assetGroupId = $('#productGroupSelector').val();
		
	if(assetGroupId && assetGroupId !== "" && segmentDataModel){
		iziToast.info({  title: 'Information Box', color : 'green', message: 'Starting product personalization...'});
	
		var segmentId = segmentDataModel.id;    
	    var urlStr = baseLeoAdminUrl + '/cdp/segment/set-recommended-items';
	    var params = { "assetGroupId": assetGroupId, "segmentId" : segmentId , "isProductGroup": true};
	    
	    LeoAdminApiUtil.callPostAdminApi(urlStr, params, function (json) {
	         if (json.httpCode === 0 && json.errorMessage === '') {
	        	if(segmentFirstProfileId !== false ) {
	        		// select the first profile of this segment and load recommended items
	        		loadRecommendedProductsOfProfile(segmentFirstProfileId, false, function(len){
	 		    		$('#max_recommended_products').html(len)
	 		    	});
	 		    }	 		    
	 		    var msg = 'Setting recommended products for customer personalization is successful !';
	        	iziToast.info({ title: 'Information Box', color : 'green',  message: msg  });	 		    
	         }
	    });
	}
}


const removeRecommendedItemsInSegment = function(isProductGroup, assetGroupId, callback){
	if(assetGroupId && assetGroupId !== "" && segmentDataModel){
		iziToast.info({  title: 'Information Box', color : 'yellow', message: 'Starting to remove recommended items'});
	
		var segmentId = segmentDataModel.id;    
	    var urlStr = baseLeoAdminUrl + '/cdp/segment/remove-recommended-items';
	    var params = { "assetGroupId": assetGroupId, "segmentId" : segmentId, "isProductGroup": isProductGroup };
	    LeoAdminApiUtil.callPostAdminApi(urlStr, params, function (json) {
	         if (json.httpCode === 0 && json.errorMessage === '') {
	        	iziToast.info({title:'Information Box', color : 'green', message: 'Successfully remove recommended items in the segment'});
	        	callback();
	         }
	    });
	}
}

const removeRecommendedContentsInSegment= function(removeAll){
	var selectedGroupId = removeAll === true ? 'remove_all' : $('#contentGroupSelector').val();	 
	removeRecommendedItemsInSegment(false, selectedGroupId, function(){
		if(window.segmentFirstProfileId !== false){
			loadRecommendedContentsOfProfile(window.segmentFirstProfileId, false, function(len){
	    		$('#max_recommended_contents').html(len);
	    	});
		}
	})
}

const removeRecommendedProductsInSegment= function(removeAll){
	var selectedGroupId = removeAll === true ? 'remove_all' : $('#productGroupSelector').val();	
	removeRecommendedItemsInSegment(true, selectedGroupId, function(){
		if(window.segmentFirstProfileId !== false){
			loadRecommendedProductsOfProfile(window.segmentFirstProfileId, false, function(len){
	    		$('#max_recommended_products').html(len);
	    	});
		}
	})
}

// Data Service for Segment Data Activation

const loadDataServiceSelection = function(segmentId, segmentName){
	var okButton = $('#btnOkActivateSegment');
	okButton.data("segmentId",segmentId).data("segmentName",segmentName).hide();
	$('#dataServiceSelector_loader').show();
	$('#dataServiceSelector_holder').hide();
	
	var listNode = $('#dataServiceSelector');
	listNode.find('option').remove().trigger("chosen:updated");
	
	var forSynchronization = listNode.data('forSynchronization') === "true";
	var forDataEnrichment = listNode.data('forDataEnrichment') === "true";
	var forPersonalization = listNode.data('forPersonalization') === "true";
	var startIndex = 0, numberResult = 10000;
	
	var callback = function(dataList){
		okButton.show()
		$('#dataServiceSelector_loader').hide();
		$('#dataServiceSelector_holder').show();
		
		if(dataList.length > 0){
			_.forEach(dataList,function(e,i) {
				var option = '<option value="'+ e.id +'" >' + e.name + '</option>';
				listNode.append(option);
			});
		}
		else {
			listNode.append('<option value="" > No available service for data enrichment</option>');
		}
		// show UI
		listNode.chosen({width: "100%"}).trigger("chosen:updated");
	};
	dataServiceLoader("", true, forPersonalization, forDataEnrichment, forSynchronization, startIndex, numberResult, callback);
}

const setTimeToRunDataService = function() {
	// time selector
	var h = new Date().getUTCHours();
	h = h > 9 ? h : '0' + h;
	var m = (new Date().getUTCMinutes() + 2) % 60;
	m = m > 9 ? m : '0' + m;
	var defaultHour =  h + ':' + m;
	$('#dataServiceDateTimeStart').val(defaultHour);
	
	var node = $('#box_everyday_at_time');
	var schedulingTime = $('#dataServiceSchedulingTime').val();
	if(schedulingTime > 0 ) {
		node.show();
	}
	else {
		node.hide();
	}
}


// time to run activation rule
var SchedulingTimeUnit = "hour";
const SchedulingTimeMap = {}

// TODO
SchedulingTimeMap['0'] = 'Run immediately and repeat every 5 minutes';
SchedulingTimeMap['1'] = 'Run every 1 ' + SchedulingTimeUnit + ' starting from specific time';
SchedulingTimeMap['2'] = 'Run every 2 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['3'] = 'Run every 3 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['4'] = 'Run every 4 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['5'] = 'Run every 5 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['6'] = 'Run every 6 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['7'] = 'Run every 7 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['8'] = 'Run every 8 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['9'] = 'Run every 9 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['10'] = 'Run every 10 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['12'] = 'Run every 12 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['15'] = 'Run every 15 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['20'] = 'Run every 20 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['24'] = 'Run every 24 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['48'] = 'Run every 48 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['72'] = 'Run every 72 ' + SchedulingTimeUnit + 's starting from specific time';
SchedulingTimeMap['168'] = 'Run every week starting from specific time';
SchedulingTimeMap['336'] = 'Run every 2 weeks starting from specific time';
SchedulingTimeMap['672'] = 'Run every 4 weeks starting from specific time';

// Url Link in grid
function ServiceSchedulingTime(config) {
    jsGrid.Field.call(this, config);
};
ServiceSchedulingTime.prototype = new jsGrid.Field({
    sorter: function(date1, date2) {
        return 0;
    },
    itemTemplate: function(e) {
		var v = SchedulingTimeMap[''+e.schedulingTime];
		if(e.timeToStart !== ''){
			v = (typeof v === 'string') ? (v + ' <b class="highlight_text">' + e.timeToStart + '</b>'): ""; 
		}
    	return '<div style="font-size:14px;" target="_blank" >' + v + '</div>';
    },
    insertTemplate: function(value) {
        return this._insertCode = value;
    },
    editTemplate: function(value) {
        return this._editCode = value;
    },
    insertValue: function() {
        return this._insertCode;
    },
    editValue: function() {
        return this._editCode;
    }
});
jsGrid.fields.ServiceSchedulingTime = ServiceSchedulingTime;

const initActivationJsGridPlugin = function() {
	function ActivationButtons(config) {
	    jsGrid.Field.call(this, config);
	};
	ActivationButtons.prototype = new jsGrid.Field({
	    sorter: function(date1, date2) {
	        return 0;
	    },
	    itemTemplate: function(buttonState) {
	    	var activationRuleId = buttonState.id;
	    	var dataServiceName = buttonState.dataServiceName;
	    	var active = buttonState.active;
	    	console.log("activationRuleId ", activationRuleId)
	    	console.log("active ", active)
	    	
	    	var btnHtml = '<div class="col-lg-12" ><button type="button" class="activation-btn btn btn-do-now btn-sm" onclick="loadActivationLogsOfSegment(this)"';
	    	btnHtml += (' data-activation-rule-id="' + activationRuleId  + '" ' );
	    	btnHtml += (' data-activation-service-name="' + dataServiceName  + '" ' );
	    	btnHtml += '><i class="fa fa-info-circle" style="font-size:1.2em" aria-hidden="true"></i> View Logs </button></div>';

			btnHtml = '<div class="col-lg-12" ><button type="button" class="activation-btn btn btn-do-now btn-sm" onclick="manuallyRunActivationOfSegment(this)"';
	    	btnHtml += (' data-activation-rule-id="' + activationRuleId  + '" ' );
	    	btnHtml += (' data-activation-service-name="' + dataServiceName  + '" ' );
	    	btnHtml += '><i class="fa fa-play" style="font-size:1.2em" aria-hidden="true"></i> Manually Run </button></div>';
	    	
	    	if(active) {
	    		btnHtml += '<div class="col-lg-12" ><button type="button" class="activation-btn btn btn-warning btn-sm" onclick="stopActivationForSegment(this)"';
		    	btnHtml += (' data-activation-rule-id="' + activationRuleId  + '" ' );
		    	btnHtml += (' data-activation-service-name="' + dataServiceName  + '" ' );
		    	btnHtml += '><i class="fa fa-stop" style="font-size:1.2em" aria-hidden="true"></i> Stop Scheduler </button></div>';
	    	}
	    	else {
	    		btnHtml += '<div class="col-lg-12" ><button type="button" class="activation-btn btn btn-goto-router btn-sm" onclick="startActivationForSegment(this)"';
		    	btnHtml += (' data-activation-rule-id="' + activationRuleId  + '" ' );
		    	btnHtml += (' data-activation-service-name="' + dataServiceName  + '" ' );
		    	btnHtml += '><i class="fa fa-play" style="font-size:1.2em" aria-hidden="true"></i> Start Scheduler </button></div>';
		    	
		    	btnHtml += '<div class="col-lg-12" ><button type="button" class="activation-btn btn btn-danger btn-sm" onclick="removeActivationForSegment(this)"';
		    	btnHtml += (' data-activation-rule-id="' + activationRuleId  + '" ' );
		    	btnHtml += (' data-activation-service-name="' + dataServiceName  + '" ' );
		    	btnHtml += '><i class="fa fa-trash-o" style="font-size:1.2em" aria-hidden="true"></i> Remove </button></div>';
	    	}
	    	
	    	return btnHtml;
	    },
	    insertTemplate: function(value) {
            return this._insertCode = value;
        },
        editTemplate: function(value) {
            return this._editCode = value;
        },
        insertValue: function() {
            return this._insertCode;
        },
        editValue: function() {
            return this._editCode;
        }
	});
	jsGrid.fields.ActivationButtons = ActivationButtons;
}

const loadActivationRules = function(dataList){
	// table view
	var createTableView = function(nodeSelector, dataModel) {
		$(nodeSelector).jsGrid({
		    width: "100%",
		    height: "auto",
			autoload: false,
		    inserting: false,
		    editing: false,
		    sorting: true,
		    paging: false,
		    data: dataModel,			
		    fields: [
				{ name: "updatedAt", title : '<i class="fa fa-info-circle" aria-hidden="true"></i> Updated at', type: "JsGridLocalTime", align: "center" ,  width: 56, sorting: true},
				{ name: "schedulingTime", title : '<i class="fa fa-bolt" aria-hidden="true"></i> Rule to trigger', type: "ServiceSchedulingTime", align: "center" , sorting: false},
		     	{ name: "dataServiceName", title : '<i class="fa fa-info-circle" aria-hidden="true"></i> Data Service Name', type: "BoldText", align: "center" , sorting: false},	
				{ name: "active", title : '<i class="fa fa-info-circle" aria-hidden="true"></i> Active', type: "CheckBoxIcon", align: "center" ,  width: 28, sorting: false},	
		        { name: "ownerUsername", title : '<i class="fa fa-info-circle" aria-hidden="true"></i> Created by', type: "BoldText", align: "center" ,  width: 50, sorting: false},		        
		     	{ name: "buttonState", title : "Actions", type: "ActivationButtons", width: 50, align: "center"}
		    ]
		});  
	}
	// models
	var forPersonalization = [], forEnrichment = [], forSync = [];
	dataList.forEach(function(e){
		e.buttonState = {id: e.id, active: e.active, dataServiceName: e.dataServiceName};
		e.schedulingTime = {'schedulingTime':e.schedulingTime, 'timeToStart':e.timeToStart} 
		
		if(e.purpose === 'personalization'){
			forPersonalization.push(e);
		}
		else if(e.purpose === 'data_enrichment'){
			forEnrichment.push(e);
		}
		else if(e.purpose === 'synchronization'){
			forSync.push(e);
		}
	});
	
	// sort data
	var sorterByDate = function(a, b) { 
	    if(a.updatedAt > b.updatedAt) 
			return -1;
		else if(a.updatedAt < b.updatedAt) 
			return 1;
		else
			return 0;
	}
	forPersonalization.sort(sorterByDate);
	forEnrichment.sort(sorterByDate);
	forSync.sort(sorterByDate);
	
	// ActivationButtons
	initActivationJsGridPlugin();
	
	// render the models
	createTableView("#segment_personalization_services", forPersonalization)
	createTableView("#segment_enrichment_services", forEnrichment)
	createTableView("#segment_synchronization_services", forSync)
}


const loadSchedulingTimeSelect = function(){
	var node = $('#dataServiceSchedulingTime');
	_.forOwn(SchedulingTimeMap, function(label, time) {
		var n = $('<option value="'+time+'" >'+label+'</option>')
		node.append(n);
	} );
}

const showDataPersonalizationDialog = function(segmentId, segmentName, segmentSize) {
	$('#dialogSelectDataService').modal({ backdrop: 'static', keyboard: false });
	$('span.purpose_activation_name').text('data personalization');
	$('#btnOkActivateSegment').data('purpose','personalization');
	// load data 
	var listNode = $('#dataServiceSelector');
	listNode.data('forSynchronization','false')
	listNode.data('forDataEnrichment','false')
	listNode.data('forPersonalization','true')
	loadDataServiceSelection(segmentId, segmentName)
}

const showDataEnrichmentDialog = function(segmentId, segmentName, segmentSize) {
	$('#dialogSelectDataService').modal({ backdrop: 'static', keyboard: false });
	$('span.purpose_activation_name').text('data enrichment');
	$('#btnOkActivateSegment').data('purpose','data_enrichment');
	// load data 
	var listNode = $('#dataServiceSelector');
	listNode.data('forSynchronization','false')
	listNode.data('forDataEnrichment','true')
	listNode.data('forPersonalization','false')
	loadDataServiceSelection(segmentId, segmentName)
}

const showDataSynchronizationDialog = function(segmentId, segmentName, segmentSize) {	
	$('#dialogSelectDataService').modal({ backdrop: 'static', keyboard: false });
	$('span.purpose_activation_name').text('data synchronization');
	$('#btnOkActivateSegment').data('purpose','synchronization');
	// load data 
	var listNode = $('#dataServiceSelector');
	listNode.data('forSynchronization','true')
	listNode.data('forDataEnrichment','false')
	listNode.data('forPersonalization','false')
	loadDataServiceSelection(segmentId, segmentName)
}

const showAutomatedCampaignsDialog = function(segmentId, segmentName, segmentSize) {
	$('#dialogSelectDataService').modal({ backdrop: 'static', keyboard: false });
	$('span.purpose_activation_name').text('data personalization');
	$('#btnOkActivateSegment').data('purpose','personalization');
	// load data of campaign
}

const activateDataService = function(buttonNode) {
	var purpose = $(buttonNode).data('purpose');
	var dataServiceId = $('#dataServiceSelector').val();
	var segmentId = $(buttonNode).data('segmentId');
	var segmentName = $(buttonNode).data('segmentName');
	
	// time to run activation
	var schedulingTime = parseInt($('#dataServiceSchedulingTime').val()); // store in hour unit
	var timeToStart = $('#dataServiceDateTimeStart').val();
	
	if(typeof dataServiceId === "string" && dataServiceId !== ""){
		$('#dialogSelectDataService').modal('hide');
		
		var params = {"purpose": purpose, "segmentId" : segmentId, "dataServiceId" : dataServiceId };
		params['schedulingTime'] = schedulingTime;
		params['timeToStart'] = timeToStart;
		
		LeoAdminApiUtil.callPostAdminApi('/cdp/data-service/create-activation', params, function(json) { 
			if(json.data !== ''){
				var info =  'The segment [' + segmentName + '] is successfully activated for ' + purpose.replace('_',' ');
				notifySuccessMessage(info, function(){
					refreshSegmentDetails();
				});
			}
			else {
				var msg = 'Data activation for [' + segmentName + '], is failed, please set valid configs for the service: ' + dataServiceId;
				notifyErrorMessage(msg)
			}
		})
	} else {
		iziToast.error({ title: 'Error', message: "Please select an active service"});
	}
}

const stopActivationForSegment = function(node){
	var id = $(node).data("activation-rule-id");
	var serviceName = $(node).data("activation-service-name");
	var params = {"activationRuleId": id};
	LeoAdminApiUtil.callPostAdminApi('/cdp/data-service/stop-activation', params, function(json) { 
		if(json.data === true){
			var info = 'The activation rule of service [' + serviceName + '] is successfully stopped ! ';
			notifySuccessMessage(info, function(){
				refreshSegmentDetails();
			});
		}
		else {
			var info =  'There is an error when stop the activation rule of service [' + serviceName + '] ! ';
			notifyErrorMessage(info)
		}
	})
}


const startActivationForSegment = function(node){
	var id = $(node).data("activation-rule-id");
	var serviceName = $(node).data("activation-service-name");
	var params = {"activationRuleId": id};
	LeoAdminApiUtil.callPostAdminApi('/cdp/data-service/start-activation', params, function(json) { 
		if(json.data === true){
			var info =  'The activation rule of service [' + serviceName + '] is successfully started ! ';
			notifySuccessMessage(info, function(){
				refreshSegmentDetails();
			});
		}
		else {
			var info =  'There is an error when start the activation rule of service [' + serviceName + '] ! ';
			notifyErrorMessage(info)
		}
	})
}

const removeActivationForSegment = function(node){
	var id = $(node).data("activation-rule-id");
	var serviceName = $(node).data("activation-service-name");
	var params = {"activationRuleId": id};
	LeoAdminApiUtil.callPostAdminApi('/cdp/data-service/remove-activation', params, function(json) { 
		if(json.data === true){
			var info =  'The activation rule of service [' + serviceName + '] is successfully removed ! ';
			notifySuccessMessage(info, function(){
				refreshSegmentDetails();
			});
		}
		else {
			var info =  'There is an error when remove the activation rule of service [' + serviceName + '] ! ';
			notifyErrorMessage(info)
		}
	})
}

const loadActivationLogsOfSegment = function(node){
	var activationRuleId = $(node).data("activation-rule-id");
	console.log("load Action Logs of ActivationRule ID: " + activationRuleId)
	
	var callback = function(){
		$('#activationEventLogsDialog').modal({ backdrop: 'static', keyboard: false });
	}
	loadSystemEventsTable('Activation Rule', 'activationEventLogsHolder', 'ActivationRule', activationRuleId, 'leocdp', callback, [6]);
}


const manuallyRunActivationOfSegment = function(node){
	var id = $(node).data("activation-rule-id");
	var serviceName = $(node).data("activation-service-name");
	var params = {"activationRuleId": id};
	LeoAdminApiUtil.callPostAdminApi('/cdp/data-service/manually-run-activation', params, function(json) { 
		if(json.data === true){
			var info =  'The activation rule of service [' + serviceName + '] is manually run ! ';
			notifySuccessMessage(info, function(){
				refreshSegmentDetails();
			});
		}
		else {
			var info =  'There is an error when start the activation rule of service [' + serviceName + '] ! ';
			notifyErrorMessage(info)
		}
	})
}
