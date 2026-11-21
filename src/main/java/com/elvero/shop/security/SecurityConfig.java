package com.elvero.shop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * URL-level rules mirror the Express routers. Role rules per endpoint (authorizeRoles("ADMIN"), ...)
 * are written as @PreAuthorize on the controllers as each module is converted.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String BASE = "/api/ecommerce";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error", "/uploads/**").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/ecommerce/uploads/**"
                        ).permitAll()
                        // Auth
                        .requestMatchers(BASE + "/auth/my-addresses", BASE + "/auth/password-set").authenticated()
                        .requestMatchers(BASE + "/auth/**").permitAll()

                        // Razorpay webhook
                        .requestMatchers(HttpMethod.POST, BASE + "/payments/webhook").permitAll()

                        // Public storefront GET APIs
                        .requestMatchers(HttpMethod.GET,
                                BASE + "/products",
                                BASE + "/products/",
                                BASE + "/products/categories",
                                BASE + "/products/categories/**",
                                BASE + "/products/departments",
                                BASE + "/products/search",
                                BASE + "/products/featured",
                                BASE + "/products/trending",
                                BASE + "/products/new-arrivals",
                                BASE + "/products/best-sellers",
                                BASE + "/products/on-sale",
                                BASE + "/products/filters",
                                BASE + "/products/category/**",
                                BASE + "/products/subcategory/**",
                                BASE + "/products/variants/**",
                                BASE + "/products/*",
                                BASE + "/products/*/variants",
                                BASE + "/products/*/related",
                                BASE + "/products/*/recommendations"
                        ).permitAll()

                        // Public subcategory lookups used by storefront
                        .requestMatchers(HttpMethod.GET,
                                BASE + "/sub-categories/category/**",
                                BASE + "/sub-categories/*"
                        ).permitAll()

                        // Admin area
                        .requestMatchers(HttpMethod.GET,BASE + "/admin/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                BASE + "/categories/**"
                        ).permitAll()
//                        .requestMatchers(BASE + "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, BASE + "/sub-categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, BASE + "/sub-categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, BASE + "/sub-categories/**").hasRole("ADMIN")

                        .anyRequest().authenticated())
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, entryPoint),
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }

    /** Node used bcrypt with 12 rounds; hashes ($2a/$2b/$2y) are compatible. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
