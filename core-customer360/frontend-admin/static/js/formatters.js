/* Customer 360 Admin -- display formatters, label maps, and badge-class helpers.
 * Pure functions only; no DOM/API access here. */
window.C360 = window.C360 || {};

(function (C360) {
  "use strict";

  function fmtInt(v) { return (v === null || v === undefined) ? "—" : Number(v).toLocaleString(); }

  function fmtMoney(v, currency) {
    if (v === null || v === undefined) return "—";
    var n = Number(v);
    try {
      return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(n) + " " + (currency || "");
    } catch (e) { return n + " " + (currency || ""); }
  }

  function fmtPercent(v) { return (v === null || v === undefined) ? "—" : (Number(v) * 100).toFixed(0) + "%"; }
  function fmtScore(v) { return (v === null || v === undefined) ? "—" : Number(v).toFixed(1); }
  function fmtDate(v) { if (!v) return "—"; var d = new Date(v); return d.toLocaleDateString(); }
  function fmtDateTime(v) { if (!v) return "—"; var d = new Date(v); return d.toLocaleString(); }

  function initialsOf(name) {
    if (!name) return "?";
    var parts = name.trim().split(/\s+/);
    return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : "")).toUpperCase();
  }

  function shortId(id) { return id ? (id.substring(0, 8) + "…") : ""; }

  function maskMiddle(text, headLen, tailLen) {
    if (!text) return "";
    headLen = headLen === undefined ? 5 : headLen;
    tailLen = tailLen === undefined ? 3 : tailLen;
    if (text.length <= headLen + tailLen) return text;
    return text.substr(0, headLen) + "..." + text.substr(-tailLen);
  }

  function titleCase(s) { return (s || "").replace(/_/g, " ").replace(/\b\w/g, function (c) { return c.toUpperCase(); }); }

  var DOMAIN_LABELS = { banking: "Retail Banking", retail: "Retail Commerce", real_estate: "Real Estate", travel: "Travel" };
  var CHANNEL_ICONS = { mobile_app: "📱", web: "💻", internet_banking: "🏦", pos: "🛍️", call_center: "☎️", live_chat: "💬", branch_visit: "🏢", email: "✉️" };
  var CATEGORY_ICONS = { FINANCE: "💰", STOCK_TRADING: "📈", FEEDBACK: "⭐", GENERAL: "🔑", COMMERCE: "🛒", TRAVEL: "✈️", REAL_ESTATE: "🏠", EDUCATION: "🎓", SERVICE_INDUSTRY: "🛎️" };

  function lifecycleBadgeClass(stage) {
    var map = {
      vip: "bg-purple-100 text-purple-700", customer: "bg-green-100 text-green-700", lead: "bg-blue-100 text-blue-700",
      prospect: "bg-slate-100 text-slate-600", dormant: "bg-yellow-100 text-yellow-700", churn_risk: "bg-red-100 text-red-700"
    };
    return map[stage] || "bg-slate-100 text-slate-600";
  }

  function churnBadgeClass(tier) {
    var map = { low: "bg-green-100 text-green-700", medium: "bg-yellow-100 text-yellow-700", high: "bg-orange-100 text-orange-700", critical: "bg-red-100 text-red-700" };
    return map[tier] || "bg-slate-100 text-slate-600";
  }

  C360.fmt = {
    int: fmtInt,
    money: fmtMoney,
    percent: fmtPercent,
    score: fmtScore,
    date: fmtDate,
    dateTime: fmtDateTime,
    initials: initialsOf,
    shortId: shortId,
    maskMiddle: maskMiddle,
    titleCase: titleCase,
    DOMAIN_LABELS: DOMAIN_LABELS,
    CHANNEL_ICONS: CHANNEL_ICONS,
    CATEGORY_ICONS: CATEGORY_ICONS,
    lifecycleBadgeClass: lifecycleBadgeClass,
    churnBadgeClass: churnBadgeClass
  };
})(window.C360);
