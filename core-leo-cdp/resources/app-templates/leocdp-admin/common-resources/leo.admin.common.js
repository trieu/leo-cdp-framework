
/**
 * ################## common utils functions ####################
 */

const LeoCdpAdmin = {}

LeoCdpAdmin.registerSession = function(eventBus){
	var usersession = getUserSession();
	var userAddress = 'notifications.' + currentUserProfile.key;
	var payload = {usersession: usersession, address: userAddress}
	var key = 'notifications.register.session';
	try {
		
		if(typeof eventBus === 'object') {
			eventBus.send(key, payload);
		}
		else {
			var eventBus = new EventBus( LeoCdpAdmin.urlWebSocket );
			eventBus.onopen = function () {
				eventBus.send(key, payload);
			}
		}
	} catch(e){
		console.log(e)
	}
	
}

LeoCdpAdmin.initSystemNotifications = LeoCdpAdmin.initSystemNotifications || function(systemUser) {
	if(window.dnsDomainWebSocket === null || window.dnsDomainWebSocket.length === 0 ){
		console.error(' Please set webSocketDomain in leocdp-metadata.properties to initSystemNotifications ');
		return;
	}
	LeoCdpAdmin.urlWebSocket = 'https://' + dnsDomainWebSocket + '/eventbus';
	console.log(' [initSystemNotifications] urlWebSocket: ' + LeoCdpAdmin.urlWebSocket);
	
	var notificationCount = systemUser.notifications.length;
    var eventBus = new EventBus( LeoCdpAdmin.urlWebSocket );
	var userAddress = 'notifications.' + systemUser.key;
	
	eventBus.onopen = function() {
		eventBus.registerHandler(userAddress, function(error, data) {
			if(error){
				console.log("Received error: ", error);
				return;
			}
			console.log("Received data: ", data);
			
			var notiObj = data.body || {};
			var url = notiObj.url || "";
			var type = notiObj.type || "";
			var message = notiObj.message || "";
			var dataFor = notiObj.dataFor || "";
			
			if( type === "registered") {
				console.log("==> Ready to get notification for user: " + notiObj.message);
				$('#notification_count').text(notificationCount)
			}
			else if(type === 'segment-data-exporting') {
				notifyExportingDataIsReady(message, url);
				//	show button			
				setCsvExportingDataUrl(dataFor, url, new Date());
			}
			else if(type === 'reload') {
				location.reload()
			}
			else if(type === 'router') {
				location.hash = url;
			}
		});
		
		LeoCdpAdmin.registerSession(eventBus);
    };

	eventBus.onclose = function() {
    	console.log('EventBus closed');
		LeoCdpAdmin.registerSession();
  	};

	setInterval(LeoCdpAdmin.registerSession, 60000);
}

LeoCdpAdmin.loadView = LeoCdpAdmin.loadView || function(uri, divSelector, callback, i18nTurnOff) {
	//caching view in LocalStorage
	var time2Live = typeof window.apiCacheTime2Live === 'number' ? window.apiCacheTime2Live : 5; // in munute
	var webTemplateCache = typeof window.webTemplateCache === 'boolean' ? window.webTemplateCache : true;
	var cacheKey = 'lview_' + getCacheKey(uri + divSelector, {});
	var resultHtml = false;
	if (webTemplateCache) {
		resultHtml = lscache.get(cacheKey);
	}

	if (resultHtml) {
		//set HTML into view placeholder from cached
		$(divSelector).empty().html(resultHtml);
		$(window).scrollTop(0);

		if (typeof callback === 'function') {
			try {
				callback.apply();
			} catch (error) {
				console.error(error);
			}
		}
	} else {
		if (uri.indexOf("?") > 0) {
			uri += ("&_=" + cacheBustingKey);
		}
		else {
			uri += ("?_=" + cacheBustingKey);
		}
		var fullUrl = window.baseLeoAdminUrl + uri;
		$.ajax({
			url: fullUrl,
			type: 'GET',
			success: function(htmlTpl) {
				// load HTML with i18n data
				if (i18nTurnOff === true) {
					$(divSelector).empty().html(htmlTpl);
					lscache.set(cacheKey, htmlTpl, time2Live);
				} else {
					var i18nModel = typeof window.i18nLeoAdminData === 'function' ? window.i18nLeoAdminData() : {};
					var finalHtml = Handlebars.compile(htmlTpl)(i18nModel);
					$(divSelector).empty().html(finalHtml);
					lscache.set(cacheKey, finalHtml, time2Live);
				}
				$(window).scrollTop(1);

				if (typeof callback === 'function') {
					try {
						callback.apply();
					} catch (error) {
						console.error(error);
					}
				}
				//
				showNodesByIndustryModel()
			},
			error: function(data) {
				console.error("loadView.error: ", data);
			}
		});
	}
}

