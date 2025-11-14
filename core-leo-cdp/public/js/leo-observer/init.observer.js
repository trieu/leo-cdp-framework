// ===============================
// Global configs
// ===============================
var ROOT_DOMAIN = '';
var PARENT_ORIGIN = '*';
var INJECTED_VISITOR_ID = false;

if (location.hash.length > 0) {
  var toks = location.hash.substring(1).split('_');
  ROOT_DOMAIN = toks[0];
  PARENT_ORIGIN = toks[1];

  if (toks.length === 3) {
    var tok = toks[2];
    if (typeof tok === "string" && tok.length > 20) {
      INJECTED_VISITOR_ID = tok;
    }
  }
} else {
  console.error('ROOT_DOMAIN is empty');
}

var LEO_LOG_URL = '//' + ROOT_DOMAIN;

// Base URLs
var PREFIX_SESSION_INIT_URL = LEO_LOG_URL + "/cxs-pf-init";
var PREFIX_UPDATE_PROFILE_URL = LEO_LOG_URL + "/cxs-pf-update";
var PREFIX_EVENT_VIEW_URL = LEO_LOG_URL + "/etv";
var PREFIX_EVENT_ACTION_URL = LEO_LOG_URL + "/eta";
var PREFIX_EVENT_CONVERSION_URL = LEO_LOG_URL + "/etc";
var PREFIX_EVENT_FEEDBACK_URL = LEO_LOG_URL + "/efb";

// Configs
var OBSERVE_WITH_FINGERPRINT = true;

// ===============================
// Utils
// ===============================

// Cross-browser event binding
function bindEvent(element, eventName, eventHandler) {
  if (element.addEventListener) {
    element.addEventListener(eventName, eventHandler, false);
  } else if (element.attachEvent) {
    element.attachEvent('on' + eventName, eventHandler);
  }
}

// Send message to parent window
function sendMessage(msg) {
  window.parent.postMessage(msg, PARENT_ORIGIN);
}

// Load external JS
function loadScript(src, callback) {
  var s = document.createElement('script');
  s.type = 'text/javascript';
  s.src = src;
  s.async = true;

  s.onload = s.onreadystatechange = function () {
    if (!this.readyState || this.readyState === 'complete') {
      if (callback) callback();
    }
  };

  document.getElementById('leoproxy').appendChild(s);
}

// ===============================
// Proxy handler
// ===============================
var queueTracking = [];
var queueUpdating = [];
var synchVisitorCall = false;

function proxyCallHandler(data) {
  console.log("proxyCallHandler", data);

  if (data.call === 'getContextSession' && data.params) {
    LeoEventObserver.getContextSession(data.params);
  }
  else if (data.call === 'doTracking' && data.eventType && data.params) {
    queueTracking.push(data);
  }
  else if (data.call === 'updateProfile' && data.params) {
    queueUpdating.push(data);
  }
  else if (data.call === 'synchLeoVisitorId') {
    synchVisitorCall = data;
  }
}

// Listen for messages from parent
bindEvent(window, 'message', function (e) {
  var json = {};
  try {
    json = JSON.parse(e.data);
  } catch (e) {
    console.error(e);
  }

  if (typeof json.call === "string") {
    proxyCallHandler(json);
  }
});

// ===============================
// Main init routine
// ===============================
function codeIsReady() {
  LeoEventObserver.initFingerprint(function (fingerprintId) {

    // Sync visitor ID
    LeoEventObserver.visitorId = LeoEventObserver.getVisitorId();

    // Notify parent window that observer is ready
    setTimeout(function () {
      sendMessage("LeoObserverProxyLoaded");
    }, 300);

    // Queue processor
    var queueProcessor = function () {
      var len = queueTracking.length;
      while (len > 0) {
        var data = queueTracking.pop();
        LeoEventObserver.doTracking(data.eventType, data.params);
        len--;
      }

      len = queueUpdating.length;
      while (len > 0) {
        var data = queueUpdating.pop();
        LeoEventObserver.updateProfile(data.params);
        len--;
      }

      if (synchVisitorCall !== false) {
        sendMessage("synchLeoVisitorId-" + LeoEventObserver.visitorId);
        synchVisitorCall = false;
      }
    };

    setInterval(queueProcessor, 799);
  });
}

// ===============================
// Load remote observer script
// ===============================

var cbKey = "20251016";
var leoObserverJsUrl =
  "https://gcore.jsdelivr.net/gh/USPA-Technology/leo-cdp-static-files@v0.9.1/js/leo-observer/leo.observer.min.js?cb=" + cbKey;

var isDev = ROOT_DOMAIN === "obs.example.com";
if (isDev) {
  leoObserverJsUrl =
    "https://obs.example.com/public/js/leo-observer-src/leo.observer.js?cb=" + (new Date().getTime());
}

loadScript(leoObserverJsUrl, codeIsReady);
