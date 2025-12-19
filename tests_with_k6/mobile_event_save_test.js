import http from "k6/http";
import { check, sleep } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.2.0/index.js";
import { generateReport } from "./report_utils.js";

/* =====================================================
 * ENV CONFIG
 * ===================================================== */
const DEFAULT_CDP_HOSTNAME = "datahub4dcdp.bigdatavietnam.org";
const DEFAULT_MAX_USER = 500;

const DEFAULT_TOKEN_KEY = "TEST_TOKEN_KEY";
const DEFAULT_TOKEN_VALUE = "TEST_TOKEN_VALUE";

export const CDP_HOSTNAME = __ENV.CDP_HOSTNAME || DEFAULT_CDP_HOSTNAME;
export const ACCESS_TOKEN_KEY = __ENV.ACCESS_TOKEN_KEY || DEFAULT_TOKEN_KEY;
export const ACCESS_TOKEN_VALUE = __ENV.ACCESS_TOKEN_VALUE || DEFAULT_TOKEN_VALUE;
export const MAX_USER = (__ENV.MAX_USER && Number(__ENV.MAX_USER)) || DEFAULT_MAX_USER;

/* =====================================================
 * PERFORMANCE THRESHOLDS
 * ===================================================== */
export const P95_THRESHOLD_MS = 4000;
export const ERROR_RATE_LIMIT = 0.01;

/* =====================================================
 * K6 OPTIONS
 * ===================================================== */
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

/* =====================================================
 * REALISTIC NAME DATA
 * ===================================================== */
const FIRST_NAMES = [
  "Anh", "Minh", "Huy", "Khoa", "Long", "Tuấn", "Nam",
  "Linh", "Trang", "Mai", "Hà", "Ngọc", "Vy",
  "David", "Michael", "Daniel", "Emma", "Sophia", "Olivia"
];

const LAST_NAMES = [
  "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Vũ",
  "Smith", "Johnson", "Brown", "Taylor"
];

/* =====================================================
 * MOBILE USER AGENTS (REALISTIC)
 * ===================================================== */
const ANDROID_UAS = [
  "Mozilla/5.0 (Linux; Android 13; SM-G990E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36 OneInvest/1.3.2",
  "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36 OneInvest/1.3.2"
];

const IOS_UAS = [
  "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/21D50 OneInvest/1.3.2",
  "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/20G75 OneInvest/1.3.2"
];

/* =====================================================
 * GLOBAL VU STATE (STABLE IDENTITY)
 * ===================================================== */
const vuState = {};

/* =====================================================
 * DEVICE & USER MODEL
 * ===================================================== */
function createHumanIdentity(vu) {
  const first = FIRST_NAMES[vu % FIRST_NAMES.length];
  const last = LAST_NAMES[vu % LAST_NAMES.length];
  const email = `${first.toLowerCase()}.${last.toLowerCase()}${vu}@example.com`;

  return { first, last, email };
}

function getOrCreateDevice(vu) {
  if (!vuState[vu]) {
    const isAndroid = vu % 2 === 0;
    const human = createHumanIdentity(vu);

    vuState[vu] = {
      platform: isAndroid ? "android" : "ios",
      device_uuid: `${isAndroid ? "android" : "ios"}-${uuidv4()}`,
      ua: isAndroid
        ? ANDROID_UAS[vu % ANDROID_UAS.length]
        : IOS_UAS[vu % IOS_UAS.length],
      first_name: human.first,
      last_name: human.last,
      email: Math.random() < 0.7 ? human.email : "",
      dataLabels: "investing-mobile",
      installed_at: Date.now() - Math.floor(Math.random() * 30 * 86400000),
    };
  }
  return vuState[vu];
}

/* =====================================================
 * HEADERS
 * ===================================================== */
function buildHeaders(device) {
  return {
    "Content-Type": "application/json",
    Accept: "application/json",
    Connection: "keep-alive",
    "User-Agent": device.ua,
    tokenkey: ACCESS_TOKEN_KEY,
    tokenvalue: ACCESS_TOKEN_VALUE,
  };
}

/* =====================================================
 * EVENT FACTORY
 * ===================================================== */
function buildEvent(metric, device, tpName, tpUrl, eventData = {}) {
  return {
    metric,
    primary_email: device.email || "",
    first_name: device.first_name,
    last_name: device.last_name,
    touchpoint_name: tpName,
    touchpoint_url: tpUrl,
    user_agent: device.ua,
    user_device_uuid: device.device_uuid,
    event_data: {
      timestamp: Date.now(),
      platform: device.platform,
      ...eventData,
    },
    profile_data: device.email
      ? {
          dataLabels: device.dataLabels,
          installed_at: device.installed_at,
        }
      : {},
  };
}

/* =====================================================
 * SEND EVENT
 * ===================================================== */
function sendEvent(device, payload) {
  const url = `https://${CDP_HOSTNAME}/api/event/save`;

  const res = http.post(url, JSON.stringify(payload), {
    headers: buildHeaders(device),
  });

  check(res, {
    "status is 200": (r) => r.status === 200,
    "latency < 3s": (r) => r.timings.duration < 3000,
  });

  return res;
}

/* =====================================================
 * MAIN FLOW (MOBILE SESSION)
 * ===================================================== */
export default function () {
  const device = getOrCreateDevice(__VU);

  if (__VU === 1 && __ITER === 0) {
    console.log(`CDP_HOSTNAME=${CDP_HOSTNAME}`);
    console.log(`MAX_USER=${MAX_USER}`);
  }

  sendEvent(
    device,
    buildEvent(
      "app-launch",
      device,
      "investing-mobile-launch",
      "app://investing#launch"
    )
  );

  sendEvent(
    device,
    buildEvent(
      "overview-view",
      device,
      "investing-mobile-overview",
      "app://investing#overview"
    )
  );

  if (device.email && Math.random() < 0.5) {
    sendEvent(
      device,
      buildEvent(
        "login-success",
        device,
        "investing-mobile-login",
        "app://investing#login",
        {
          user_id: `${__VU}-${Math.floor(Math.random() * 1e9)}`,
        }
      )
    );
  }

  sendEvent(
    device,
    buildEvent(
      "portfolio-view",
      device,
      "investing-mobile-portfolio",
      "app://investing#portfolio",
      {
        portfolio_value: 100000000,
        positions_count: Math.floor(Math.random() * 5),
      }
    )
  );

  if (Math.random() < 0.4) {
    sendEvent(
      device,
      buildEvent(
        "app-background",
        device,
        "investing-mobile-lifecycle",
        "app://investing#background"
      )
    );
  }

  sleep(1 + Math.random() * 2);
}

/* =====================================================
 * REPORT
 * ===================================================== */
export function handleSummary(data) {
  return generateReport(
    data,
    "mobile_sdk_event_tracking",
    MAX_USER,
    CDP_HOSTNAME
  );
}
