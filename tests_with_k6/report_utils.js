import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.2/index.js";

// ---------------------------------------
//   HUMAN PERFORMANCE VERDICT
// ---------------------------------------
function verdict(p95, errorRate) {
  if (errorRate > 0.05) return "❌ Critical: Error rate too high";
  if (p95 > 10000) return "❌ Critical: API too slow (P95 > 10s)";
  if (p95 > 5000) return "⚠️ Slow under load (P95 > 5s)";
  if (p95 > 2000) return "🟡 Acceptable but needs improvement";
  return "🟢 Excellent performance";
}

// ---------------------------------------
//   SUMMARY GENERATOR (stdout + HTML)
// ---------------------------------------
export function generateReport(data, reportName = "performance_report", maxUsers = "N/A") {
  const localTime = new Date().toLocaleString();

  const httpReqs = data.metrics.http_reqs?.values?.count || 0;
  const durationMs = data.state.testRunDurationMs || 1;
  const rps = (httpReqs / (durationMs / 1000)).toFixed(2);

  const p95 = data.metrics.http_req_duration?.values["p(95)"] || 0;
  const errorRate = data.metrics.http_req_failed?.values.rate || 0;

  const finalVerdict = verdict(p95, errorRate);

  const summaryText =
    `\n========== LEO CDP — Load Test Summary ==========\n` +
    `Concurrent Users       : ${maxUsers}\n` +
    `Requests per Second    : ${rps}\n` +
    `HTTP P95 (ms)          : ${p95.toFixed(2)}\n` +
    `Error Rate             : ${(errorRate * 100).toFixed(2)}%\n` +
    `Verdict                : ${finalVerdict}\n` +
    `Completed at           : ${localTime}\n` +
    `==================================================\n`;

  return {
    stdout:
      textSummary(data, { indent: " ", enableColors: true }) +
      summaryText,

    [`results/${reportName}.html`]:
      `<!-- Report generated at: ${localTime} -->\n` +
      htmlReport(data, {
        title: `${reportName} — ${localTime}`,
      }),
  };
}
