package com.facturacion.sistemaweb.controller;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.*;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.facturacion.sistemaweb.model.*;
import com.facturacion.sistemaweb.service.*;

@Controller @Profile("sqlserver") @RequestMapping("/modulos/facturas")
@PreAuthorize("isAuthenticated()")
public class FacturaController {
    private final FacturaService facturas;
    private final FacturaRegistroService registro;
    private final FacturaEnvios envios;
    public FacturaController(FacturaService facturas,FacturaRegistroService registro,FacturaEnvios envios) { this.facturas=facturas;this.registro=registro;this.envios=envios; }
    @InitBinder("facturaForm") void campos(WebDataBinder binder) {
        binder.setAllowedFields("idCliente","idFormaPago","token","lineas[*].idProducto","lineas[*].cantidad");binder.setAutoGrowCollectionLimit(200);
        binder.registerCustomEditor(java.math.BigDecimal.class,new FacturaCantidadEditor());
    }
    @GetMapping
    public String listar(@RequestParam(defaultValue="") String q,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue="0") int pagina,Model model) {
        model.addAttribute("filtro",q);model.addAttribute("desde",desde);model.addAttribute("hasta",hasta);
        try { model.addAttribute("facturas",facturas.listar(q,desde,hasta,pagina)); }
        catch(FacturaException ex) { model.addAttribute("facturas",Page.empty());model.addAttribute("errorMessage",ex.getMessage()); }
        return "facturas/lista";
    }
    @GetMapping("/nuevo")
    public String nuevo(HttpSession session,Model model) {
        var datos=facturas.formulario();var form=new FacturaForm();form.setToken(envios.nuevo(session,datos.usuarioId()));
        return preparar(form,datos,model);
    }
    private String preparar(FacturaForm form,FacturaVista.Formulario datos,Model model) {
        model.addAttribute("facturaForm",form);model.addAttribute("datos",datos);return "facturas/formulario";
    }
    @PostMapping
    public String registrar(@Valid @ModelAttribute("facturaForm") FacturaForm form,BindingResult errors,
            HttpSession session,Model model,RedirectAttributes flash) {
        if(!errors.hasErrors()) try {
            Long id=registro.registrar(session,form);flash.addFlashAttribute("successMessage","Factura registrada correctamente.");
            return "redirect:/modulos/facturas/"+id;
        } catch(FacturaException ex) { model.addAttribute("errorMessage",ex.getMessage()); }
        catch(ConcurrencyFailureException ex) { model.addAttribute("errorMessage","Los datos están siendo utilizados por otra operación. Reintenta el envío."); }
        catch(DataIntegrityViolationException ex) { model.addAttribute("errorMessage","No se guardó la factura. Un dato relacionado cambió; revisa la selección."); }
        return preparar(form,facturas.formulario(),model);
    }
    @GetMapping("/{idFactura}") public String detalle(@PathVariable Long idFactura,Model model) {
        model.addAttribute("factura",facturas.consultar(idFactura));return "facturas/detalle";
    }
    @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND) @ResponseBody
    public String inexistente() { return "No se encontró la factura solicitada."; }
    @ExceptionHandler(FacturaException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) @ResponseBody
    public String invalida(FacturaException ex) { return ex.getMessage(); }
    @ExceptionHandler(InvalidPropertyException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) @ResponseBody
    public String demasiadasLineas() { return "El formulario de factura no es válido. Abre una nueva factura."; }
}
