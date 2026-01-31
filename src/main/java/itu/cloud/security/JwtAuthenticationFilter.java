package itu.cloud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import itu.cloud.entities.Utilisateur;
import itu.cloud.service.ParametreService;
import itu.cloud.service.UtilisateurService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ParametreService parametreService;
    private final UtilisateurService utilisateurService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ParametreService parametreService, UtilisateurService utilisateurService) {
        this.jwtUtil = jwtUtil;
        this.parametreService = parametreService;
        this.utilisateurService = utilisateurService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String secret = parametreService.getJwtSecret();
            try {
                Jws<Claims> jws = jwtUtil.parseToken(secret, token);
                Claims claims = jws.getBody();

                // Prefer lookup by email because firestore document id != postgres id
                String email = claims.get("email", String.class);
                Utilisateur u = null;
                if (email != null && !email.isBlank()) {
                    u = utilisateurService.findByEmail(email).orElse(null);
                }
                if (u == null) {
                    Object userIdObj = claims.get("userId");
                    Integer userId = null;
                    if (userIdObj instanceof Number) {
                        userId = ((Number) userIdObj).intValue();
                    } else if (userIdObj instanceof String) {
                        try { userId = Integer.parseInt((String) userIdObj); } catch (NumberFormatException ignored) {}
                    }
                    if (userId != null) {
                        u = utilisateurService.findById(userId).orElse(null);
                    }
                }
                 if (u == null) {
                     response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Utilisateur inconnu");
                     return;
                 }
                if (u.getBloqueJusqua() != null && u.getBloqueJusqua().isAfter(java.time.Instant.now())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Compte bloqué");
                    return;
                }
                if (u.getActif() == null || !u.getActif()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Compte desactive");
                    return;
                }

                // set authentication with role if present
                String role = claims.get("role", String.class);
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER"));
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(u.getId(), null, Collections.singletonList(authority));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalide ou expiré");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
