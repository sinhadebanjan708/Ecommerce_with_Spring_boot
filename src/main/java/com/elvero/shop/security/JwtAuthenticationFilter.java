
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

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "auth.error";
    public static final String EXPIRED = "ACCESS_TOKEN_EXPIRED";
    public static final String INVALID = "INVALID_ACCESS_TOKEN";

    private final JwtService jwtService;
    private final RestAuthenticationEntryPoint entryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            RestAuthenticationEntryPoint entryPoint
    ) {
        this.jwtService = jwtService;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7).trim();

            if (token.isEmpty()) {
                reject(request, response, INVALID);
                return;
            }

            try {
                DecodedJWT jwt = jwtService.verifyAccessToken(token);

                Long userId = jwt.getClaim("user_id").asLong();
                List<String> roles = JwtService.roles(jwt);

                if (userId == null) {
                    reject(request, response, INVALID);
                    return;
                }

                AuthenticatedUser principal = new AuthenticatedUser(
                        userId,
                        jwt.getClaim("email").asString(),
                        roles
                );

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority(
                                "ROLE_" + role.trim().toUpperCase()
                        ))
                        .toList();

                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                authorities
                        )
                );

            } catch (TokenExpiredException e) {
                reject(request, response, EXPIRED);
                return;

            } catch (JWTVerificationException e) {
                reject(request, response, INVALID);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            String errorCode
    ) throws IOException, ServletException {

        SecurityContextHolder.clearContext();

        request.setAttribute(AUTH_ERROR_ATTRIBUTE, errorCode);

        entryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException(
                        "Access token authentication failed"
                )
        );
    }
}
