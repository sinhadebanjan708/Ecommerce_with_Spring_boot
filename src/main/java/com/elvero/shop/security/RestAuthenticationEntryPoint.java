package com.elvero.shop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** 401 responses with the same bodies and codes as the Node authMiddleware. */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object reason = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        if (JwtAuthenticationFilter.EXPIRED.equals(reason)) {
            body.put("code", JwtAuthenticationFilter.EXPIRED);
            body.put("message", "Access token has expired");
        } else if (JwtAuthenticationFilter.INVALID.equals(reason)) {
            body.put("code", JwtAuthenticationFilter.INVALID);
            body.put("message", "Invalid access token");
        } else {
            body.put("message", "Access token is required");
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
