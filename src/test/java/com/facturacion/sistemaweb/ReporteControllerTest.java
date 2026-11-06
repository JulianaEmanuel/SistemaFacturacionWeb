package com.facturacion.sistemaweb;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.facturacion.sistemaweb.controller.ReporteController;
import com.facturacion.sistemaweb.model.*;
import com.facturacion.sistemaweb.security.SecurityConfig;
import com.facturacion.sistemaweb.service.*;

@WebMvcTest(ReporteController.class) @ActiveProfiles("sqlserver") @Import({SecurityConfig.class,CapturaVistasConfig.class})
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class ReporteControllerTest {
    @Test void graficoSeRenderizaConImportesDelResultadoAutorizado() throws Exception {
        var base=ReporteExportTest.resultado(ReporteTipo.VENTAS,List.of(ReporteExportTest.fila("2026-10-30","30 octubre","","4.6000")));
        var data=new ReporteResultado(base.tipo(),base.filtro(),base.alcance(),base.administrador(),base.resumen(),base.filas(),List.of(new ReporteResultado.Punto("30 octubre",new java.math.BigDecimal("4.6000"))),"line",false);
        when(reportes.consultar(any(),any())).thenReturn(data);
        mvc.perform(get("/modulos/reportes/ventas").with(user("ana"))).andExpect(status().isOk()).andExpect(content().string(containsString("data-total=\"4.6000\"")));
    }
    @Autowired MockMvc mvc;
    @MockitoBean ReporteService reportes;
    @MockitoBean ReportePdfService pdf;
    @MockitoBean ReporteExcelService excel;
    @MockitoBean UserDetailsService users;
    void datos() {
        when(reportes.consultar(any(),any())).thenAnswer(i -> ReporteExportTest.resultado(i.getArgument(0),List.of(ReporteExportTest.fila("2","José Cliente","0012345678","4.6000"))));
        when(reportes.consultarPanel(any())).thenAnswer(i -> {
            var filas=List.of(ReporteExportTest.fila("2","José Cliente","0012345678","4.6000"));
            return new ReportePanel(ReporteExportTest.resultado(ReporteTipo.RESUMEN,filas),filas,filas,filas,1);
        });
    }
    @Test void resumenIncluyeCuatroGraficasYDatosDetalladosAutorizados() throws Exception {
        datos();mvc.perform(get("/modulos/reportes").with(user("ana")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Evolución de ventas")))
            .andExpect(content().string(containsString("Ventas por cliente")))
            .andExpect(content().string(containsString("Productos más vendidos")))
            .andExpect(content().string(containsString("Subtotal e IVA")))
            .andExpect(content().string(containsString("Ver datos detallados")))
            .andExpect(content().string(containsString("/js/reportes-panel.js")));
        verify(reportes).consultarPanel(any());
    }
    @Test void anonimoNoConsultaNiDescarga() throws Exception {
        for(String path:List.of("/modulos/reportes","/modulos/reportes/ventas","/modulos/reportes/ventas/pdf","/modulos/reportes/clientes/excel","/modulos/reportes/empleados/imprimir"))
            mvc.perform(get(path)).andExpect(redirectedUrl("/login"));verifyNoInteractions(reportes,pdf,excel);
    }
    @Test void todosLosReportesRenderizanConFiltrosYDatosAutorizados() throws Exception {
        datos();mvc.perform(get("/modulos/reportes").with(user("ana"))).andExpect(status().isOk()).andExpect(view().name("reportes/index"));
        for(var tipo:ReporteTipo.values()) mvc.perform(get("/modulos/reportes/"+tipo.getSlug()).with(user("ana"))).andExpect(status().isOk())
            .andExpect(content().string(containsString("Ventas propias"))).andExpect(content().string(containsString("Excel")))
            .andExpect(content().string(not(containsString("name=\"empleado\""))));
    }
    @Test void filtrosSeEntreganAlServicioSinRolesDelNavegador() throws Exception {
        datos();mvc.perform(get("/modulos/reportes/productos").with(user("ana")).param("cliente","2").param("producto","3").param("orden","importe").param("limite","10"))
            .andExpect(status().isOk());verify(reportes).consultar(eq(ReporteTipo.PRODUCTOS),argThat(f -> f.getCliente()==2L && f.getProducto()==3L && f.getOrden().equals("importe")));
    }
    @Test void listaCerradaRechazaNombresNoPermitidos() throws Exception {
        mvc.perform(get("/modulos/reportes/no-existe").with(user("ana"))).andExpect(status().isNotFound());
        mvc.perform(get("/modulos/reportes/no-existe/pdf").with(user("ana"))).andExpect(status().isNotFound());verifyNoInteractions(reportes,pdf,excel);
    }
    @Test void fechasMalformadasNoSeConsultanNiExportan() throws Exception {
        mvc.perform(get("/modulos/reportes/ventas").with(user("ana")).param("desde","no-fecha"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Revisa las fechas")));
        mvc.perform(get("/modulos/reportes/ventas/excel").with(user("ana")).param("desde","no-fecha")).andExpect(status().isBadRequest());verifyNoInteractions(reportes,excel);
    }
    @Test void servicioDeniegaEmpleadoAjenoTambienEnDescargas() throws Exception {
        when(reportes.consultar(any(),any())).thenThrow(new org.springframework.security.access.AccessDeniedException("Alcance ajeno"));
        when(pdf.generar(any(),any())).thenThrow(new org.springframework.security.access.AccessDeniedException("Alcance ajeno"));
        when(excel.generar(any(),any())).thenThrow(new org.springframework.security.access.AccessDeniedException("Alcance ajeno"));
        mvc.perform(get("/modulos/reportes/ventas").with(user("ana")).param("empleado","99")).andExpect(status().isForbidden());
        mvc.perform(get("/modulos/reportes/ventas/pdf").with(user("ana")).param("empleado","99")).andExpect(status().isForbidden());
        mvc.perform(get("/modulos/reportes/ventas/excel").with(user("ana")).param("empleado","99")).andExpect(status().isForbidden());
    }
    @Test void pdfYExcelUsanMimesYNombreDeArchivoSeguro() throws Exception {
        when(pdf.generar(any(),any())).thenReturn(new byte[]{1,2});when(excel.generar(any(),any())).thenReturn(new byte[]{3,4});
        mvc.perform(get("/modulos/reportes/ventas/pdf").with(user("ana"))).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Content-Disposition","inline; filename=\"reporte-ventas.pdf\""))
            .andExpect(header().string("Cache-Control",containsString("no-store")));
        mvc.perform(get("/modulos/reportes/clientes/excel").with(user("ana"))).andExpect(status().isOk()).andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition","attachment; filename=\"reporte-clientes.xlsx\""));
    }
    @Test void impresionNoIncluyeMenuNiGraficosDeOtrosDatos() throws Exception {
        datos();mvc.perform(get("/modulos/reportes/clientes/imprimir").with(user("ana"))).andExpect(status().isOk()).andExpect(view().name("reportes/imprimible"))
            .andExpect(content().string(containsString("José Cliente"))).andExpect(content().string(not(containsString("id=\"appSidebar\""))));
    }
    @Test void paginacionConservaFiltrosSinEscaparDosVecesLosSeparadores() throws Exception {
        var filas=java.util.stream.IntStream.range(0,30).mapToObj(i -> ReporteExportTest.fila(""+i,"Cliente "+i,"0012345678901","4.6000")).toList();
        var result=ReporteExportTest.resultado(ReporteTipo.CLIENTES,filas);
        when(reportes.consultar(any(),any())).thenReturn(result);
        mvc.perform(get("/modulos/reportes/clientes").with(user("ana")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("&amp;pagina=1")))
            .andExpect(content().string(not(containsString("&amp;amp;pagina"))));
        mvc.perform(get("/modulos/reportes/clientes").with(user("ana")).param("pagina","1"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Cliente 29")));
    }
    @Test void csrfSigueHabilitadoYReportesNoPermiteEscrituras() throws Exception {
        mvc.perform(post("/modulos/reportes/ventas").with(user("ana"))).andExpect(status().isForbidden());
        mvc.perform(post("/modulos/reportes/ventas").with(user("ana")).with(csrf())).andExpect(status().isMethodNotAllowed());verifyNoInteractions(reportes);
    }
    @Test void erroresDeLimiteSeMuestranYNoTruncan() throws Exception {
        when(reportes.consultar(any(),any())).thenThrow(new ReporteException("El reporte supera 10.000 filas."));
        mvc.perform(get("/modulos/reportes/clientes").with(user("ana"))).andExpect(status().isOk()).andExpect(content().string(containsString("10.000")));
    }
}
