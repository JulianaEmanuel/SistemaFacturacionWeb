package com.facturacion.sistemaweb.controller;

import java.security.Principal;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class CuentaActualAdvice {
    @ModelAttribute("cuentaActual") public String cuenta(Principal principal) { return principal==null?"":principal.getName(); }
}
