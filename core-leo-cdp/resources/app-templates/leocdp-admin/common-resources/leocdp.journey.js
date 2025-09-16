const eventDataTypes = [
    { name: "First-party Data", id: 1 },
    { name: "Second-party Data", id: 2 },
    { name: "Third-party Data", id: 3 }
];

const journeyStages = [
    { name: "AWARENESS", id: 1 },
    { name: "ATTRACTION", id: 2 },
    { name: "ASK", id: 3 },
    { name: "ACTION", id: 4 },
    { name: "ADVOCACY", id: 5 }
];

const journeyStagesMap = {
	1 : "AWARENESS",
	2 : "ATTRACTION",
	3 : "ASK",
	4 : "ACTION",
	5 : "ADVOCACY"
}

const scoringModels = [
    { name: "Lead Score", id: 1 },
    { name: "Prospect Score", id: 2 },
    { name: "Engagement Score", id: 3 },
    { name: "Loyalty Score", id: 12 },
    { name: "Data Quality Score", id: 4 },
    { name: "CAC Score", id: 5 },
    { name: "CLV Score", id: 6 },
    { name: "Credit Score", id: 11 },
    { name: "Churn Score", id: 13},
    { name: "CES Score", id: 7 },
    { name: "CSAT Score", id: 8 },
    { name: "CFS Score", id: 9 },
    { name: "NPS Score", id: 10 }
];

const initJourneyMapAndTouchpointHubs = function(){
	var containerSelector = '#journeyFlowChart';
	var containerNode = $(containerSelector).html('<div class="loader"></div>');
	var callback = function(journeyMap, touchpointCount){
		containerNode.empty();
		
		loadJourneyMapEventMatrix(window.currentJourneyMapId, 'journey_map_event_matrix');
		
		// var maxSize = touchpointCount * 3;
		// loadJourneyTouchpointHubReport(window.currentJourneyMapId, maxSize);
		
		// data authorization
		if( window.currentUserProfile.role >=5 ) {
			loadSystemUsersForDataAuthorization(true, journeyMap, $('#authorizedJourneyViewers'), $('#authorizedJourneyEditors'));
		}
	}
	loadJourneyMapAndTouchpointHubs(currentJourneyMapId, containerSelector, '#journeyMapName, #authorizedJourneyName', "#touchpoint_hub_table", callback);
}

const refreshJourneyMapView = function(){
	if(currentJourneyMapId){
		location.hash  = "#calljs-leoCdpRouter('Data_Journey_Map','"+currentJourneyMapId+"','_refresh_"+new Date().getTime()+"')";	
	}
}

const loadJourneyFlowReport = function(){
	showJourneyFlowReport({"journeyMapId": currentJourneyMapId}, 'journeyFlowReport' )
}

const updateJourneyMap = function(isInsert){
	var callback = function(json){
		if(json.data != ""){
			// refresh view
			refreshJourneyMapView();
		}
		else {
			console.error('updateJourneyMap ', rs)
		}
	};
	
	var touchpointHubData = JSON.stringify($("#touchpoint_hub_table").jsGrid("option", "data"));
	var params = {'touchpointHubJson': touchpointHubData, 'journeyMapId': currentJourneyMapId };
	
	LeoAdminApiUtil.callPostAdminApi('/cdp/journeymap/save', params, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
			callback(json);
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
   });
}
	
const loadFeedbackTemplateItems = function(obsid, callback){
	LeoAdminApiUtil.getSecuredData('/cdp/asset-item/asset-templates', {"obsid":obsid}, function(json){ 
		cxFeedbackTemplates = json.data.cxFeedbackTemplates;
		landingPageTemplates = json.data.landingPageTemplates;
		callback();
    });
}

const loadEventMetrics = function(){
	LeoAdminApiUtil.getSecuredData('/cdp/funnel/event-metrics', {} , function(json){  
		eventMetrics = json.data;
	})
}

var loadJourneyTemplate = function(){
	iziToast.info({
	    title: 'Information',
	    message: 'This function is under development and is not yet available'
	});
}

var jsGridItemUrlTemplate = function(value) {
	value = value ? value.trim() : '';
	if( value.indexOf("http")===0 ){
		var a = $("<a>").attr('href',"javascript:").attr('title',value).attr('onclick',"openUrlInNewTab(event,this)").text(value);
		return $('<div class="long_url_holder" ></div>').append(a);
	}
    return $('<div class="long_url_holder" ></div>').html(value);
}

function initJourneyMapNameEditor() {
	var defaultName = "Journey Map: " + new Date().toDateString();
	
	var updateName = function(name){
		var params = {'journeyMapName': name, 'journeyMapId': currentJourneyMapId };
		LeoAdminApiUtil.callPostAdminApi('/cdp/journeymap/update-name', params, function (json) {
	        if (json.httpCode === 0 && json.errorMessage === '') {
				iziToast.success({
    	    	    title: 'Data Journey Map',
    	    	    message: '<b>' + name +  '</b> is updated successfully!',
    	    	    timeout: 2500
    	    	});
    	    	loadJourneyMapList(true);
	        } else {
	            LeoAdminApiUtil.logErrorPayload(json);
	        }
	   });
	}
	
	var nameNode = $('#journeyMapName');
	$("#journeyMapNameEditor").blur(function () {
		var name = $(this).val().trim();
		name = name === "" ? defaultName : name;
        
        $('#journeyMapNameEditor').hide();
        
        if(nameNode.text() !== name){
        	nameNode.text(name).show();
			updateName(name);
		}
		else {
			nameNode.show();
		}
	});
	
	$("#journeyMapNameEditor").keyup(function (event) {
		var keycode = (event.keyCode ? event.keyCode : event.which);
		if(keycode == '13'){
			var name = $(this).val().trim();
			name = name === "" ? defaultName : name;
	        $('#journeyMapNameEditor').hide();
	        
	        if(nameNode.text() !== name){
	        	nameNode.text(name).show();
				updateName(name);
			}
			else {
				nameNode.show();
			}
		}
	});
}

