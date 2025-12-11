// System Management and System Event Monitoring 
const EXPIRED_AFTER_21_DAYS = 30240;
const EXPIRED_AFTER_1_HOUR = 60;

function getUrlParams(url) {
    const search = url ? new URL(url).search : window.location.search;
    const params = new URLSearchParams(search);
    const result = {};

    for (const [key, value] of params.entries()) {
        result[key] = value;
    }
    return result;
}

const loadSystemUserLoginSession = function() {
    var urlStr = baseLeoAdminUrl + '/user/login-session';
    LeoAdminApiUtil.callPostApi(urlStr, {}, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
            var usersession = json.data.userSession;
            loginFormHandler(usersession);
            
            var captchaImgBase64 = 'data:image/png;base64,'+ json.data.captchaImage;
            $('#captcha_img').show().attr('src', captchaImgBase64);
               
            $('#system_user_login_loader').hide();             
            $('#system_user_login_div').show();
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}

const loginFormHandler = function(session) {
    var apiLoginHandler = function () {
        var urlStr = baseLeoAdminUrl + '/user/check-login';

        var user = $('#username').val();
        var pass = $('#password').val();
        var captchaStr = $('#captcha').val();
        
        var params = {
            'userlogin': user,
            'userpass': pass,
            'usersession': session,
            'captcha' : captchaStr
        };
        
        $('#info-login').html("<b>Sending data ...</b>").show();
        var loginHandler = function (json) {
            if (json.errorMessage === '') {
            	//OK, set session data
                var encryptionkey = json.data;
            	
                lscache.set('usersession', session, EXPIRED_AFTER_21_DAYS);
                lscache.set('encryptionkey', encryptionkey, EXPIRED_AFTER_21_DAYS);

                // update UI
                $("#system_user_login_form, #btn_system_login").hide();                    
                $('#info-login').html('<i class="fa fa-check-circle fa-2" aria-hidden="true"></i> <b> Login successfully ! </b> ');
                
                //wait and reload
                setTimeout(function () { window.location.reload(); }, 3000);
            } else {
            	$('#info-login').hide();
                $('#error-login').html('<i class="fa fa-exclamation-circle" aria-hidden="true"></i> ' + json.errorMessage).show().delay(5000).fadeOut();
                // failed
                setTimeout(function () { LeoAdminApiUtil.logErrorPayload(json); }, 2000);
            }
        };
        LeoAdminApiUtil.callPostApi(urlStr, params, loginHandler);
    };

    $('#btn_system_login').click(apiLoginHandler);
    $("#username").on('keyup', function (e) {
        if (e.keyCode === 13) {
            $("#password").focus();
        }
    });
    $("#password").on('keyup', function (e) {
        if (e.keyCode === 13) {
            $("#captcha").focus();
        }
    });
    $("#captcha").on('keyup', function (e) {
        if (e.keyCode === 13) {
            apiLoginHandler();
        }
    });
}    

const loadLoginSessionSSO = function() {
	var ssoGetSessionUrl = ssoSessionUrl + '?_t=' + new Date().getTime();
    $('#sso_login_url').attr('href', ssoGetSessionUrl);

	if(showDefaultLogin) {
		$('#default_login_url').show()
	}

	var urlStr = baseLeoAdminUrl + '/user/login-session';
	
    LeoAdminApiUtil.callPostApi(urlStr, {'sso':true}, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
            var usersession = json.data.userSession || "";
			var ssoUserSession = json.data.ssoUserSession || "";
			var ssoUserEmail = json.data.ssoUserEmail || "";			
			
            loadCheckSSO(ssoUserEmail, ssoUserSession, usersession);
                           
            $('#system_user_login_loader').hide();             
            $('#system_user_login_div').show();
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}

const doSSOlogout = function(ssousersession) {
	if(ssoLogoutUrl && ssousersession && ssoLogoutUrl.indexOf('http')>=0){
		location.href = ssoLogoutUrl + "?sid="+ssousersession + "&t=" + new Date().getTime();
	}
	else {
		console.error(ssoLogoutUrl, ssousersession. ssoLogoutUrl)
	}
}

const loadCheckSSO = function(ssoUserEmail, ssoUserSession, session) {
    
	if(ssoUserEmail.length > 0 && ssoUserSession.length > 0) {
		$('#email').val(ssoUserEmail);
		$('#email_panel').show();
		$('#sso_login_url, #default_login_url').hide();
		
		var urlStr = baseLeoAdminUrl + '/user/check-sso';
        
        var params = {
            'ssoUserEmail': ssoUserEmail,
            'usersession': session
        };
        
        $('#info-login').html("<b>Checking data ...</b>").show();
        var loginHandler = function (json) {
            if (json.errorMessage === '') {
            	//OK, set session data
                var encryptionkey = json.data;
            	
				lscache.set('ssousersession', ssoUserSession, EXPIRED_AFTER_1_HOUR);
                lscache.set('usersession', session, EXPIRED_AFTER_1_HOUR);
                lscache.set('encryptionkey', encryptionkey, EXPIRED_AFTER_1_HOUR);

                // update UI
                $("#system_user_login_form, #btn_system_login").hide();                    
                $('#info-login').html('<i class="fa fa-check-circle fa-2" aria-hidden="true"></i> <b> Login successfully ! </b> ');
                
                //wait and reload
                setTimeout(function () { window.location.reload(); }, 3000);
            } else {
            	$('#info-login').hide();
                $('#error-login').html('<i class="fa fa-exclamation-circle" aria-hidden="true"></i> ' + json.errorMessage).show().delay(5000).fadeOut();
                // failed
                setTimeout(function () { LeoAdminApiUtil.logErrorPayload(json); }, 2000);
            }
        };
        LeoAdminApiUtil.callPostApi(urlStr, params, loginHandler);
	}
} 
    
const initSystemUserLogin = function(ssoLogin) {
	// show logo
	$('#login_logo').attr('src', window.pageHeaderLogo).show();
	// check login
	var usersession = lscache.get('usersession');
    var encryptionkey = lscache.get('encryptionkey');
	if (typeof usersession === 'string' && typeof encryptionkey === 'string' ) {
        console.log("You login OK with session " + usersession);
    } 
    else {
		if(ssoLogin ===  true){
			loadLoginSessionSSO()
		}
		else {
			// no session login, request user login
        	loadSystemUserLoginSession();
		}		
        console.log('loadSystemUserLoginSession is called ')
    }
}

const loadSystemEventsTable = function(eventObjectName, tableDomHolderId, objectName, objectId, requestUserLogin, callback, lengthMenuData) {
	var action = '', accessIp = '';
	
    var usersession = getUserSession();
    if (usersession) {
		// set html of table UI into DOM
		var tpl = _.template( $('#system-event-table-tpl').html() );
		var htmlTable = tpl({'eventObjectName': eventObjectName}) ;
		$('#' + tableDomHolderId).empty().html(htmlTable);
		
		// define DOM holder selectors
		var containerDomSel = '#system_event_list';
		var loaderDomSel = '#system_event_loader';
		var tableDomSel = '#system_event_table';
		
		// define table schema
	    var collumnList = [
	    	{
	           "data": "createdAt" // 0 
	        },
	   		{
	           "data": "action" // 1
	       	},
	       	{
	           "data": "accessIp" // 2
	       	},
	       	{
	            "data": "userAgent" // 3
	      	},
	      	{
	            "data": "data" // 4
	        }
	   	];
   
	    // make Ajax request and show data in the view of DataTable 
	    lengthMenuData = typeof lengthMenuData === 'object' ? lengthMenuData :  [20, 30, 40];
        var obj = $(tableDomSel).DataTable({
        	"lengthMenu": [lengthMenuData, lengthMenuData],
        	"lengthChange": lengthMenuData.length > 1 ? true: false,
        	'processing': true,
            'serverSide': true,
            'searching': false,
            'search': { return: false },
            'ordering': true,
            'serverMethod': 'POST',
            'ajax': {
                url: baseLeoAdminUrl + "/cdp/system-control/action-logs",
                contentType: 'application/json',
                beforeSend: function (request) {
                	$(loaderDomSel).show();
                	$(containerDomSel).hide();
                    request.setRequestHeader("leouss", usersession);
                },
                data: function (d) {
                	d.objectName = objectName;
                	d.objectId = objectId;
                	d.action = action;
                	d.accessIp = accessIp;
                	d.requestUserLogin = requestUserLogin;
                	
                	var order = d.order[0];
                	d.sortField = collumnList[order.column].data;
                	d.sortAsc = order.dir === "asc" ;
                	d.searchValue = d.search.value;
                	delete d.order; delete d.search;
                    return JSON.stringify(d);
                },
                dataSrc: function ( json ) {
                	var canInsertData = json.canInsertData;
		 	    	var canEditData = json.canEditData;
		     		var canDeleteData = json.canDeleteData;
		     		
                	$(tableDomSel).data('canInsertData', canInsertData)
		     		$(tableDomSel).data('canEditData', canEditData)
		     		$(tableDomSel).data('canDeleteData', canDeleteData)
            		
            		// done show UI
            		$(loaderDomSel).hide();
                	$(containerDomSel).show();
                	
                	if(typeof callback === 'function') {
                    	callback();
                    }                	
            		return json.data;
                 }
            },
            'columnDefs': [
            	{
                    "render": function (data, type, row) {
                        var date = toLocalDateTime(data);
                        return '<div class="text-center" style="color:#3300ff; width:78px; margin: auto;" ><mark>'  + date + '</mark></div>';
                    },
                    "targets": 0
                },
                {
                    "render": function (data, type, row) {
                    	return '<div style="font-size:0.9em; width:180px; text-align:center; margin: auto;word-wrap: break-word;" ><mark>' +  data + '</mark></div>';
                    },
                    "targets": 1,
                    "orderable": false
                },
                {
                    "render": function (data, type, row) {
                        return '<div style="font-size:0.9em; text-align:center; margin: auto; word-wrap: break-word;"><mark>' +  data + '</mark></div>';
                    },
                    "targets": 2,
                    "orderable": false
                },
                {
                    "render": function (data, type, row) {
                        return '<div style="font-size:0.8em; text-align:center; width:260px; margin: auto; word-wrap: break-word;" >' +  data + '</div>';
                    },
                    "targets": 3,
                    "orderable": false
                },
            	{
                    "render": function (data, type, row) {
                        return '<div style="font-size:0.78em; word-wrap: break-word; width:400px; margin: auto;" >' + textTruncate(data, 300) + '</div>';
                    },
                    "targets": 4,
                    "orderable": false
                }
            ],
            'columns': collumnList,
            'order': [[0, 'desc']]
        });
        return obj;
    }
}

const loadSystemEventsFromLoginUser = function(requestUserLogin) {
	return loadSystemEventsTable('User Login', 'panel_login_account_view_logs', 'SystemUser', '', requestUserLogin)
}


var loadSystemInfoConfigs = function() {
	LeoAdminApiUtil.getSecuredData('/cdp/system-config/list-all',{}, function(json) { 
		var holder = $('#system_configs_holder');
		_.forEach(json.data,function(e,i) {
			//console.log(e)
			
			var id = e.id;
			systemConfigMap[id] = e;
			e.disabledText = e.configs.read_only ? 'disabled' : '';
			
			if(typeof e.description === 'string' && typeof e.configs.service_api_url === 'string' ) {
				var boxTpl = _.template( $('#tpl_system_config_box').html() );
				holder.append( boxTpl(e) );
				
				if(e.configs.service_api_url === 'local-system'){
					$('#' + id + '_buttons_div').find('button').attr('disabled','disabled');				
				}
				
				if(e.status === 1){
					$('#' + id + '_api_url_div').addClass('highlight_text')
				}
			}
			
		});
		holder.find('div.active_false button').attr('class','btn btn-default').attr('disabled','disabled');
		holder.find('div.panel_active_false').find('input').attr('disabled','disabled');
		holder.find('i.config_description').tooltip();

		// profile metadata
		var profileMetadata = _.map(systemConfigMap['profile_data_fields'].coreFieldConfigs, function(e,k){
			if(e.dataQualityScore > 0){
				e['coreAttribute'] = true;
				return e;	
			}
			return false; 
		})
		profileMetadata = _.filter(profileMetadata, function(e) { 
		    return e !== false; 
		});
		profileMetadata = _.sortBy(profileMetadata, [function(o) { 
			return o.identityResolution ? 1000 * o.dataQualityScore : o.dataQualityScore 
		}]).reverse();

		$('#profileAttributeCount').text(profileMetadata.length)
		renderProfileMetaDataTable(profileMetadata);
		
		// leocdp metadata
		var leocdpMetadata = (systemConfigMap['leo_cdp_metadata'] && systemConfigMap['leo_cdp_metadata'].configs) || {};
	    renderSsoSettings(leocdpMetadata)
	})
}

function renderSsoSettings(leocdpMetadata){
	leocdpMetadata['keycloakCallbackUrl'] = baseLeoAdminUrl + '/_ssocdp/callback';

	// Map field IDs directly to metadata keys
	var fields = [
	    'ssoLogin',
	    'ssoLoginUrl',
	    'keycloakRealm',
	    'keycloakClientId',
	    'keycloakClientSecret',
	    'keycloakCallbackUrl',
	    'keycloakVerifySSL'
	];
	
// Initialize inputs + watch for changes
	fields.forEach(function (field) {
	    var $el = $('#' + field);
	
	    // Set initial value safely
	    if ($el.length) {
	        $el.val(leocdpMetadata[field] || '');
	
	        // Sync changes back to metadata object
	        $el.on('change input', function () {
	            leocdpMetadata[field] = $(this).val();
	        });
	    }
	});
}

function saveSsoSettings(){
	saveSystemConfigs(systemConfigMap['leo_cdp_metadata'],function(){
		notifySuccessMessage('All SSO Settings are saved successfully !')
	})
}

function saveSystemConfigs(configs, callbackSaveOk){
	var params = {'objectJson' : JSON.stringify(configs) };
	LeoAdminApiUtil.callPostAdminApi('/cdp/system-config/save', params, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
        	if( typeof callbackSaveOk === 'function'){
				callbackSaveOk()
			}
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
   	});
}
	
function renderProfileMetaDataTable(profileMetadata) {
	 var gridControls = { title : "Controls", type: "control", width: 80, editButton: true, deleteButton: false, 
		itemTemplate:  function(value, item) {
             var $result = jsGrid.fields.control.prototype.itemTemplate.apply(this, arguments);
             
             var isCoreAttribute = item.coreAttribute;
             var attributeName = item.attributeName;
             var setRuleIcon, setRulesLabel;
             setRuleIcon = '<i class="fa fa-cogs event-set-rules" style="" aria-hidden="true"></i>';
             setRulesLabel = 'Edit Event Name: ' + attributeName;
             
             var $setRulesButton = $("<button>").data("attributeName",attributeName).attr({class: "jsgrid-custom-button", title : setRulesLabel}).html(setRuleIcon)
             .click(function(e) {
             	console.log("setRules button ", $(this).data('attributeName'));
             	
             	underDevelopment(this);
                 e.stopPropagation();
             });       
             
             var cssClass = "customGridEditbutton jsgrid-button jsgrid-edit-button";
             var $customEditButton = $("<button>").data("attributeName",attributeName).attr({class: cssClass, title: "Edit Attribute"})
             .click(function(e) {
             	//underDevelopment(this);
                 //e.stopPropagation();
                 console.log("Edit button ", $(this).data('attributeName'))
             });

             var cssClass = "customGridDeletebutton jsgrid-button jsgrid-delete-button";
            	var $customDeleteButton = $("<button>").data("attributeName",attributeName).attr({class: cssClass, title:"Delete Attribute"})
            	.click(function(e) {
            		var attributeName = $(this).data('attributeName');
            		if( isCoreAttribute ){
            			notifyErrorMessage("The attribute <b>[" + attributeName + "]</b> is the <b>attribute of system</b>. You can not delete this attribute.")
            		} else {
                		deleteEventMetricMetaData(attributeName)
            		}
               	e.stopPropagation();
             });

           	var rs = $("<div>");
           	if(! isCoreAttribute ) {
           		rs.addClass("editable_profile_attribute");
           		rs.attr("title","Click to edit or delete")
           	} else {
           		rs.attr("title","The attribute of system")
           	}
           	rs.append($customEditButton).append($customDeleteButton);
           	
           	return rs;
      	}
  	}
	
	$("#profile_metadata_table").jsGrid({
		data: profileMetadata,
	    width: "100%",
	    height: "auto",
	    inserting: false,
	    editing: true,
	    sorting: true,
	    paging: false,
	    confirmDeleting : true,
	    deleteConfirm: function(item) {
	    	if(item.coreAttribute) {
	    		return "The attribute <b>[" + item.attributeName + "]</b> is the <b>attribute of system</b>. You can not delete this attribute.";
		    }
            return "The attribute [" + item.attributeName + "] will be removed. Are you sure ? ";
        },
        onItemInserting: function(args) {
        	
	    },
	    onItemInserted: function(args) {
	    	
	    }, 
	    onItemEditing: function(args) {
           
	    },
	    onItemUpdated: function(args) {
	    	var item = args.item;
	    	console.log(item)
	    	var profileConfigs = systemConfigMap['profile_data_fields'];
	    	var obj = profileConfigs.coreFieldConfigs[item.attributeName];
	    	if(obj) {
	    		obj.dataQualityScore = item.dataQualityScore;
	    		saveSystemConfigs(profileConfigs, function(){
					notifySuccessMessage('The attribute <b>[' + item.attributeName +  ']</b> is saved successfully !')
				});
	    	}
	    }, 
	    onItemDeleting: function(args) {
	       
	    },
	    onRefreshed: function (args) {
	    	
	    },
	    fields: [
			{ name: "identityResolution", title : "Identity Resolution Key", align: 'center', type: "CheckBoxIcon", width: 64, sorter: "string", 
					editTemplate: function(isChecked) {
				    	return '<div class="text-center">' + getCheckedBoxIcon(isChecked) + '</div>';
				    }
	        },
	        { name: "attributeName", title : "Attribute Name", align: 'center' , type: "text", validate: "required", validate: "required", editing: false },
	        { name: "label", title : "Label", align: 'center' , type: "text", validate: "required", width: 120, editing: false },
	        { name: "type", title : "Data Type", align: 'center' , type: "text", validate: "required", editing: false, 
	        	itemTemplate: function(value, item) { 
					var idx = value.lastIndexOf('.')+1;
					value = idx > 0 ? value.substr(idx) : value;
	        		return '<div class="profile_field_type"> ' + value + ' </div>'; 
	        	} 
	        },
	        { name: "dataQualityScore", title : "Data Quality Score", type: "number", align: 'center', width: 76, validate: "required",
	        	itemTemplate: function(value, item) { 
	        		if(value > 0){
	        			return '<div class="highlight_text positive_dqs"> ' + value + ' </div>'; 
	        		}
	        		else if(value < 0){
	        			return '<div class="highlight_text negative_dqs"> ' + value + ' </div>'; 
	        		}
	        		return value;
	        	}
	        }
	        ,{ name: "synchronizable", title : "Synchronizable ", align: 'center' , type: "CustomCheckBox",  width: 64, validate: "required", sorter: "string"}
	        , gridControls
	    ]
	});  
}
	
function callDataQualityComputeJob(){		
	LeoAdminApiUtil.callPostAdminApi('/cdp/system-config/call-data-quality-compute-job', {}, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
	    	notifySuccessMessage('<b>Call Data Quality Compute Job</b> is called successfully!')
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
   });
}

