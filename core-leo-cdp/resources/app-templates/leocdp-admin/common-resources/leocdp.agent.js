// 
var agentStatusReadyHtml = '<i class="fa fa-check-square" aria-hidden="true" style="font-size:1.1em"></i> The service is active';
var dataServiceMap = {};

var renderAgent = function(dataList){
	var holder = $('#agents_holder').empty().show();
	dataServiceMap = {};
	
	_.forEach(dataList,function(e,i) {
		var id = e.id;
		dataServiceMap[id] = e;
		var active = false;
		if(e.status == 1){
			active = true;
		} 
		e
		var boxTpl = _.template( $('#tpl_agent_view').html() );
		holder.append( boxTpl(e) );
		holder.find('i.config_description').tooltip();
		if(active){
			$('#service_box_' + id).addClass('active_service_box');
			$('#service_box_header_' + id).addClass('config_ready');
		}
		// set label
		if(e.status === 1){
			var s = agentStatusReadyHtml;
			if(e.startedAt != null){
				s += (' and working ')
			}
			$('#' + id + '_api_key_label').html(s).addClass('config_ready');
		}
	});
	$('#services_tooltip').tooltip();
}

var dataServiceLoader = function(keywords, filterServiceValue, forPersonalization, forDataEnrichment, forSynchronization, startIndex, numberResult, renderCallback){
	// query params
	var params = {"keywords": keywords, "filterServiceValue" : filterServiceValue, "startIndex": startIndex, "numberResult": numberResult};
	params['forPersonalization'] = forPersonalization;
	params['forDataEnrichment'] = forDataEnrichment;
	params['forSynchronization'] = forSynchronization;
	// call server
	LeoAdminApiUtil.getSecuredData('/cdp/agent/list', params, function(json) { 
		if(typeof renderCallback === "function" && typeof json.data === "object" ) {
    		renderCallback(json.data)
    	}
	});
}

var loadAgents = function(keywords, filterServiceValue){
	$('#agents_pagination').empty();
	$('#agents_loader').show();
	$('#agents_holder').hide();	
	$('#filter_agents_keywords').on('keyup', function (e) {
		// enter to search
        if (e.keyCode === 13) {
        	loadAllAgents()
        }
    });	
	
	var startIndex = 0, numberResult = 10000;	
	var callback = function(data) {
		$('#agents_loader').hide();
		if(data.length > 0){
			$('#agents_pagination').pagination({
				dataSource: data,
				className: "paginationjs paginationjs-big",
			    pageSize: 6,
			    callback: function(dataList, pagination) {
			    	renderAgent(dataList)
			    },
			    beforePageOnClick: function(){
			    	
			    }
			});
		}
		else {
			$('#agents_holder').show().html('<div class="col-lg-12 row highlight_text"> <h3 style="text-align: center;" >No results found !</h3> </div>');
		}
	};	
	var forPersonalization = true, forDataEnrichment = true, forSynchronization = true;
	if(filterServiceValue === 'active_for_personalization'){
		forDataEnrichment = false;
		forSynchronization = false;
	}
	else if(filterServiceValue === 'active_for_enrichment'){
		forPersonalization = false;
		forSynchronization = false;
	}
	else if(filterServiceValue === 'active_for_synchronization'){
		forDataEnrichment = false;
		forPersonalization = false;
	}
	dataServiceLoader(keywords, filterServiceValue, forPersonalization, forDataEnrichment, forSynchronization, startIndex, numberResult, callback);
}

function resetAgentFilter() {
	$('#filter_agents_keywords').val('');
	$('#select_filter_agents').val('all');
	loadAllAgents();
}

function loadAllAgents(){	
	var keywords = $('#filter_agents_keywords').val();
	var filterServiceValue = $('#select_filter_agents').val();
	loadAgents(keywords, filterServiceValue)
}


function saveAgentInfo(node){
	var id = $(node).data('id');
	var data = dataServiceMap[id];
	
	data.name = $('#dataServiceName').val().trim(); 
	data.dagId = $('#dataServiceDagId').val().trim(); 
	data.description = $('#dataServiceDescription').val().trim(); 
	
	data.forDataEnrichment = $('#forDataEnrichment').prop('checked');
	data.forPersonalization = $('#forPersonalization').prop('checked');
	data.forSynchronization = $('#forSynchronization').prop('checked');
	
	var configData = {};
	$('#dataServiceConfigList div.config_item').each(function() {
		var configName = $(this).find('input[name="configName"]').val();
		var configValue = $(this).find('input[name="configValue"]').val();
		var type = $(this).find('input[name="configValue"]').attr('type');
		
		if(configName.length > 0) {
			if(type === "number" && configValue.length > 0){
				configData[configName] = parseInt(configValue);
			}
			else {
				configData[configName] = configValue;
			}
		}
	});
	data.configs = configData;
	data.status = $('#agentStatus').prop('checked') ? 1 : 0;
	
	if(data.name === '') {
		notifyErrorMessage("Service name is empty, please enter a name")
		return;
	}
	
	var callback = function(rs){
		if(rs != null){
			notifySavedOK('Activation Data Service', 'The data of "<b>'+data.name+'</b>" has been saved successfully')
			$('#dialogSetAgentInfo').modal('hide');			
			loadAgents();
		}
		else {
			console.error('saveAgentInfo ', rs)
		}
	};
	LeoAdminApiUtil.callPostAdminApi('/cdp/agent/save', {'objectJson':JSON.stringify(data) }, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
			callback(json.data);
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
   	}, callback);
}

