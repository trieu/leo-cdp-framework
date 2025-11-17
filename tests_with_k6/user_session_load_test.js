import http from "k6/http";
import { check, sleep } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.2.0/index.js";
import { generateReport } from "./report_utils.js";


// =============================
//   LOAD FROM ENV WITH FALLBACK
// =============================
const DEFAULT_CDP_HOSTNAME = "datahub4dcdp.bigdatavietnam.org";
const DEFAULT_EVENT_OBSERVER_ID = "2orlGAG48Iq4UWRzp3Ol6k";
const DEFAULT_MAX_USER = 1100;

// =============================
//   LEO CDP OBSERVER CONFIG
// =============================
export const CDP_HOSTNAME = __ENV.CDP_HOSTNAME || DEFAULT_CDP_HOSTNAME;


// =============================
// 1) Go to LEO CDP Demo Admin, 
// 2) Copy valid EVENT_OBSERVER_ID at https://dcdp.bigdatavietnam.org/#calljs-leoCdpRouter('Data_Journey_Map','')
// =============================
export const EVENT_OBSERVER_ID = __ENV.EVENT_OBSERVER_ID || DEFAULT_EVENT_OBSERVER_ID;

// ==========================================
// LEO CDP LOAD TEST CONFIG
// ==========================================

// Max concurrent users for this test, recommended: >= 500 for realistic load
export const MAX_USER = (__ENV.MAX_USER && Number(__ENV.MAX_USER)) || DEFAULT_MAX_USER;

// Performance thresholds (extracted as constants)
export const P95_THRESHOLD_MS = 5000;   // 95% request must be < 5s
export const ERROR_RATE_LIMIT = 0.01;   // <1% total errors allowed

// Optional: control ramping speed
export const RAMP_SPEED = "10s";        // duration for small step ramps


// Just to show the resolved configuration at test start
console.log(`Using CDP_HOSTNAME = ${CDP_HOSTNAME}`);
console.log(`Using EVENT_OBSERVER_ID = ${EVENT_OBSERVER_ID}`);
console.log(`Using MAX_USER = ${MAX_USER}`);

// ==========================================
// K6 OPTIONS
// ==========================================
export const options = {
  stages: [
    // Smooth warm-up
    { duration: RAMP_SPEED, target: Math.floor(MAX_USER * 0.10) },  // 10%
    { duration: RAMP_SPEED, target: Math.floor(MAX_USER * 0.20) },  // 20%
    { duration: "20s", target: Math.floor(MAX_USER * 0.33) },       // 33%
    { duration: "30s", target: Math.floor(MAX_USER * 0.50) },       // 50%
    { duration: "60s", target: Math.floor(MAX_USER * 0.75) },       // 75%
    // Steady load phase (very important)
    { duration: "120s", target: MAX_USER },                         // soak at peak
    // Cool down
    { duration: "10s", target: 0 },
  ],

  thresholds: {
    http_req_duration: [`p(95)<${P95_THRESHOLD_MS}`],       // dynamic threshold
    http_req_failed: [`rate<${ERROR_RATE_LIMIT}`],          // < ERROR_RATE_LIMIT
  },
};


// =============================
//   GLOBAL VU STATE
// =============================
const sessionMap = {};

// =============================
//   USER AGENTS (realistic pool)
// =============================
const USER_AGENTS = [
  // Mobile
  "Mozilla/5.0 (iPhone; CPU iPhone OS 15_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
  "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0 Mobile Safari/537.36",
  "Mozilla/5.0 (Linux; Android 12; Samsung SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0 Mobile Safari/537.36",

  // Desktop
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0 Safari/537.36",
  "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_2) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
  "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36",

  // Firefox
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:118.0) Gecko/20100101 Firefox/118.0",
  "Mozilla/5.0 (Macintosh; Intel Mac OS X 12.5; rv:117.0) Gecko/20100101 Firefox/117.0",
];

// =============================
//   Get or assign User-Agent
// =============================
function getUserAgent(vu) {
  if (!sessionMap[vu].ua) {
    const ua = USER_AGENTS[Math.floor(Math.random() * USER_AGENTS.length)];
    sessionMap[vu].ua = ua;
  }
  return sessionMap[vu].ua;
}


// =============================
//   SESSION GENERATION
// =============================
function createNewSession() {
  return {
    visid: uuidv4().replace(/-/g, ""),
    fgp: uuidv4().replace(/-/g, ""),
    ctxsk: uuidv4().substring(0, 22), // realistic short session key
    sessionStart: Date.now(),
  };
}

function getSession(vu) {
  const now = Date.now();
  let s = sessionMap[vu];

  if (!s) {
    s = createNewSession();
    sessionMap[vu] = s;
    return s;
  }

  const ageMin = (now - s.sessionStart) / 60000;
  if (ageMin > 30) {
    s = createNewSession();
    sessionMap[vu] = s;
  }
  return s;
}

// =============================
//   MAIN TEST EXECUTION
// =============================
export default function () {
  const vu = __VU;
  const session = getSession(vu);
  const userAgent = getUserAgent(vu);

  const headers = {
    "User-Agent": userAgent,
    "Accept": "application/json, text/plain, */*",
    "Connection": "keep-alive",
  };

  // -----------------------------------------------------
  //   1. INIT SESSION CALL (cxs-pf-init)
  // -----------------------------------------------------
  const initParams = {
    obsid: EVENT_OBSERVER_ID,
    mediahost: "bigdatavietnam.org",
    tpurl: "https%3A%2F%2Fwww.bigdatavietnam.org%2F",
    tpname: "Big%20Data%20Vietnam",
    fgp: session.fgp,
    visid: session.visid,
  };

  const initUrl =
    `https://${CDP_HOSTNAME}/cxs-pf-init?` +
    Object.entries(initParams)
      .map(([k, v]) => `${k}=${v}`)
      .join("&");

  const initRes = http.get(initUrl, { headers });

  check(initRes, {
    "INIT: status 200": (r) => r.status === 200,
    "INIT: duration < 8s": (r) => r.timings.duration < 8000,
  });

  // -----------------------------------------------------
  //   2. PAGE-VIEW EVENT (etv)
  // -----------------------------------------------------
  const pageviewParams = {
    obsid: EVENT_OBSERVER_ID,
    mediahost: "bigdatavietnam.org",
    tprefurl: "https://www.youtube.com",
    tprefdomain: "youtube.com",
    tpurl: "https%3A%2F%2Fwww.bigdatavietnam.org%2F",
    tpname: "Big%20Data%20Vietnam",
    metric: "page-view",
    eventdata: encodeURIComponent(JSON.stringify({ demo: 1, CCU_TEST: MAX_USER })),
    visid: session.visid,
    fgp: session.fgp,
    ctxsk: session.ctxsk,
  };

  let params_str = Object.entries(pageviewParams).map(([k, v]) => `${k}=${v}`).join("&");
  const pvUrl = `https://${CDP_HOSTNAME}/etv?` + params_str;
  const pvRes = http.get(pvUrl, { headers });

  check(pvRes, {
    "ETV: status 200": (r) => r.status === 200,
    "ETV: duration < 8s": (r) => r.timings.duration < 8000,
  });

  sleep(1 + Math.random() * 2);
}

// =============================
//   REPORT OUTPUT
// =============================
export function handleSummary(data) {
  // The report is generated at ./tests_with_k6/results/user_session_load_test.html
  return generateReport(data, "user_session_load_test", MAX_USER);
}
// =============================