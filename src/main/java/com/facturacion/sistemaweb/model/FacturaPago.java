package com.facturacion.sistemaweb.model;
import java.math.BigDecimal;
import jakarta.persistence.*;
@Entity @Table(name="FacturaPago",schema="dbo",uniqueConstraints=@UniqueConstraint(columnNames="ID_FACTURA"))
public class FacturaPago {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="ID_FACTURA_PAGO") private Long id;
    @Column(name="ID_FACTURA",nullable=false) private Long idFactura;
    @Column(name="ID_FORMA_PAGO",nullable=false) private Integer idFormaPago;
    @Column(name="NOMBRE_FORMA_PAGO",nullable=false,length=80) private String nombre;
    @Column(name="IMPORTE",nullable=false,precision=18,scale=4) private BigDecimal importe;
    protected FacturaPago(){}
    public FacturaPago(Long factura,FormaPago forma,BigDecimal importe){this.idFactura=factura;this.idFormaPago=forma.getId();this.nombre=forma.getNombre();this.importe=importe;}
    public Long getIdFactura(){return idFactura;}public Integer getIdFormaPago(){return idFormaPago;}public String getNombre(){return nombre;}public BigDecimal getImporte(){return importe;}
}