Handlebars.registerHelper('formatCurrency', function(value) {
	return value.toString().replace(/(\d)(?=(\d\d\d)+(?!\d))/g, "$1,");
});

Handlebars.registerHelper('eachProperty', function(context, options) {
	var ret = "";
	for (var prop in context) {
		ret = ret + options.fn({
			property: prop,
			value: context[prop]
		});
	}
	return ret;
});


function getCompiledTemplate(domSelector) {
	return Handlebars.compile($(domSelector).html().trim());
}

function getFullUrlStaticMedia(uri, defaultUriIfEmpty, staticBaseUrl) {
	var st = (staticBaseUrl || window.staticBaseUrl) || '';
	if (uri && uri !== '' && uri.indexOf('http') < 0) {
		return st + uri;
	}
	return defaultUriIfEmpty || '';
}
function registerUserToGetNotifications(systemUser) {
	console.log(' registerUserToGetNotifications ');
	
    var eventBus = new EventBus('/eventbus');
	var userAddress = 'notifications.' + systemUser.key;
	eventBus.onopen = function() {
		eventBus.registerHandler(userAddress, function(error, message) {
		  	console.log("Received message: ", message);
			var status = message.body.status || "";
			if(status === "registered") {
				notifySuccessMessage("Ready to get notification for user: " + message.body.userlogin)
			}
			else if(status === 'segment-data-exporting') {
				var s = "Click here to download file of segment data exporting: <a target='_blank' href='" + message.body.url + "'> DOWNLOAD </a>";
				notifySuccessMessage(s)
			}
		});
		
		var usersession = getUserSession();
		eventBus.send('notifications.register.session', { usersession: usersession, address: userAddress});
    };
	eventBus.onclose = function() {
    	console.log('EventBus closed');
  	};
}
function getQueryMapFromUrl(url) {
	var vars = [],
		hash;
	var hashes = url.slice(url.indexOf('?') + 1).split('&');
	for (var i = 0; i < hashes.length; i++) {
		hash = hashes[i].split('=');
		vars.push(hash[0]);
		vars[hash[0]] = hash[1];
	}
	return vars;
}

function textTruncate(str, length, ending) {
	if (length == null) {
		length = 100;
	}
	if (ending == null) {
		ending = '...';
	}
	if (str.length > length) {
		return str.substring(0, length - ending.length) + ending;
	} else {
		return str;
	}
};

function toTitleCase(str) {
	return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
}

function toDateStringVN(date) {
	var dd = date.getDate();
	var mm = date.getMonth() + 1; //January is 0!

	var yyyy = date.getFullYear();
	if (dd < 10) {
		dd = '0' + dd;
	}
	if (mm < 10) {
		mm = '0' + mm;
	}
	var str = dd + '/' + mm + '/' + yyyy;
	return str;
}

function getCacheKey(url, params) {
	var text = url + JSON.stringify(params);
	return md5(text);
}

function isMobileDevice() {
	var isMobile = false; //initiate as false
	// device detection
	if (
		/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|ipad|iris|kindle|Android|Silk|lge |maemo|midp|mmp|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i
			.test(navigator.userAgent) ||
		/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i
			.test(navigator.userAgent.substr(0, 4))) {
		isMobile = true;
	}
	return isMobile;
}

function getUserSession() {
	var usersession = lscache.get('usersession');
	if (typeof usersession === 'string') {
		return usersession;
	}
	else {
		console.error('usersession is empty');
		return "";
	}
}

/**
 * ################## LeoAdminApiUtil functions ####################
 * 
 */
var LeoAdminApiUtil = window.LeoAdminApiUtil || {};

