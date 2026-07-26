/* Customer 360 Admin -- Segments (Audience Builder) list + detail view. */
window.C360 = window.C360 || {};

(function (C360) {
  "use strict";

  var fmt = C360.fmt;
  var api = C360.config.api;
  var showApiError = C360.config.showApiError;

  var listState = { skip: 0, limit: 20 };
  var matchedState = { skip: 0, limit: 20 };
  var currentSegmentId = null;

  function processedByLabel(v) { return v === "ai_agent" ? "AI Agent" : "Human"; }
  function processedByBadgeClass(v) { return v === "ai_agent" ? "bg-purple-100 text-purple-700" : "bg-slate-100 text-slate-600"; }
  function activeLabel(v) { return v ? "Active" : "Inactive"; }
  function activeBadgeClass(v) { return v ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-500"; }
  function domainLabel(domain) { return fmt.DOMAIN_LABELS[domain] || (domain === "all" ? "All domains" : fmt.titleCase(domain)); }

  function segmentRowVm(s) {
    return $.extend({}, s, {
      domainLabel: domainLabel(s.domain),
      processedByLabel: processedByLabel(s.processed_by),
      processedByBadgeClass: processedByBadgeClass(s.processed_by),
      activeLabel: activeLabel(s.is_active),
      activeBadgeClass: activeBadgeClass(s.is_active),
      memberCountLabel: fmt.int(s.member_count),
      createdLabel: fmt.date(s.created_at)
    });
  }

  function segmentDetailVm(s) {
    return $.extend({}, s, {
      domainLabel: domainLabel(s.domain),
      processedByLabel: processedByLabel(s.processed_by) + (s.processed_by === "ai_agent" ? "" : " (jQuery QueryBuilder)"),
      processedByBadgeClass: processedByBadgeClass(s.processed_by),
      activeLabel: activeLabel(s.is_active),
      activeBadgeClass: activeBadgeClass(s.is_active),
      memberCountLabel: fmt.int(s.member_count),
      lastComputedLabel: fmt.dateTime(s.last_computed_at),
      createdLabel: fmt.dateTime(s.created_at),
      updatedLabel: fmt.dateTime(s.updated_at),
      hasSqlRules: !!s.sql_rules,
      hasJsonRules: !!(s.json_rules && Object.keys(s.json_rules).length)
    });
  }

  function loadList(append) {
    if (!append) { listState.skip = 0; $("#segments-tbody").empty(); }
    $("#segments-list-loading").removeClass("hidden");
    $("#segments-list-empty").addClass("hidden");

    api("/segments/", { skip: listState.skip, limit: listState.limit })
      .done(function (segments) {
        $("#segments-list-loading").addClass("hidden");
        var vms = segments.map(segmentRowVm);
        $("#segments-tbody").append(C360.templates.render("segments-rows", { segments: vms }));
        var total = $("#segments-tbody tr").length;
        $("#segments-count-label").text(total + " segment" + (total === 1 ? "" : "s") + " shown");
        $("#segments-list-empty").toggleClass("hidden", total > 0);
        $("#btn-segments-load-more").toggleClass("hidden", segments.length < listState.limit);
        listState.skip += segments.length;
      })
      .fail(function (xhr) { $("#segments-list-loading").addClass("hidden"); showApiError("loading segments", xhr); });
  }

  function loadMatchedProfiles(segmentId, append) {
    if (!append) { matchedState.skip = 0; $("#segment-matched-tbody").empty(); }
    $("#segment-matched-loading").removeClass("hidden");
    $("#segment-matched-empty").addClass("hidden");

    api("/segments/" + segmentId + "/matched-profiles", { skip: matchedState.skip, limit: matchedState.limit })
      .done(function (profiles) {
        $("#segment-matched-loading").addClass("hidden");
        var vms = profiles.map(C360.listView.rowVm);
        $("#segment-matched-tbody").append(C360.templates.render("profiles-rows", { profiles: vms }));
        var total = $("#segment-matched-tbody tr").length;
        $("#segment-matched-count-label").text(total + " matched profile" + (total === 1 ? "" : "s") + " shown");
        $("#segment-matched-empty").toggleClass("hidden", total > 0);
        $("#btn-segment-matched-load-more").toggleClass("hidden", profiles.length < matchedState.limit);
        matchedState.skip += profiles.length;
      })
      .fail(function (xhr) { $("#segment-matched-loading").addClass("hidden"); showApiError("loading matched profiles", xhr); });
  }

  function loadDetail(segmentId) {
    currentSegmentId = segmentId;
    matchedState.skip = 0;
    $("#segment-detail-content").empty();
    $("#segment-detail-loading").removeClass("hidden");

    api("/segments/" + segmentId)
      .done(function (segment) {
        $("#segment-detail-loading").addClass("hidden");
        $("#segment-detail-content").html(C360.templates.render("segment-details", segmentDetailVm(segment)));
        loadMatchedProfiles(segmentId, false);
      })
      .fail(function (xhr) {
        $("#segment-detail-loading").addClass("hidden");
        showApiError("loading segment detail", xhr);
      });
  }

  function showList() {
    $("#segment-view-detail").addClass("hidden");
    $("#segment-view-list").removeClass("hidden");
  }

  function showDetail(segmentId) {
    $("#segment-view-list").addClass("hidden");
    $("#segment-view-detail").removeClass("hidden");
    loadDetail(segmentId);
  }

  function load() {
    showList();
    loadList(false);
  }

  function bindEvents() {
    $(document).on("click", ".segment-row", function () { showDetail($(this).data("id")); });
    $(document).on("click", "#btn-segments-load-more", function () { loadList(true); });
    $(document).on("click", "#btn-segment-matched-load-more", function () { loadMatchedProfiles(currentSegmentId, true); });
    $(document).on("click", "#btn-back-to-segments", showList);
  }

  C360.segmentsView = { load: load, bindEvents: bindEvents };
})(window.C360);
