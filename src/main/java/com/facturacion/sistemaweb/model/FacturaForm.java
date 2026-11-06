package com.facturacion.sistemaweb.model;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class FacturaForm {
    @NotNull(message="Selecciona una forma de pago.") @Positive private Integer idFormaPago;
    public Integer getIdFormaPago(){return idFormaPago;}
    public void setIdFormaPago(Integer v){idFormaPago=v;}
    @NotNull(message="Selecciona un cliente.") @Positive private Long idCliente;
    @NotBlank(message="El formulario ha caducado. Abre un nuevo formulario.") @Size(max=36) private String token;
    @NotEmpty(message="Agrega al menos un producto.") @Size(max=200,message="La factura admite hasta 200 líneas.")
    @Valid private List<FacturaLineaForm> lineas=new ArrayList<>();
    public Long getIdCliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente=idCliente; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token=token; }
    public List<FacturaLineaForm> getLineas() { return lineas; }
    public void setLineas(List<FacturaLineaForm> lineas) { this.lineas=lineas; }
}
