/* Customer 360 Admin -- Profile Detail view (dashboard). */
window.C360 = window.C360 || {};

(function (C360) {
  "use strict";

  var fmt = C360.fmt;
  var api = C360.config.api;
  var showApiError = C360.config.showApiError;

  var currentProfileId = null;
  var currentContentType = "";
  var timelineLimit = 8;

  function periodDays() { return parseInt($("#period-select").val(), 10) || 90; }

  function timelineEntryVm(t) {
    var icon = t.kind === "transaction" ? "💳" : t.kind === "contact" ? "💬" : (fmt.CATEGORY_ICONS[(t.subtitle || "").toUpperCase()] || "🔔");
    return {
      icon: icon,
      title: t.title,
      channelLabel: fmt.titleCase(t.channel) || "—",
      timeLabel: fmt.dateTime(t.occurred_at),
      amountLabel: t.amount ? fmt.money(t.amount, t.currency) : null
    };
  }

  function buildDetailVm(profile, engagement, channelActivity, topInterests, timeline) {
    var displayName = profile.persona_name || profile.full_name || ("Profile " + fmt.shortId(profile.master_profile_id));

    var channels = [];
    if ((profile.device_ids || []).length) channels.push({ icon: "📱", label: "Mobile App", badge: profile.device_ids.length + " device(s)" });
    if ((profile.cookie_ids || []).length) channels.push({ icon: "💻", label: "Web / Cookies", badge: profile.cookie_ids.length });
    if ((profile.advertising_ids || []).length) channels.push({ icon: "📣", label: "Advertising IDs", badge: profile.advertising_ids.length });
    if (profile.email) channels.push({ icon: "✉️", label: "Email", badge: fmt.maskMiddle(profile.email) });
    if (profile.phone_number) channels.push({ icon: "☎️", label: "Phone", badge: fmt.maskMiddle(profile.phone_number) });
    if (profile.external_ids && Object.keys(profile.external_ids).length) channels.push({ icon: "🔗", label: "External IDs", badge: Object.keys(profile.external_ids).length });
    if (profile.account_numbers && profile.account_numbers.length) channels.push({ icon: "🏦", label: "Bank Accounts", badge: profile.account_numbers.length });
    if (!channels.length) channels.push({ icon: "—", label: "No identifiers captured yet", badge: "" });

    var attributeChips = [];
    if (profile.attributes) {
      Object.keys(profile.attributes).forEach(function (k) {
        attributeChips.push(fmt.titleCase(k) + ": " + profile.attributes[k]);
      });
    }

    var timelineVms = (timeline || []).map(timelineEntryVm);

    return {
      master_profile_id: profile.master_profile_id,
      domain: profile.domain,
      displayName: displayName,
      initials: fmt.initials(displayName),
      statusLabel: profile.status_code === 1 ? "Active Profile" : "Inactive Profile",
      statusBadgeClass: profile.status_code === 1 ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-600",
      personaName: profile.persona_name || "—",
      acquisitionSource: profile.acquisition_source || "—",
      firstSeenLabel: fmt.date(profile.created_at),
      lastSeenLabel: fmt.dateTime(profile.last_activity_at),
      tierLabel: profile.membership_tier || profile.clv_segment || "—",
      kycStatus: profile.kyc_status || "unknown",
      domainLabel: fmt.DOMAIN_LABELS[profile.domain] || fmt.titleCase(profile.domain),
      customerSinceLabel: fmt.date(profile.customer_since),
      lifecycleLabel: fmt.titleCase(profile.lifecycle_stage) || "—",
      personaSummary: profile.persona_summary || ("Profile in the " + (fmt.DOMAIN_LABELS[profile.domain] || profile.domain) + " domain, currently in the '" + fmt.titleCase(profile.lifecycle_stage) + "' lifecycle stage."),
      channels: channels,
      hasAttributes: attributeChips.length > 0,
      attributeChips: attributeChips,
      hasTags: (profile.segmentation_tags || []).length > 0,
      segmentationTags: profile.segmentation_tags || [],
      hasInterests: (topInterests || []).length > 0,
      topInterests: topInterests || [],

      periodDays: engagement.period_days,
      engagementScoreLabel: fmt.score(profile.engagement_score),
      totalLogins: fmt.int(engagement.total_logins),
      totalTransactions: fmt.int(engagement.total_transactions),
      totalSpentLabel: fmt.money(engagement.total_spent, engagement.currency),
      avgTransactionLabel: fmt.money(engagement.avg_transaction_amount, engagement.currency),
      lastInteractionLabel: fmt.dateTime(engagement.last_interaction_at),

      appSessions: fmt.int(channelActivity.app_sessions),
      webSessions: fmt.int(channelActivity.web_sessions),
      customerServiceContacts: fmt.int(channelActivity.customer_service_contacts),
      channelTransactions: fmt.int(channelActivity.transactions),

      hasTimeline: timelineVms.length > 0,
      timeline: timelineVms,

      lead_grade: profile.lead_grade || "—",
      leadScoreLabel: fmt.percent(profile.lead_conversion_probability),
      churn_risk_tier: profile.churn_risk_tier || "—",
      churnTextClass: (profile.churn_risk_tier === "high" || profile.churn_risk_tier === "critical") ? "text-red-600" : "text-slate-400",
      churnScoreLabel: fmt.percent(profile.churn_probability),
      predictiveClvLabel: fmt.money(profile.predictive_clv, ""),
      historicalClvLabel: fmt.money(profile.historical_clv, ""),
      completenessLabel: (profile.profile_completeness_score !== null && profile.profile_completeness_score !== undefined) ? Number(profile.profile_completeness_score).toFixed(0) + "%" : "—",
      identityConfidenceLabel: fmt.score(profile.identity_confidence_score),
      scoresUpdatedLabel: fmt.dateTime(profile.scores_updated_at)
    };
  }

  function loadContentItems(masterProfileId, itemType) {
    var params = { master_profile_id: masterProfileId, limit: 8 };
    if (itemType) params.item_type = itemType;
    api("/content-items/recommended", params)
      .done(function (items) {
        var vms = items.map(function (it) {
          return $.extend({}, it, { publishedLabel: fmt.date(it.published_at), ctaLabelOrDefault: it.cta_label || "View" });
        });
        $("#content-items-list").html(C360.templates.render("content-items", { hasItems: vms.length > 0, items: vms }));
      })
      .fail(function (xhr) { showApiError("loading personalized items", xhr); });
  }

  function loadMoreTimeline() {
    timelineLimit += 8;
    api("/master-profiles/" + currentProfileId + "/timeline", { limit: timelineLimit }).done(function (timeline) {
      var vms = (timeline || []).map(timelineEntryVm);
      var html = vms.map(function (t) {
        return '<li class="flex gap-3"><div class="w-2 h-2 mt-1.5 rounded-full bg-indigo-500 flex-shrink-0"></div>' +
          '<div class="flex-1 flex items-start justify-between gap-3"><div><div class="text-sm font-medium">' + t.icon + " " + $("<div>").text(t.title).html() + '</div>' +
          '<div class="text-xs text-slate-400">' + t.timeLabel + " &middot; " + $("<div>").text(t.channelLabel).html() + '</div></div>' +
          (t.amountLabel ? '<span class="text-xs bg-slate-100 rounded-full px-2 py-1 whitespace-nowrap">' + $("<div>").text(t.amountLabel).html() + '</span>' : "") + '</div></li>';
      }).join("");
      $("#detail-content").find("ol").first().html(html);
    });
  }

  function load(masterProfileId) {
    currentProfileId = masterProfileId;
    currentContentType = "";
    timelineLimit = 8;
    $(".content-tab-btn").removeClass("bg-indigo-600 text-white").addClass("bg-slate-100");
    $(".content-tab-btn[data-type='']").removeClass("bg-slate-100").addClass("bg-indigo-600 text-white");
    $("#detail-content").empty();
    $("#detail-loading").removeClass("hidden");

    var profileHash = "#master_profile-" + masterProfileId;
    if (location.hash !== profileHash) location.hash = profileHash;

    var days = periodDays();
    $.when(
      api("/master-profiles/" + masterProfileId),
      api("/master-profiles/" + masterProfileId + "/engagement-summary", { days: days }),
      api("/master-profiles/" + masterProfileId + "/channel-activity", { days: days }),
      api("/master-profiles/" + masterProfileId + "/top-interests", { limit: 5 }),
      api("/master-profiles/" + masterProfileId + "/timeline", { limit: timelineLimit })
    ).done(function (profileRes, engagementRes, channelRes, interestsRes, timelineRes) {
      var vm = buildDetailVm(profileRes[0], engagementRes[0], channelRes[0], interestsRes[0], timelineRes[0]);
      $("#detail-loading").addClass("hidden");
      $("#detail-content").html(C360.templates.render("profile-details", vm));
      loadContentItems(masterProfileId, "");
    }).fail(function (xhr) {
      $("#detail-loading").addClass("hidden");
      showApiError("loading profile detail", xhr);
    });
  }

  function reload() { if (currentProfileId) load(currentProfileId); }

  function bindEvents() {
    $(document).on("click", ".btn-copy-id", function () {
      var val = $(this).data("value");
      navigator.clipboard && navigator.clipboard.writeText(String(val));
      var btn = $(this);
      btn.text("copied!");
      setTimeout(function () { btn.text("copy"); }, 1200);
    });

    $(document).on("click", "#btn-timeline-more", loadMoreTimeline);

    $(document).on("click", ".content-tab-btn", function () {
      $(".content-tab-btn").removeClass("bg-indigo-600 text-white").addClass("bg-slate-100");
      $(this).removeClass("bg-slate-100").addClass("bg-indigo-600 text-white");
      currentContentType = $(this).data("type") || "";
      if (currentProfileId) loadContentItems(currentProfileId, currentContentType);
    });

    $("#period-select").on("change", reload);
  }

  C360.detailView = { load: load, reload: reload, bindEvents: bindEvents };
})(window.C360);
