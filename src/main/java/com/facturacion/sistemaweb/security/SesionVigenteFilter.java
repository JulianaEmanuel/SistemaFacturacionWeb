package com.facturacion.sistemaweb.security;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import com.facturacion.sistemaweb.repository.AdministracionRepository;

/** Revisa estado sin recuperar credenciales; desactivaciones se aplican a sesiones existentes. */
public class SesionVigenteFilter extends OncePerRequestFilter {
    private final AdministracionRepository repository;
    public SesionVigenteFilter(AdministracionRepository repository) { this.repository=repository; }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            var usuario=repository.buscarNombre(auth.getName());
            if(usuario.isEmpty() || !usuario.get().activo() || auth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ACCOUNT_ID_"+usuario.get().id()))) {
                new SecurityContextLogoutHandler().logout(req,res,auth);
                res.sendRedirect(req.getContextPath()+"/login?expired"); return;
            }
        }
        chain.doFilter(req,res);
    }
}
