/* Customer 360 Admin -- app bootstrap: injects static partials, wires up
 * tab/view switching, the settings modal, and kicks off the initial load. */
window.C360 = window.C360 || {};

(function (C360) {
  "use strict";

  Handlebars.registerHelper("json", function (v) { return JSON.stringify(v); });

  // Every nav tab is addressable via location.hash (e.g. #overview, #profiles)
  // so the current view survives reloads/back-forward navigation. A profile
  // detail view is addressable as #master_profile-<id> (set by
  // profile-detail-view.js when it loads).
  var TAB_NAMES = ["overview", "profiles", "segments", "journeys", "scoring", "analytics", "datasources", "admin"];
  var DEFAULT_TAB = "profiles";
  var PROFILE_HASH_RE = /^master_profile-(.+)$/;
  var lastRoutedProfileId = null;

  function currentHashTab() {
    var tab = (location.hash || "").replace(/^#/, "").split("/")[0];
    return TAB_NAMES.indexOf(tab) !== -1 ? tab : DEFAULT_TAB;
  }

  function setActiveTab(tab) {
    $(".tab-btn").removeClass("active");
    $(".tab-btn[data-tab='" + tab + "']").addClass("active");
  }

  function showListView() {
    $("#view-overview, #view-detail, #view-placeholder, #view-segments").addClass("hidden");
    $("#view-list").removeClass("hidden");
    setActiveTab("profiles");
  }

  function showOverviewView() {
    $("#view-list, #view-detail, #view-placeholder, #view-segments").addClass("hidden");
    $("#view-overview").removeClass("hidden");
    setActiveTab("overview");
    C360.overviewView.load();
  }

  function showDetailView(masterProfileId) {
    $("#view-list, #view-overview, #view-placeholder, #view-segments").addClass("hidden");
    $("#view-detail").removeClass("hidden");
    setActiveTab("profiles");
    lastRoutedProfileId = masterProfileId;
    C360.detailView.load(masterProfileId);
  }

  // Dispatches on the current location.hash: routes to a profile detail view
  // for #master_profile-<id>, otherwise falls back to tab-based routing.
  // Skips re-loading the detail view if it's already showing that profile
  // (e.g. when profile-detail-view.js itself set the hash after loading).
  function route() {
    var hash = (location.hash || "").replace(/^#/, "");
    var profileMatch = hash.match(PROFILE_HASH_RE);
    if (profileMatch) {
      var masterProfileId = decodeURIComponent(profileMatch[1]);
      if (masterProfileId !== lastRoutedProfileId) showDetailView(masterProfileId);
      return;
    }
    lastRoutedProfileId = null;
    routeToTab(currentHashTab());
  }

  function showSegmentsView() {
    $("#view-list, #view-overview, #view-detail, #view-placeholder").addClass("hidden");
    $("#view-segments").removeClass("hidden");
    setActiveTab("segments");
    C360.segmentsView.load();
  }

  function showPlaceholder(tab) {
    $("#view-list, #view-overview, #view-detail, #view-segments").addClass("hidden");
    $("#view-placeholder").removeClass("hidden");
    $("#placeholder-title").text(C360.fmt.titleCase(tab));
    setActiveTab(tab);
  }

  // Renders the view for a tab name without touching location.hash. Always
  // reuses the cached partials already injected into the DOM from
  // static/templates/ (see the $(function(){...}) bootstrap below) rather
  // than re-fetching or rebuilding markup.
  function routeToTab(tab) {
    if (tab === "profiles") { showListView(); } else if (tab === "overview") { showOverviewView(); } else if (tab === "segments") { showSegmentsView(); } else { showPlaceholder(tab); }
  }

  function bindChrome() {
    $("#btn-back-to-profiles").on("click", function () { location.hash = DEFAULT_TAB; });

    $(".tab-btn").on("click", function () {
      var tab = $(this).data("tab");
      var newHash = "#" + tab;
      if (location.hash === newHash) {
        // hashchange won't fire if the hash didn't actually change.
        routeToTab(tab);
      } else {
        location.hash = tab;
      }
    });

    $(window).on("hashchange", route);

    $("#btn-export-pdf").on("click", function () { window.print(); });

    $("#btn-settings").on("click", function () {
      var cfg = C360.config.current;
      $("#settings-api-base").val(cfg.apiBase);
      $("#settings-tenant-id").val(cfg.tenantId);
      $("#settings-modal").removeClass("hidden");
    });
    $("#btn-settings-cancel").on("click", function () { $("#settings-modal").addClass("hidden"); });
    $("#btn-settings-save").on("click", function () {
      C360.config.save($("#settings-api-base").val(), $("#settings-tenant-id").val());
      location.reload();
    });
  }

  $(function () {
    C360.templates.loadAll().done(function () {
      $("#app-header").html(C360.templates.html("tabs"));
      $("#view-list").html(C360.templates.html("profiles-list"));
      $("#view-placeholder").html(C360.templates.html("placeholder"));
      $("#segment-view-list").html(C360.templates.html("segments-list"));
      $("body").append(C360.templates.html("settings-modal"));

      $("#footer-api-base").text(C360.config.current.apiBase);

      bindChrome();
      C360.listView.bindEvents(showDetailView);
      C360.detailView.bindEvents();
      C360.segmentsView.bindEvents();

      C360.config.pingHealth();
      setInterval(C360.config.pingHealth, 30000);

      route();
      C360.listView.load(false);
    }).fail(function () {
      $("#alert-banner").removeClass("hidden").text(
        "Failed to load UI templates from static/templates/. Serve this folder with a static HTTP server " +
        "(opening index.html via file:// blocks the template/API requests)."
      );
    });
  });
})(window.C360);
