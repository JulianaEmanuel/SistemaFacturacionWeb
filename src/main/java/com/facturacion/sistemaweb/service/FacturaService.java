package com.facturacion.sistemaweb.service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import com.facturacion.sistemaweb.model.*;
import com.facturacion.sistemaweb.repository.*;

@Service @Profile("sqlserver") @Transactional(readOnly=true)
@PreAuthorize("isAuthenticated()")
public class FacturaService {
    private final FacturaRepository facturas;
    private final DetalleFacturaRepository detalles;
    private final PersonaRepository personas;
    private final ProductoRepository productos;
    private final EntityManager em;
    private final EmpleadoSesionService sesion;
    private final FacturaCalculoService calculo;
    public FacturaService(FacturaRepository facturas,DetalleFacturaRepository detalles,PersonaRepository personas,
            ProductoRepository productos,EntityManager em,EmpleadoSesionService sesion,FacturaCalculoService calculo) {
        this.facturas=facturas;this.detalles=detalles;this.personas=personas;this.productos=productos;this.em=em;this.sesion=sesion;this.calculo=calculo;
    }
    public Long usuarioId() { return sesion.actual(false).id(); }
    private static String nombre(Persona persona) { return persona.getNombresPersona().stripTrailing()+" "+persona.getApellidosPersona().stripTrailing(); }
    public FacturaVista.Formulario formulario() {
        var cuenta=sesion.actual(false);String empleado=null,impedimento=null;
        try { empleado=nombre(sesion.empleado(cuenta.id(),false).getPersona()); }
        catch(FacturaException ex) { impedimento=ex.getMessage(); }
        return new FacturaVista.Formulario(cuenta.id(),empleado,impedimento,
            personas.listarOrdenadas().stream().map(p -> new FacturaVista.ClienteOpcion(p.getIdPersona(),p.getCedulaPersona().stripTrailing(),nombre(p))).toList(),
            productos.listarOrdenados().stream().map(p -> new FacturaVista.ProductoOpcion(p.getIdProducto(),p.getNombreProducto().stripTrailing(),p.getPrecioUnitario(),p.getIvaProducto())).toList());
    }
    public Page<FacturaVista> listar(String q,LocalDate desde,LocalDate hasta,int pagina) {
        var cuenta=sesion.actual(false);
        Long empleado=cuenta.administrador()?null:sesion.empleado(cuenta.id(),false).getIdEmpleado();
        if(pagina<0 || pagina>100000) throw new FacturaException("Página no válida.");
        if((desde!=null && (desde.isBefore(LocalDate.of(1753,1,1)) || desde.isAfter(LocalDate.of(9999,12,30))))
            || (hasta!=null && (hasta.isBefore(LocalDate.of(1753,1,1)) || hasta.isAfter(LocalDate.of(9999,12,30))))
            || (desde!=null && hasta!=null && desde.isAfter(hasta))) throw new FacturaException("Revisa el rango de fechas.");
        String filtro=q==null?"":q.strip();
        if(filtro.length()>100) throw new FacturaException("La búsqueda admite hasta 100 caracteres.");
        return facturas.buscar(empleado,filtro,desde==null?null:desde.atStartOfDay(),hasta==null?null:hasta.plusDays(1).atStartOfDay(),PageRequest.of(pagina,20))
            .map(f -> vista(f,List.of()));
    }
    public FacturaVista consultar(Long id) {
        var cuenta=sesion.actual(false);
        var factura=facturas.completo(id).orElseThrow(() -> new NoSuchElementException("Factura no encontrada."));
        if(!cuenta.administrador() && !Objects.equals(sesion.empleado(cuenta.id(),false).getIdEmpleado(),factura.getEmpleado().getIdEmpleado()))
            throw new AccessDeniedException("No tienes permiso para consultar esta factura.");
        var lineas=detalles.deFactura(id).stream().map(d -> new FacturaVista.Linea(d.getProducto().getIdProducto(),d.getProducto().getNombreProducto().stripTrailing(),
            d.getCantidad(),d.getPrecio(),d.getIva(),d.getSubtotal(),d.getTotal().subtract(d.getSubtotal()),d.getTotal())).toList();
        return vista(factura,lineas);
    }
    private FacturaVista vista(Factura f,List<FacturaVista.Linea> lineas) {
        Persona cliente=personas.findById(f.getIdPersona()).orElse(null);
        return new FacturaVista(f.getIdFactura(),f.getFecha(),cliente==null?"—":cliente.getCedulaPersona().stripTrailing(),
            cliente==null?"Cliente no disponible (ID "+f.getIdPersona()+")":nombre(cliente),nombre(f.getEmpleado().getPersona()),
            f.getSubtotal(),f.getTotal().subtract(f.getSubtotal()),f.getTotal(),lineas);
    }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Long registrar(FacturaForm form) {
        var cuenta=sesion.actual(true);
        Empleado empleado=sesion.empleado(cuenta.id(),true);
        if(form.getIdCliente()==null || form.getIdCliente()<1) throw new FacturaException("Selecciona un cliente válido.");
        Persona cliente=em.find(Persona.class,form.getIdCliente(),LockModeType.PESSIMISTIC_READ);
        if(cliente==null) throw new FacturaException("El cliente seleccionado ya no existe.");
        var cantidades=calculo.acumular(form.getLineas());
        List<DetalleFactura> lineas=new ArrayList<>();
        BigDecimal subtotal=new BigDecimal("0.0000"),total=new BigDecimal("0.0000");
        for(var entry:cantidades.entrySet()) {
            Producto producto=em.find(Producto.class,entry.getKey(),LockModeType.PESSIMISTIC_READ);
            if(producto==null) throw new FacturaException("Uno de los productos seleccionados ya no existe.");
            var valor=calculo.calcular(producto,entry.getValue());
            DetalleFactura detalle=new DetalleFactura();detalle.setProducto(producto);detalle.setCantidad(valor.cantidad());
            detalle.setPrecio(valor.precio());detalle.setIva(valor.porcentaje());detalle.setSubtotal(valor.subtotal());detalle.setTotal(valor.total());lineas.add(detalle);
            subtotal=calculo.importe(subtotal.add(valor.subtotal()));total=calculo.importe(total.add(valor.total()));
        }
        Factura factura=new Factura();factura.setEmpleado(empleado);factura.setIdPersona(cliente.getIdPersona());
        factura.setFecha(LocalDateTime.now(ZoneId.of("America/Guayaquil")));factura.setSubtotal(subtotal);factura.setTotal(total);
        factura=facturas.saveAndFlush(factura);
        for(var linea:lineas) linea.setFactura(factura);
        detalles.saveAllAndFlush(lineas);
        return factura.getIdFactura();
    }
}
