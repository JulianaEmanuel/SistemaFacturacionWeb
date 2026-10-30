const currentDate = document.getElementById("current-date");

if (currentDate) {
	currentDate.textContent = new Intl.DateTimeFormat("es", {
		weekday: "long",
		day: "numeric",
		month: "long",
		year: "numeric"
	}).format(new Date());
}

const monthlySalesCanvas = document.getElementById("monthlySalesChart");

if (monthlySalesCanvas && window.Chart) {
	new Chart(monthlySalesCanvas, {
		type: "line",
		data: {
			labels: ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"],
			datasets: [{
				data: Array(12).fill(null),
				borderColor: "#15856a",
				backgroundColor: "rgb(21 133 106 / 12%)",
				borderWidth: 2,
				pointRadius: 0,
				tension: 0.35,
				fill: true
			}]
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			plugins: {
				legend: { display: false },
				tooltip: { enabled: false }
			},
			scales: {
				x: {
					grid: { display: false },
					border: { display: false },
					ticks: { color: "#788780", font: { family: "DM Sans", size: 10 } }
				},
				y: {
					display: false,
					min: 0,
					max: 1
				}
			}
		}
	});
}