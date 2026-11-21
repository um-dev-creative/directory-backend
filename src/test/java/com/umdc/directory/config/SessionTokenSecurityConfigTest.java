package com.umdc.directory.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// The chain lets the API and the probes through and denies everything else; the authentication of the API is the session
/// interceptor's job (covered by the library), so a request reaches the controller here without any token.
@SpringJUnitWebConfig(SessionTokenSecurityConfigTest.TestApp.class)
class SessionTokenSecurityConfigTest {

    @Configuration
    @EnableWebSecurity
    static class TestApp {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            return new SessionTokenSecurityConfig("/api/v1").sessionTokenSecurityFilterChain(http);
        }

        @Bean
        Probe probe() {
            return new Probe();
        }
    }

    @RestController
    static class Probe {
        @RequestMapping({"/api/v1/businesses", "/actuator/health", "/actuator/health/liveness", "/actuator/env", "/v3/api-docs",
                "/swagger-ui/index.html", "/internal/other"})
        String ok() {
            return "ok";
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        // spring-security-test is not a dependency: add the security filter chain by hand
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
    }

    @Test
    void theApiPassesTheFilterChainWithoutABearerToken() throws Exception {
        mvc.perform(get("/api/v1/businesses")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/businesses")).andExpect(status().isOk());
    }

    @Test
    void theHealthProbesAreOpen() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }

    @Test
    void theApiDocumentationIsOpen() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void everythingElseIsDenied() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
        mvc.perform(get("/internal/other")).andExpect(status().isForbidden());
    }
}
