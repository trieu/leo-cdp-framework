/*
 * LEO JS code for LEO CDP - version 0.9.5 - built on 2025.12.15
 */
// ------------ LEO Proxy ------------------
(function() {
	var leoObserverId = window.leoObserverId || "";
	if(typeof window.leoObserverBatchSize !== 'number') {
		window.leoObserverBatchSize = 10;
	}
	var TIME_TO_ADD_PROXY_IFRAME = 888;
    
    if (typeof window.LeoObserverProxy === "undefined" && typeof leoObserverId === 'string' ) {
    	
    	var leoProxyOrigin = location.protocol + '//' + location.hostname;
    	var targetPostMessage = 'https://' + window.leoObserverLogDomain;
        var proxyHtmlUrl = 'https://' + window.leoObserverLogDomain + "/public/html/leo-event-proxy.html#";

        var LeoObserverProxy = { 'synchLeoVisitorCallback' : false };
        window.LeoObserverProxy = LeoObserverProxy;
        window.LeoIframeProxy = false;
        
        var iframeId = "leotech_event_proxy";
        setTimeout(function(){
        	
        	var LEO_SYN_PREFIX = 'leosyn=';
        	var node = document.getElementById(iframeId);

        	if( node == null ){
        		var iframeProxyUrl = proxyHtmlUrl + window.leoObserverLogDomain + '_' + leoProxyOrigin;
    	        if(typeof window.injectedVisitorId === 'string' ) {
    	        	iframeProxyUrl = iframeProxyUrl + '_' +  window.injectedVisitorId;
    	        	// alert(iframeProxyUrl)
    	        }
    	        else if( location.search.indexOf(LEO_SYN_PREFIX) > 0 ) {
    	        	// inject Unique Visitor ID to landing page
    	        	var search = location.search.substring(1);
    	        	var map = JSON.parse('{"' + decodeURI(search).replace(/"/g, '\\"').replace(/&/g, '","').replace(/=/g,'":"') + '"}');
    	        	var leosyn = map['leosyn'] || '';
    	        	iframeProxyUrl = iframeProxyUrl + '_' + leosyn;
    	        }

    	        //cross domain iframe
    	        var iframeProxy = document.createElement("iframe");
    	        iframeProxy.setAttribute("style", "display:none!important;width:0px!important;height:0px!important;" );
    	        iframeProxy.width = 0;
    	        iframeProxy.height = 0;
    	        iframeProxy.id = iframeId;
    	        iframeProxy.name = iframeId;
    	        iframeProxy.src = iframeProxyUrl;

    	        //append to trigger iframe post back data to server
    	        var body = document.getElementsByTagName("body");
    	        if (body.length > 0) {
    	            body[0].appendChild(iframeProxy);
    	            window.LeoIframeProxy = iframeProxy;
    	        }
        	}
        },TIME_TO_ADD_PROXY_IFRAME);

        // Put message to the queue in the child iframe
        var putEventToQueue = function(msg) {
            // Make sure you are sending a string, and to stringify JSON
        	if(window.LeoIframeProxy){
        		window.LeoIframeProxy.contentWindow.postMessage(msg, targetPostMessage);
        	}
        };

        LeoObserverProxy.messageHandler = function(hash) {
            if (hash === "LeoObserverProxyLoaded") {
 				initLeoContextSession()
            } 
            else if (hash === "LeoObserverProxyReady") {
            	var f = window.leoObserverProxyReady;
                if (typeof f === "function") {
                	f();
                }
            }
            else if (hash.indexOf('synchLeoVisitorId') === 0 && typeof LeoObserverProxy.synchLeoVisitorCallback === 'function') {
            	LeoObserverProxy.synchLeoVisitorCallback(hash.substring('synchLeoVisitorId-'.length));
            }  
        }
        
        // Listen to messages from parent window
        // addEventListener support for IE8
        function bindEvent(element, metricName, eventHandler) {
            if (element.addEventListener) {
                element.addEventListener(metricName, eventHandler, false);
            } else if (element.attachEvent) {
                element.attachEvent('on' + metricName, eventHandler);
            }
        }
        
        bindEvent(window, 'message', function(e) {
        	console.log(" bindEvent.onmessage ===> e.origin " + e.origin + " targetPostMessage " + targetPostMessage + " data " + e.data)
        	if (e.origin !== targetPostMessage) {
        		return;
        	}  
        	LeoObserverProxy.messageHandler(e.data);
        });

        var getObserverParams = function(metricName, eventData, profileObject, extData, transactionId, shoppingCartItems, transactionValue, currencyCode ) {
			var tprefurl =  document.referrer;
			var tprefdomain = extractRootDomain(document.referrer);
			
            var mediaHost = extractRootDomain(document.location.href);
			var tpname = window.srcTouchpointName || document.title;
            var tpurl = window.srcTouchpointUrl || document.location.href;
            var batchSize = window.leoObserverBatchSize;

			var screen =  ""
			if(window.screen) {
				screen = window.screen.width + "x" + window.screen.height; 
			}
			            
            // tracking parameters
            var params = {
                'obsid': leoObserverId,
                'batchsize': batchSize,
                'mediahost': mediaHost,
				'screen': screen,
                'tprefurl': encodeURIComponent(tprefurl),
                'tprefdomain': tprefdomain,
                'tpurl': encodeURIComponent(tpurl),
                'tpname' : encodeURIComponent(tpname)
            };
            
            if(typeof metricName === "string" && typeof eventData === "object"){
            	params['metric'] = metricName;                
             	params['eventdata'] = encodeURIComponent(JSON.stringify(eventData)); 
            }
            if(typeof profileObject === "object"){
            	params['profiledata'] = JSON.stringify(profileObject); 
            }
            if(typeof extData === "object"){
            	params['extData'] = JSON.stringify(extData); 
            }
            if(typeof shoppingCartItems === "object"){
            	params['tsid'] =  typeof transactionId === "string" ? transactionId : ""; 
            	params['scitems'] = JSON.stringify(shoppingCartItems); 
            	params['tsval'] = typeof transactionValue === "number" ? transactionValue : 0; 
            	params['tscur'] =  typeof currencyCode === "string" ?  currencyCode : "USD"; 
            }
            return params;
        }
        
        
        var extractRootDomain = function(url){
        	try {
        		var toks = new URL(url).hostname.split('.');
        		return toks.slice(-1 * (toks.length - 1)).join('.');
        	} catch(e) {} return "";
        };

		var initLeoContextSession = function(){
            var payload = JSON.stringify({
                'call': 'getContextSession',
                'params': getObserverParams(false)
            });
            putEventToQueue(payload);
		}
		
		LeoObserverProxy.synchLeoVisitorId = function(callback) {
			LeoObserverProxy.synchLeoVisitorCallback = callback;
            var payload = JSON.stringify({
                'call': 'synchLeoVisitorId'
            });
            putEventToQueue(payload);
        }

        // event-view(pageview|screenview|storeview|trueview|placeview,contentId,sessionKey,visitorId)
        LeoObserverProxy.recordViewEvent = function(metricName, eventData) {
            if (typeof eventData !== "object") {
            	eventData = {};
            }
            var params = getObserverParams(metricName, eventData);
            var payload = JSON.stringify({
                'call': 'doTracking',
                'params': params,
                'eventType': 'view'
            });
            putEventToQueue(payload);
        }

        // event-action(click|play|touch|contact|watch|test,sessionKey,visitorId)
        LeoObserverProxy.recordActionEvent = function(metricName, eventData) {
            if (typeof eventData === "object") {
                var params = getObserverParams(metricName, eventData);
                var payload = JSON.stringify({
                    'call': 'doTracking',
                    'params': params,
                    'eventType': 'action'
                });
                putEventToQueue(payload);
            }
        }

        // event-conversion(add_to_cart|submit_form|checkout|join,sessionKey,visitorId)
        LeoObserverProxy.recordConversionEvent = function(metricName, eventData, transactionId, shoppingCartItems, transactionValue, currencyCode) {
            if (typeof eventData === "object") {
                var params = getObserverParams(metricName,eventData, false, false, transactionId, shoppingCartItems, transactionValue, currencyCode);
                var payload = JSON.stringify({
                    'call': 'doTracking',
                    'params': params,
                    'eventType': 'conversion'
                });
                putEventToQueue(payload);
            }
        }
        
        // event-feedback(submit-survey|submit-ces-form|submit-csat-form|submit-nps-form)
        LeoObserverProxy.recordFeedbackEvent = function(metricName, eventData) {
            if (typeof eventData === "object") {
                var params = getObserverParams(metricName, eventData);
                var payload = JSON.stringify({
                    'call': 'doTracking',
                    'params': params,
                    'eventType': 'feedback'
                });
                putEventToQueue(payload);
            }
        }
        
        // update contact profile using Embedded Web Form
        LeoObserverProxy.updateProfileBySession = function(profileObject, extData) {
            if (typeof profileObject === "object") {
                var payload = JSON.stringify({
                    'call': 'updateProfile',
                    'params': getObserverParams(false,false,profileObject, extData)
                });
                putEventToQueue(payload);
            }
        }

        window.LeoObserverProxy = LeoObserverProxy;
    }
})();