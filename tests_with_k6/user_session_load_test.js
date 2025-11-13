import http from "k6/http";
import { check, sleep } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.2.0/index.js";
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.2/index.js";

// ===== CDP HOSTNAME  =====
const CDP_HOSTNAME = "obs.example.com";

// ===== GLOBAL STATE (per VU) =====
const sessionMap = {}; // store all VU sessions here

// ===== USER AGENT POOL (random like real users) =====
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

// Get random UA on first request per VU
function getUserAgent(vu) {
  if (!sessionMap[vu].ua) {
    const ua = USER_AGENTS[Math.floor(Math.random() * USER_AGENTS.length)];
    sessionMap[vu].ua = ua;
  }
  return sessionMap[vu].ua;
}

// ===== TEST CONFIG =====
export const options = {
  stages: [
    { duration: "30s", target: 10 },
    { duration: "30s", target: 50 },
  ],
  thresholds: {
    http_req_duration: ["p(95)<8000"],
    http_req_failed: ["rate<0.01"],
  },
};

// === CREATE A NEW 30-MIN SESSION ===
function createNewSession() {
  return {
    visid: uuidv4().replace(/-/g, ""),
    fgp: uuidv4().replace(/-/g, ""),
    sessionStart: Date.now(),
  };
}

// === GET OR REFRESH SESSION FOR CURRENT VU ===
function getSession(vu) {
  const now = Date.now();
  let s = sessionMap[vu];

  if (!s) {
    s = createNewSession();
    sessionMap[vu] = s;
    return s;
  }

  const sessionAgeMinutes = (now - s.sessionStart) / 1000 / 60;
  if (sessionAgeMinutes > 30) {
    s = createNewSession();
    sessionMap[vu] = s;
  }

  return s;
}

// ===== TEST EXECUTION =====
export default function () {
  const vu = __VU;
  const session = getSession(vu);

  const endpoint = "https://" + CDP_HOSTNAME + "/cxs-pf-init";

  const params = {
    obsid: "5V8iSpjtr9RbwpePb4Xq0G",
    mediahost: "bigdatavietnam.org",
    tpurl: "https%3A%2F%2Fwww.bigdatavietnam.org%2F",
    tpname: "Big%20Data%20Vietnam",
    fgp: session.fgp,
    visid: session.visid,
  };

  const url = `${endpoint}?${Object.entries(params)
    .map(([k, v]) => `${k}=${v}`)
    .join("&")}`;

  // Random realistic user agent
  const userAgent = getUserAgent(vu);

  // HTTP headers
  const headers = {
    "User-Agent": userAgent,
    "Accept": "application/json, text/plain, */*",
    "Connection": "keep-alive",
  };

  const res = http.get(url, { headers });

  check(res, {
    "status = 200": (r) => r.status === 200,
    "duration < 8s": (r) => r.timings.duration < 8000,
  });

  sleep(1 + Math.random() * 2);
}

// ===== REPORT OUTPUT =====
export function handleSummary(data) {
  const localTime = new Date().toLocaleString();

  return {
    stdout:
      textSummary(data, { indent: " ", enableColors: true }) +
      `\nTest finished at (local time): ${localTime}\n`,

    "results/user_session_load_test.html":
      "<!-- Test run at local time: " + localTime + " -->\n" +
      htmlReport(data, { title: "User Session Init Load Test - " + localTime }),
  };
}
