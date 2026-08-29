package com.dsgp.beneficiary.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Foundation security configuration for the DSGP platform.
 *
 * <p>This class establishes the security architecture skeleton:
 * <ul>
 *   <li>Stateless session management (JWT-based — filter to be added in later phase)</li>
 *   <li>BCrypt password encoder bean</li>
 *   <li>Method-level security enabled ({@code @PreAuthorize} support)</li>
 *   <li>All requests are temporarily permitted during the architecture setup phase</li>
 * </ul>
 *
 * <p><strong>Roles defined in the architecture:</strong>
 * <ul>
 *   <li>{@code FIELD_OFFICER} — Ground verification</li>
 *   <li>{@code DISTRICT_OFFICER} — Review and escalation</li>
 *   <li>{@code FINANCE_APPROVER} — Fund release approval</li>
 *   <li>{@code ADMINISTRATOR} — System and scheme setup</li>
 * </ul>
 *
 * <p><strong>Phase:</strong> Architecture setup.
 * JWT filter, UserDetailsService, and full RBAC will be implemented
 * in the Security module implementation phase.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * Configures the HTTP security filter chain.
     *
     * <p>During the architecture setup phase, all endpoints are permitted
     * to allow the application context to start and basic API testing.
     * Full JWT-based authentication and role-based authorization will be
     * applied in the Security implementation phase.
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — using stateless JWT authentication
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session — no HTTP session will be created
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Permit all requests during architecture setup phase.
            // TODO: Replace with role-based rules in Security implementation phase:
            //   .requestMatchers("/api/v1/auth/**").permitAll()
            //   .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRATOR")
            //   .requestMatchers("/api/v1/beneficiary/**").hasAnyRole("FIELD_OFFICER", "ADMINISTRATOR")
            //   .anyRequest().authenticated()
            .authorizeHttpRequests(auth ->
                auth.anyRequest().permitAll());

        // TODO: Add JWT authentication filter here in Security implementation phase:
        //   http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder bean — used for hashing user passwords.
     *
     * <p>Strength factor defaults to 10 (computationally appropriate for production).
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
