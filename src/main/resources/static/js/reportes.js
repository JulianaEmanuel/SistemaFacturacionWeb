(() => {
    "use strict";
    document.getElementById("reporte-imprimir")?.addEventListener("click", () => window.print());
    const canvas = document.getElementById("reporte-chart");
    if (!canvas) return;
    const aviso = document.getElementById("reporte-chart-aviso");
    if (typeof window.Chart !== "function") { aviso.textContent = "El gráfico no está disponible. Consulta la tabla de resultados."; return; }
    const points = [...document.querySelectorAll("#reporte-chart-datos li")];
    const values = points.map(p => /^-?\d+(\.\d+)?$/.test(p.dataset.total || "") ? Number(p.dataset.total) : NaN);
    if (!points.length || values.some(v => !Number.isFinite(v))) { aviso.textContent = "No hay datos válidos para el gráfico."; return; }
    const horizontal = canvas.dataset.horizontal === "true";
    if (horizontal) canvas.parentElement.style.height = `${Math.min(1200, Math.max(360, points.length * 34))}px`;
    new window.Chart(canvas, {
        type: canvas.dataset.tipo === "line" ? "line" : "bar",
        data: { labels: points.map(p => p.dataset.etiqueta), datasets: [{ label: "Total facturado", data: values,
            borderColor: "#15856a", backgroundColor: horizontal ? "#15856a" : "rgba(21, 133, 106, .22)", borderWidth: 2, tension: .2 }] },
        options: { responsive: true, maintainAspectRatio: false, indexAxis: horizontal ? "y" : "x",
            plugins: { legend: { display: false } }, scales: horizontal ? { x: { beginAtZero: true } } : { y: { beginAtZero: true } } }
    });
})();
