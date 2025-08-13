// 
var dataServiceStatusReadyHtml = '<i class="fa fa-check-square" aria-hidden="true" style="font-size:1.1em"></i> The service is active';
var dataServiceMap = {};

var renderDataService = function(dataList){
	var holder = $('#data_services_holder').empty().show();
	dataServiceMap = {};
	
	_.forEach(dataList,function(e,i) {
		var id = e.id;
		dataServiceMap[id] = e;
		var active = false;
		if(e.configs.service_api_key && e.configs.service_api_key != ""){
			e.service_api_key = e.configs.service_api_key.substr(0,5) + "...";
			active = true;
		} else{
			e.service_api_key = "";
		}
		var boxTpl = _.template( $('#tpl_data_service_view').html() );
		holder.append( boxTpl(e) );
		holder.find('i.config_description').tooltip();
		if(active){
			$('#service_box_' + id).addClass('active_service_box');
			$('#service_box_header_' + id).addClass('config_ready');
		}
		// set label
		if(e.status === 1){
			var s = dataServiceStatusReadyHtml;
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
	LeoAdminApiUtil.getSecuredData('/cdp/data-service/list', params, function(json) { 
		if(typeof renderCallback === "function" && typeof json.data === "object" ) {
    		renderCallback(json.data)
    	}
	});
}

var loadDataServices = function(keywords, filterServiceValue){
	$('#data_services_pagination').empty();
	$('#data_services_loader').show();
	$('#data_services_holder').hide();	
	$('#filter_data_services_keywords').on('keyup', function (e) {
		// enter to search
        if (e.keyCode === 13) {
        	loadAllDataServices()
        }
    });	
	
	var startIndex = 0, numberResult = 10000;	
	var callback = function(data) {
		$('#data_services_loader').hide();
		if(data.length > 0){
			$('#data_services_pagination').pagination({
				dataSource: data,
				className: "paginationjs paginationjs-big",
			    pageSize: 6,
			    callback: function(dataList, pagination) {
			    	renderDataService(dataList)
			    },
			    beforePageOnClick: function(){
			    	
			    }
			});
		}
		else {
			$('#data_services_holder').show().html('<div class="col-lg-12 row highlight_text"> <h3 style="text-align: center;" >No results found !</h3> </div>');
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

function resetDataServiceFilter() {
	$('#filter_data_services_keywords').val('');
	$('#select_filter_data_services').val('all');
	loadAllDataServices();
}

function loadAllDataServices(){	
	var keywords = $('#filter_data_services_keywords').val();
	var filterServiceValue = $('#select_filter_data_services').val();
	loadDataServices(keywords, filterServiceValue)
}


function saveDataServiceInfo(node){
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
	data.status = $('#dataServiceStatus').prop('checked') ? 1 : 0;
	
	if(data.name === '') {
		notifyErrorMessage("Service name is empty, please enter a name")
		return;
	}
	
	var callback = function(rs){
		if(rs != null){
			notifySavedOK('Activation Data Service', 'The data of "<b>'+data.name+'</b>" has been saved successfully')
			$('#dialogSetDataServiceInfo').modal('hide');			
			loadDataServices();
		}
		else {
			console.error('saveDataServiceInfo ', rs)
		}
	};
	LeoAdminApiUtil.callPostAdminApi('/cdp/data-service/save', {'objectJson':JSON.stringify(data) }, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
			callback(json.data);
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
   	}, callback);
}

function newDataService(){
	LeoAdminApiUtil.getSecuredData('/cdp/data-service/get',{"dataServiceName":"", "dataServiceId": ""}, function(json) { 
		var data = json.data;
		showDataServiceDialog(data);
		dataServiceMap[data.id] = data;
	})
}

function loadDataService(id){
	var data = dataServiceMap[id];
	if(typeof data === 'object'){
		showDataServiceDialog(data);
	}
	else {
		notifyErrorMessage("Can not load data for service: " + id);
	}
}

function resetDataService(id){
	
	var doResetService = function(id){
		$('#' + id + '_api_key').val('')
		$('#' + id + '_api_url').val('')
		
		var data = dataServiceMap[id];
		_.forOwn(data.configs,function(value, key) {
			data.configs.service_api_url = "";
			data.configs.service_api_key = "";
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
				loadDataServices();
			}
			else {
				console.error('resetDataService ', rs)
			}
		};
		var params = {'objectJson' : JSON.stringify(data) };
		LeoAdminApiUtil.callPostAdminApi('/cdp/data-service/save', params, function (json) {
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
		 var resetCallback = "resetDataService_" + id;
		 window[resetCallback] = function () {
			doResetService(id)
		 }
		 $('#delete_callback').val(resetCallback);
		 $('#deletedInfoTitle').text("Service ID: "+ id).show();
		 $('#deletedInfoMsg').text('Do you want to reset API Key and API URL of data service permanently?');
	}
	
	
}

function deleteDataService(id){
	$('#delete_callback').val('');
	$('#confirmDeleteDialog').modal({ focus: true });
	if (id) {
		 var callback = "deleteDataService_" + id;
		 window[callback] = function () {
			LeoAdminApiUtil.callPostAdminApi('/cdp/data-service/delete', {'dataServiceId' : id }, function (json) {
		    if (json.httpCode === 0 && json.errorMessage === '') {
			    	if(json.data === id){
						iziToast.success({ title: 'Data Service', message: 'The service <b>' + id +'</b> is successfully deleted!', timeout: 3000 });
						loadDataServices();
					}
					else {
						console.error('deleteDataService ', json)
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

function showDataServiceDialog(data) {
	if(typeof data.id !== 'string'){
		notifyErrorMessage("Invalid data service: " + data);
		return;
	}
	
	$('#dialogSetDataServiceInfo').modal({
		backdrop: 'static',
		keyboard: false
	});
	
	var id = data.id;
	$('#dataServiceSaveButton').data("id",id); 
	
	$('#dataServiceName').val(data.name); 
	$('#dataServiceDescription').val(data.description); 
	$('#dataServiceDagId').val(data.dagId || ''); 
	$('#dataServiceStatus').prop('checked', data.status == 1);
	
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
		addNewConfigForDataService(id, e)
	});
}


function addNewConfigForDataService(id, e){
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

function deleteConfigOfDataService(id){
	$('#' + id).remove();
}