function newAgent(){
	LeoAdminApiUtil.getSecuredData('/cdp/agent/get',{"dataServiceName":"", "dataServiceId": ""}, function(json) { 
		var data = json.data;
		showAgentDialog(data);
		dataServiceMap[data.id] = data;
	})
}

function loadAgent(id){
	var data = dataServiceMap[id];
	if(typeof data === 'object'){
		showAgentDialog(data);
	}
	else {
		notifyErrorMessage("Can not load data for service: " + id);
	}
}

function resetAgent(id){
	
	var doResetService = function(id){
		$('#' + id + '_api_key').val('')
		$('#' + id + '_api_url').val('')
		
		var data = dataServiceMap[id];
		_.forOwn(data.configs,function(value, key) {
			data.configs.service_api_url = "";
			data.startedAt = null;
		});
		data.status = 0;
		var callback = function(rs){
			if(rs != null){
				iziToast.success({
		    	    title: 'Data Service',
		    	    message: 'The config of <b>'+data.name+'</b> is reset OK!',
		    	    timeout: 3000,
		    	});
				loadAgents();
			}
			else {
				console.error('resetAgent ', rs)
			}
		};
		var params = {'objectJson' : JSON.stringify(data) };
		LeoAdminApiUtil.callPostAdminApi('/cdp/agent/save', params, function (json) {
	        if (json.httpCode === 0 && json.errorMessage === '') {
				callback(json.data);
	        } else {
	            LeoAdminApiUtil.logErrorPayload(json);
	        }
	   	}, callback);
	}
	
	$('#delete_callback').val('');
	$('#confirmDeleteDialog').modal({ focus: true });
	if (id) {
		 var resetCallback = "resetAgent_" + id;
		 window[resetCallback] = function () {
			doResetService(id)
		 }
		 $('#delete_callback').val(resetCallback);
		 $('#deletedInfoTitle').text("Service ID: "+ id).show();
		 $('#deletedInfoMsg').text('Do you want to reset API Key and API URL of data service permanently?');
	}
	
	
}

function deleteAgent(id){
	$('#delete_callback').val('');
	$('#confirmDeleteDialog').modal({ focus: true });
	if (id) {
		 var callback = "deleteAgent_" + id;
		 window[callback] = function () {
			LeoAdminApiUtil.callPostAdminApi('/cdp/agent/delete', {'dataServiceId' : id }, function (json) {
		    if (json.httpCode === 0 && json.errorMessage === '') {
			    	if(json.data === id){
						iziToast.success({ title: 'Data Service', message: 'The service <b>' + id +'</b> is successfully deleted!', timeout: 3000 });
						loadAgents();
					}
					else {
						console.error('deleteAgent ', json)
		    		}
		        } else {
		            LeoAdminApiUtil.logErrorPayload(json);
		        }
		   	});
		 }
		 $('#delete_callback').val(callback);
		 $('#deletedInfoTitle').text("Service ID: "+ id).show();
		 $('#deletedInfoMsg').text('Do you want to delete the selected service permanently?');
	}
}

function showAgentDialog(data) {
	if(typeof data.id !== 'string'){
		notifyErrorMessage("Invalid data service: " + data);
		return;
	}
	
	$('#dialogSetAgentInfo').modal({
		backdrop: 'static',
		keyboard: false
	});
	
	var id = data.id;
	$('#dataServiceSaveButton').data("id",id); 
	
	$('#dataServiceName').val(data.name); 
	$('#dataServiceDescription').val(data.description); 
	$('#dataServiceDagId').val(data.dagId || ''); 
	$('#agentStatus').prop('checked', data.status == 1);
	
	$('#forPersonalization').prop('checked', data.forPersonalization);
	$('#forDataEnrichment').prop('checked', data.forDataEnrichment);
	$('#forSynchronization').prop('checked', data.forSynchronization);
	
	var holder = $('#dataServiceConfigList');
	holder.empty();
	var i = 0;
	_.forOwn(data.configs, function(value, key) {
		var time = new Date().getTime() + i;
		i++;
		var e = { serviceId : id, configName : key, configValue : value, time: time};
		addNewConfigForAgent(id, e)
	});
}


function addNewConfigForAgent(id, e){
	var time = new Date().getTime() + randomInteger(0,10000);
	var data = e != null ? e : {serviceId : id, configName : "", configValue : "", time: time};
	console.log(data)
	if(typeof data.configValue === "number"){
		data.inputType = "number";
	}
	else {
		data.inputType = "text";
	}
	var itemTpl = _.template( $('#tpl_config_row_item').html() );
	
	$('#dataServiceConfigList').append(itemTpl(data)).find('input:last').focus();
}

function deleteConfigOfAgent(id){
	$('#' + id).remove();
}