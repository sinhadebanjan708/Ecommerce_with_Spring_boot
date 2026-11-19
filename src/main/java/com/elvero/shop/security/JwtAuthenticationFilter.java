package com.elvero.shop.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Port of authMiddleware. A valid Bearer token authenticates the request. A bad token never blocks public
 * routes; it only records the reason so the entry point can answer protected routes exactly like Node did.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "auth.error";
    public static final String EXPIRED = "ACCESS_TOKEN_EXPIRED";
    public static final String INVALID = "INVALID_ACCESS_TOKEN";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (token.isEmpty()) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, INVALID);
            } else {
                authenticate(request, token);
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            DecodedJWT jwt = jwtService.verifyAccessToken(token);
            Long userId = jwt.getClaim("user_id").asLong();
            List<String> roles = JwtService.roles(jwt);
            if (userId == null) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, INVALID);
                return;
            }
            AuthenticatedUser principal = new AuthenticatedUser(userId, jwt.getClaim("email").asString(), roles);
            // Node upper-cased roles before comparing, so do the same.
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim().toUpperCase()))
                    .toList();
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
        } catch (TokenExpiredException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, EXPIRED);
        } catch (JWTVerificationException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, INVALID);
        }
    }
}