if (LeoAdminApiUtil.isLoaded !== true) {
	(function() {
		var obj = {
			isLoaded: false,
			debug: false,
			baseLeoAdminUrl: window.baseLeoAdminUrl || 'http://localhost:9070',
			baseUploadApi: window.baseUploadApi || 'http://localhost:9070'
		};

		obj.callPostApi = function(urlStr, data, okCallback, errorCallback) {
			if (typeof okCallback !== 'function') {
				console.error('callback must be a function');
			}
			if (typeof urlStr !== 'string') {
				console.error('urlStr must be a string');
			}
			$.ajax({
				url: urlStr,
				crossDomain: true,
				data: JSON.stringify(data),
				contentType: 'application/json',
				type: 'POST',
				error: function(jqXHR, exception) {
					notifyErrorMessage('WE GET AN ERROR AT URL:' + urlStr);
					if (typeof errorCallback === 'function') {
						errorCallback();
					}
				}
			}).done(function(json) {
				console.log("callPostApi", urlStr, data, json);
				okCallback(json);
			});
		}

		obj.callPostAdminApi = function(urlStr, data, callback, errorCallback) {
			if (typeof callback !== 'function') {
				console.error('callback must be a function');
			}
			var usersession = window.leoCdpUserSession || lscache.get('usersession');
			if (typeof usersession === 'string') {
				window.leoCdpUserSession = usersession;				
				var payload = _.cloneDeep(data)
				payload['usersession'] = usersession;

				jQuery.ajax({
					type: 'POST',
					crossDomain: true,
					url: urlStr,
					data: JSON.stringify(payload),
					contentType: 'application/json',
					dataType: "json",
					success: function(json) {
						callback(json);
					},
					error: function(responseData, textStatus, errorThrown) {
						console.error('callPostAdminApi POST failed.', responseData);
						var err = responseData.responseJSON.errorMessage ? responseData.responseJSON.errorMessage : "System Error"
						notifyErrorMessage(err);
						if (typeof errorCallback === 'function') {
							errorCallback(err);
						}
					}
				});

			} else {
				console.error("lscache.get('usersession') is NULL");
				callback({
					"uri": urlStr,
					"data": "",
					"errorMessage": "No Authentication",
					"httpCode": 501
				});
			}
		}

		obj.getSecuredData = function(urlStr, data, callback, refreshCache) {
			console.log('getSecuredData : ' + urlStr, data);
			if (typeof callback !== 'function') {
				console.error('callback must be a function');
			}
			var usersession = window.leoCdpUserSession || lscache.get('usersession');
			if (typeof usersession === 'string') {
				window.leoCdpUserSession = usersession;
				if (refreshCache === true) {
					data['_t'] = new Date().getTime();
				}
				jQuery.ajax({
					type: 'GET',
					crossDomain: true,
					url: urlStr,
					data: data,
					beforeSend: function(xhr) {
						xhr.setRequestHeader('leouss', usersession);
					},
					contentType: 'application/json',
					dataType: "json",
					success: function(json) {
						callback(json);
						if(dataApiCache){
							lscache.set(cacheKey, JSON.stringify(json), time2Live);
						}
					},
					error: function(responseData, textStatus, errorThrown) {
						console.error('getSecuredData POST failed.' + responseData);
						notifyErrorMessage(responseData);
					}
				});
			}
			else {
				notifyErrorMessage("lscache.get('usersession') is NULL");
				callback({
					"uri": urlStr,
					"data": "",
					"errorMessage": "No Authentication",
					"httpCode": 501
				});
			}
		}

		obj.renderViewForSecuredData = function(apiURI, objParams, containerIdSelector, tplIdSelector, beforeRenderCallback, afterRenderCallback) {
			if (!window.baseDeliveryApi || !apiURI) {
				console.error('baseDeliveryApi and apiURI must be not NULL');
				return;
			}

			//build with client cache busting
			var urlStr = baseDeliveryApi + apiURI;
			LeoAdminApiUtil.getSecuredData(urlStr, objParams, function(json) {
				var stUrl = json.staticBaseUrl || '';
				if (json.httpCode === 0 && json.errorMessage === '' && json.data) {
					var data = json.data;

					var template = getCompiledTemplate(tplIdSelector); //ID selector of template
					var containerNode = $(containerIdSelector); //ID selector of container list
					containerNode.empty();

					for (var k in data) {
						var obj = data[k];
						var contentType = obj.type;

						//before append
						if (typeof beforeRenderCallback === 'function') {
							obj = beforeRenderCallback(obj, stUrl);
						}

						//render DOM
						var html = template(obj);
						containerNode.append(html);

						// after append
						if (typeof afterRenderCallback === 'function') {
							var domNode = containerNode.children().last()
							afterRenderCallback(contentType, domNode, stUrl);
						}
					}
				}
			});
		}

		obj.httpGetDataJson = function(uriStr) {
			var usersession = lscache.get('usersession');
			if (usersession) {
				return jQuery.ajax({
					url: obj.baseLeoAdminUrl + uriStr,
					type: 'GET',
					dataType: 'json',
					beforeSend: function(xhr) {
						xhr.setRequestHeader('leouss', usersession);
					}
				});
			} else {
				console.error('No usersession found in lscache')
			}
		}

		obj.debugLog = function(data) {
			if (window.debugMode === true) {
				console.log("caller context: ", obj.debugLog.caller);
				[].forEach.call(arguments, function(el) {
					console.log(el);
				});
			}
		}

		obj.logErrorPayload = function(json) {
			console.error("logErrorPayload", json);
			var errorCode = json.httpCode;
			var title = json.errorMessage;
			var message = "ErrorCode: " + errorCode;

			var onClosingCallback;

			if (errorCode === 501) {
				message += ". You need to do login !";
				onClosingCallback = function(instance, toast, closedBy) {
					// reload page if error Authentication
					LeoAdminApiUtil.logout(function() {
						location.reload();
					});
				};		
				setTimeout(onClosingCallback,6666)		
			}
			else {
				onClosingCallback = function() {
					// do nothing
				}
			}

			iziToast.error({
				timeout: 5000,
				title: title,
				message: message,
				onClosing: onClosingCallback
			});
		}

		obj.isLoginSessionOK = function() {
			var usersession = lscache.get('usersession');
			var encryptionkey = lscache.get('encryptionkey');
			return typeof usersession === 'string' && typeof encryptionkey === 'string';
		}

		obj.logout = function(callback) {
			var ssousersession = lscache.get('ssousersession');
			lscache.flush();
			setTimeout(function() {
				if(ssoLogin) {
					doSSOlogout(ssousersession)	
				}
				else {
					if (typeof callback === 'function') callback();
					else window.location = '/';
				}
			}, 2000);
			
			
		}

		obj.formater = {
			toDateString: function(data) {
				var date = moment.utc(parseFloat(data)).local().format('YYYY-MM-DD HH:mm:ss');
				return date;
			}
		}

		LeoAdminApiUtil = obj;
		LeoAdminApiUtil.isLoaded = true;
	}());
}

