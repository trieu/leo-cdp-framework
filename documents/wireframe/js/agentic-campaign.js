/* ============================================================
 * LEO CDP – Agentic Campaign Builder
 * Fully aligned with provided HTML
 * ============================================================ */

/* -------------------------------
 * Constants & Label Maps
 * ------------------------------- */

const AGENT_PERSONA_LABELS = {
  sales: "The Sales Expert",
  support: "The Customer Helper",
};

const COMM_FREQUENCY_LABELS = {
  smart: "Smart Capping (AI Decides)",
  strict: "Strict: Max 1 msg / 24h",
};

/* -------------------------------
 * Initial Campaign Model
 * ------------------------------- */

const initialCampaignModel = {
  id: "CP-CART-RECOVERY-2025",
  name: "Cart Recovery – AI Adaptive",

  /* === Segment (must match <option value="...">) === */
  targetSegment: "high-intent",
  targetSegmentName: "High Intent Shoppers (AI Segment)",

  /* === Agent Persona (must match data-persona) === */
  agentPersona: "sales",

  /* === Constraints (must match select values) === */
  maxBudget: 75,
  commFrequency: "smart",

  /* === Scenarios (editor + viewer tables) === */
  scenarios: [
    {
      id: 1,
      description: "Price-sensitive user abandoned at payment step",
      decision: "coupon",
      decisionLabel: "Send 10% Coupon",
      reasoning:
        "User viewed pricing multiple times and exited at payment. Small incentive historically converts this cohort.",
    },
    {
      id: 2,
      description: "Returning VIP customer, low churn risk",
      decision: "reminder",
      decisionLabel: "Send Reminder (No Discount)",
      reasoning:
        "High purchase frequency and low price sensitivity. Reminder avoids margin erosion.",
    },
    {
      id: 3,
      description: "First-time visitor from paid ads",
      decision: "popup",
      decisionLabel: "Web Popup",
      reasoning:
        "User lacks brand trust signals. Social proof popup reduces hesitation better than email.",
    },
  ],

  /* === Report Data (Chart.js) === */
  reportData: {
    labels: ["08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00"],
    views: [95, 160, 280, 420, 390, 540, 610],
    clicks: [12, 21, 60, 92, 81, 120, 138],
    buys: [1, 4, 13, 24, 21, 36, 42],
  },
};

/* -------------------------------
 * Campaign App
 * ------------------------------- */

class CampaignApp {
  constructor(model) {
    this.model = model;
    this.mode = "editor";
    this.chart = null;
  }

  /* ---------- Init ---------- */

  init() {
    this.loadEditor();
    this.bindEvents();
    this.initScenarioDialog();
  }

  /* ---------- Editor ---------- */

  loadEditor() {
    $("#campaignName").val(this.model.name);
    $("#targetSegmentSelect").val(this.model.targetSegment);
    $("#maxBudget").val(this.model.maxBudget);
    $("#commFrequency").val(this.model.commFrequency);

    $("#agentPersonaSelection .agent-card").removeClass("active");
    $(
      `#agentPersonaSelection .agent-card[data-persona="${this.model.agentPersona}"]`
    ).addClass("active");

    $("#seeTargetSegmentEditor").text(
      this.model.targetSegmentName.split("(")[0].trim()
    );

    this.renderScenarioTable(
      this.model.scenarios,
      "#userScenarioTableEditor tbody",
      true
    );

    this.initChart("campaignChartInEditor");
  }

  saveFromEditor() {
    const segmentText = $("#targetSegmentSelect option:selected").text();

    this.model.name = $("#campaignName").val();
    this.model.targetSegment = $("#targetSegmentSelect").val();
    this.model.targetSegmentName = segmentText;
    this.model.agentPersona = $(
      "#agentPersonaSelection .agent-card.active"
    ).data("persona");
    this.model.maxBudget = parseInt($("#maxBudget").val(), 10);
    this.model.commFrequency = $("#commFrequency").val();
  }

  /* ---------- View ---------- */

  renderView() {
    $("#viewCampaignName").text(this.model.name);
    $("#viewTargetSegment").text(this.model.targetSegmentName);

    $("#viewAgentPersona").text(
      AGENT_PERSONA_LABELS[this.model.agentPersona] || "Unknown"
    );

    $("#viewMaxBudget").text(`$${this.model.maxBudget}`);

    $("#viewCommFrequency").text(
      COMM_FREQUENCY_LABELS[this.model.commFrequency] || "Unknown"
    );

    $("#seeTargetSegmentViewer").text(
      this.model.targetSegmentName.split("(")[0].trim()
    );

    this.renderScenarioTable(
      this.model.scenarios,
      "#userScenarioTableViewer tbody",
      false
    );

    this.initChart("campaignChartInView");
  }

  /* ---------- Scenario Table ---------- */

