/**
 * LeoEventObserver version 0.9.2 - built on 2025.12.11
 */

// ------------------------------ Leo Data Request ----------------------------------------//

(function(global) {
    'use strict';

    // Configuration
    var CONFIG = {
        CONTENT_TYPE_JSON: 'application/json; charset=UTF-8',
        CONTENT_TYPE_FORM: 'application/x-www-form-urlencoded',
        RETRY_DELAY: 2000,
        DEBUG: false // Set to true to see logs
    };

    // Logger Utility
    function log(msg, type) {
        if (!window.console) return;
        var prefix = "[LeoCDP] ";
        if (type === 'error') {
            window.console.error(prefix + msg);
        } else if (CONFIG.DEBUG) {
            window.console.log(prefix + msg);
        }
    }

    // --- Network Layer ---

    function createXHR() {
        if (window.XMLHttpRequest) {
            return new XMLHttpRequest();
        }
        // Legacy IE Fallback (Only if absolutely necessary)
        try {
            return new ActiveXObject("Microsoft.XMLHTTP");
        } catch (e) {
            log("XHR not supported.", "error");
            return null;
        }
    }

    var Network = {
        /**
         * Generic Request Handler
         */
        request: function(method, url, data, headers, callback) {
            var xhr = createXHR();
            if (!xhr) return;

            xhr.open(method, url, true);
            
            // Set Headers
            if (headers) {
                for (var key in headers) {
                    if (headers.hasOwnProperty(key)) {
                        xhr.setRequestHeader(key, headers[key]);
                    }
                }
            }

            xhr.withCredentials = true;

            xhr.onreadystatechange = function() {
                if (xhr.readyState === 4) {
                    var isSuccess = (xhr.status >= 200 && xhr.status < 300) || xhr.status === 304 || xhr.status === 1223;
                    if (callback) {
                        callback(isSuccess, xhr.responseText, xhr);
                    }
                }
            };

            xhr.send(data);
        },

        get: function(url, callback) {
            this.request("GET", url, null, null, function(success, response, xhr) {
                if (success && callback) {
                    callback(response);
                } else {
                    log("GET failed: " + url, "error");
                }
            });
        },

        post: function(url, data, callback) {
            var headers = { 'Content-Type': CONFIG.CONTENT_TYPE_FORM };
            this.request("POST", url, data, headers, function(success, response) {
                if (success && callback) {
                    callback(response);
                } else if (!success) {
                    log("POST failed: " + url, "error");
                }
            });
        },

        /**
         * Tries to use sendBeacon (best for analytics), falls back to XHR
         */
        sendBeaconOrXHR: function(url, payload, callback) {
            // 1. Try Beacon API (Modern, non-blocking, survives page unload)
            if (navigator.sendBeacon) {
                // Note: sendBeacon sends POST by default. 
                // We use Blob to ensure the correct Content-Type if the backend requires it.
                var blob = new Blob([payload], { type: CONFIG.CONTENT_TYPE_FORM });
                var queued = navigator.sendBeacon(url, blob);
                if (queued) {
                    if (callback) callback(true);
                    return;
                }
            }

            // 2. Fallback to XHR
            this.post(url, payload, function(resp) {
                if (callback) callback(true, resp);
            });
        }
    };

    // --- Batching & Queue Layer ---

    var BatchManager = {
        queues: {},
        isFlushing: {},

        /**
         * Standardized payload creator
         */
        createPayload: function(events) {
            return "events=" + encodeURIComponent(JSON.stringify(events));
        },

        /**
         * Adds item to queue and checks constraints
         */
        enqueue: function(url, data, batchSize) {
            if (!this.queues[url]) {
                this.queues[url] = [];
            }
            this.queues[url].push(data);

            if (this.queues[url].length >= batchSize) {
                this.flush(url);
            }
        },

        /**
         * Sends data safely.
         * Logic: Move data to buffer -> Send -> If Fail, return to queue.
         */
        flush: function(url) {
            var self = this;
            var queue = this.queues[url];

            if (!queue || queue.length === 0 || this.isFlushing[url]) {
                return;
            }

            // Lock this URL to prevent race conditions
            this.isFlushing[url] = true;

            // Take a snapshot of current events (Buffer)
            var buffer = queue.slice(0); 
            // Optimistically clear the main queue (we will restore if fail)
            this.queues[url] = []; 

            var eventCount = buffer.length;
            var payload = this.createPayload(buffer);
            var finalUrl = url + (url.indexOf('?') === -1 ? '?' : '&') + "eventcount=" + eventCount;

            Network.sendBeaconOrXHR(finalUrl, payload, function(success) {
                self.isFlushing[url] = false;
                
                if (success) {
                    log("Batch sent successfully: " + eventCount + " events to " + url);
                } else {
                    // FATAL: Network failed. Restore data to the FRONT of the queue to preserve order.
                    log("Batch failed. Restoring " + eventCount + " events to queue.", "error");
                    self.queues[url] = buffer.concat(self.queues[url]);
                }
            });
        }
    };

    // --- Global Interface ---

    var LeoCorsRequest = {
        get: function(url, callback) {
            Network.get(url, callback);
        },
        
        post: function(url, params, callback) {
            Network.post(url, params, callback);
        },

        /**
         * Add event to batch queue.
         * @param {string} url - The endpoint URL
         * @param {object} paramsObj - The data object to send
         * @param {number} batchSize - Trigger send when queue reaches this size
         */
        batchSend: function(url, paramsObj, batchSize) {
            BatchManager.enqueue(url, paramsObj, batchSize || 10);
        }
    };

    // --- Automatic Flush Timer ---
    
    // Flush all queues every 5 seconds
    setInterval(function() {
        for (var url in BatchManager.queues) {
            if (BatchManager.queues.hasOwnProperty(url)) {
                // Use closure or pass variable explicitly to avoid loop scope issues
                BatchManager.flush(url);
            }
        }
    }, 5000);

    // --- Flush on Page Unload (Crucial for CDPs) ---
    // Ensures we don't lose the last few clicks when user closes tab
    if (window.addEventListener) {
        window.addEventListener('unload', function() {
            for (var url in BatchManager.queues) {
                if (BatchManager.queues.hasOwnProperty(url)) {
                    BatchManager.flush(url);
                }
            }
        });
    }

    // Expose to Global
    global.LeoCorsRequest = LeoCorsRequest;

})(typeof window === 'undefined' ? this : window);