function isValidURL(str) {
	var pattern = new RegExp('^(https?:\\/\\/)?' + // protocol
		'((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|' + // domain name
		'((\\d{1,3}\\.){3}\\d{1,3}))' + // OR ip (v4) address
		'(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*' + // port and path
		'(\\?[;&a-z\\d%_.~+=-]*)?' + // query string
		'(\\#[-a-z\\d_]*)?$', 'i'); // fragment locator
	return !!pattern.test(str);
}

function convertTimeInputToMinutes(domSelector) {
	var selectedTime = moment($(domSelector).val(), 'HH:mm');
	var duration = moment.duration(selectedTime.diff(new Date()));
	var minutes = duration.asMinutes();
	if (minutes < 0) {
		// if the selected time is negative value, add 12 hours (720 minutes) to current value
		minutes = Math.abs(minutes) + 720;
	}
	return Math.ceil(minutes);
}


const errorNoAuthorization = function() {
	iziToast.error({
		title: 'Error',
		message: 'You don not have authorization to use this function, please contact your system administrator.'
	});
}

const appendItemCount = function(domSelector, collectorKey, collectorCount) {
	var value = new Number(collectorCount).toLocaleString();
	var html = '<h5 class="collector-item"> ' + collectorKey + ' &nbsp; <span class="label label-default collector-count" > ' + value + ' </span>  </h5>';
	$(domSelector).append(html)
}


const underDevelopment = function(domNode) {
	var msg = 'The feature is under development!';
	if (domNode) {
		msg = '<b>The feature "' + $(domNode).attr('title') + '" is under development. It will be available in version 2.0! </b>';
	}
	iziToast.info({
		title: 'Information Box',
		color: 'yellow',
		message: msg
	});
}

