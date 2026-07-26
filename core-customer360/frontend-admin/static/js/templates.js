/* Customer 360 Admin -- template loader.
 * Fetches every partial HTML fragment from static/templates/ over ajax,
 * compiles the data-driven ones with Handlebars, and registers the
 * profile-detail card partials so static/templates/profile-details.html can
 * include them via {{> name}}. Keeping each card in its own file makes the
 * dashboard easy to extend without touching a single giant template. */
window.C360 = window.C360 || {};

(function (C360) {
  "use strict";

  var BASE = "static/templates/";

  // Rendered directly (compiled Handlebars template functions).
  var STANDALONE = ["profiles-rows", "profile-details", "content-items", "overview-dashboard", "segments-rows", "segment-details"];

  // Injected as static HTML once (no Handlebars variables of their own).
  var STATIC_HTML = ["tabs", "settings-modal", "profiles-list", "placeholder", "segments-list"];

  // Registered as Handlebars partials so profile-details.html can do {{> name}}.
  var PARTIALS = [
    "identity", "channels", "overview", "segments",
    "engagement", "activity", "timeline", "scoring", "personalized-items"
  ];

  var ALL_NAMES = STANDALONE.concat(STATIC_HTML, PARTIALS);

  var raw = {};
  var compiled = {};

  function loadAll() {
    var requests = ALL_NAMES.map(function (name) {
      return $.get(BASE + name + ".html").done(function (text) {
        raw[name] = text;
        compiled[name] = Handlebars.compile(text);
      });
    });
    return $.when.apply($, requests).done(function () {
      PARTIALS.forEach(function (name) { Handlebars.registerPartial(name, raw[name]); });
    });
  }

  function render(name, context) {
    return compiled[name] ? compiled[name](context || {}) : "";
  }

  function html(name) {
    return raw[name] || "";
  }

  C360.templates = { loadAll: loadAll, render: render, html: html };
})(window.C360);
