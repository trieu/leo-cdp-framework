/**
 * @license 
 * Display.js for standard display banner, version v2020sep
 */
if (!window._LeoAdRenderProcessed) {

    //loading GA JavaScript
    /*
    (function (i, s, o, g, r, a, m) {
        i['GoogleAnalyticsObject'] = r;
        i[r] = i[r] || function () {
            (i[r].q = i[r].q || []).push(arguments)
        }, i[r].l = 1 * new Date();
        a = s.createElement(o),
            m = s.getElementsByTagName(o)[0];
        a.async = 1;
        a.src = g;
        m.parentNode.insertBefore(a, m)
    })(window, document, 'script', 'https://www.google-analytics.com/analytics.js', 'leogajs');
    */

    /**
     * ##########################################################
     * viewability check @see https://github.com/kahwee/viewability (1.93kb)
     */
    ! function (e, t) {
        "object" == typeof exports && "object" == typeof module ? module.exports = t() : "function" == typeof define && define.amd ? define([], t) : "object" == typeof exports ? exports.viewability = t() : e.viewability = t()
    }("undefined" != typeof self ? self : this, function () {
        return function (e) {
            function t(u) {
                if (n[u]) return n[u].exports;
                var o = n[u] = {
                    i: u,
                    l: !1,
                    exports: {}
                };
                return e[u].call(o.exports, o, o.exports, t), o.l = !0, o.exports
            }
            var n = {};
            return t.m = e, t.c = n, t.d = function (e, n, u) {
                t.o(e, n) || Object.defineProperty(e, n, {
                    configurable: !1,
                    enumerable: !0,
                    get: u
                })
            }, t.n = function (e) {
                var n = e && e.__esModule ? function () {
                    return e.default
                } : function () {
                    return e
                };
                return t.d(n, "a", n), n
            }, t.o = function (e, t) {
                return Object.prototype.hasOwnProperty.call(e, t)
            }, t.p = "", t(t.s = 2)
        }([function (e, t, n) {
            "use strict";
            e.exports = function (e) {
                var t = window.innerHeight,
                    n = e.getBoundingClientRect().top,
                    u = e.getBoundingClientRect().bottom,
                    o = u - n;
                return n > t ? {
                    value: 0,
                    state: "EL_IS_BELOW_VIEW"
                } : u <= 0 ? {
                    value: 0,
                    state: "EL_IS_ABOVE_VIEW"
                } : n >= 0 && u <= t ? {
                    value: 1,
                    state: "EL_IS_WITHIN_VERTICAL_VIEW"
                } : n < 0 && u > t ? {
                    value: t / o,
                    state: "EL_BOTTOM_AND_TOP_TRUNCATED"
                } : n < 0 && u <= t ? {
                    value: u / o,
                    state: "EL_TOP_TRUNCATED"
                } : n >= 0 && u > t ? {
                    value: (t - n) / o,
                    state: "EL_BOTTOM_TRUNCATED"
                } : {
                    value: 0,
                    state: "EL_IS_NOT_WITHIN_VIEW"
                }
            }
        }, function (e, t, n) {
            "use strict";
            e.exports = function (e) {
                var t = window.innerWidth,
                    n = e.getBoundingClientRect().left,
                    u = e.getBoundingClientRect().right,
                    o = u - n;
                return n > t ? {
                    value: 0,
                    state: "EL_IS_TOO_RIGHT"
                } : u <= 0 ? {
                    value: 0,
                    state: "EL_IS_TOO_LEFT"
                } : n >= 0 && u <= t ? {
                    value: 1,
                    state: "EL_IS_WITHIN_HORIZONTAL_VIEW"
                } : n < 0 && u > t ? {
                    value: t / o,
                    state: "EL_LEFT_AND_RIGHT_TRUNCATED"
                } : n < 0 && u <= t ? {
                    value: u / o,
                    state: "EL_LEFT_TRUNCATED"
                } : n >= 0 && u > t ? {
                    value: (t - n) / o,
                    state: "EL_RIGHT_TRUNCATED"
                } : {
                    value: 0,
                    state: "EL_IS_NOT_WITHIN_VIEW"
                }
            }
        }, function (e, t, n) {
            "use strict";
            e.exports = {
                vertical: n(0),
                horizontal: n(1),
                isElementOnScreen: n(3)
            }
        }, function (e, t, n) {
            "use strict";
            var u = n(1),
                o = n(0);
            e.exports = function (e, t) {
                return t ? o(e).value * u(e).value == 1 : o(e).value * u(e).value > 0
            }
        }])
    });

    /**
     *  ##########################################################
     * lscache library
     * Copyright (c) 2011, Pamela Fox
     *
     */
    (function (root, factory) {
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
    }(this, function () {

        // Prefix for all lscache keys
        var CACHE_PREFIX = 'lscache-';

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
        function supportsStorage() {
            var key = '__lscachetest__';
            var value = key;

            if (cachedStorage !== undefined) {
                return cachedStorage;
            }

            // some browsers will throw an error if you try to access local storage (e.g. brave browser)
            // hence check is inside a try/catch
            try {
                if (!localStorage) {
                    return false;
                }
            } catch (ex) {
                return false;
            }

            try {
                setItem(key, value);
                removeItem(key);
                cachedStorage = true;
            } catch (e) {
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
             */
            set: function (key, value, time) {
                if (!supportsStorage()) return;

                // If we don't get a string value, try to stringify
                // In future, localStorage may properly support storing non-strings
                // and this can be removed.

                if (!supportsJSON()) return;
                try {
                    value = JSON.stringify(value);
                } catch (e) {
                    // Sometimes we can't stringify due to circular refs
                    // in complex objects, so we won't bother storing then.
                    return;
                }

                try {
                    setItem(key, value);
                } catch (e) {
                    if (isOutOfSpace(e)) {
                        // If we exceeded the quota, then we will sort
                        // by the expire time, and then remove the N oldest
                        var storedKeys = [];
                        var storedKey;
                        eachKey(function (key, exprKey) {
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
                        storedKeys.sort(function (a, b) {
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
                            return;
                        }
                    } else {
                        // If it was some other error, just give up.
                        warn("Could not add item with key '" + key + "'", e);
                        return;
                    }
                }

                // If a time is specified, store expiration info in localStorage
                if (time) {
                    setItem(expirationKey(key), (currentTime() + time).toString(EXPIRY_RADIX));
                } else {
                    // In case they previously set a time, remove that info from localStorage.
                    removeItem(expirationKey(key));
                }
            },

            /**
             * Retrieves specified value from localStorage, if not expired.
             * @param {string} key
             * @return {string|Object}
             */
            get: function (key) {
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
            remove: function (key) {
                if (!supportsStorage()) return;

                flushItem(key);
            },

            /**
             * Returns whether local storage is supported.
             * Currently exposed for testing purposes.
             * @return {boolean}
             */
            supported: function () {
                return supportsStorage();
            },

            /**
             * Flushes all lscache items and expiry markers without affecting rest of localStorage
             */
            flush: function () {
                if (!supportsStorage()) return;

                eachKey(function (key) {
                    flushItem(key);
                });
            },

            /**
             * Flushes expired lscache items and expiry markers without affecting rest of localStorage
             */
            flushExpired: function () {
                if (!supportsStorage()) return;

                eachKey(function (key) {
                    flushExpiredItem(key);
                });
            },

            /**
             * Appends CACHE_PREFIX so lscache will partition data in to different buckets.
             * @param {string} bucket
             */
            setBucket: function (bucket) {
                cacheBucket = bucket;
            },

            /**
             * Resets the string being appended to CACHE_PREFIX so lscache will use the default storage behavior.
             */
            resetBucket: function () {
                cacheBucket = '';
            },

            /**
             * @returns {number} The currently set number of milliseconds each time unit represents in
             *   the set() function's "time" argument.
             */
            getExpiryMilliseconds: function () {
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
            setExpiryMilliseconds: function (milliseconds) {
                expiryMilliseconds = milliseconds;
                maxDate = calculateMaxDate(expiryMilliseconds);
            },

            /**
             * Sets whether to display warnings when an item is removed from the cache or not.
             */
            enableWarnings: function (enabled) {
                warnings = enabled;
            }
        };

        // Return the module
        return lscache;
    }));


    /*
     *  ##########################################################
     * Ad Render JS for Leo Ad Server
     * */
    (function (global, undefined) {
    	// TODO
        var BASE_AD_DELIVERY_URI = window.leoAdServerDomain + '/ads/query';
        var adBeacons = {};

        function callBeaconLogTracking(opts) {
            // Make sure we have a base object for opts
            opts = opts || {};
            // Setup defaults for options
            opts.url = opts.url || null;
            opts.vars = opts.vars || {};
            opts.error = opts.error || function () {};
            opts.success = opts.success || function () {};

            //cache busting
            opts.vars.cb = Math.floor(Math.random() * 10e12);

            // Split up vars object into an array
            var varsArray = [];
            for (var key in opts.vars) {
                varsArray.push(encodeURIComponent(key) + '=' + encodeURIComponent(opts.vars[key]));
            }

            // Build query string
            var qString = varsArray.join('&');

            // Create a beacon if a url is provided
            if (opts.url) {
                // Create a brand NEW image object
                var beacon = new Image();
                // Attach the event handlers to the image object
                if (beacon.onerror) {
                    beacon.onerror = opts.error;
                }
                if (beacon.onload) {
                    beacon.onload = opts.success;
                }
                // Attach the src for the script call
                if (opts.noParameter) {
                    beacon.src = opts.url;
                } else {
                    if (opts.url.indexOf('?') > 0) {
                        beacon.src = opts.url + '&' + qString;
                    } else {
                        beacon.src = opts.url + '?' + qString;
                    }
                }
            }
        }


        //https://gist.github.com/jtpaasch/2568647
        function loadScript(file, callback) {
            var script = document.createElement('script');
            script.src = file, script.async = true;
            if (callback !== undefined) {
                script.onreadystatechange = function () {
                    if (script.readyState === 'loaded' ||
                        script.readyState === 'complete') {
                        script.onreadystatechange = null;
                        callback(file);
                    }
                };
                script.onload = function () {
                    callback(file);
                };
            }
            document.body.appendChild(script);
        }

        function generateUUID() {
            var d = new Date().getTime();
            var uuid = 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g,
                function (c) {
                    var r = (d + Math.random() * 16) % 16 | 0;
                    d = Math.floor(d / 16);
                    return (c == 'x' ? r : (r & 0x3 | 0x8)).toString(16);
                });
            return uuid;
        }

        function getRandomInt(min, max) {
            return Math.floor(Math.random() * (max - min + 1) + min);
        }

        function getProtocol() {
            if (location.protocol.indexOf('file') === 0) return 'http:';
            return location.protocol;
        }

        function getBaseUrlTracking() {
            //FIXME add more server 1 .. 5
            // var c = getRandomInt(1, 1);
            var url = getProtocol() + '//' + window.leoObserverLogDomain +  '/track/ads';
            return url;
        }

        //set style tag into head
        function addCSS(el, css) {
            var body = el.getElementsByTagName('body')[0];
            var s = el.createElement('style');
            s.setAttribute('type', 'text/css');
            if (s.styleSheet) { // IE
                s.styleSheet.cssText = css;
            } else { // the world
                s.appendChild(el.createTextNode(css));
            }
            body.appendChild(s);
        }

        function hashStringToNumber(s) {
            var hash = 0,
                i, char;
            if (s.length == 0) return hash;
            for (i = 0, l = s.length; i < l; i++) {
                char = s.charCodeAt(i);
                hash = ((hash << 5) - hash) + char;
                hash |= 0; // Convert to 32bit integer
            }
            return hash;
        };

        function ajaxCorsRequest(url, callback) {
            var xhttp = new XMLHttpRequest();
            var method = 'GET';
            if ("withCredentials" in xhttp) {
                xhttp.open(method, url, true);

            } else if (typeof XDomainRequest != "undefined") {
                xhttp = new XDomainRequest();
                xhttp.open(method, url);
            } else {
                // CORS is not supported by the browser.
                xhttp = null;
                if (typeof callback === 'function') {
                    callback(false);
                }
                return false;
            }
            xhttp.onreadystatechange = function () {
                if (xhttp.readyState == 4 && xhttp.status == 200 && (typeof callback === 'function')) {
                    callback(xhttp.responseText);
                }
            }
            xhttp.onerror = function () {
                if (window.console) {
                    window.console.error(' ajaxCorsRequest for URL: ' + url);
                }
            };
            xhttp.withCredentials = true;
            xhttp.send();
        }
        
        
        var LeoAdRender = { visitorId : '' };

        function buildAdUrlRequest(placementIds) {
            var time = new Date().getTime();
            var q = 'vid=' + LeoAdRender.visitorId;
            for (var i = 0; i < placementIds.length; i++) {
                q += ('&pms=' + placementIds[i]);
            }
            q += ('&rurl=' + encodeURIComponent(document.referrer ? document.referrer : ''));
            q += ('&surl=' + encodeURIComponent(document.location.href));
            //q += ('&cxt=' + encodeURIComponent(document.title));
            q += ('&t=' + time);
            //var adServerId = getRandomInt(1, 5);
            var baseUrl = getProtocol() + '//' +  BASE_AD_DELIVERY_URI;
            return baseUrl + '?' + q;
        }

        LeoAdRender.handleAdClick = function (adId) {
            // log click
            callBeaconLogTracking({
                url: getBaseUrlTracking(),
                vars: {
                    metric: 'click',
                    adid: adId,
                    beacon: adBeacons[adId]
                }
            });
            return false;
        }

        function getImageNode(title, width, height, srcUrl, adId, beacon, clickthroughUrl) {
            var imgId = 'leoads-iframe-ad-' + adId;
            var img = document.createElement('img');

            if (width == '100%' && height == '100%') {
                img.setAttribute('style', 'margin: 0 !important; padding: 0 !important;max-width:' + width);
            } else {
                img.setAttribute('style', 'margin: 0 !important; padding: 0 !important; width:' + width + 'px;height:' + height + 'px');
                img.setAttribute('width', width)
                img.setAttribute('height', height)
            }

            img.setAttribute('src', srcUrl);
            img.setAttribute('id', imgId);

            var ins = document.createElement('ins');
            ins.style.display = 'inline-table';
            ins.style.border = 'none';
            ins.style.margin = '0px';
            ins.style.padding = '0';
            ins.style.position = 'relative';
            ins.style['background-color'] = 'transparent';
            ins.style.width = width + 'px';
            ins.setAttribute('title', title);
            ins.appendChild(img);

            var aNode = document.createElement('a');
            aNode.setAttribute('href', clickthroughUrl);
            aNode.setAttribute('target', '_blank');
            aNode.setAttribute('id', 'leoads-click-handle-' + adId);
            aNode.setAttribute('onclick', 'LeoAdRender.handleAdClick(' + adId + ')');
            aNode.appendChild(ins);
            return aNode;
        }

        function getIframeNode(title, width, height, srcUrl, adId, beacon, clickTag) {
            var iframeId = 'leoads-iframe-ad-' + adId;
            var baseUrl = getProtocol() + srcUrl;
            var adIframe = document.createElement("iframe");
            adIframe.setAttribute('width', width);
            adIframe.setAttribute('height', height);
            adIframe.setAttribute('src', 'about:blank');
            adIframe.setAttribute('frameborder', '0');
            adIframe.setAttribute('marginwidth', '0');
            adIframe.setAttribute('marginheight', '0');
            adIframe.setAttribute('vspace', '0');
            adIframe.setAttribute('hspace', '0');
            adIframe.setAttribute('allowtransparency', 'true');
            adIframe.setAttribute('scrolling', 'no');
            adIframe.setAttribute('allowfullscreen', 'true');
            adIframe.setAttribute('style', 'margin:0!important;padding:0!important;');
            adIframe.setAttribute('id', iframeId);

            var ins = document.createElement('ins');
            ins.style.display = 'inline-table';
            ins.style.border = 'none';
            ins.style.margin = '0px';
            ins.style.padding = '0';
            ins.style.position = 'relative';
            ins.style['background-color'] = 'transparent';
            ins.style.width = width;
            ins.setAttribute('title', title);
            ins.appendChild(adIframe);

            function processHtmlAd(html) {
                setTimeout(function () {
                    var dstDoc = adIframe.contentDocument || adIframe.contentWindow.document;
                    dstDoc.open();
                    var headTag = "<head><base href=\"" + baseUrl + "\">";
                    headTag += '<style>body * {margin:auto;}</style>';
                    html = html.replace("<head>", headTag);
                    if (clickTag != '') {
                        html = html.replace("<body", "<body onclick=window.open('" + clickTag + "','_blank')");
                    } else {
                        var t = new Date().getTime();
                        var clickTrack = getBaseUrlTracking() + '?metric=click&adid=' + adId + '&beacon=' + beacon + '&t=' + t;
                        html = html.replace("</body>", `<script> var CLICK_TRACKING_URL="` + clickTrack + `" </script></body>`);
                    }
                    dstDoc.write(html);
                    dstDoc.close();
                    adIframe.style['visibility'] = 'hidden';
                    if (height === '100%') {
                        setTimeout(function () {
                            var newH = dstDoc.body.offsetHeight;
                            if (newH > 0) {
                                adIframe.setAttribute('height', newH);
                                adIframe.style['overflow-y'] = 'hidden';
                                adIframe.style['max-height'] = newH + 'px';
                            }
                            adIframe.style['visibility'] = 'visible';
                        }, 3000);
                    }
                }, 3);
            }

            var key = 'hd1_html_' + hashStringToNumber(srcUrl);
            var html = lscache.get(key);
            if (html) {
                processHtmlAd(html);
            } else {
                ajaxCorsRequest(srcUrl, function (html) {
                    lscache.set(key, html, 1440); // one day
                    processHtmlAd(html);
                });
            }

            return ins;
        }

        function processMediaTagging(data) {
            var urls = data.streamAdTrackingUrls;
            var js3rdCode = data.js3rdCode;
            //log all 3rd tracking URL
            if (typeof urls === 'object') {
                for (var i = 0; i < urls.length; i++) {
                    var obj = urls[i];
                    if (obj) {
                        var delay = obj.delay * 1000;
                        var url = obj.url;
                        setTimeout(function (u) {
                            callBeaconLogTracking({
                                url: u,
                                vars: {}
                            });
                        }, delay, url);
                    }
                }
            }
            if (typeof js3rdCode === 'string') {
                var adIframe = document.createElement("iframe");
                adIframe.setAttribute('width', 0);
                adIframe.setAttribute('height', 0);
                adIframe.setAttribute('src', 'about:blank');
                adIframe.setAttribute('style', 'display:none!important;');
                document.getElementsByTagName('body')[0].appendChild(adIframe);
                var dstDoc = adIframe.contentDocument || adIframe.contentWindow.document;
                dstDoc.write(js3rdCode);
                dstDoc.close();
            }
        }

        function renderWithPostscribe(placementNode, adCode) {
            var tagJS = 'https://cdnjs.cloudflare.com/ajax/libs/postscribe/2.0.8/postscribe.min.js';
            var callback = function () {
                //console.log(placementNode, adCode);
                window.postscribe(placementNode.parentNode, adCode);
            }
            loadScript(tagJS, callback);
        }

        //TODO add logo HTML, CSS
        var logoBASE64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAA3NCSVQICAjb4U/gAAAACXBIWXMAAAByAAAAcgGxhqEaAAAAGXRFWHRTb2Z0d2FyZQB3d3cuaW5rc2NhcGUub3Jnm+48GgAAAKhQTFRF/////wAA8nkv9Hsr9Xws83ws9Hws9Hws9Hws9Hws9Hws9H0t9IEz9II09YQ49YY69Yc89YpB9YxD9pBK9pFL9pRP95ta951d96Bh+KVp+Kpv+Kpw+LB5+bN9+byK+r6O+r+Q+sGT+sOV+sOW+sWY+saZ+seb+8ui+9Cp/Nm2/Nq3/Nu5/Ny6/ePF/eTG/evQ/u3U/vDY/vbh//jk//nm//vo//zq//3rSA/Q4gAAAAp0Uk5TAAEmcH+As7Xm9myQZpsAAACVSURBVBhXZU/HFsMgDCNQCHG6Z7r3XqTp+P8/KxY9NTqArGfZshAeUhtrjZbMPSKVEJCoCHUcSkbMigKtN1P+lPejv5u/X0N2SaHRcNmkU8ekIgyEfEC9JxMjrH/b58+IZlcWLITtbXFvFVkQ2DJ3B7c8Ug0WHtrYnfqr9WSPoWEtUfYoxlj7C0ZU7YRg5eil4/7O/wJ+TwxJXMcwHgAAAABJRU5ErkJggg==";
        var logoCSS = '.leo_ads{background: #e6f4ff; border-radius: 5px; position: absolute; z-index: 99999; right: 0; top: 0;' +
            'line-height: 15px!important; padding: 0px!important; text-align: left;' +
            'font-family: arial!important; font-size: 9px !important; cursor: pointer; overflow: hidden!important;}';
        logoCSS += '.leo_ads .leo_ads_name{margin-left: 4px!important;display: none;}';
        logoCSS += '.leo_ads:hover .leo_ads_name{display: inline-block!important;}';
        logoCSS += '.leo_ads > a{color: #464646!important; text-decoration: none!important;}';
        logoCSS += '.leo_ads .leo_ads_name > b{color: #3F51B5!important;}';
        logoCSS += '.leo_ads .leoads_brand{float: right; width: 16px!important; height: 16px!important; background: #fff url(' + logoBASE64 + ') no-repeat center !important;}';

        function processAdData(data, placementNode) {
            var adId = data.adId;
            var adCode = (data.adCode + '').trim();
            if (adId == 0 && adCode != '') {
                //passback to default ad code of placement
                renderWithPostscribe(placementNode, adCode);
                return;
            }

            // process for ad code from line item
            var css = 'display:block;border:none;margin:0 auto;padding:0;position:relative;overflow:hidden;background-color:transparent;text-align:center;';

            var timeToShowLogo = data.timeToShowLogo || 1400;
            var clickTag = data.clickthroughUrl;
            var adUrl = data.adMedia;

            var beacon = data.adBeacon;
            var width = data.width > 0 ? (data.width + 'px') : '100%';
            var height = data.height > 0 ? (data.height + 'px') : '100%';

            var placementId = data.placementId;
            var title = data.clickActionText;
            var adType = data.adType;
            var tracking3rdUrls = data.tracking3rdUrls || [];

            adBeacons[adId] = beacon;
            //console.log(adType); console.log(adCode);
            var pmadid = placementNode.getAttribute('data-leoadid');
            if (!pmadid) {
                placementNode.setAttribute('data-leoadid', adId);
                placementNode.setAttribute('data-width', data.width);
                placementNode.setAttribute('data-height', data.height);
                placementNode.setAttribute('data-ad-type', adType);
                placementNode.setAttribute('id', placementId + "-leoads-holder-" + +adId);

                var renderOk = false;
                ///////////////////////  BEGIN render for specific ad type /////////////////////////////

                //ADTYPE_HTML5_DISPLAY_AD = 5;//HTML5 Interactive Ad
                if (adType === 5) {
                    css = css + ';width:' + width + ';height:' + height;
                    placementNode.setAttribute('style', css);
                    var srcUrl = adUrl;
                    var renderedAdNode = getIframeNode(title, width, height, srcUrl, adId, beacon, clickTag);
                    placementNode.appendChild(renderedAdNode);
                    renderOk = true;
                }
                //ADTYPE_IMAGE_DISPLAY_AD = 6;// static banner: jpeg, gif, png
                else if (adType === 6) {
                    css = css + ';width:' + width + ';height:' + height;
                    placementNode.setAttribute('style', css);
                    var renderedAdNode = getImageNode(title, width, height, adUrl, adId, beacon, clickTag);
                    placementNode.appendChild(renderedAdNode);
                    renderOk = true;
                }
                // Ad Code in HTML or JavaScript, is render in Safeframe
                else if ((adType === 9 || adType === 7) && adCode != '') {
                    css = css + ';width:' + width + ';';
                    if (height === '100%') css = css + 'height:auto;';

                    placementNode.setAttribute('style', css);
                    var adIframeId = 'leoads_frame_' + placementId;
                    var adIframe = document.createElement("iframe");

                    var closeAd = function () {
                        adIframe.setAttribute('height', '0');
                        adIframe.setAttribute('width', '0');
                        placementNode.style.display = 'none';
                        adIframe.style.display = 'none';
                    }

                    window[adIframeId] = {
                        'onclose': closeAd,
                        'updateSize': function (iframeObj) {
                            setTimeout(function () {
                                var h = adDoc.body.offsetHeight;
                                console.log(adId + ' iframe.offsetHeight ' + h);
                                //iframeObj.style.height = h + 'px';
                                //adIframe.setAttribute('height', h);
                            }, 900);
                        }
                    };
                    adIframe.setAttribute('style', 'margin:0 auto!important; padding:0!important');
                    adIframe.setAttribute('width', width);
                    adIframe.setAttribute('height', height);
                    adIframe.setAttribute('src', 'about:blank');
                    adIframe.setAttribute('frameborder', '0');
                    adIframe.setAttribute('marginwidth', '0');
                    adIframe.setAttribute('marginheight', '0');
                    adIframe.setAttribute('vspace', '0');
                    adIframe.setAttribute('hspace', '0');
                    adIframe.setAttribute('allowtransparency', 'true');
                    adIframe.setAttribute('scrolling', 'no');
                    adIframe.setAttribute('allowfullscreen', 'true');
                    adIframe.setAttribute('id', adIframeId);
                    //adIframe.setAttribute('onload', 'window["' + adIframeId + '"].updateSize(this)');

                    //must append iframe before get contentDocument
                    placementNode.appendChild(adIframe);
                    var adDoc = adIframe.contentDocument || adIframe.contentWindow.document;
                    adDoc.write(adCode);
                    adDoc.write('<style type="text/css">body{text-align:center!important;margin-right:0px;margin-left:0px;margin-bottom:0px;margin-top:0px;} body * {margin:auto}</style>');
                    adDoc.write('<script> function closeAd(){parent["' + adIframeId + '"].onclose()};<\/script>');
                    adDoc.close();

                    //done write ad code, hide the placement util ad load is done                   
                    adIframe.setAttribute('height', 1);
                    adIframe.style.visibility = 'hidden';
                    var resizeAdIframe = function () {
                        //show when has ads
                        var h = adDoc.body.offsetHeight;
                        if (adDoc.body.children.length > 0 && h > 0) {
                            adIframe.style.visibility = 'visible';
                            adIframe.setAttribute('height', h);
                            adDoc.body.style.backgroundColor = 'transparent';
                            return true;
                        }
                        return false;
                    }
                    setTimeout(function () {
                        resizeAdIframe();
                        setTimeout(function () {
                            //resize for slow ad response
                            if (!resizeAdIframe()) {
                                //auto hide if no ads for Google Ads (DFP code)
                                var isNoAd = adDoc.body.children[0] && adDoc.body.children[0].style.display === 'none';
                                isNoAd = isNoAd || (adDoc.querySelector('iframe') === null && adDoc.querySelector('img'));
                                if (isNoAd) {
                                    closeAd();
                                }
                            }
                        }, 3800);
                    }, 1200);

                    if (adId > 0) {
                        //case Ad Media is render by 3rd JavaScript                        
                        window.focus();
                        var listener = window.addEventListener('blur', function () {
                            if (document.activeElement === document.getElementById(adIframeId)) {
                                // clicked ad in iframe
                                LeoAdRender.handleAdClick(adId);
                            }
                            window.removeEventListener('blur', listener);
                        });
                    }
                    renderOk = true;
                }
                // Direct JavaScript Code for OutStream
                else if (adType === 3 && adCode != '') {
                    loadScript(adCode);
                    renderOk = true;
                }

                ///////////////////////  END render for specific ad type /////////////////////////////

                if (renderOk && adId > 0) {
                    // log imp to Ad Server
                    callBeaconLogTracking({
                        url: getBaseUrlTracking(),
                        vars: {
                            metric: 'impression',
                            adid: adId,
                            beacon: adBeacons[adId]
                        }
                    });

                    // GA tracking
                    /*
                    leogajs('send', {
                        hitType: 'event',
                        eventCategory: 'adtrack',
                        eventAction: 'ad_' + adId,
                        eventLabel: 'impression',
                        eventValue: 1
                    });
                    */

                    // log imp for 3rd party
                    for (var j = 0; j < tracking3rdUrls.length; j++) {
                        var trackUrl = tracking3rdUrls[j];
                        callBeaconLogTracking({
                            url: trackUrl,
                            vars: {},
                            noParameter: true
                        });
                    }

                } else {
                    callBeaconLogTracking({
                        url: getBaseUrlTracking(),
                        vars: {
                            metric: 'errorjs',
                            adid: adId,
                            pmid: placementId,
                            beacon: adBeacons[adId]
                        }
                    });
                }

                if (renderOk) {
                    setTimeout(function () {
                        //render logo trademark, check at placement TAG first
                        var hideLogo = placementNode.getAttribute('data-hide-logo') === "true";
                        if (!hideLogo) {
                            //check from server
                            hideLogo = data.hideLogo;
                        }
                        if (data.width === 0 && data.height === 0) {
                            //turn off show logo if responsive
                            hideLogo = true;
                        }
                        if (!hideLogo) {
                            var logoADS = document.createElement("div");
                            logoADS.setAttribute("class", "leo_ads");
                            var bsUrl = 'https://leocdp.com';
                            logoADS.innerHTML += '<a target="_blank" href="' + bsUrl + '">' +
                                '<span class="leoads_brand"></span><span class="leo_ads_name">Ads by <b>LeoAds</b></span></a>';

                            var iframe = placementNode.getElementsByTagName("iframe")[0];
                            if (iframe) {
                                var sfSrc = iframe.getAttribute('src');
                                if (sfSrc.indexOf('//') < 0) {
                                    placementNode.appendChild(logoADS);
                                    addCSS(document, logoCSS);
                                }
                            } else {
                                placementNode.appendChild(logoADS);
                                addCSS(document, logoCSS);
                            }

                        }
                    }, timeToShowLogo);
                }
            }
        }

        LeoAdRender.getAds = function (pmIds, mapPmIdsNodes) {
            var h = function (text) {
                var ads = JSON.parse(text);
                for (var i = 0; i < ads.length; i++) {
                    var data = ads[i];
                    var placementId = data.placementId + '';
                    var pmNodes = mapPmIdsNodes[placementId];
                    if (pmNodes) {
                        var node = pmNodes.pop();
                        if (node) {
                            processAdData(data, node);
                        }
                    }
                    if (data.adType === 0) {
                        //pixel for Media 3rd Tagging: Comscore
                        processMediaTagging(data);
                    }
                }
            }
            // make request server
            var url = buildAdUrlRequest(pmIds) + '&at=display';
            ajaxCorsRequest(url, h);
        }

        LeoAdRender.render = function (visitorId) {
        	LeoAdRender.visitorId = visitorId;
        	
            var pmIds = [],
                mapPmIdsNodes = [];
            if (!window._LeoAdRenderProcessed) {
                window._LeoAdRenderProcessed = true;
            }

            var nodes = document.getElementsByClassName('leoads-placement');

            var cssPm = 'border:none!important;background:transparent!important;margin:0 auto!important;display:block!important;padding:5px 0!important';
            for (var i = 0; i < nodes.length; i++) {
                if (nodes[i]) {
                    var node = nodes[i];
                    var pmId = 0;

                    //case if tag is <ins class="leoads-placement" data-leoads-placement="1001" >
                    var spmid = node.getAttribute('data-leoads-placement');
                    var hdloaded = node.getAttribute('data-leoadsloaded');
                    var sid = node.getAttribute('id');
                    if (typeof spmid === 'string' && typeof sid != 'string' && typeof hdloaded != 'string') {
                        pmId = parseInt(spmid);
                        pmIds.push(parseInt(spmid));
                        node.setAttribute('data-leoadsloaded', 'true');
                        node.setAttribute('style', cssPm);
                    }

                    if (pmId > 0) {
                        var k = pmId + '';
                        if (typeof mapPmIdsNodes[k] !== 'object') {
                            mapPmIdsNodes[k] = [];
                        }
                        mapPmIdsNodes[k].push(node);
                    }
                }
            }
            if (pmIds.length > 0) {
                LeoAdRender.getAds(pmIds, mapPmIdsNodes);
            }

            var meta = document.createElement('meta');
            meta.setAttribute('name', 'referrer');
            meta.setAttribute('content', 'unsafe-url');
            var head = document.getElementsByTagName('head')[0];
            if (head) {
                head.appendChild(meta);
            }
        }

        global.LeoAdRender = LeoAdRender;

    })(typeof window === 'undefined' ? this : window);

    //put at the end

}