function saveJourneyDataAuthorization(){
	// data authorization of journey map
	var authorizedViewers = $('#authorizedJourneyViewers').val() || [];
	var authorizedEditors = $('#authorizedJourneyEditors').val() || [];
	var params = {'authorizedViewers': authorizedViewers, 'authorizedEditors': authorizedEditors, 'journeyMapId': currentJourneyMapId };
	params.updateAllProfilesInJourney = $('#updateAllProfilesInJourney').prop("checked") === true;
	LeoAdminApiUtil.callPostAdminApi('/cdp/journeymap/update-data-authorization', params, function (json) {
        if (json.data === currentJourneyMapId) {
			iziToast.success({
	    	    title: 'Data Journey Map',
	    	    message: '<b>The Data Journey Authorization of ' + $('#journeyMapName').text() +  '</b> is updated successfully!',
	    	    timeout: 3000
	    	});
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
   });
}

function loadJourneyStagesList(callbackUpdate) {
	var listNode = $('#journeyStagesList');
	journeyStages.forEach(function(stage){
		var option = '<option value="'+ stage.id +'" >' + stage.name + '</option>';
		listNode.append(option);
	})
	listNode.val("1").change();	
	// show UI
	listNode.chosen({
        width: "100%",
        no_results_text: "Oops, nothing found!"
    }).trigger("chosen:updated").change(function(){
		if(typeof callbackUpdate === "function" ){
			callbackUpdate($(this).val())
		}
    });
}
	
function loadJourneyMapList(refresh, callbackUpdate, showAllDataOption, callbackDataLoaded) {
	var listNode = $('#journeyMapList');
	if(refresh === true){
		listNode.find('option').remove().trigger("chosen:updated");
	}
	// request server
	LeoAdminApiUtil.getSecuredData('/cdp/journeymap/list', { "_" : new Date().getTime() }, function(json){
		if (json.httpCode === 0 && json.errorMessage === '') {
        	var journeyMaps = json.data;

			var selectedNodeValue = window.currentJourneyMapId;
        	
			// showAllDataOption only for admin
        	if(showAllDataOption === true && currentUserProfile.role >= 5){
				var option = '<option value="" > All Journey Maps </option>';
				listNode.append(option);
			}
			
			
			journeyMaps.forEach(function(journey){
				var option = '<option value="'+ journey.id +'" >' + journey.name + '</option>';						
				listNode.append(option);	
			})			

			// set selected value
			listNode.val(selectedNodeValue).change();
			
			if(typeof callbackDataLoaded === "function"){
				callbackDataLoaded(listNode.val(), listNode.find("option:selected").text())
			}
			
			// show UI
			listNode.chosen({
		        width: "100%",
		        no_results_text: "Oops, nothing found!"
		    }).trigger("chosen:updated").change(function(){
				if(typeof callbackUpdate === "function" ){
					var seletedId = $(this).val()
					var name = $(this).find("option:selected").text()
					window.currentJourneyMapId = seletedId;
					
					callbackUpdate(seletedId, name);
				}
				else {
					window.currentJourneyMapId = $(this).val();
		    		refreshJourneyMapView()	
				}
		    });
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
	});
}

function loadTouchpointHubList(defaultTouchpointHubId, refresh, callbackUpdate, callbackDataLoaded) {
	var listNode = $('#touchpointHubList');
	if(refresh === true){
		listNode.find('option').remove().trigger("chosen:updated");
	}
	
	var journeyMapId = $('#journeyMapList').val();
	if(journeyMapId === ""){
		journeyMapId = "id_default_journey";
	}
	// request server
	LeoAdminApiUtil.getSecuredData('/cdp/touchpointhub/list', {"journeyMapId": journeyMapId}, function(json){
		if (json.httpCode === 0 && json.errorMessage === '') {
        	var touchpointHubs = json.data;
        	// load list HTML

        	touchpointHubs.forEach(function(touchpointHub){
				var option = '<option value="'+ touchpointHub.id +'" >' + touchpointHub.name + '</option>';
				listNode.append(option);
			})
			
			// set selected value
			if(typeof defaultTouchpointHubId === "string") {
				listNode.val(defaultTouchpointHubId).change();	
			}
			
			if(typeof callbackDataLoaded === "function"){
				callbackDataLoaded(listNode.val(), listNode.find("option:selected").text())
			}
			
			// show UI
			listNode.chosen({
		        width: "100%",
		        no_results_text: "Oops, nothing found!"
		    }).trigger("chosen:updated").change(function(){
				if(typeof callbackUpdate === "function" ){
					var seletedId = $(this).val()
					var name = $(this).find("option:selected").text()
					callbackUpdate(seletedId, name);
				}
		    });
			$('#touchpointHubList_chosen').css('z-index',"99")
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
	});
}
	
function editJourneyMapName(){
	var nameNode = $('#journeyMapName');
    $('#journeyMapNameEditor').val(nameNode.text()).show().focus();
    nameNode.hide();
}

function createNewJourneyMap(){
	$('#dialogCreateNewJourneyMap').modal({
		backdrop: 'static',
		keyboard: false
	});
}

function saveNewJourneyMap(){
	var journeyMapName = $('#newJourneyMapName').val();
	if(journeyMapName.length <= 5) {
		iziToast.error({
    	    title: 'Journey Map Creation',
    	    message: 'The journey map <b>'+journeyMapName+'</b> is too short, please enter at least 6 characters !',
    	    timeout: 3000,
    	});
		return;
	}
	
	var callServer = function(){
		var params = {'journeyMapName': journeyMapName };
		LeoAdminApiUtil.callPostAdminApi('/cdp/journeymap/create-new', params, function (json) {
	        if (json.httpCode === 0 && json.errorMessage === '') {
	        	if(typeof json.data === 'string'){
	        		currentJourneyMapId = json.data;
	        		var msg = 'The journey map <b>'+journeyMapName+'</b> has been created created successfully !';
	        		notifySavedOK('Journey Data Map', msg)
	        		refreshJourneyMapView();
	        	}
	        } else {
	            LeoAdminApiUtil.logErrorPayload(json);
	        }
	   });
	}
	
	$('#dialogCreateNewJourneyMap').modal('hide');
	setTimeout(callServer, 1000);
}
	
function deleteCurrentJourneyMap(){
	if(typeof currentJourneyMapId === "string" && currentJourneyMapId != 'id_default_journey' ) {
		var journeyMapName = $('#journeyMapList').find('option:checked').text();
		
		$('#delete_callback').val('');
		$('#confirmDeleteParentChildDialog').modal({ focus: true });
		
		// init checkbox
		$('#deleteChildItemMsg').text("Delete all touchpoint hubs and related data in this journey map!");
		$('#deleteChildItemCheckbox').prop('checked', true );
		$('#deleteChildItem').show();
		
        var callback = "deleteJourneyMap" + currentJourneyMapId;
        
        //callback for OK button
	    window[callback] = function () {
	    	var urlStr = baseLeoAdminUrl + '/cdp/journeymap/delete';
	    	
	    	var checked = $('#deleteChildItemCheckbox').is(":checked");
	        LeoAdminApiUtil.callPostAdminApi(urlStr, { 'journeyMapId': currentJourneyMapId}, function (json) {
	            if (json.httpCode === 0 ) {
	                if(json.data === true){
	                	$('#deleteChildItem').hide();
	                	$('#deleteChildItemMsg').text('')
	                	
	                	iziToast.success({
	                	    title: 'OK',
	                	    message: 'Successfully deleted the journey map "'+ journeyMapName +'"',
	                	    onClosing: function(instance, toast, closedBy){
	                	    	// go to default map
	                	    	currentJourneyMapId = "id_default_journey";
	                	    	refreshJourneyMapView();
	                	    }
	                	});
	                } 
	                else {
	                	iziToast.error({
	                	    title: 'Error',
	                	    message: json.data,
	                	    onClosing: function(instance, toast, closedBy){
	                	    	locatio.href = '/admin';
	                	    }
	                	});
	                }
	            } else {
	                $('#error-on-save').html(json.errorMessage).show().delay(5000).fadeOut('slow');
	                LeoAdminApiUtil.logErrorPayload(json);
	            }
	        });
	    }
	    $('#delete_parent_callback').val(callback);
	    $('#deletedParentInfoTitle').text(journeyMapName);
	    $('#deletedParentInfoMsg').text('Do you want to delete the journey map ?');
	}
}

var loadJourneyMap = function(journeyMapId, containerSelector, titleSelector) {
	LeoAdminApiUtil.getSecuredData('/cdp/journeymap/get', { "id" : journeyMapId , "_": new Date().getTime()}, function(json){
		if(json.errorCode > 0){
			iziToast.error({
	    	    title: 'Data Journey Map',
	    	    message: json.errorMessage
	    	});
			return;
		}
	 
	 	var touchpointHubTypes = json.data.touchpointHubTypes;
   		var journeyMap = json.data.journeyMap;
   		var defaultMetricName = journeyMap.defaultMetricName;
   		var name = journeyMap.name;
   		var journeyStages = journeyMap.journeyStages;
   		var journeyLinks = journeyMap.journeyLinks;
   		var journeyNodes =  journeyMap.journeyNodes;
   		
   		// var touchpointHubIndex = journeyMap.touchpointHubIndex;
   		var touchpointHubMap = journeyMap.touchpointHubMap;
   		
   		$(titleSelector).text(name);
   		
   		renderJourneyFlowChart(containerSelector, defaultMetricName, journeyStages, journeyNodes, journeyLinks, touchpointHubMap);
   	})
}

function deleteTouchpointHubConfirmation(id, name){
	if (id && name) {
		$('#delete_callback').val('');
		$('#confirmDeleteDialog').modal({ focus: true });
		
		var callback = "deleteTouchpointHubConfirmation" + id;
		window[callback] = function () {	   		     	
   		    LeoAdminApiUtil.callPostAdminApi('/cdp/touchpointhub/delete', {'id' : id}, function (json) {
   	            if (json.httpCode === 0 && json.errorMessage === '') {
   	            	iziToast.success({
	    	    	    title: 'Data Journey Map',
	    	    	    message: name + ' is deleted successfully!',
	    	    	    timeout: 3000,
	    	    	    onClosed: function () {
	    	    	    	refreshJourneyMapView();
	    	    	    }
	    	    	});
   	            } else {
   	                LeoAdminApiUtil.logErrorPayload(json);
   	            }
   	       	});
		}
		var msg = "Do you want to delete the selected touchpoint hub permanently?";
		$('#delete_callback').val(callback);
		$('#deletedInfoTitle').text("Touchpoint Hub Name: "+ name).show();
		$('#deletedInfoMsg').text(msg);
	}
}

const getJourneyLevelList = function(){
   	var list = [];
	var levelDeep = 42;
	for(var i = 1; i <= levelDeep; i++){
		var item = { Name: '[ ' + i + ' ]', Id: i };
		list.push(item);
	}
	return list;
}

var loadJourneyMapAndTouchpointHubs = function(journeyMapId, containerSelector, titleSelector, touchpointHubsSelector, callback) {
	LeoAdminApiUtil.getSecuredData('/cdp/journeymap/get', { "id" : journeyMapId , "_": new Date().getTime() }, function(json){
		if(json.errorCode > 0){
			iziToast.error({
	    	    title: 'Data Journey Map',
	    	    message: json.errorMessage
	    	});
			return;
		}
	 
	 	// touchpointHubTypes data
	 	var touchpointHubTypes = json.data.touchpointHubTypes;
	 	
	 	// journeyMap data
   		var journeyMap = json.data.journeyMap;
   		
   		// attributes of journeyMap
   		var name = journeyMap.name;
   		var defaultMetricName = journeyMap.defaultMetricName;   		
   		var journeyStages = journeyMap.journeyStages;
   		var journeyLinks = journeyMap.journeyLinks;
   		var journeyNodes =  journeyMap.journeyNodes;
   		
   		var touchpointHubIndex = journeyMap.touchpointHubIndex;
   		var touchpointHubMap = journeyMap.touchpointHubMap;
   		
   		// journey name
   		$(titleSelector).text(name);
   		
   		// callback
   		if(typeof callback === "function"){
   			callback(journeyMap, journeyNodes.length)
   		}
   		
   		// visualize the flow design with report
   		renderJourneyFlowChart(containerSelector, defaultMetricName, journeyStages, journeyNodes, journeyLinks, touchpointHubMap);
   		
   		var touchpointHubs = [];
   		for(var name in touchpointHubMap){
   			var index = touchpointHubIndex[name];
   			var touchpointHub = touchpointHubMap[name];
   			touchpointHub.index = index + 1;
   			touchpointHubs[index] = touchpointHub;
   		}

   		
   		var touchpointHubTypeSelects = [];
   		Object.keys(touchpointHubTypes).forEach(key => {
   			var typeNum = parseInt(key);
   			if(typeNum > 0 && typeNum !== 30 && typeNum < 1000){
   				touchpointHubTypeSelects.push({ typeName: touchpointHubTypes[key].replaceAll('_',' '), type: typeNum });
   			}
   		});
   		
   		// get to check permission
   		var canInsertData = json.canInsertData;
    	var canEditData = json.canEditData;
		var canDeleteData = json.canDeleteData;
   		var insertRowMoved = false, insertedIndex = -1;
   		
   		$(touchpointHubsSelector).jsGrid({
			data: touchpointHubs,
   		    width: "100%",
   		    height: "auto",
   		 	inserting: canInsertData,
		    editing: canEditData,
		    sorting: true,
		    paging: false,
		    confirmDeleting: false,
   		    onItemInserting: function(args) {
	
   		    	var totalProfile = typeof args.item.totalProfile === 'number' ? args.item.totalProfile : 0;
   		    	args.item.totalProfile = totalProfile;

				var journeyLevel = safeParseInt( $('.jsgrid-grid-body tbody tr:last td:first').text() ) - 1;
				args.item.journeyLevel = journeyLevel;
				//console.log('onItemInserting item', args.item.totalProfile)
   		    	
   		    	var tpType = args.item.type;
   		    	var isAvailable = touchpointHubMap[args.item.name] == null;
   		    	if( ! isAvailable ) {
   		    	 	args.cancel = true;
		            iziToast.error({
	    	    	    title: 'Data Journey Map',
	    	    	    message: args.item.name +  ' is duplicated, please enter a different name !'
	    	    	});
		            return;
   		    	}
   		    	
   		    	var validType = ! (tpType === 30 || tpType >= 1000);
   		    	if(insertedIndex > 0 && validType ){
	        		args.item.index = insertedIndex;
		        	insertedIndex = -1;
	        	}
				
   		    },
   		 	onItemUpdating : function(args) {
   		    	var item = args.item;
   		    	var tpType = item.type;
   		    	
   		    	var name = item.name;   		    	
   		    	var readOnly =  tpType === 30 || tpType >= 1000;
   		    	if(readOnly) {
   		    		args.cancel = true;
		            iziToast.error({
	    	    	    title: 'Data Journey Map',
	    	    	    message: name +  ' is read-only touchpoint, you can not update it !'
	    	    	});
   		    	} 
   		    }, 
   		    onItemInserted: function(args) {
   		    	iziToast.success({
    	    	    title: 'Data Journey Map',
    	    	    message: args.item.name +  ' is inserted successfully!',
    	    	    timeout: 2500,
    	    	    onClosed: function () {
    	    	    	updateJourneyMap(true);
    	    	    }
    	    	});
   		    }, 
   		 	onItemUpdated: function(args) {
   		    	iziToast.success({
    	    	    title: 'Data Journey Map',
    	    	    message: args.item.name +  ' is updated successfully!',
    	    	    timeout: 2500,
    	    	    onClosed: function () {
    	    	    	updateJourneyMap(false);
    	    	    }
    	    	});
   		    }, 
   		 	onItemDeleting : function(args) {
   		 		args.cancel = true;
   		 		var item = args.item;
		    	var name = item.name;
		    	var editable = item.type != 30;
		    	if(editable) {
		    		deleteTouchpointHubConfirmation(item.id, name);
   		    	}
		    	else { 
		    		iziToast.error({
	    	    	    title: 'Data Journey Map',
	    	    	    message: name +  ' is read-only touchpoint hub, you can not delete it !'
	    	    	});
	   		    }
   		    },  
	   		onRefreshed: function (args) {
	   		    if ( ! insertRowMoved ) {
	   		        var insertRow = args.grid._insertRow;
	   		        var gridBody = args.grid._bodyGrid;
	   		        console.log(insertRow)
	   		        var layer = safeParseInt( $('.jsgrid-grid-body tbody tr:last td:first').text() ) - 1;
	   		     	$(insertRow).find('td:first').text('[' + layer + ']');
	   		        $( "<tfoot></tfoot>" ).appendTo(gridBody).append(insertRow);
	   		    }
	   		},
   		    fields: [
				{ name: "journeyLevel", title : "Level", type: "select", items: getJourneyLevelList(), valueField: "Id", textField: "Name", width: 24 },
   		    	{ name: "name", title : "Name", type: "text", width: 60, validate: "required", align: "center" , css:"touchpoint_hub_name", sorting: false},
   		     	{ name: "type", title : "Type", type: "select", items: touchpointHubTypeSelects, valueField: "type", textField: "typeName", width: 58 , css:"datatable_text", 
   		            insertTemplate: function () {
   		                var $insertControl = jsGrid.fields.select.prototype.insertTemplate.call(this);
   		                $insertControl.change(function () {
   		                    var selectedValue = $(this).val();
   		                    if( selectedValue == 30 || selectedValue >= 1000 ){
   		                    	iziToast.error({ title: 'Data Journey Map', message: 'Can not select DATA OBSERVER, VISITOR REPORT, LEAD REPORT and CUSTOMER REPORT !' });
   		                    	$("#touchpoint_hub_table").jsGrid("cancelEdit");
   		                    }
   		                });
   		                return $insertControl;
   		            },
   		            editTemplate: function (value) {
   		                var $editControl = jsGrid.fields.select.prototype.editTemplate.call(this, value);
   		                if(value == 30) {
   		                	return "DATA OBSERVER";
   		                }	
   		                return $editControl;
   		            },
   		            itemTemplate: function (value) {
		                var $itemTemplate = jsGrid.fields.select.prototype.itemTemplate.call(this, value);
		                if(value == 30) {
		                	return "DATA OBSERVER";
		                }	
		                return $itemTemplate;
		            },
   		        },	
   		     	{ name: "firstPartyData", title : "First-party Data", type: "CustomCheckBox",  validate: "required", width: 34, align: "center", sorting: false },   		     	
   		     	{ name: "totalProfile", title : "Total Profile", type: "number", width: 25, editing: false, align: "center", css:"datatable_text",
	   		     		insertTemplate: function () {
	   		                var input = this.__proto__.insertTemplate.call(this);
	   		                input.val(0);
	   		                return input;
	   		            },
   		     			itemTemplate: function (value) { value = typeof value === 'number' ? value : 0; return "<span>" + value.toLocaleString() + "</span>"; } 
   		     			,validate: { validator: "range",message: 
   		     				function(value, item) {
   		     					return "The total profile should be between 0 and 1,000,000,000. Entered age is \"" + value + "\" is out of specified range.";
   		     				},
   		     			param: [0, 1000000000]
   		     		}
   		     	},
   		     	{ name: "url", title : "URL or Location Address", type: "text", itemTemplate: jsGridItemUrlTemplate , validate: "required", align: "center", sorting: false },
   		     	{ type: "control",  deleteButton: canDeleteData, width: 22, sorting: false }
   		    ]
   		}); 
		$("#touchpoint_hub_table").jsGrid("sort", "journeyLevel");

		// check Authorization
   		if( ! canInsertData || ! canEditData ){
   			$("#touchpoint_hub_table").find('.jsgrid-button').click(errorNoAuthorization);
			
   		}
   		
   	})
} 

const getJsTrackingCodeGA4 = function(){
	var jsCode = "";
	var measurementId = $('#measurement_id_GA4').val().trim();
	if(measurementId !== ""){
		var tpl = _.template( $('#google-analytics-javascript-tpl').html() );
		jsCode += ( "\n" + tpl({"measurementId": measurementId}) + "\n"  );
	}
	if(jsCode !== ""){
		return '<script>' + jsCode + '<\/script>\n';
	}
	return "";
}
    
const getJsCodeEventTrackingFunctions = function(){
	var jsCode = "";
	var c = 0;
	eventMetrics.forEach(function(e){
		if(e.showInObserverJS === true) {
			c++;
			var tplId = '#leo-tracking-view-event-code-tpl';
			
			if( e.journeyStage === 3 || e.journeyStage === 4 || e.journeyStage === 5){
				if(e.scoreModel === 6){
					tplId = '#leo-tracking-conversion-event-code-tpl';	
				}
				else {
					tplId = '#leo-tracking-action-event-code-tpl';
				}
			}
			else if(e.journeyStage === 6){
				tplId = '#leo-tracking-feedback-event-code-tpl';
			}
			var tpl = _.template( $(tplId).html() );
			var model = { "counter" : c, "eventMetricId": e.id, "eventMetricName": e.eventLabel.replaceAll(" ","") };
			jsCode += tpl(model) ;
		}
	});
	return jsCode;
}
	
const getTrackingJsCode =  function(btn) {
	var idDialog = "webDataObserverCodeDialog";
	var editorSelector = '#data_observer_code';
	
	var json = JSON.parse(decodeURIComponent($(btn).data('json')));
	console.log('getTrackingJsCode ',json)
	
	var leoObserverId = json.id;
	var touchpointHubName = json.name;
	var type = json.type;
	var dataSourceUrl = json.dataSourceUrl;
	var javascriptTags = json.javascriptTags || [];
	var jsTagsStr = javascriptTags.length > 0 ? javascriptTags[0].trim() : '';
	
	var leoObserverDomain = window.baseLeoObserverDomain;
	var staticFileDomain = window.baseStaticDomain;
	
	if(type > 0 && type < 12){
		$('#'+idDialog).modal({ backdrop: 'static', keyboard: false });
		
		//init code preview editor
		$('#code_holder .CodeMirror').remove();
		
		var codeNode = $(editorSelector)[0];
		var editor = CodeMirror.fromTextArea(codeNode, {
			mode:  "htmlmixed",
			lineNumbers: true,
            styleActiveLine: true,
            matchBrackets: true,
            readOnly: false,
	    });
		
		editor.setSize(null, 393);
		setTimeout(function() {
			var setupCode = function() {
				
				var autoTracking = $('#leo_observer_auto_track').is(":checked");
				var autoCollectUTM  = $('#leo_observer_auto_collect_UTM').is(":checked");
				var autoTrackGA4  = $('#leo_observer_auto_collect_GA4').is(":checked");
				var autoTrackLinks = $('#leo_observer_auto_track_all_links').is(":checked");
				var autoTrackButtons = $('#leo_observer_auto_track_all_buttons').is(":checked");
				
				// get JS code
				var trackingCodeGA4 = autoTrackGA4 ? getJsTrackingCodeGA4() : "";
				var eventTrackingFunctions = getJsCodeEventTrackingFunctions();
				
				//copy from textarea template into editor, then selectAll for copy
				var tpl = $('#webDataObserverCodeDialog .code').val().trim();
				var code = tpl.replace('__leoObserverId__', leoObserverId)
				code = code.replace('__tpHubName__', touchpointHubName)
				code = code.replace('__leoObserverDomain__', leoObserverDomain);
				code = code.replace('__staticFileDomain__', staticFileDomain);
				code = code.replace('__eventTrackingFunctions__', eventTrackingFunctions);
				
				if(autoTracking) {
					if(autoCollectUTM){
						code = code.replace('__autoTrackingFunctions__', "LeoObserver.recordEventPageView(parseDataUTM())");
					}
					else {
						code = code.replace('__autoTrackingFunctions__', "LeoObserver.recordEventPageView()");
					}
				} else {
					code = code.replace('__autoTrackingFunctions__', "// No auto tracking, please call it yourself");
				}
				
				if(autoTrackLinks) {
					code += '\nLeoObserver.addTrackingAllLinks();\n';
				}
				if(autoTrackButtons) {
					code += '\nLeoObserver.addTrackingAllButtons();\n';
				}
				
				if(typeof jsTagsStr !== 'string' || jsTagsStr === ''){
					jsTagsStr =  trackingCodeGA4 + '<script>\n' + code + '\n<\/script>';
				}
				
				editor.getDoc().setValue(jsTagsStr)
				editor.refresh();
				editor.execCommand('selectAll');
			}
			
			var sel = '#leo_observer_auto_track, #leo_observer_auto_collect_UTM, #leo_observer_auto_collect_GA4, #leo_observer_auto_track_all_links, #leo_observer_auto_track_all_buttons';
			$(sel).change(function() {
				setupCode();
			});
			$('#btn_ok_auto_collect_GA4').click(function() {
				setupCode();
			});
			setupCode();
			
			// copy code into clipboard
			var buttonSelector = '#'+idDialog+ ' button.btn-copy-code';
			addHandlerCopyCodeButton(editorSelector, buttonSelector);
		},350);
		
		var btntest = $('#testObserverChromeExtBtn');
		btntest.attr({'leoObserverId':leoObserverId, 'leoObserverDomain':leoObserverDomain, 'staticFileDomain':staticFileDomain, "dataSourceUrl":dataSourceUrl});
		btntest.click(function(){
			var leoObserverId = $(this).attr('leoObserverId')
			var leoObserverDomain = $(this).attr('leoObserverDomain')
			var staticFileDomain = $(this).attr('staticFileDomain')
			var dataSourceUrl = $(this).attr('dataSourceUrl');
			dataSourceUrl = (dataSourceUrl.indexOf("?") > 0) ? dataSourceUrl : (dataSourceUrl + "?")
			var url = dataSourceUrl + "_leoObserverLogDomain="+ leoObserverDomain +"&_leoObserverId="+leoObserverId+"&_leoObserverCdnDomain="+staticFileDomain;
			window.open(url, "_blank")
		})
	} else {
		notifyErrorMessage("Can not get tracking code of offline touchpoint, please use QR code")
	}
}

const getCxFeedbackJsCode =  function(btn) {
	var json = JSON.parse(decodeURIComponent($(btn).data('json')));

	var leoObserverId = json.id;
	var type = json.type;
	if( type < 30 ){
		var callback = function() {
			
			var hasData = $('#feedbackTemplateSelector option').length === 0 && cxFeedbackTemplates.length > 0;
			if( hasData ) {
				for(var i in cxFeedbackTemplates) { 
					var feedbackFormData = cxFeedbackTemplates[i];
					var option = '<option value="'+i+'" >' + feedbackFormData.title + '</option>';
					$('#feedbackTemplateSelector').append(option);
				}
			}
			
			var idDialog = "dialogEmbeddedCxFeedbackForm";
			$('#'+idDialog).modal({ backdrop: 'static', keyboard: false });
			$('#feedbackTemplateSelector').val("0").change();
			
			// init CX Embedded JS Code
			initEmbeddedFeedbackForm(leoObserverId, idDialog, cxFeedbackTemplates[0]);

			var chosenNode = $('#feedbackTemplateSelector');
        	chosenNode.chosen({
                width: "100%",
                no_results_text: "Oops, nothing found!"
            }).trigger("chosen:updated").change(function(){
            	// console.log("chosen:updated" + $(this).val())
            	var index = parseInt($(this).val());
            	var item = cxFeedbackTemplates[index];
            	initEmbeddedFeedbackForm(leoObserverId, idDialog, item);
            });
            
            // checkbox change to selection list change, to refresh cx_qrFormURL and __feedbackFormFullUrl__
            $('#leo_survey_in_shared_devices').change(function(){
				chosenNode.trigger("change");
			});
		}
    	loadFeedbackTemplateItems(leoObserverId, callback)
	} 
	else {
		notifyErrorMessage("Can not get feedback code for the touchpoint hub type = " + type )
	}
}
	
const getWebLeadFormJsCode =  function(btn) {
	var json = JSON.parse(decodeURIComponent($(btn).data('json')));

	var leoObserverId = json.id;
	var touchpointHubName = json.name;
	var type = json.type;
	
	if(type > 0 && type < 12){
		$('#webLeadFormCodeDialog').modal({ backdrop: 'static', keyboard: false });
		
		//init code preview editor
		$('#code_holder .CodeMirror').remove();
		var codeNode = $('#webLeadFormCodeDialog .code')[0];
		var editor = CodeMirror.fromTextArea(codeNode, {
			mode:  "htmlmixed",
			lineNumbers: true,
            styleActiveLine: true,
            matchBrackets: true,
            readOnly: true,
	    });
		editor.setSize(null, 480);
		setTimeout(function() {
			//copy from textarea template into editor, then selectAll for copy
			var tpl = $('#webLeadFormCodeDialog .code').val().trim();
			var code = tpl.replace('__leoObserverId__', leoObserverId)
			code = code.replace('__tpHubName__', touchpointHubName)
			code = code.replace('__LeoObserverDomain__', baseLeoObserverDomain);
			editor.getDoc().setValue( '<script>\n' + code + '\n<\/script>' )
			editor.refresh();
			editor.execCommand('selectAll')
		},350);
	}
}
	
const getQrImageFormCode =  function(btn) {
	var jsonStr = decodeURIComponent($(btn).data('json'));
	
	var json = JSON.parse(jsonStr);
	console.log("getQrImageFormCode ",json)
	
	var leoObserverId = json.id;
	var touchpointHubName = json.name;
	var type = json.type;
	var qrCodeData = json.qrCodeData;
	var staticFileDomain = window.baseStaticDomain;
	var baseUploadDomain = window.baseUploadDomain;
	
	$('#qrImageFormCodeDialog').modal({
		backdrop: 'static',
		keyboard: false
	});
	
	var qrImageList = '';
	var landingPageUrl = qrCodeData.landingPageUrl;
	var qrCodeImage = qrCodeData.qrCodeImage;
	var trackingUrl = qrCodeData.trackingUrl;
	var shortUrl = qrCodeData.shortUrl || trackingUrl.replace('/qrct/','/ct/');
	var imageUrl = 'https://' + baseUploadDomain + qrCodeImage.replace('./','/');
	$('#qrHrefUrl').attr("href",imageUrl);
	$('#qrImgSrc').attr("src",imageUrl);
	
	$('#qrTouchpointHubURL').val(landingPageUrl);
	$('#qrShortURL').val(shortUrl);
	$('#qrCodeImageURL').val(imageUrl);
}
	
const getPersonalizeContentCode = function(btn) {
	$('#itemTypePersonalization').text('content')
	getPersonalizationCode(btn, 'contents')
}

const getPersonalizeProductCode = function(btn) {
	$('#itemTypePersonalization').text('product')
	getPersonalizationCode(btn, 'products')
}

const getEmbeddedChatbotJsCode = function(btn) {
	// TODO
}

const getPersonalizationCode =  function(btn, itemType) {
	var json = JSON.parse(decodeURIComponent($(btn).data('json')));
	var leoObserverId = json.id;
	var touchpointHubName = json.name;
	var type = json.type;
	
	var idDialog = "dialogEmbeddedRecomender";
	var editorSelector = '#'+idDialog+' .code';
	if(type > 0 && type < 12){
		
		$('#'+idDialog).modal({
    		backdrop: 'static',
    		keyboard: false
		});
		
		//init code preview editor
		$('#'+idDialog+' .CodeMirror').remove();
		var codeNode = $(editorSelector)[0];
		var editor = CodeMirror.fromTextArea(codeNode, {
			mode:  "htmlmixed",
			lineNumbers: true,
            styleActiveLine: true,
            matchBrackets: true,
            readOnly: false,
	    });
		editor.setSize(null, 480);
		setTimeout(function() {
			//copy from textarea template into editor, then selectAll for copy
			var tpl = $(editorSelector).val().trim();
			var personalizationUrl = 'https://' + baseLeoObserverDomain +'/ris/html/' + itemType;
			
			var code = tpl.replace('__observerId__', leoObserverId);
			code = code.replace('__tpHubName__', touchpointHubName);
			code = code.replace('__personalizationUrl__', personalizationUrl);
			
			var scriptId = 'leo_recommender_'+leoObserverId;
			editor.getDoc().setValue( '<script id="'+scriptId+'">\n' + code + '\n<\/script>' )
			editor.refresh();
			editor.execCommand('selectAll');
			
			// copy code into clipboard
			var buttonSelector = '#'+idDialog+ ' button.btn-copy-code';
			addHandlerCopyCodeButton(editorSelector, buttonSelector);
		},500);
		
	}
}
	
const showRedisSourceInfo = function(btn){
	var jsonStr = decodeURIComponent($(btn).data('json'));
	
	var json = JSON.parse(jsonStr);
	
	var leoObserverId = json.id;
	var touchpointHubName = json.name;
	var type = json.type;
	var dataSourceUrl = json.dataSourceUrl;
	var toks = dataSourceUrl.split("/");

	var hostAndPort = toks[2];
	var touchpointName = toks[3];
	
	$('#dataSourceConfigHostAndPort').text(hostAndPort)
	$('#dataSourceConfigTouchpointName').text(touchpointName)
	
	$('#redisSourceInfoDialog').modal({ backdrop: 'static', keyboard: false });
}
	
const showObserverApiInfo = function(btn){
	var jsonStr = decodeURIComponent($(btn).data('json'));	
	var json = JSON.parse(jsonStr);
	console.log("showObserverApiInfo", json)
	
	var apiKey = json.id;
	if(apiKey === "leo_data_observer"){
		apiKey = 'default_access_key';
		$('#leoCdpApiTokenKey').val("default_access_key")
	}
	else {
		$('#leoCdpApiTokenKey').val(apiKey)
	} 	
	
	var apiToken = (typeof json.accessTokens[apiKey] === "string") ? json.accessTokens[apiKey] : "";
	$('#leoCdpApiTokenValue').val(apiToken);
	
	$('#leoCdpApiObserverName').text(json.name);
	$('#leoCdpGetProfileApiUrl').val(baseLeoAdminUrl + "/api/profile/save");
	$('#leoCdpTrackEventApiUrl').val(baseLeoAdminUrl + "/api/event/save");
	$('#leoCdpListEventsApiUrl').val(baseLeoAdminUrl + "/api/event/list?startIndex=0&numberResult=10&profileId={profile_ID}");
	$('#leoCdpWebhookUrl').val(baseLeoObserverUrl + "/webhook?tokenkey=" + apiKey +"&tokenvalue=" + apiToken +"&source=");
	
	$('#observerApiInfoDialog').modal({
		backdrop: 'static',
		keyboard: false
	});
}
	
const resetAccessTokenForLeoObserverApi = function(){
	var msg = 'Would you like to reset the API access token ?';
	if (confirm(msg)) {
		var observerId = $('#leoCdpApiTokenKey').val();
		var urlStr = baseLeoAdminUrl + '/cdp/touchpointhub/reset-access-token';
	    LeoAdminApiUtil.callPostAdminApi(urlStr, {'observerId': observerId}, function (json) {
	         if (json.httpCode === 0 && json.errorMessage === '') {
	        	 var map = json.data;
	        	 $('#leoCdpApiTokenKey').val(map.tokenKey)
	     		 $('#leoCdpApiTokenValue').val(map.tokenValue);
	        	 iziToast.success({title: 'Reset API Access Token', message: "The new API access token is generated successfully!"});
	         }
	    });
	}
}

const initDataObserverJsGridPlugin = function() {
	var LeoDataObserver = function(config) {
	    jsGrid.Field.call(this, config);
	};
	LeoDataObserver.prototype = new jsGrid.Field({
	    sorter: function(date1, date2) {
	        return 0;
	    },
	
	    itemTemplate: function(observerModel) {
	    	if(observerModel.data !== '') {
	    		console.log("observerModel ", observerModel)
	    		var jsonStr = observerModel.data;
	    		var tpHubType = observerModel.type;
	    		var firstPartyData = observerModel.firstPartyData;
	    		
	    		var btnHtml = "";
	    		if(tpHubType === 30){
	    			btnHtml += '<div style="margin:68px 0;"> <button style="width:144px!important;" type="button" class="btn btn-do-now btn-get-code" onclick="showObserverApiInfo(this)"  ';
			    	btnHtml += (' data-json="' + jsonStr  + '" ' );
			    	btnHtml += '><i class="fa fa-info-circle" style="font-size:1.1em" aria-hidden="true"></i> API & Webhook </button> </div> ';
			    	return btnHtml;
	    		}
	    		else if(tpHubType === 31) {
					btnHtml += '<div style="margin:20px 0;"> <button type="button" class="btn btn-do-now btn-get-code" onclick="showRedisSourceInfo(this)" ';
			    	btnHtml += (' data-json="' + jsonStr  + '" ' );
			    	btnHtml += '><i class="fa fa-info-circle" style="font-size:1.1em" aria-hidden="true"></i> Redis Information </button> </div>';
			    	return btnHtml;
				}
				else if(tpHubType === 32) {
					btnHtml += '<div style="margin:20px 0;"> <button type="button" class="btn btn-do-now btn-get-code" onclick="showKafkaSourceInfo(this)" ';
			    	btnHtml += (' data-json="' + jsonStr  + '" ' );
			    	btnHtml += '><i class="fa fa-info-circle" style="font-size:1.1em" aria-hidden="true"></i> Kafka Information </button> </div>';
			    	return btnHtml;
				}
				else if(tpHubType === 33) {
					btnHtml += '<div style="margin:20px 0;"> <button type="button" class="btn btn-do-now btn-get-code" onclick="getQrImageFormCode(this)" ';
					btnHtml += (' data-json="' + jsonStr  + '" ' );
			    	btnHtml += '><i class="fa fa-qrcode" style="font-size:1.1em" aria-hidden="true"></i> Get QR Code </button> </div>';
			    	return btnHtml;
				}
	    		else {
	    			return loadObserverActionButtons(firstPartyData, tpHubType, jsonStr);
	    		}
	    	}
	    	return "";
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
	jsGrid.fields.LeoDataObserver = LeoDataObserver;
}

const loadObserverActionButtons = function(firstPartyData, tpHubType, jsonStr) {
	var html = '<div class="btn-group" > <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown"> <i class="fa fa-code" style="font-size:1.1em" aria-hidden="true"></i> Get Code</button>';
	html += '<button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown"> <span class="caret"></span> </button>';
	html += '<ul class="dropdown-menu" role="menu">';
	
	if(firstPartyData) {
		html += '<li> <button type="button" class="btn btn-do-now btn-sm btn-get-code" onclick="getQrImageFormCode(this)" ';
		html += (' data-json="' + jsonStr  + '" ' );
		html += '><i class="fa fa-qrcode" style="font-size:1.1em" aria-hidden="true"></i>  QR Image Code </button> </li>';
		
		if(tpHubType > 0 && tpHubType < 12) {
			// event tracking
			html += '<li> <button type="button" class="btn btn-do-now  btn-sm btn-get-code" onclick="getTrackingJsCode(this)" ';
			html += (' data-json="' + jsonStr  + '" ' );
			html += '><i class="fa fa-code" style="font-size:1.1em" aria-hidden="true"></i> Event Tracking Code </button> </li>';
			
			// feedback / survey
			html += '<li> <button type="button" class="btn btn-do-now btn-sm btn-get-code" onclick="getCxFeedbackJsCode(this)" ';
			html += (' data-json="' + jsonStr  + '" ' );
			html += '><i class="fa fa-code" style="font-size:1.1em" aria-hidden="true"></i> Survey Rating Forms </button> </li>';
	    	
			// content
			html += '<li> <button type="button" class="btn btn-do-now  btn-sm btn-get-code" onclick="getPersonalizeContentCode(this)" ';
			html += (' data-json="' + jsonStr  + '" ' );
			html += '><i class="fa fa-code" style="font-size:1.1em" aria-hidden="true"></i> Content Personalization </button> </li>';
			
			// product
			html += '<li> <button type="button" class="btn btn-do-now  btn-sm btn-get-code" onclick="getPersonalizeProductCode(this)" ';
			html += (' data-json="' + jsonStr  + '" ' );
			html += '><i class="fa fa-code" style="font-size:1.1em" aria-hidden="true"></i> Product Personalization  </button> </li>';
			
			// embedded chatbot
			html += '<li> <button type="button" class="btn btn-do-now btn-sm btn-get-code" onclick="getEmbeddedChatbotJsCode(this)" ';
			html += (' data-json="' + jsonStr  + '" ' );
			html += '><i class="fa fa-code" style="font-size:1.1em" aria-hidden="true"></i> Embedded Chatbot </button> </li>';
		} 
		else {
			html += '<li> <button type="button" class="btn btn-do-now btn-sm btn-get-code" onclick="getCxFeedbackJsCode(this)" ';
			html += (' data-json="' + jsonStr  + '" ' );
			html += '><i class="fa fa-code" style="font-size:1.1em" aria-hidden="true"></i> Survey Rating Forms </button> </li>';
		}
	} else {
		html += '<li> <button type="button" class="btn btn-do-now btn-sm btn-get-code" onclick="getQrImageFormCode(this)"';
		html += (' data-json="' + jsonStr  + '" ' );
		html += '><i class="fa fa-qrcode" style="font-size:1.1em" aria-hidden="true"></i> QR Image Code </button> </li>';
	}
	
	html += '<li> <button type="button" class="btn btn-sm btn-do-now btn-get-code" onclick="showObserverApiInfo(this)" ';
	html += (' data-json="' + jsonStr  + '" ' );
	html += '><i class="fa fa-info-circle" style="font-size:1.1em" aria-hidden="true"></i> API & Webhook </button> </li> ';
	
	html += '  </ul> </div>';
	return html;
}

const showDataObserverTable = function(json){
	var observerTableSelector = "#data_observers_table";
	
	var eventObservers = json.data.eventObservers;
	var touchpointHubTypes = json.data.touchpointHubTypes;
	
	for(var i=0; i< eventObservers.length; i++ ){
		var eventObserver = eventObservers[i];
		eventObserver.index = i + 1;
		var typeName = touchpointHubTypes[eventObserver.type];
		eventObserver.typeName = typeName;

		var firstPartyData = eventObserver.firstPartyData;
		var touchpointHubType = eventObserver.type;
	
		var jsonObj = {id: eventObserver.id, name : eventObserver.name, type : touchpointHubType, typeName : typeName };
		jsonObj['collectDirectly'] = eventObserver.collectDirectly;
		jsonObj['qrCodeData'] = eventObserver.qrCodeData;
		jsonObj['accessTokens'] = eventObserver.accessTokens;
		jsonObj['securityCode'] = eventObserver.securityCode;
		jsonObj['dataSourceUrl'] = eventObserver.dataSourceUrl;
		jsonObj['javascriptTags'] = eventObserver.javascriptTags;
		var jsonStr = JSON.stringify(jsonObj);
		eventObserver.observerModel = {type : touchpointHubType, data : encodeURIComponent(jsonStr), firstPartyData :firstPartyData};
	}
	
	$(observerTableSelector).empty().jsGrid({
		data: eventObservers,
	    width: "100%",
	    height: "auto",
	    scroll: true,
	    inserting: false,
	    editing: false,
	    sorting: false,
	    paging: false,
		onRefreshed: function() {
			$(".jsgrid-grid-body").css("min-height", "380px"); 
			console.log("showDataObserverTable length " + eventObservers.length)
		},
	    fields: [
			{ name: "journeyLevel", title : "Level", type: "select", items: getJourneyLevelList(), valueField: "Id", textField: "Name", width: 24 },
			{ name: "name", title : "Name", type: "string",  align: "center" , css:"observer_name" },   		
	    	{ name: "id", title : "Unique ID", type: "string" , width: 68,  align: "center", css:"observer_id"},   		             	
	     	{ name: "firstPartyData", title : "First-party Data", type: "CheckBoxIcon", width: 40, align: "center" },
	     	{ name: "collectDirectly", title : "Tracking directly", type: "CheckBoxIcon", width: 40, align: "center" },
	     	{ name: "dataSourceUrl", title : "URL", type: "UrlLink", width: 120, align: "center", css:"observer_url" },
	     	{ name: "observerModel", title : "Actions", type: "LeoDataObserver", width: 72, align: "center"}
	    ]
	}).jsGrid("sort", "journeyLevel");
}


const initDataObserverList = function() {
	// plugin
	initDataObserverJsGridPlugin();

	$('#txt_filter_keywords_observers').keypress(function(event){
	  	var keycode = (event.keyCode ? event.keyCode : event.which);
	  	if(keycode == '13'){
			var keywords = $(this).val().trim();
			if(keywords.length > 0) {
				filterDataObserverList(keywords)
			}
			else {
				loadDataObserverList()
			}
	  	}
	});
	
	loadDataObserverList();
}

const loadDataObserverList = function() {
	$('#txt_filter_keywords_observers').val('')
	LeoAdminApiUtil.getSecuredData('/cdp/observers', {"journeyMapId": currentJourneyMapId } , showDataObserverTable )
} 

const filterDataObserverList = function(filterKeywords) {
	LeoAdminApiUtil.getSecuredData('/cdp/observers', {"journeyMapId": currentJourneyMapId, 'filterKeywords' : filterKeywords } , showDataObserverTable )
}

const loadDataJourneyMapsByFilter = function(rowSelectHandlerName, containerDomSel, loaderDomSel, tableDomSel, filterParams,viewableJourneyMaps, editableJourneyMaps) {
    var collumnList = [
       	{
           "data": "name" // 0
       	},
       	{
            "data": "authorizedViewers" // 1
      	},
      	{
            "data": "authorizedEditors" // 2
      	},
       	{
            "data": "createdAt" // 3
      	},
       	{
            "data": "updatedAt" // 4
      	},
       	{
           "data": "defaultMetricName" // 5
       	}
   	];
    
    var usersession = getUserSession();
    if (usersession) {
        var obj = $(tableDomSel).DataTable({
        	"lengthMenu": [[20, 30, 40], [20, 30, 40]],
        	'processing': true,
            'serverSide': true,
            'searching': true,
            'search': {
                return: true,
            },
            'ordering': true,
            'serverMethod': 'POST',
            'ajax': {
                url: baseLeoAdminUrl + "/cdp/journeymaps/filter",
                contentType: 'application/json',
                beforeSend: function (request) {
                	$(loaderDomSel).show();
                	$(containerDomSel).hide();
                    request.setRequestHeader("leouss", usersession);
                },
                data: function (d) {
                	var order = d.order[0];
                	d.sortField = collumnList[order.column].data;
                	d.sortAsc = order.dir === "asc" ;
                	d.searchValue = d.search.value;
                	delete d.order; delete d.search;
                	
                	if(typeof filterParams === "function"){
                		d = filterParams(d);
                		$(tableDomSel).data('authorizedViewer', d.authorizedViewer);
                		$(tableDomSel).data('authorizedEditor', d.authorizedEditor);
                	}
                    return JSON.stringify(d);
                },
                dataSrc: function ( json ) {
                	var canInsertData = json.canInsertData;
		 	    	var canEditData = json.canEditData;
		     		var canDeleteData = json.canDeleteData;
		     		
                	$(tableDomSel).data('canInsertData', canInsertData)
		     		$(tableDomSel).data('canEditData', canEditData)
		     		$(tableDomSel).data('canDeleteData', canDeleteData)
		     		
		     		if(typeof loadDataCallback === "function"){
						loadDataCallback(json)
					}
            		
            		// done show UI
            		$(loaderDomSel).hide();
                	$(containerDomSel).show();
                	
            		return json.data;
                 }
            },
            'columnDefs': [
            	{
                    "render": function (data, type, row) {
                        return '<div class="highlight_text text-center" title="' + data + '" >' + textTruncate(data, 150) + '</div>';
                    },
                    "targets": 0,
                    "orderable": false
                },
                {
                    "render": function (data, type, row) {
                    	var authorizedViewer = $(tableDomSel).data('authorizedViewer');
                    	var checked = data.includes(authorizedViewer);
                    	var id = row.id + "-viewer";
                    	if(typeof viewableJourneyMaps === "object") viewableJourneyMaps[row.id] = checked;
                      	var html = getTableRowCheckedBox(rowSelectHandlerName, tableDomSel, id, checked);
                        return html;
                    },
                    "targets": 1
                },
                {
                    "render": function (data, type, row) {
                    	var authorizedEditor = $(tableDomSel).data('authorizedEditor');
                    	var checked = data.includes(authorizedEditor);
                    	var id = row.id + "-editor";
                    	if(typeof editableJourneyMaps === "object") editableJourneyMaps[row.id] = checked;
                      	var html = getTableRowCheckedBox(rowSelectHandlerName, tableDomSel, id, checked);
                        return html;
                    },
                    "targets": 2
                },
                {
                    "render": function (data, type, row) {
                        var date = moment.utc(data).local().format('YYYY-MM-DD HH:mm:ss');
                        return '<div class="small text-center" style="color:#3300ff;" >'  + date + '</div>';
                    },
                    "targets": 3
                },
                {
                    "render": function (data, type, row) {
                        var date = moment.utc(data).local().format('YYYY-MM-DD HH:mm:ss');
                        return '<div class="small text-center" style="color:#3300ff;" >'  + date + '</div>';
                    },
                    "targets": 4
                },
                {
                    "render": function (data, type, row) {
                        return '<div class=" text-center"  >' +  data + '</div>';
                    },
                    "targets": 5
                },
                {
                    "render": function (data, type, row) {
                        var html = '';
                        var callJsViewStr = "#calljs-leoCdpRouter('Data_Journey_Map','" + row.id + "')";
                        html += '<a class="control" title="Segment Report" href="' + callJsViewStr + '" > <i class="fa fa-info-circle" aria-hidden="true"></i> View</a>';
                        return html;
                    },
                    "targets": 6
                }
            ],
            'columns': collumnList
        });
        return obj;
    }
}

var loadJourneyMapEventMatrix = function(journeyMapId, containerId, beginFilterDate, endFilterDate, callback){
	var queryFilter = {'journeyMapId' : journeyMapId, 'beginFilterDate': beginFilterDate || "", 'endFilterDate': endFilterDate || ""};	    	
	var urlStr = baseLeoAdminUrl + '/cdp/analytics360/event-matrix';
    LeoAdminApiUtil.getSecuredData(urlStr, queryFilter , function (json) {
    	var data = json.data;
        if (typeof data === "object") {
        	renderMatrixChart('Event Matrix Report', containerId, data.xLabels, data.yLabels, data.dataList)
        	if(typeof callback ==="function"){
				callback(data)
			}
        } else {
            console.error(json);
        }
    });
}

const loadJourneyTouchpointHubReport = function(journeyMapId, maxTouchpointHubSize){
	var containerId = 'journeyObserverChart';
    $('#'+containerId).html('<div class="loader"></div>');
    var params = {"journeyMapId": journeyMapId, "maxTouchpointHubSize" : maxTouchpointHubSize}
    var urlStr = baseLeoAdminUrl + '/cdp/analytics360/touchpoint-hub-report';
    LeoAdminApiUtil.getSecuredData(urlStr, params, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
        	var list = json.data;
        	if(list.length > 0){
        		var labels = [];
        	    var dataList = [];
        	    list.forEach(function(e){
        	    	labels.push(e.name);
        	    	dataList.push(e.eventCount);
        	    })
        	    renderHorizontalBarChart(containerId, labels, dataList)
        	}
        	else {
        		$("#" + containerId).html('<div class="alert alert-info" role="alert"> No data available </h4>')
        	}
        	
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}


function showJourneyFlowReport(queryFilter, containerId) {
	var containerNode = $('#'+containerId).empty().html('<div class="loader"></div>');
	var urlStr = baseLeoAdminUrl + '/cdp/analytics360/touchpoint-flow-report';
	queryFilter['startIndex'] = 0;
	queryFilter["numberFlow"] = 200; // number of flow edges
	LeoAdminApiUtil.getSecuredData(urlStr, queryFilter , function (json) {    	
        if (json.httpCode === 0 && json.errorMessage === '') {
        	containerNode.empty()
        	var graphElements = { nodes: json.data.nodes, edges: json.data.edges }
        	var rootNodeId = json.data.rootNodeId;
        	flowNetworkGraph = renderDirectedGraph(containerId, graphElements, rootNodeId);
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}

function setJourneyFlowGraphView(){
	if(typeof flowNetworkGraph === 'object') {
		// hack to show graph
		setTimeout(function(){
			flowNetworkGraph.fit(true)
		},300)
	}
}



// DEMO to load and test the report as a graph
function loadJourneyFlowReportDemo(){
	const MAX_ZOOM = 5;
	const MIN_ZOOM = 0.5;
	var avgPageview = 20;
	var avgPurchase = 7;
	var bfLayout = { name: 'breadthfirst', fit: true, directed: true,  padding: 30 , spacingFactor: 1.8};
	var dgLayout =  { name: 'dagre', rankDir: "LR", fit: true, directed: true,  padding: 50, spacingFactor: 1.9, nodeSep: 60 , rankSep: 30 }
	
	var cy = cytoscape({
	  container: document.getElementById('journeyFlowReport'),

	  boxSelectionEnabled: false,
	  autounselectify: true,
	  minZoom: MIN_ZOOM,
	  maxZoom: MAX_ZOOM,

	  style: cytoscape.stylesheet()
	    .selector('node')
	      .css({
	        'height': 80,
	        'width': 80,
	        'background-fit': 'cover',
	        'border-color': '#000',
	        'border-width': 3,
	        'border-opacity': 0.5
	      })
	    .selector('edge')
	      .css({
	    	'curve-style': 'unbundled-bezier',
			'width': 4,
			'target-arrow-shape': 'triangle',
			'line-color': '#ffaaaa',
			'target-arrow-color': '#ffaaaa'
	      })
	    .selector('#youtube')
	      .css({
	        'background-image': 'https://cdn-icons-png.flaticon.com/128/3670/3670147.png'
	      })
	    .selector('#tiktok')
	      .css({
	        'background-image': 'https://cdn-icons-png.flaticon.com/128/3669/3669950.png'
	      }) 
	    .selector('#google')
	      .css({
	        'background-image': 'https://cdn-icons-png.flaticon.com/128/281/281764.png'
	      })
	    .selector('#website')
	      .css({
	        'background-image': 'https://cdn-icons-png.flaticon.com/128/1927/1927746.png'
	      })
	  .selector('#sen_spa')
	      .css({
	        'background-image': 'https://cdn-icons-png.flaticon.com/128/3649/3649531.png'
	      })
	  .selector('#customer_service')
	      .css({
	        'background-image': 'https://cdn-icons-png.flaticon.com/128/1028/1028011.png'
	      })
	  .selector('#facebook')
	      .css({
	        'background-image': 'https://cdn-icons-png.flaticon.com/128/5968/5968764.png'
	      })
	  .selector('#customer_lead')
	      .css({
	        'background-image': 'https://cdn-icons-png.flaticon.com/128/950/950237.png'
	      })
	  .selector('#engaged_customer')
	      .css({
	        'background-image': 'https://cdn-icons-png.flaticon.com/128/4149/4149882.png'
	  })
	  .selector('edge[weight>0]').style({
		 	'label': 'data(weight)',
	  	 	'background-color': '#61bffc',  
			'font-size': 20,
			'width': 5,
			'line-color': '#9BB6FB',
			'target-arrow-color': '#0048FF'
	    })
	  .selector('edge[weight >= '+avgPageview+']').style({
			'line-color': '#618CF9',
	    	'target-arrow-color': '#0048FF',
	    	'width': 7
	   	})
	  .selector('edge[weight >= '+avgPurchase+']').style({
	    	'line-color': '#F3A06D',
	    	'target-arrow-color': '#F91805',
	    	'width': 9
	   	})
	  .selector('edge[weight=0]').style({
			'background-color': '#61bffc',  
			'font-size': 20,
			'line-color': '#9BB6FB',
			'target-arrow-color': '#0048FF'
	   	})
	  .selector('node[type="cdp_profile"]').style({
		 	'shape': 'round-rectangle', 'padding': '6px', 'line-color': '#F3A06D'
	   	}),

	  elements: {
	    nodes: [
	      { data: { id: 'google', type: "" } },
	      { data: { id: 'youtube' } },
	      { data: { id: 'tiktok' } },
	      { data: { id: 'website' } },
	      { data: { id: 'sen_spa' } },
	      { data: { id: 'customer_service' } },
	      { data: { id: 'facebook' } },
	      { data: { id: 'customer_lead' , type: "cdp_profile" } },
	      { data: { id: 'engaged_customer' , type: "cdp_profile" } }
	    ],
	    edges: [
	      { data: { source: 'google', target: 'youtube', weight : 0  } },
	      { data: { source: 'google', target: 'tiktok', weight : 0  } },
	      { data: { source: 'google', target: 'website', weight : 31  } },
	      { data: { source: 'tiktok', target: 'website', weight : 27 } },
	      { data: { source: 'youtube', target: 'website', weight : 31 } },
	      { data: { source: 'youtube', target: 'facebook', weight : 43 } },
	      { data: { source: 'website', target: 'sen_spa', weight : 12 } },
	      { data: { source: 'engaged_customer', target: 'customer_service', weight : 3 } },
	      { data: { source: 'facebook', target: 'customer_lead', weight : 12 } },
	      { data: { source: 'customer_lead', target: 'website', weight : 6 } },
	      { data: { source: 'customer_lead', target: 'sen_spa', weight : 9 } },
	      { data: { source: 'website', target: 'engaged_customer', weight : 21 } },
	      { data: { source: 'sen_spa', target: 'engaged_customer', weight : 11 } }
	    ]
	  },

	  // set layout of graph 
	  layout: dgLayout 
	}); // cy init
	
	initPanZoomController(MIN_ZOOM, MAX_ZOOM, cy)
	
	cy.nodeHtmlLabel([
	  {
	    query: 'node', // cytoscape query selector
	    halign: 'center', // title vertical position. Can be 'left',''center, 'right'
	    valign: 'top', // title vertical position. Can be 'top',''center, 'bottom'
	    halignBox: 'center', // title vertical position. Can be 'left',''center, 'right'
	    valignBox: 'top', // title relative box vertical position. Can be 'top',''center, 'bottom'
	    cssClass: '', // any classes will be as attribute of <div> container for every title
	    tpl(data) {
	    	var name = data.id.toUpperCase();
	    	if(data.type === "cdp_profile") {
	    		return '<h4 class="cy_node_profile" > ' + name + '</h4>'; 
	    	}
	    	else {
	    		return '<h4 class="cy_node_touchpoint" > ' + name + '</h4>'; 
	    	}
	    }
	  }
	]);
	
	cy.fit(50);
}

const loadBehavioralEventList = function(){
	var profileFunnelOptions = {
			block: {
   	   			highlight: true,
	   	   		fill: {
	                type: 'gradient'
	            }
   	   	    },
	        chart: {
	        	curve: { enabled: true, height: 36 },
	            height: 360,
	            bottomWidth : 0.57,
	            bottomPinch: 2
	        },
	        tooltip: {
	            enabled: true
	        },
	        label: {
	            fontSize: '14px'
	        },
	        events: {
	            click: {
	                block: function (data) {
	                    console.log(data.label.raw);
	                }
	            }
	        }
	};

    var saveEventMetricMetaData = function(item){
		var callback = function(json){
    		if(json.data != ""){
    			location.reload(true);
    		}
    		else {
    			console.error('saveEventMetricMetaData ', rs)
    		}
    	};
		var params = {'eventMetricJson' : JSON.stringify(item) };
    	LeoAdminApiUtil.callPostAdminApi('/cdp/funnel/event-metric/save', params, function (json) {
            if (json.httpCode === 0 && json.errorMessage === '') {
    			callback(json);
            } else {
                LeoAdminApiUtil.logErrorPayload(json);
            }
       });
	}
    
    var deleteEventMetricMetaData = function(eventName) {
	 	$('#delete_callback').val('');
    	$('#confirmDeleteDialog').modal({ focus: true });
    	if (typeof eventName === "string" ) {
    	     var callback = "deleteEventMetricMetaData" + eventName;
    	     window[callback] = function () {
    	    	var params = {'eventName' : eventName};
   		    	LeoAdminApiUtil.callPostAdminApi('/cdp/funnel/event-metric/delete', params, function (json) {
   		            if (json.httpCode === 0 && json.errorMessage === '') {
   		            	setTimeout(function(){
   		            		location.reload(true);
   		            	}, 700)
   		            } else {
   		                LeoAdminApiUtil.logErrorPayload(json);
   		            }
   		       });
    	     }
    	     $('#delete_callback').val(callback);
    	     $('#deletedInfoTitle').text("Event Metric: " + eventName).show();
    	     $('#deletedInfoMsg').text('Delete Data Confirmation');
    	}
	}
    
    LeoAdminApiUtil.getSecuredData('/cdp/funnel/metadata', {}, function(json){ 
		 //console.log(json.data);	
		 var canInsertData = json.canInsertData; 
    	 var canEditData = json.canEditData;
		 var canDeleteData = json.canDeleteData;
		
		 if( ! canEditData ){
			$('button.data-control').attr('disabled','disabled');
		 }
		
		 behavioralMetricsData = json.data['behavioral_metrics'];
		 var currentFlowName = behavioralMetricsData[0] ? behavioralMetricsData[0].flowName : "general_business_event";
		 
		 customerFunnelStages = json.data['general_data_funnel']; 
		 
		 renderFunnelChart("#profile_funnel",customerFunnelStages, profileFunnelOptions, getColorCodeProfileFunnel)
		 
		 // TODO
		 var insertItemReady = false, insertedIndex = -1;
		 var gridControls =  { 
			type: "control", width: 70, editButton: true, deleteButton: true, itemTemplate: function(value, item) {
                var $result = jsGrid.fields.control.prototype.itemTemplate.apply(this, arguments);
                
                var isSystemMetric = item.systemMetric;
                var eventName = item.eventName;
                var setRuleIcon, setRulesLabel;
                setRuleIcon = '<i class="fa fa-cogs event-set-rules" style="" aria-hidden="true"></i>';
                setRulesLabel = 'Edit Event Name: ' + eventName;
                
                var $setRulesButton = $("<button>").data("eventName",eventName).attr({class: "jsgrid-custom-button", title : setRulesLabel}).html(setRuleIcon)
                .click(function(e) {
                	console.log("setRules button ", $(this).data('eventName'));
                	
                	underDevelopment(this);
                    e.stopPropagation();
                });       
                
                var cssClass = "customGridEditbutton jsgrid-button jsgrid-edit-button";
                var $customEditButton = $("<button>").data("eventName",eventName).attr({class: cssClass,title:"Edit Event Metric"})
                .click(function(e) {
                	//underDevelopment(this);
                    //e.stopPropagation();
                    
                    console.log("Edit button ", $(this).data('eventName'))
                });

                var cssClass = "customGridDeletebutton jsgrid-button jsgrid-delete-button";
               	var $customDeleteButton = $("<button>").data("eventName",eventName).attr({class: cssClass,title:"Delete Event Metric"})
               		.click(function(e) {
                   		var eventName = $(this).data('eventName');
                   		if( isSystemMetric ){
                   			iziToast.info({
    		    	    	    title: 'Information Box',
    		    	    	    message: 'The event <b>[' + eventName + ']</b> is the <b>event metric of system</b>. You can not delete this metric !'
    		    	    	});
                   		} else {
                       		deleteEventMetricMetaData(eventName)
                   		}
                   		
                      	e.stopPropagation();
                	});

               	var rs = $("<div>");
               	if(! isSystemMetric ) {
               		rs.addClass("editable_event_metric");
               		rs.attr("title","Click to edit or delete")
               	} else {
               		rs.attr("title","Event metric of system")
               	}
               	rs.append($customEditButton).append($customDeleteButton);
               	
               	return rs;
             }
         };
		 
		 $("#event_metrics_table").jsGrid({
			    width: "100%",
			    height: "auto",

			    inserting: canInsertData,
			    confirmDeleting : canDeleteData,
			    editing: true,
			    sorting: false,
			    paging: false,
			    data: behavioralMetricsData,
	                	
			    deleteConfirm: function(item) {
			    	if(item.systemMetric) {
			    		return "The event metric <b>[" + item.eventName + "]</b> is the <b>event metric of system</b>. You can not delete this metric.";
				    }
		            return "The event metric [" + item.eventName + "] will be removed. Are you sure ? ";
		        },

		        onItemInserting: function(args) {
		        	$('tr > td.jsgrid-cell:nth-of-type(1)').each(function(){
		        		var eventName = $(this).text().trim();
		        		if(eventName === args.item.eventName) {
		        			args.cancel = true;
				            iziToast.info({
			    	    	    title: 'Info',
			    	    	    message: 'The event [' + eventName +  '] is already existed in the metric list!'
			    	    	});
		        		}
		        	})
	   		    	// default for new inserting item
	   		    	args.item.systemMetric = false;
	   		    	args.item.dataType = 1;
	   		    	args.item.flowName = currentFlowName;
	   		    },
			   
			    onItemInserted: function(args) {
			    	var item = args.item;
			    	if(item.eventName.trim() === "" || item.eventLabel.trim() === "") {
			    		 args.cancel = true;
			    	}
			    	else {
			    		saveEventMetricMetaData(args.item)
			    	}
			    }, 
			    
			    onItemEditing: function(args) {
			    	if(args.item.systemMetric) {
				    	iziToast.info({
		    	    	    title: 'Information Box',
		    	    	    message: 'The event <b>[' + args.item.eventName +  ']</b> is the event metric of system. Be careful when you edit this metric !',
		    	    	    timeout: 5000
		    	    	});
			    	}
			    },
			    
			    onItemUpdated: function(args) {
			    	saveEventMetricMetaData(args.item)
			    }, 
			    
			    onItemDeleting: function(args) {
			        if(args.item.systemMetric) {
			            args.cancel = true;
			        }
			    },
			    
			    onRefreshed: function (args) {
			    	$('input.jsgrid-insert-mode-button').click(function(){
			        	console.log($('tr.jsgrid-insert-row').find('input[type="number"]'))
			           	$('tr.jsgrid-insert-row').find('input[type="number"]').val(0);
			        })
			    },

			    fields: [
			    	{ name: "eventName", title : "Event Name", align: 'center' , type: "text", validate: "required", validate: "required" },
			        { name: "eventLabel", title : "Event Label", align: 'center' , type: "text", validate: "required" },
			    	{ name: "score", title : "Event Score", type: "number", align: 'center', width: 40, validate: "required" },
			        { name: "cumulativePoint", title : "Loyalty Point", type: "number", width: 44, align: 'center', validate: "required" },
			        { name: "funnelStageId", title : "Default Funnel Stage", type: "select", items: customerFunnelStages, valueField: "id", textField: "name" },
			        { name: "scoreModel", title : "Scoring Model", type: "select", items: scoringModels, width: 80, valueField: "id", textField: "name" },
			    	{ name: "journeyStage", type: "select", title : "CX Journey Stage", items: journeyStages, align: 'center', width: 74, valueField: "id", textField: "name" },
			        { name: "showInObserverJS", title : "Show in JS Code", type: "CustomCheckBox", width: 48, align: "center" },
			        gridControls
			    ]
			});
		 
	});
}
