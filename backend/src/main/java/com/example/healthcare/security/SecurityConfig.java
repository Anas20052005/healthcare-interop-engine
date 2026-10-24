package com.example.healthcare.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security configuration for the HL7-FHIR Integration Engine.
 * Configures stateless JWT authentication, password encoding, CORS, and endpoint authorization.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    private static final String PROD_FRONTEND_ORIGIN = "https://healthcare-interop-engine-frontend.vercel.app";
    private static final List<String> DEFAULT_DEV_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost",
            "http://localhost:80",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173"
    );

    @Value("${cors.allowed-origins:https://healthcare-interop-engine-frontend.vercel.app,http://localhost:3000,http://localhost:5173,http://localhost,http://localhost:80}")
    private String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            UserDetailsService userDetailsService,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers("/api/mock-fhir/**").permitAll()
                        .requestMatchers("/api/destinations/**").authenticated()
                        .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/hospitals").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/hospitals/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/hospitals", "/api/hospitals/**").authenticated()
                        .requestMatchers("/api/hl7/**").authenticated()
                        .requestMatchers("/api/patients/**").authenticated()
                        .requestMatchers("/api/encounters/**").authenticated()
                        .requestMatchers("/api/observations/**").authenticated()
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        java.util.Set<String> origins = new java.util.LinkedHashSet<>();

        // Always guarantee production frontend origin and local dev origins
        origins.add(PROD_FRONTEND_ORIGIN);
        origins.addAll(DEFAULT_DEV_ORIGINS);

        // Include any additional environment-configured origins, trimming whitespace and trailing slashes
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                    .forEach(origins::add);
        }

        configuration.setAllowedOrigins(new java.util.ArrayList<>(origins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Cache-Control",
                "Pragma"
        ));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