var notifyErrorMessage = function(msg, callback) {
	iziToast.error({
		timeout: 4000,
		title: 'Error', icon: 'fa fa-info',
		message: msg,
		onClosing: function(instance, toast, closedBy) {
			if (typeof callback === 'function') {
				callback();
			}
		}
	});
	console.error(msg);
}

const notifySuccessMessage = function(msg, callback) {
	iziToast.success({
		timeout: 4000, icon: 'fa fa-check-square-o',
		title: 'Information',
		message: msg,
		onClosing: function(instance, toast, closedBy) {
			if (typeof callback === 'function') {
				callback();
			}
		}
	});
}

const notifyExportingDataIsReady = function(msg, url) {
	var s = msg + " <b><a target='_blank' href='" + url + "'> Click here to DOWNLOAD </a> </b>";
	iziToast.success({
		timeout: 120000, icon: 'fa fa-check-square-o',
		title: 'Data Downloader',
		message: s
	});
}

const notifySavedOK = function(objectName, msg) {
	var s = typeof msg === 'string' ? msg : 'Data has been saved successfully';
	iziToast.success({
		timeout: 3300, icon: 'fa fa-check-square-o',
		title: typeof objectName === 'string' ? objectName : 'Information',
		message: s
	});
}


const leoCdpDocumentation = function(slug) {
	iziToast.info({
		title: 'Information Box',
		color: 'green',
		message: 'We are working hard, as much as we can',
		onClosing: function(instance, toast, closedBy) {
			// log error
		}
	});
}

const showNodesByIndustryModel = function() {
	// show all DOM nodes that data-industry in attributes
	var showIndustryModels = window.industryDataModels || [];
	showIndustryModels.forEach(function(e) {
		$("#page_main_content").find("*[data-industry='" + e + "']").show()
	})
}

const setupCollapsePanels = function() {
	$('.panel-collapse').on('show.bs.collapse', function() {
		$(this).siblings('.collapse-panel-heading').addClass('active');
	});
	$('.panel-collapse').on('hide.bs.collapse', function() {
		$(this).siblings('.collapse-panel-heading').removeClass('active');
	});
}

$.fn.isInViewport = function() {
	var elementTop = $(this).offset().top;
	var elementBottom = elementTop + $(this).outerHeight();

	var viewportTop = $(window).scrollTop();
	var viewportBottom = viewportTop + $(window).height();

	return elementBottom > viewportTop && elementTop < viewportBottom;
};



const loadSystemUsersForDataAuthorization = function(canSetAuthorization, dataModel, viewerDomSelector, editorDomSelector) {
	var selectedEditors = dataModel.authorizedEditors || [];
	var selectedViewers = dataModel.authorizedViewers || [];
	var htmlViewers = "", htmlEditors = "";

	// load login accounts
	if (canSetAuthorization) {
		var urlStr = baseLeoAdminUrl + '/user/list-for-selection';
		LeoAdminApiUtil.callPostAdminApi(urlStr, {}, function(json) {
			if (json.httpCode === 0 && typeof json.data === 'object') {
				var list = json.data;

				list.forEach(function(e) {
					// viewer for all roles not superadmin, only a God can do everything
					if (selectedViewers.includes(e.userLogin)) {
						htmlViewers += ('<option selected value="' + e.userLogin + '">' + e.userLogin + '</option>')
					} else {
						htmlViewers += ('<option value="' + e.userLogin + '">' + e.userLogin + '</option>')
					}
					// editor only for ROLE_DATA_OPERATOR, ROLE_DATA_ADMIN
					if (e.role > 1) {
						if (selectedEditors.includes(e.userLogin)) {
							htmlEditors += ('<option selected value="' + e.userLogin + '">' + e.userLogin + '</option>')
						} else {
							htmlEditors += ('<option value="' + e.userLogin + '">' + e.userLogin + '</option>')
						}
					}
				});

				// init UI
				viewerDomSelector.html(htmlViewers).show().chosen({
					width: "100%",
					no_results_text: "Oops, nothing found!"
				});
				editorDomSelector.html(htmlEditors).show().chosen({
					width: "100%",
					no_results_text: "Oops, nothing found!"
				});
			} else {
				LeoAdminApiUtil.logErrorPayload(json);
			}
		});
	}
	else {
		$('#divSegmentViewers, #divSegmentEditors').hide();
		
		selectedViewers.forEach(function(userLogin) {
			htmlViewers += ('<option selected="true" disabled="true" value="' + userLogin + '">' + userLogin + '</option>')
		});
		viewerDomSelector.html(htmlViewers).chosen();

		selectedEditors.forEach(function(userLogin) {
			htmlEditors += ('<option selected="true" disabled="true" value="' + userLogin + '">' + userLogin + '</option>')
		});
		editorDomSelector.html(htmlEditors).chosen();
	}

}

