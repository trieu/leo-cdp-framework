/* Customer 360 Admin -- configuration + API client.
 * All profile/business data is fetched live from customer360-api (FastAPI),
 * which reads PostgreSQL. Nothing here is hardcoded demo data.
 *
 * NOTE: when served by frontend-admin/app.py (FastAPI), this file is NOT
 * used -- an explicit route renders jinja/config.js.j2 instead, injecting
 * FRONTEND_API_HOSTNAME/FRONTEND_TENANT_ID from the environment. This copy
 * is kept only as the default fallback when this folder is served by a
 * plain static file server (see README.md). Keep both files in sync. */
window.C360 = window.C360 || {};

(function (C360) {
  "use strict";

  var DEFAULTS = {
    apiBase: "http://localhost:8000/api/v1",
    tenantId: "11111111-1111-1111-1111-111111111111"
  };

  function getConfig() {
    return {
      apiBase: localStorage.getItem("c360.apiBase") || DEFAULTS.apiBase,
      tenantId: localStorage.getItem("c360.tenantId") || DEFAULTS.tenantId
    };
  }

  var CONFIG = getConfig();

  function api(path, params) {
    return $.ajax({
      url: CONFIG.apiBase + path,
      method: "GET",
      data: params || {},
      dataType: "json",
      headers: { "X-Tenant-Id": CONFIG.tenantId }
    });
  }

  function showApiError(context, xhr) {
    var msg = "Could not reach the Customer 360 API at " + CONFIG.apiBase + " (" + context + "). " +
      "Make sure customer360-api is running and reachable, and CORS is enabled. " +
      (xhr && xhr.status ? ("HTTP " + xhr.status) : "");
    $("#alert-banner").removeClass("hidden").text(msg);
    $("#api-status-dot").removeClass("bg-green-500 bg-slate-300").addClass("bg-red-500");
  }

  function pingHealth() {
    $.ajax({ url: CONFIG.apiBase.replace(/\/api\/v1$/, "") + "/health", method: "GET", dataType: "json", timeout: 4000 })
      .done(function () {
        $("#api-status-dot").removeClass("bg-red-500 bg-slate-300").addClass("bg-green-500");
        $("#alert-banner").addClass("hidden");
      })
      .fail(function (xhr) { showApiError("health check", xhr); });
  }

  function saveConfig(apiBase, tenantId) {
    localStorage.setItem("c360.apiBase", apiBase.trim());
    localStorage.setItem("c360.tenantId", tenantId.trim());
  }

  C360.config = {
    get: getConfig,
    current: CONFIG,
    api: api,
    showApiError: showApiError,
    pingHealth: pingHealth,
    save: saveConfig
  };
})(window.C360);
