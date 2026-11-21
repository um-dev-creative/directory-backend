package com.umdc.directory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/// Spring Security chain of the service, the same model as mercury and zerobound-rest: the chain lets the API through and the
/// authentication happens at the MVC layer, in security-oauth's `SessionJwtInterceptor`, which validates the `session-token` header
/// through `SessionJwtServiceImpl` (signature, expiry, token type and the revocation list) and answers 401 otherwise.
///
/// It replaces the library's `SecurityConfig` (excluded in `DirectoryBackendApplication`), a Keycloak-style JWT resource-server chain that
/// asked for a bearer token no client sends, needed a `jwk-set-uri` and read its keystore from the classpath only, which a GraalVM
/// binary does not embed. The interceptor covers exactly `umdc.api.endpoint` + `/**`, so that same property is the only API matcher
/// opened here and the two cannot drift apart. Everything not listed is denied.
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SessionTokenSecurityConfig {

    private final String apiPath;

    /// @param apiPath the base path the session interceptor protects (`umdc.api.endpoint`)
    public SessionTokenSecurityConfig(@Value("${umdc.api.endpoint}") String apiPath) {
        this.apiPath = apiPath;
    }

    /// @param http the Spring Security builder
    /// @return the stateless chain: API and probes open to the filter chain, everything else denied
    @Bean
    public SecurityFilterChain sessionTokenSecurityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        // authenticated by SessionJwtInterceptor
                        .requestMatchers(apiPath + "/**").permitAll()
                        // container HEALTHCHECK and Kubernetes probes: liveness never touches Postgres, MongoDB, Redis or Kafka
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources",
                                "/swagger-resources/**").permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}