const EventMetric = {};
EventMetric.NO_SCORING = 0;

// segmentation
EventMetric.SCORING_LEAD_METRIC = 1;
EventMetric.SCORING_PROSPECT_METRIC = 2;
EventMetric.SCORING_ENGAGEMENT_METRIC = 3;
EventMetric.SCORING_DATA_QUALITY_METRIC = 4;
EventMetric.SCORING_ACQUISITION_METRIC = 5;
EventMetric.SCORING_LIFETIME_VALUE_METRIC = 6;

// CX
EventMetric.SCORING_EFFORT_METRIC = 7;
EventMetric.SCORING_SATISFACTION_METRIC = 8;
EventMetric.SCORING_FEEDBACK_METRIC = 9;
EventMetric.SCORING_PROMOTER_METRIC = 10;

// loyalty
EventMetric.SCORING_CREDIT_METRIC = 11;
EventMetric.SCORING_LOYALTY_METRIC = 12;

const NO_IMAGE_URL = 'https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/No_image_available.svg/200px-No_image_available.svg.png';

const SocialMediaIconMap = {
	'twitter': '<b> <i class="fa fa-twitter-square" aria-hidden="true"></i> Twitter: </b> ',
	'linkedin': '<b> <i class="fa fa-linkedin-square" aria-hidden="true"></i> LinkedIn: </b> ',
	'facebook': '<b> <i class="fa fa-facebook-square" aria-hidden="true"></i> Facebook: </b> ',
	'instagram': '<b> <i class="fa fa-instagram" aria-hidden="true"></i> Instagram: </b> ',
	'skype': '<b> <i class="fa fa-skype" aria-hidden="true"></i> Skype: </b> ',
	'github': '<b> <i class="fa fa-github" aria-hidden="true"></i> Github: </b> ',
	'whatsapp': '<b> <i class="fa fa-whatsapp" aria-hidden="true"></i> WhatsApp: </b> ',
	'apple': '<b> <i class="fa fa-apple" aria-hidden="true"></i> Apple: </b> ',
	'medium': '<b> <i class="fa fa-medium" aria-hidden="true"></i> Medium: </b> ',
	'youtube': '<b> <i class="fa fa-youtube" aria-hidden="true"></i> Youtube: </b> ',
	'zalo': '<b> <i class="fa fa-phone-square" aria-hidden="true"></i> Zalo: </b> ',
	'amazon': '<b> <i class="fa fa-amazon" aria-hidden="true"></i> Amazon: </b> ',
	'quora': '<b> <i class="fa fa-quora" aria-hidden="true"></i> Quora: </b> ',
	'bitbucket': '<b> <i class="fa fa-bitbucket" aria-hidden="true"></i> Bitbucket: </b> ',
	'tiktok': '<b> <i class="fa fa-play-circle" aria-hidden="true"></i> Tiktok: </b> '
}

const PersonalContactIconsMap = {
	'Citizen ID 1': '<b> <i class="fa fa-id-card-o" aria-hidden="true"></i> Citizen ID 1:  </b>  ',
	'Passport ID 1': '<b> <i class="fa fa-id-card-o" aria-hidden="true"></i> Passport ID 1:  </b>  ',
	'Citizen ID 2': '<b> <i class="fa fa-id-card-o" aria-hidden="true"></i> Citizen ID 2:  </b>  ',
	'Passport ID 2': '<b> <i class="fa fa-id-card-o" aria-hidden="true"></i> Passport ID 2:  </b>  ',
	'Citizen ID 3': '<b> <i class="fa fa-id-card-o" aria-hidden="true"></i> Citizen ID 3:  </b>  ',
	'Passport ID 3': '<b> <i class="fa fa-id-card-o" aria-hidden="true"></i> Passport ID 3:  </b>  ',
	'Citizen ID 4': '<b> <i class="fa fa-id-card-o" aria-hidden="true"></i> Citizen ID 4:  </b>  ',
	'Passport ID 4': '<b> <i class="fa fa-id-card-o" aria-hidden="true"></i> Passport ID 4:  </b>  ',
	'Citizen ID 5': '<b> <i class="fa fa-id-card-o" aria-hidden="true"></i> Citizen ID 5:  </b>  ',
	'Passport ID 5': '<b> <i class="fa fa-id-card-o" aria-hidden="true"></i> Passport ID 5:  </b>  '
}

