package com.facturacion.sistemaweb.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

public record FacturaVista(Long id,LocalDateTime fecha,String cedula,String cliente,String empleado,
        BigDecimal subtotal,BigDecimal iva,BigDecimal total,List<Linea> lineas,Contacto contacto,Pago pago) {
    public FacturaVista(Long id,LocalDateTime fecha,String cedula,String cliente,String empleado,BigDecimal subtotal,BigDecimal iva,BigDecimal total,List<Linea> lineas){this(id,fecha,cedula,cliente,empleado,subtotal,iva,total,lineas,new Contacto(null,null,null,false),null);}
    public record Contacto(String direccion,String telefono,String correo,boolean historico) { }
    public record Pago(String nombre,BigDecimal importe){public String importeVisible(){return "$ "+dinero(importe);}}
    public static String dinero(BigDecimal valor) { return valor.setScale(2,RoundingMode.HALF_UP).toPlainString(); }
    public String subtotalVisible() { return dinero(subtotal); }
    public String ivaVisible() { return dinero(iva); }
    public String totalVisible() { return dinero(total); }
    public record Linea(Long idProducto,String nombre,BigDecimal cantidad,BigDecimal precio,
        BigDecimal porcentaje,BigDecimal subtotal,BigDecimal iva,BigDecimal total) {
        public String cantidadVisible() { return cantidad.stripTrailingZeros().toPlainString(); }
        public String precioVisible() { return "$ " + dinero(precio); }
        public String porcentajeVisible() { return porcentaje.stripTrailingZeros().toPlainString().replace('.',',')+" %"; }
        public String subtotalVisible() { return dinero(subtotal); }
        public String ivaVisible() { return dinero(iva); }
        public String totalVisible() { return dinero(total); }
    }
    public record ProductoOpcion(Long id,String nombre,BigDecimal precio,BigDecimal iva) { }
    public record ClienteOpcion(Long id,String cedula,String nombre,String direccion,String telefono,String correo) {
        public ClienteOpcion(Long id,String cedula,String nombre){this(id,cedula,nombre,null,null,null);}
    }
    public record FormaPagoOpcion(Integer id,String nombre) { }
    public record Formulario(Long usuarioId,String empleado,String impedimento,List<ClienteOpcion> clientes,List<ProductoOpcion> productos,List<FormaPagoOpcion> formasPago,boolean migrada) {
        public Formulario(Long usuarioId,String empleado,String impedimento,List<ClienteOpcion> clientes,List<ProductoOpcion> productos){this(usuarioId,empleado,impedimento,clientes,productos,List.of(),false);}
    }
}
