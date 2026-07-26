/* Customer 360 Admin -- app bootstrap: injects static partials, wires up
 * tab/view switching, the settings modal, and kicks off the initial load. */
window.C360 = window.C360 || {};

(function (C360) {
  "use strict";

  Handlebars.registerHelper("json", function (v) { return JSON.stringify(v); });

  function setActiveTab(tab) {
    $(".tab-btn").removeClass("active");
    $(".tab-btn[data-tab='" + tab + "']").addClass("active");
  }

  function showListView() {
    $("#view-detail, #view-placeholder").addClass("hidden");
    $("#view-list").removeClass("hidden");
    setActiveTab("profiles");
  }

  function showDetailView(masterProfileId) {
    $("#view-list, #view-placeholder").addClass("hidden");
    $("#view-detail").removeClass("hidden");
    setActiveTab("profiles");
    C360.detailView.load(masterProfileId);
  }

  function showPlaceholder(tab) {
    $("#view-list, #view-detail").addClass("hidden");
    $("#view-placeholder").removeClass("hidden");
    $("#placeholder-title").text(C360.fmt.titleCase(tab));
    setActiveTab(tab);
  }

  function bindChrome() {
    $("#btn-back-to-profiles").on("click", showListView);

    $(".tab-btn").on("click", function () {
      var tab = $(this).data("tab");
      if (tab === "profiles") { showListView(); } else { showPlaceholder(tab); }
    });

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
      $("body").append(C360.templates.html("settings-modal"));

      $("#footer-api-base").text(C360.config.current.apiBase);

      bindChrome();
      C360.listView.bindEvents(showDetailView);
      C360.detailView.bindEvents();

      C360.config.pingHealth();
      setInterval(C360.config.pingHealth, 30000);

      showListView();
      C360.listView.load(false);
    }).fail(function () {
      $("#alert-banner").removeClass("hidden").text(
        "Failed to load UI templates from static/templates/. Serve this folder with a static HTTP server " +
        "(opening index.html via file:// blocks the template/API requests)."
      );
    });
  });
})(window.C360);
