/* Customer 360 Admin -- Master Profiles list view. */
window.C360 = window.C360 || {};

(function (C360) {
  "use strict";

  var fmt = C360.fmt;
  var api = C360.config.api;
  var showApiError = C360.config.showApiError;

  var state = { skip: 0, limit: 20, q: "", domain: "", lifecycle_stage: "" };
  var searchDebounce = null;

  function rowVm(p) {
    var displayName = p.persona_name || p.full_name || ("Profile " + fmt.shortId(p.master_profile_id));
    return $.extend({}, p, {
      displayName: displayName,
      initials: fmt.initials(displayName),
      shortId: fmt.shortId(p.master_profile_id),
      tierLabel: p.membership_tier || p.clv_segment || "—",
      lifecycleLabel: fmt.titleCase(p.lifecycle_stage) || "—",
      lifecycleBadgeClass: fmt.lifecycleBadgeClass(p.lifecycle_stage),
      churnBadgeClass: fmt.churnBadgeClass(p.churn_risk_tier),
      clvLabel: (p.predictive_clv !== null && p.predictive_clv !== undefined) ? fmt.money(p.predictive_clv, "") : "—",
      engagementLabel: (p.engagement_score !== null && p.engagement_score !== undefined) ? fmt.score(p.engagement_score) : "—",
      lastActivityLabel: p.last_activity_at ? fmt.date(p.last_activity_at) : "—"
    });
  }

  function load(append) {
    if (!append) { state.skip = 0; $("#profiles-tbody").empty(); }
    $("#list-loading").removeClass("hidden");
    $("#list-empty").addClass("hidden");

    var params = { skip: state.skip, limit: state.limit };
    if (state.domain) params.domain = state.domain;
    if (state.lifecycle_stage) params.lifecycle_stage = state.lifecycle_stage;
    if (state.q) params.q = state.q;

    api("/master-profiles/", params)
      .done(function (profiles) {
        $("#list-loading").addClass("hidden");
        var vms = profiles.map(rowVm);
        $("#profiles-tbody").append(C360.templates.render("profiles-rows", { profiles: vms }));
        var total = $("#profiles-tbody tr").length;
        $("#list-count-label").text(total + " profile" + (total === 1 ? "" : "s") + " shown");
        $("#list-empty").toggleClass("hidden", total > 0);
        $("#btn-load-more").toggleClass("hidden", profiles.length < state.limit);
        state.skip += profiles.length;
      })
      .fail(function (xhr) { $("#list-loading").addClass("hidden"); showApiError("loading profiles", xhr); });
  }

  function bindEvents(onRowSelected) {
    $(document).on("click", ".profile-row", function () { onRowSelected($(this).data("id")); });

    $("#search-input").on("input", function () {
      var val = $(this).val();
      clearTimeout(searchDebounce);
      searchDebounce = setTimeout(function () { state.q = val; load(false); }, 350);
    });
    $("#domain-filter").on("change", function () { state.domain = $(this).val(); load(false); });
    $("#lifecycle-filter").on("change", function () { state.lifecycle_stage = $(this).val(); load(false); });
    $("#btn-load-more").on("click", function () { load(true); });
  }

  C360.listView = { load: load, bindEvents: bindEvents, rowVm: rowVm };
})(window.C360);
