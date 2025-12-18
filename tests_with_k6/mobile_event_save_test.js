import http from "k6/http";
import { check, sleep } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.2.0/index.js";
import { generateReport } from "./report_utils.js";

// =============================
// ENV CONFIG
// =============================
const DEFAULT_CDP_HOSTNAME = "datahub.investing.vn";
const DEFAULT_MAX_USER = 500;

// ⚠️ REQUIRED TOKENS (match nginx header_tokenkey / header_tokenvalue)
const DEFAULT_TOKEN_KEY = "TEST_TOKEN_KEY";
const DEFAULT_TOKEN_VALUE = "TEST_TOKEN_VALUE";

export const CDP_HOSTNAME =
  __ENV.CDP_HOSTNAME || DEFAULT_CDP_HOSTNAME;

export const ACCESS_TOKEN_KEY =
  __ENV.ACCESS_TOKEN_KEY || DEFAULT_TOKEN_KEY;

export const ACCESS_TOKEN_VALUE =
  __ENV.ACCESS_TOKEN_VALUE || DEFAULT_TOKEN_VALUE;

export const MAX_USER =
  (__ENV.MAX_USER && Number(__ENV.MAX_USER)) || DEFAULT_MAX_USER;

// =============================
// PERFORMANCE THRESHOLDS
// =============================
export const P95_THRESHOLD_MS = 3000;
export const ERROR_RATE_LIMIT = 0.01;

// =============================
// K6 OPTIONS
// =============================
export const options = {
  stages: [
    { duration: "10s", target: Math.floor(MAX_USER * 0.1) },
    { duration: "20s", target: Math.floor(MAX_USER * 0.3) },
    { duration: "30s", target: Math.floor(MAX_USER * 0.5) },
    { duration: "60s", target: MAX_USER },
    { duration: "10s", target: 0 },
  ],
  thresholds: {
    http_req_duration: [`p(95)<${P95_THRESHOLD_MS}`],
    http_req_failed: [`rate<${ERROR_RATE_LIMIT}`],
  },
};

// =============================
// MOBILE USER AGENTS (REAL LOGS)
// =============================
const MOBILE_UAS = [
  "Dart/3.9 (dart:io)",
  "TargetPlatform.android",
  "Mozilla/5.0 (Linux; Android 10; Samsung SM-N960F Build/QP1A.190711.020) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36 OneInvest/1.0.0(176552)",
  "Mozilla/5.0 (Linux; Android 13; Samsung SM-G990E Build/TP1A.220624.014) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36 OneInvest/1.0.0(176552)",
];

// =============================
// GLOBAL VU STATE
// =============================
const vuState = {};

// =============================
// DEVICE / VISITOR MODEL
// =============================
function getOrCreateDevice(vu) {
  if (!vuState[vu]) {
    vuState[vu] = {
      device_uuid: Math.random() > 0.5 ? `android-${uuidv4()}` : "",
      email: Math.random() > 0.5 ? `user_${vu}@example.com` : "",
      first_name: "Visitor",
      last_name: "",
      dataLabels: "investing-mobile",
      ua: MOBILE_UAS[Math.floor(Math.random() * MOBILE_UAS.length)],
    };
  }
  return vuState[vu];
}

// =============================
// HEADERS (CRITICAL FIX HERE)
// =============================
function buildHeaders(device) {
  return {
    "Content-Type": "application/json",
    "Accept": "application/json",
    "Connection": "keep-alive",
    "User-Agent": device.ua,

    // 🔥 for Event Observer in LEO CDP Data Journey Map
    "tokenkey": ACCESS_TOKEN_KEY,
    "tokenvalue": ACCESS_TOKEN_VALUE,
  };
}

// =============================
// EVENT FACTORY
// =============================
function buildEvent(metric, device, tpName, tpUrl, eventData = {}) {
  return {
    metric,
    primary_phone: "",
    primary_email: device.email || "",
    first_name: device.first_name,
    last_name: device.last_name,
    touchpoint_name: tpName,
    touchpoint_url: tpUrl,
    event_data: {
      timestamp: Date.now(),
      ...eventData,
    },
    profile_data: device.email
      ? {
          dataLabels: device.dataLabels,
          personUri:
            "https://cdn-icons-png.flaticon.com/512/3607/3607444.png",
        }
      : {},
    user_agent: device.ua,
    user_device_uuid: device.device_uuid,
  };
}

// =============================
// SEND EVENT
// =============================
function sendEvent(device, payload) {
  const url = `https://${CDP_HOSTNAME}/api/event/save`;

  const res = http.post(
    url,
    JSON.stringify(payload),
    { headers: buildHeaders(device) }
  );

  check(res, {
    "status is 200": (r) => r.status === 200,
    "latency < 3s": (r) => r.timings.duration < 3000,
  });

  return res;
}

// =============================
// MAIN FLOW (MATCHES REAL LOGS)
// =============================
export default function () {
  const vu = __VU;
  const device = getOrCreateDevice(vu);

  if (__VU === 1 && __ITER === 0) {
    console.log(`CDP_HOSTNAME = ${CDP_HOSTNAME}`);
    console.log(`ACCESS_TOKEN_KEY = ${ACCESS_TOKEN_KEY}`);
    console.log(`ACCESS_TOKEN_VALUE = ${ACCESS_TOKEN_VALUE}`);
    console.log(`MAX_USER = ${MAX_USER}`);
  }

  // overview-view
  sendEvent(
    device,
    buildEvent(
      "overview-view",
      device,
      "investing-mobile-overview",
      "app://example.investing#overview"
    )
  );

  // watchlist-page-view
  sendEvent(
    device,
    buildEvent(
      "watchlist-page-view",
      device,
      "investing-mobile-watchlist",
      "app://example.investing#watchlist"
    )
  );

  // login-success (sometimes duplicated in real logs)
  if (Math.random() < 0.4) {
    sendEvent(
      device,
      buildEvent(
        "login-success",
        device,
        "investing-mobile-personallogin",
        "app://example.investing#personal",
        {
          email: device.email || "guest@example.com",
          account_name: device.email || "Guest",
          user_id: `${Math.floor(Math.random() * 1e18)}`,
        }
      )
    );
  }

  // portfolio-view
  sendEvent(
    device,
    buildEvent(
      "portfolio-view",
      device,
      "investing-mobile-asset",
      "app://example.investing#asset",
      {
        profolio_value: 100000000,
        positions_count: 0,
      }
    )
  );

  // app-background / foreground lifecycle
  if (Math.random() < 0.5) {
    sendEvent(
      device,
      buildEvent(
        "app-background",
        device,
        "investing-mobile-more",
        "app://example.investing#more"
      )
    );
  }

  if (Math.random() < 0.5) {
    sendEvent(
      device,
      buildEvent(
        "app-foreground",
        device,
        "investing-mobile-more",
        "app://example.investing#more",
        { duration: Math.floor(Math.random() * 10) }
      )
    );
  }

  sleep(1 + Math.random() * 2);
}

// =============================
// REPORT
// =============================
export function handleSummary(data) {
  return generateReport(
    data,
    "mobile_sdk_event_save_with_token",
    MAX_USER,
    CDP_HOSTNAME
  );
}
