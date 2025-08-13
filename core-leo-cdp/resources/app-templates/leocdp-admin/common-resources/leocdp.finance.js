function loadFinance360Profile() {
	const customerReport = {
		profile_id: "cus-xyz-123",
		preferences: {
			risk_appetite: "Moderate",
			preferred_assets: [
				"VN30 Stocks",
				"Government Bonds",
				"Real Estate Funds",
			],
		},
		investment_summary: {
			stocks: 65000,
			bonds: 30000,
			cash: 5000,
		},
		recent_activities: [
			{
				timestamp: "2025-08-06T09:15:00+07:00",
				action: "Buy",
				asset: "FPT",
				amount: "50 shares @ 112,000 VND",
			},
			{
				timestamp: "2025-08-06T10:45:00+07:00",
				action: "Sell",
				asset: "HPG",
				amount: "100 shares @ 27,500 VND",
			},
			{
				timestamp: "2025-08-06T11:30:00+07:00",
				action: "Dividend Received",
				asset: "VNM",
				amount: "300,000 VND",
			},
			{
				timestamp: "2025-08-06T13:00:00+07:00",
				action: "Add to Watchlist",
				asset: "SSI",
				amount: "",
			},
			{
				timestamp: "2025-08-06T14:15:00+07:00",
				action: "Cancel Order",
				asset: "MWG",
				amount: "Pending sell order (30 shares)",
			},
		],
	};

	$(document).ready(function() {
		$("#customer_risk").text(customerReport.preferences.risk_appetite);
		$("#preferred_assets").text(
			customerReport.preferences.preferred_assets.join(", ")
		);

		const ctx = document.getElementById("investmentChart").getContext("2d");
		Chart.register(ChartDataLabels);

		new Chart(ctx, {
			type: "doughnut",
			data: {
				labels: Object.keys(customerReport.investment_summary),
				datasets: [
					{
						data: Object.values(customerReport.investment_summary),
						backgroundColor: ["#94FFD1", "#A3C8FF", "#F4FFA3"],
					},
				],
			},
			options: {
				responsive: true,
				plugins: {
					title: {
						display: true,
						text: "Portfolio Allocation",
					},

					datalabels: {
						color: "#000",
					},
				},
			},
			plugins: [ChartDataLabels],
		});

		// fill activity logs with market data simulated as above
		customerReport.recent_activities.forEach((item) => {
			const li = `<li class="list-group-item">
      <strong>${item.action}:</strong> ${item.asset} – ${item.amount}
      <em class="text-muted pull-right">${new Date(
				item.timestamp
			).toLocaleString()}</em>
    </li>`;
			$("#activity_logs").append(li);
		});
	});

}