import http from "k6/http";
import { check, sleep } from "k6";

// =====================================================================
// Admin HTTP A/B smoke for the JDK-25 migration (gate G2-local).
// Hammers the admin worker's locally-served surface: the login HTML and
// the four minified JS files changed by the minify-plugin 2.1.1 bump.
// Run once against the Corretto-25 image and once against Corretto-11,
// then compare p95 / RPS / error rate (docs/05 §3 gate G2: ±10%).
//
//   docker run --rm --network docker-leocdp_default \
//     -v <repo>/tests_with_k6:/scripts grafana/k6 run \
//     -e BASE_URL=http://leocdp-admin:9070 \
//     --summary-export /scripts/out/<label>.json /scripts/admin_http_ab_test.js
// =====================================================================

const BASE_URL = __ENV.BASE_URL || "http://leocdp-admin:9070";

const ASSETS = [
  "/view/common-resources-min/leo.admin.common.js?admin=1",
  "/view/common-resources-min/leocdp.chatbot.js?admin=1",
  "/view/common-resources-min/leocdp.core-admin.js?admin=1",
  "/view/common-resources-min/leocdp.finance.js?admin=1",
];

export const options = {
  scenarios: {
    admin_surface: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "15s", target: 50 },
        { duration: "60s", target: 50 },
        { duration: "5s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],   // <1% errors
    http_req_duration: ["p(95)<2000"], // generous local bound; A/B delta is what matters
  },
};

export default function () {
  const page = http.get(`${BASE_URL}/`);
  check(page, { "login HTML 200": (r) => r.status === 200 });

  const responses = http.batch(ASSETS.map((p) => ["GET", `${BASE_URL}${p}`]));
  responses.forEach((r, i) =>
    check(r, { [`asset ${i} 200`]: (res) => res.status === 200 })
  );

  sleep(0.3);
}