const ContactIconsMap = {
	'Business Email 1': '<b> <i class="fa fa-envelope-o" aria-hidden="true"></i> Business Email 1:  </b>  ',
	'Business Mobile 1': '<b> <i class="fa fa-phone-square" aria-hidden="true"></i> Business Mobile 1:  </b> ',
	'Business Email 2': '<b> <i class="fa fa-envelope-o" aria-hidden="true"></i> Business Email 2:  </b> ',
	'Business Mobile 2': '<b> <i class="fa fa-phone-square" aria-hidden="true"></i> Business Mobile 2:  </b> ',
	'Business Email 3': '<b> <i class="fa fa-envelope-o" aria-hidden="true"></i> Business Email 3:  </b> ',
	'Business Mobile 3': '<b> <i class="fa fa-phone-square" aria-hidden="true"></i> Business Mobile 3:  </b> ',
	'Business Email 4': '<b> <i class="fa fa-envelope-o" aria-hidden="true"></i> Business Email 4:  </b>  ',
	'Business Mobile 4': '<b> <i class="fa fa-phone-square" aria-hidden="true"></i> Business Mobile 4:  </b> ',
	'Business Email 5': '<b> <i class="fa fa-envelope-o" aria-hidden="true"></i> Business Email 5:  </b>  ',
	'Business Mobile 5': '<b> <i class="fa fa-phone-square" aria-hidden="true"></i> Business Mobile 5:  </b> '
}

function findHandlebarsVariables(template) {

	const variableRegex = /\{\{([^{}]+)\}\}/g;

	// Extract matches from the template
	const matches = template.match(variableRegex);

	// Initialize an array to store unique variable names
	const variableNames = [];

	// Process each match and extract variable names
	if (matches) {
		matches.forEach(match => {
			// Extract variable names from the matched expression
			const names = match
				.replace(/\{\{|\}\}/g, '') // Remove {{ and }} from the match
				.split(/\s+/) // Split by whitespace
				.filter(Boolean); // Remove empty strings

			// Add unique variable names to the array
			names.forEach(name => {
				if (!variableNames.includes(name)) {
					variableNames.push(name);
				}
			});
		});
	}

	return variableNames;
}

const decodeURISafe = function(s) {
	if (!s) {
		return s;
	}
	if (s.toString().length < 6) {
		return s;
	}
	try {
		return decodeURIComponent(s.replaceAll(/%/g, '%25'));
	}
	catch(e){
		console.error("Error on decodeURIComponent: " + s);
	}
	return s;
}

const decodeObjectValue = function(k, v) {
	if(typeof v === 'string'){
		return decodeURISafe(v)
	}
	else if(Array.isArray(v)){
        // Convert to [key1 => value1], [key2 => value2] format
		const result = _.map(v, e => {
		    return _.map(e, (value, key) => `[${key} => ${value}]`).join(', ');
		});
		
		// Join all workshop strings
		const output = result.join(' | ');
		return output;
	}
	else {
		return (JSON.stringify(v))
	}
}

function showLoginAccountPassword(nodeId){
	var x = document.getElementById(nodeId);
	if(x){
		if (x.type === "password") {
   	    	x.type = "text";
   	  	} else {
   	    	x.type = "password";
   	  	}
	}
}

function getHtmlErrorPanel(err){
	return '<div class="alert alert-danger class="highlight_text"> <h3> <i class="fa fa-exclamation-triangle" aria-hidden="true"></i> ' + err + '</h3></div>';
}

function toLocalDateTime(data) {
	return moment.utc(data).local().format('YYYY-MM-DD HH:mm:ss');
}


function safeParseInt(input) {
	var s = typeof input === 'string' ? input.replace(/\D/g, '') : '';
    return parseInt(s)
}
