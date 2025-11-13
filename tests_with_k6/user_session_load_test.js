import http from "k6/http";
import { check, sleep } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.2.0/index.js";
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.2/index.js";
import { generateReport } from "./report_utils.js";


// =============================
//   LEO CDP OBSERVER CONFIG
// =============================
const CDP_HOSTNAME = "datahub4dcdp.bigdatavietnam.org";

// =============================
//   K6 CONFIG
// =============================
const MAX_USER = 500;

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
//   TEST CONFIG
// =============================
export const options = {
  stages: [
    { duration: "15s", target: Math.floor(MAX_USER / 4) },
    { duration: "30s", target: Math.floor(MAX_USER / 3) },
    { duration: "60s", target: Math.floor(MAX_USER / 2) },
    { duration: "120s", target: MAX_USER },
  ],
  thresholds: {
    http_req_duration: ["p(95)<8000"],
    http_req_failed: ["rate<0.01"],
  },
};

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
    obsid: "5V8iSpjtr9RbwpePb4Xq0G",
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
    obsid: "5V8iSpjtr9RbwpePb4Xq0G",
    mediahost: "bigdatavietnam.org",
    tprefurl: "https://www.youtube.com",
    tprefdomain: "youtube.com",
    tpurl: "https%3A%2F%2Fwww.bigdatavietnam.org%2F",
    tpname: "Big%20Data%20Vietnam",
    metric: "page-view",
    eventdata: encodeURIComponent(JSON.stringify({ demo: 1 })),
    visid: session.visid,
    fgp: session.fgp,
    ctxsk: session.ctxsk,
  };

  const pvUrl =
    `https://${CDP_HOSTNAME}/etv?` +
    Object.entries(pageviewParams)
      .map(([k, v]) => `${k}=${v}`)
      .join("&");

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
  return generateReport(data, "user_session_load_test", MAX_USER);
}
