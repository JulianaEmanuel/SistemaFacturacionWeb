package com.facturacion.sistemaweb.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.facturacion.sistemaweb.model.FacturaForm;

@Service @Profile("sqlserver") @PreAuthorize("isAuthenticated()")
public class FacturaRegistroService {
    private final FacturaService facturas;
    private final FacturaEnvios envios;
    public FacturaRegistroService(FacturaService facturas,FacturaEnvios envios) { this.facturas=facturas;this.envios=envios; }
    public Long registrar(HttpSession session,FacturaForm form) {
        return envios.ejecutar(session,facturas.usuarioId(),form.getToken(),() -> facturas.registrar(form));
    }
}