function callSystemControlUpgrade(){	
	notifySuccessMessage("The process to upgrade system is started")	
	LeoAdminApiUtil.callPostAdminApi('/cdp/system-control/upgrade', {}, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
			$('#upgrade_system_logs').val(json.data);
	    	notifySuccessMessage("upgrade system is done, please wait for 10 seconds and reload page", function(){
				setTimeout(function(){
					location.reload(true)
				},9999)
			})
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
   });
}

function resetSystemServiceInfo(id){
	var data = systemConfigMap[id];
	data.status = 0;
	
	_.forOwn(data.configs,function(value, key) {
		if(typeof value === "string"){
			data.configs[key] = "";
		}
	});
	

	$('#' + id + '_api_url').val('');
	$('#' + id + '_api_url_div').removeClass('highlight_text')
	$('#systemServiceStatus').prop('checked', false);
	
	var callback = function(rs){
		if(rs != null){
			iziToast.success({
	    	    title: 'System Config',
	    	    message: 'The config of <b>'+data.name+'</b> is reset successfully !',
	    	    timeout: 3000,
	    	});
		}
		else {
			console.error('saveSystemServiceInfo ', rs)
		}
	};
	var params = {'objectJson' : JSON.stringify(data) };
	LeoAdminApiUtil.callPostAdminApi('/cdp/system-config/save', params, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
			callback(json.data);
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
   	}, callback);
}