  renderScenarioTable(scenarios, tbodySelector, editable) {
    const $tbody = $(tbodySelector).empty();

    scenarios.forEach((s) => {
      const actionCol = editable
        ? `<td>
                        <button class="btn btn-warning btn-xs edit-scenario-btn"
                                data-id="${s.id}">
                            <i class="fa fa-pencil"></i> Edit
                        </button>
                   </td>`
        : "";

      $tbody.append(`
                <tr data-id="${s.id}">
                    <td><i class="fa fa-user"></i> ${s.description}</td>
                    <td><span class="label label-success">${s.decisionLabel}</span></td>
                    <td>${s.reasoning}</td>
                    ${actionCol}
                </tr>
            `);
    });
  }

  /* ---------- Chart ---------- */

  initChart(nodeId) {
    this.destroyChart();

    const ctx = document.getElementById(nodeId).getContext("2d");

    this.chart = new Chart(ctx, {
      type: "line",
      data: {
        labels: this.model.reportData.labels,
        datasets: [
          {
            label: "Views",
            data: this.model.reportData.views,
            borderColor: "rgba(54,162,235,1)",
            backgroundColor: "rgba(54,162,235,0.15)",
            tension: 0.4,
            fill: true,
          },
          {
            label: "Clicks",
            data: this.model.reportData.clicks,
            borderColor: "rgba(255,206,86,1)",
            backgroundColor: "rgba(255,206,86,0.15)",
            tension: 0.4,
          },
          {
            label: "Buys",
            data: this.model.reportData.buys,
            borderColor: "rgba(255,99,132,1)",
            backgroundColor: "rgba(255,99,132,0.15)",
            tension: 0.4,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
      },
    });
  }

  destroyChart() {
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }
  }

  /* ---------- Mode Switch ---------- */

  switchMode() {
    if (this.mode === "editor") {
      this.saveFromEditor();
      $("#editorModeContainer").hide();
      $("#viewModeContainer").show();
      $("#modeToggleBtn")
        .html('<i class="fa fa-pencil"></i> Switch to Editor Mode')
        .data("mode", "view");

      this.renderView();
      this.mode = "view";
    } else {
      this.destroyChart();
      $("#viewModeContainer").hide();
      $("#editorModeContainer").show();
      $("#modeToggleBtn")
        .html('<i class="fa fa-eye"></i> Switch to View Mode')
        .data("mode", "editor");

      this.loadEditor();
      this.mode = "editor";
    }
  }

  /* ---------- Scenario Dialog ---------- */

  initScenarioDialog() {
    $("#scenarioDialog").dialog({
      autoOpen: false,
      modal: true,
      width: 500,
      buttons: {
        Save: () => this.saveScenario(),
        Cancel: function () {
          $(this).dialog("close");
        },
      },
    });
  }

  openScenarioDialog(id) {
    if (id === "new") {
      $("#scenarioName").val("");
      $("#agentDecision").val("coupon");
      $("#reasoning").val("");
    } else {
      const s = this.model.scenarios.find((x) => x.id == id);
      $("#scenarioName").val(s.description);
      $("#agentDecision").val(s.decision);
      $("#reasoning").val(s.reasoning);
    }

    $("#scenarioDialog").data("id", id).dialog("open");
  }

  saveScenario() {
    const id = $("#scenarioDialog").data("id");
    const decision = $("#agentDecision").val();
    const decisionLabel = $("#agentDecision option:selected").text();

    if (id === "new") {
      const newId = Math.max(0, ...this.model.scenarios.map((s) => s.id)) + 1;

      this.model.scenarios.push({
        id: newId,
        description: $("#scenarioName").val(),
        decision,
        decisionLabel,
        reasoning: $("#reasoning").val(),
      });
    } else {
      const idx = this.model.scenarios.findIndex((s) => s.id == id);
      this.model.scenarios[idx] = {
        id,
        description: $("#scenarioName").val(),
        decision,
        decisionLabel,
        reasoning: $("#reasoning").val(),
      };
    }

    this.renderScenarioTable(
      this.model.scenarios,
      "#userScenarioTableEditor tbody",
      true
    );

    $("#scenarioDialog").dialog("close");
  }

  /* ---------- Events ---------- */

  bindEvents() {
    $("#modeToggleBtn").on("click", () => this.switchMode());

    $("#agentPersonaSelection").on("click", ".agent-card", function () {
      $(".agent-card").removeClass("active");
      $(this).addClass("active");
    });

    $("#targetSegmentSelect").on("change", function () {
      $("#seeTargetSegmentEditor").text(
        $(this).find("option:selected").text().split("(")[0].trim()
      );
    });

    $("#addUserScenarioBtn").on("click", () => this.openScenarioDialog("new"));

    $("#userScenarioTableEditor").on("click", ".edit-scenario-btn", (e) =>
      this.openScenarioDialog($(e.currentTarget).data("id"))
    );

    $("#manageSegmentBtn").on("click", () =>
      alert("Open Segment Management UI")
    );
  }
}

/* -------------------------------
 * Bootstrap App
 * ------------------------------- */

$(document).ready(function () {
  const app = new CampaignApp(initialCampaignModel);
  app.init();
});