// ------------------------------------------------------------------------------------//


// ------------ BEGIN lscache ------------------
/**
 * lscache library https://github.com/pamelafox/lscache
 * Copyright (c) 2011, Pamela Fox
 */
(function(root, factory) {
    if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        define([], factory);
    } else if (typeof module !== "undefined" && module.exports) {
        // CommonJS/Node module
        module.exports = factory();
    } else {
        // Browser globals
        root.lscache = factory();
    }
}(this, function() {

    // Prefix for all lscache keys
    var CACHE_PREFIX = 'leocache-';

    // Suffix for the key name on the expiration items in localStorage
    var CACHE_SUFFIX = '-cacheexpiration';

    // expiration date radix (set to Base-36 for most space savings)
    var EXPIRY_RADIX = 10;

    // time resolution in milliseconds
    var expiryMilliseconds = 60 * 1000;
    // ECMAScript max Date (epoch + 1e8 days)
    var maxDate = calculateMaxDate(expiryMilliseconds);

    var cachedStorage;
    var cachedJSON;
    var cacheBucket = '';
    var warnings = false;

    // Determines if localStorage is supported in the browser;
    // result is cached for better performance instead of being run each time.
    // Feature detection is based on how Modernizr does it;
    // it's not straightforward due to FF4 issues.
    // It's not run at parse-time as it takes 200ms in Android.
    function supportsStorage() {
        var key = '__lscachetest__';
        var value = key;

        if (cachedStorage !== undefined) {
            return cachedStorage;
        }

        // some browsers will throw an error if you try to access local storage (e.g. brave browser)
        // hence check is inside a try/catch
        
        try {
        	if (typeof window.localStorage !== "object") {
                return false;
            }
        	
            setItem(key, value);
            removeItem(key);
            cachedStorage = true;
        } catch (e) {
        	console.error(e);
            // If we hit the limit, and we don't have an empty localStorage then it means we have support
            if (isOutOfSpace(e) && localStorage.length) {
                cachedStorage = true; // just maxed it out and even the set test failed.
            } else {
                cachedStorage = false;
            }
        }
        return cachedStorage;
    }

    // Check to set if the error is us dealing with being out of space
    function isOutOfSpace(e) {
        return e && (
            e.name === 'QUOTA_EXCEEDED_ERR' ||
            e.name === 'NS_ERROR_DOM_QUOTA_REACHED' ||
            e.name === 'QuotaExceededError'
        );
    }

    // Determines if native JSON (de-)serialization is supported in the browser.
    function supportsJSON() {
        /*jshint eqnull:true */
        if (cachedJSON === undefined) {
            cachedJSON = (window.JSON != null);
        }
        return cachedJSON;
    }

    /**
     * Returns a string where all RegExp special characters are escaped with a \.
     * @param {String} text
     * @return {string}
     */
    function escapeRegExpSpecialCharacters(text) {
        return text.replace(/[[\]{}()*+?.\\^$|]/g, '\\$&');
    }

    /**
     * Returns the full string for the localStorage expiration item.
     * @param {String} key
     * @return {string}
     */
    function expirationKey(key) {
        return key + CACHE_SUFFIX;
    }

    /**
     * Returns the number of minutes since the epoch.
     * @return {number}
     */
    function currentTime() {
        return Math.floor((new Date().getTime()) / expiryMilliseconds);
    }

    /**
     * Wrapper functions for localStorage methods
     */

    function getItem(key) {
        return localStorage.getItem(CACHE_PREFIX + cacheBucket + key);
    }

    function setItem(key, value) {
        // Fix for iPad issue - sometimes throws QUOTA_EXCEEDED_ERR on setItem.
        localStorage.removeItem(CACHE_PREFIX + cacheBucket + key);
        localStorage.setItem(CACHE_PREFIX + cacheBucket + key, value);
    }

    function removeItem(key) {
        localStorage.removeItem(CACHE_PREFIX + cacheBucket + key);
    }

    function eachKey(fn) {
        var prefixRegExp = new RegExp('^' + CACHE_PREFIX + escapeRegExpSpecialCharacters(cacheBucket) + '(.*)');
        // Loop in reverse as removing items will change indices of tail
        for (var i = localStorage.length - 1; i >= 0; --i) {
            var key = localStorage.key(i);
            key = key && key.match(prefixRegExp);
            key = key && key[1];
            if (key && key.indexOf(CACHE_SUFFIX) < 0) {
                fn(key, expirationKey(key));
            }
        }
    }

    function flushItem(key) {
        var exprKey = expirationKey(key);

        removeItem(key);
        removeItem(exprKey);
    }

    function flushExpiredItem(key) {
        var exprKey = expirationKey(key);
        var expr = getItem(exprKey);

        if (expr) {
            var expirationTime = parseInt(expr, EXPIRY_RADIX);

            // Check if we should actually kick item out of storage
            if (currentTime() >= expirationTime) {
                removeItem(key);
                removeItem(exprKey);
                return true;
            }
        }
    }

    function warn(message, err) {
        if (!warnings) return;
        if (!('console' in window) || typeof window.console.warn !== 'function') return;
        window.console.warn("lscache - " + message);
        if (err) window.console.warn("lscache - The error was: " + err.message);
    }

    function calculateMaxDate(expiryMilliseconds) {
        return Math.floor(8.64e15 / expiryMilliseconds);
    }

    var lscache = {
        /**
         * Stores the value in localStorage. Expires after specified number of minutes.
         * @param {string} key
         * @param {Object|string} value
         * @param {number} time
         * @return {boolean} whether the value was inserted successfully
         */
        set: function(key, value, time) {
            if (!supportsStorage()) return false;

            // If we don't get a string value, try to stringify
            // In future, localStorage may properly support storing non-strings
            // and this can be removed.

            if (!supportsJSON()) return false;
            try {
                value = JSON.stringify(value);
            } catch (e) {
                // Sometimes we can't stringify due to circular refs
                // in complex objects, so we won't bother storing then.
                return false;
            }

            try {
                setItem(key, value);
            } catch (e) {
                if (isOutOfSpace(e)) {
                    // If we exceeded the quota, then we will sort
                    // by the expire time, and then remove the N oldest
                    var storedKeys = [];
                    var storedKey;
                    eachKey(function(key, exprKey) {
                        var expiration = getItem(exprKey);
                        if (expiration) {
                            expiration = parseInt(expiration, EXPIRY_RADIX);
                        } else {
                            // TODO: Store date added for non-expiring items for smarter removal
                            expiration = maxDate;
                        }
                        storedKeys.push({
                            key: key,
                            size: (getItem(key) || '').length,
                            expiration: expiration
                        });
                    });
                    // Sorts the keys with oldest expiration time last
                    storedKeys.sort(function(a, b) {
                        return (b.expiration - a.expiration);
                    });

                    var targetSize = (value || '').length;
                    while (storedKeys.length && targetSize > 0) {
                        storedKey = storedKeys.pop();
                        warn("Cache is full, removing item with key '" + key + "'");
                        flushItem(storedKey.key);
                        targetSize -= storedKey.size;
                    }
                    try {
                        setItem(key, value);
                    } catch (e) {
                        // value may be larger than total quota
                        warn("Could not add item with key '" + key + "', perhaps it's too big?", e);
                        return false;
                    }
                } else {
                    // If it was some other error, just give up.
                    warn("Could not add item with key '" + key + "'", e);
                    return false;
                }
            }

            // If a time is specified, store expiration info in localStorage
            if (time) {
                setItem(expirationKey(key), (currentTime() + time).toString(EXPIRY_RADIX));
            } else {
                // In case they previously set a time, remove that info from localStorage.
                removeItem(expirationKey(key));
            }
            return true;
        },

        /**
         * Retrieves specified value from localStorage, if not expired.
         * @param {string} key
         * @return {string|Object}
         */
        get: function(key) {
            if (!supportsStorage()) return null;

            // Return the de-serialized item if not expired
            if (flushExpiredItem(key)) {
                return null;
            }

            // Tries to de-serialize stored value if its an object, and returns the normal value otherwise.
            var value = getItem(key);
            if (!value || !supportsJSON()) {
                return value;
            }

            try {
                // We can't tell if its JSON or a string, so we try to parse
                return JSON.parse(value);
            } catch (e) {
                // If we can't parse, it's probably because it isn't an object
                return value;
            }
        },

        /**
         * Removes a value from localStorage.
         * Equivalent to 'delete' in memcache, but that's a keyword in JS.
         * @param {string} key
         */
        remove: function(key) {
            if (!supportsStorage()) return;

            flushItem(key);
        },

        /**
         * Returns whether local storage is supported.
         * Currently exposed for testing purposes.
         * @return {boolean}
         */
        supported: function() {
            return supportsStorage();
        },

        /**
         * Flushes all lscache items and expiry markers without affecting rest of localStorage
         */
        flush: function() {
            if (!supportsStorage()) return;

            eachKey(function(key) {
                flushItem(key);
            });
        },

        /**
         * Flushes expired lscache items and expiry markers without affecting rest of localStorage
         */
        flushExpired: function() {
            if (!supportsStorage()) return;

            eachKey(function(key) {
                flushExpiredItem(key);
            });
        },

        /**
         * Appends CACHE_PREFIX so lscache will partition data in to different buckets.
         * @param {string} bucket
         */
        setBucket: function(bucket) {
            cacheBucket = bucket;
        },

        /**
         * Resets the string being appended to CACHE_PREFIX so lscache will use the default storage behavior.
         */
        resetBucket: function() {
            cacheBucket = '';
        },

        /**
         * @returns {number} The currently set number of milliseconds each time unit represents in
         *   the set() function's "time" argument.
         */
        getExpiryMilliseconds: function() {
            return expiryMilliseconds;
        },

        /**
         * Sets the number of milliseconds each time unit represents in the set() function's
         *   "time" argument.
         * Sample values:
         *  1: each time unit = 1 millisecond
         *  1000: each time unit = 1 second
         *  60000: each time unit = 1 minute (Default value)
         *  360000: each time unit = 1 hour
         * @param {number} milliseconds
         */
        setExpiryMilliseconds: function(milliseconds) {
            expiryMilliseconds = milliseconds;
            maxDate = calculateMaxDate(expiryMilliseconds);
        },

        /**
         * Sets whether to display warnings when an item is removed from the cache or not.
         */
        enableWarnings: function(enabled) {
            warnings = enabled;
        }
    };

    // Return the module
    return lscache;
}));