function saveSystemServiceInfo(node) {
	var id = $(node).data('id');
	var data = systemConfigMap[id];
	
	var configData = {};

	$('#systemServiceConfigList input').each(function(){
		var name = $(this).attr('name');
		var type = $(this).attr('type');
		if(type === "number"){
			configData[name] = parseInt($(this).val());
		}
		else if(type === "checkbox"){
			configData[name] = $(this).prop('checked');
		}
		else {
			configData[name] = $(this).val();
		}
	});
	data.configs = configData;
	// check API key
	data.status = $('#systemServiceStatus').prop('checked') ? 1 : 0;
	var params = {'objectJson' : JSON.stringify(data) };
	var callback = function(rs){
		if(rs != null){
			$('#dialogSetSystemConfigs').modal('hide')
			iziToast.success({
	    	    title: 'System Config',
	    	    message: 'The config of <b>'+data.name+'</b> is saved OK!',
	    	    timeout: 3000,
	    	});
			
			var apiUrl = configData.service_api_url;
			$('#' + id + '_api_url').val(apiUrl);
		}
		else {
			console.error('saveSystemServiceInfo ', rs)
		}
	};
	LeoAdminApiUtil.callPostAdminApi('/cdp/system-config/save', params, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
			callback(json.data);
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
   	}, callback);
}

function setSystemServiceInfo(id){
	var data = systemConfigMap[id];
	$('#dialogSetSystemConfigs').modal({
		backdrop: 'static',
		keyboard: false
	});
	$('#systemServiceName').text(data.name); 
	$('#systemServiceDescription').text(data.description); 
	$('#systemServiceSaveButton').data("id",id); 
	$('#systemServiceStatus').prop('checked', data.status);
	
	var holder = $('#systemServiceConfigList');
	holder.empty();
	_.forOwn(data.configs,function(value, key) {
		var e = { inputName : key};
		if(typeof value === "number"){
			e.inputType = "number";
		}
		else if(typeof value === "boolean"){
			e.inputType = "checkbox";
		}
		else {
			e.inputType = "text";
		}
		var itemTpl = _.template( $('#tpl_config_row_item').html() );
		holder.append( itemTpl(e) );
		
		if(e.inputType === "checkbox"){
			holder.find('input[name="'+key+'"]').prop('checked', value);
		} else {
			holder.find('input[name="'+key+'"]').val(value)
		}

	});
}