// ------------ END LEO Cache ------------------

// ------------ BEGIN LEO Event Observer -------

var leoSessionStringKey = "leoctxsk";
var leoVisitorIdStringKey = "leocdp_vid";

(function(global, undefined) {
    var LeoEventObserver = {'fingerprintId' : ""};
    var sessionKey = false;
    var debug = true;
    
    function debugLog(data){
    	if(debug && window.console){
			window.console.log(data);
		}
    }
    
    function setSessionKey(key){
    	sessionKey = key;
    	lscache.set(leoSessionStringKey, sessionKey);
    }
    
    function getSessionKey(autoResfresh){
    	sessionKey = lscache.get(leoSessionStringKey);
		if(typeof sessionKey !== 'string' && autoResfresh === true){
			sessionKey = "";
		}
    	return sessionKey;
    }
    
    function clearSessionKey(){
    	lscache.remove(leoSessionStringKey);
    }
    
    function initFingerprint(callback){
    	var options = { excludes: { enumerateDevices : true, deviceMemory : true}}
    	Fingerprint2.get(options, function (components) {
    	    var values = components.map(function (component) { return component.value })
    	    var fingerprintId = Fingerprint2.x64hash128(values.join(''), 31)
  
    	    var oneWeekInMinutes = 10080;
    		lscache.set("leocdp_fgp", fingerprintId);

    		// callback
    		if(typeof callback === 'function') callback(fingerprintId);
    	});
    }
    

    function generateVisitorId() {
    	if(typeof INJECTED_VISITOR_ID === 'string') {
    		return INJECTED_VISITOR_ID;
    	} else {
    		var d = new Date().getTime();
	        var uuid = 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
	            var r = (d + Math.random() * 16) % 16 | 0;
	            d = Math.floor(d / 16);
	            return (c == 'x' ? r : (r & 0x3 | 0x8)).toString(16);
	        });
	        return uuid;
    	}
    }
    
    function getVisitorId() {
        var key = leoVisitorIdStringKey;
        var uuid =  lscache.get(key); 
        
        // overwrite old ID  
        if(typeof INJECTED_VISITOR_ID === 'string' && typeof uuid === 'string') {
        	if(uuid !== INJECTED_VISITOR_ID) {
        		uuid = INJECTED_VISITOR_ID;
        		
        		lscache.flush();
        		
        		setTimeout(function(){
        			lscache.set(key, uuid); //cache forever
        		},200);
        	}
        }
        
        if (typeof uuid !== 'string') {
        	uuid = generateVisitorId();
            lscache.set(key, uuid); //cache forever
        } 

        return uuid;
    }

    var doTracking = function(eventType, params) {
		var localSessionKey = getSessionKey(true);
        var trackingAjaxHandler = function(resHeaders, text) {
            var data = JSON.parse(text);
	 		if(data.sessionKey && localSessionKey !== data.sessionKey){
	        	setSessionKey(data.sessionKey);
	        }
        }

        var batchSize = Number.parseInt(params['batchsize'] || 1);
        delete params['batchsize'];

		params["visid"] = getVisitorId();

        var queryStr = objectToQueryString(params);
		var isHttpPost = false;
	
    	var prefixUrl = PREFIX_EVENT_VIEW_URL;
        if(eventType === "action"){
        	prefixUrl = PREFIX_EVENT_ACTION_URL;
        	isHttpPost = true;
        } 
        else if(eventType === "conversion"){
        	prefixUrl = PREFIX_EVENT_CONVERSION_URL;
        	isHttpPost = true;
        } 
        else if(eventType === "feedback"){
        	prefixUrl = PREFIX_EVENT_FEEDBACK_URL;
        	isHttpPost = true;
        }
        
        var url = "";
        if(isHttpPost) {
        	url = prefixUrl + '?ctxsk=' + localSessionKey;
            
            if(batchSize <= 1){
                // send of batchSize is 1 or 0
                LeoCorsRequest.post(url, queryStr, trackingAjaxHandler);
            }
            else {
                LeoCorsRequest.batchSend(url, params, batchSize)
            }
        	
        } else {
        	url = prefixUrl + '?' + queryStr + '&ctxsk=' + localSessionKey;
        	LeoCorsRequest.get(url, trackingAjaxHandler);
        }
       
        console.log("LeoCorsRequest url " + url)
		
    }
    
    var updateProfile = function(params) {
    	 var h = function(resHeaders, text) {
             //var data = JSON.parse(text);             
         }
    	 
         params['visid'] =  getVisitorId();
         var paramsStr = objectToQueryString(params);
         
         var sessionKey = getSessionKey();        
         var url = PREFIX_UPDATE_PROFILE_URL + '?' + 'ctxsk=' + sessionKey;
         
         LeoCorsRequest.post(url, paramsStr, h);
    }
       
    var objectToQueryString = function(params){
    	// FIXME add fingerprint to params
    	if(OBSERVE_WITH_FINGERPRINT){
    		params['fgp'] = lscache.get("leocdp_fgp") || LeoEventObserver.fingerprintId;
    	}
    	
    	var queryString = Object.keys(params).map((key) => {
		    return encodeURIComponent(key) + '=' + encodeURIComponent(params[key])
		}).join('&');
		return queryString;
    }
    
    function leoObserverProxyReady(data) {
    	setSessionKey(data.sessionKey);
    	
    	var vid = getVisitorId();
    	var newVisitorId = data.visitorId;
    	if(typeof newVisitorId === "string" && newVisitorId !== vid){
    		lscache.set(leoVisitorIdStringKey, newVisitorId);
    	}
    	
		sendMessage("LeoObserverProxyReady");
        debugLog(data);
    }

    var getContextSession = function(params) {
    	var leoctxsk = getSessionKey();
    	var isExpired = typeof leoctxsk !== 'string' || leoctxsk === '';
    	//isExpired = true; // TODO to debug, uncomment this line
    	
    	if( isExpired ){
    		// the cache is expired
    		var h = function(resHeaders, text) {
                var data = JSON.parse(text);
                if(data.status === 101){
                	leoObserverProxyReady(data);
                }
                else {
                	console.error(data)
                }
            }
            var queryStr = objectToQueryString(params);
            var vsId = getVisitorId();
            var url = PREFIX_SESSION_INIT_URL + '?' + queryStr + '&visid=' + vsId;
            LeoCorsRequest.get(url, h);
    	}
    	else {
    		// the cache is valid
    		sendMessage("LeoObserverProxyReady");
    	}
    }

    LeoEventObserver.doTracking = doTracking;
    LeoEventObserver.getContextSession = getContextSession;
    LeoEventObserver.updateProfile = updateProfile;
    LeoEventObserver.initFingerprint = initFingerprint;
    LeoEventObserver.getVisitorId = getVisitorId;

    global.LeoEventObserver = LeoEventObserver;

})(typeof window === 'undefined' ? this : window);