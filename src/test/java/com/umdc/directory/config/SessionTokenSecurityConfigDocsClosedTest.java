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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Production default (`umdc.docs.enabled=false`): Swagger UI and api-docs are denied, the API and the probes stay as before.
@SpringJUnitWebConfig(SessionTokenSecurityConfigDocsClosedTest.TestApp.class)
class SessionTokenSecurityConfigDocsClosedTest {

    @Configuration
    @EnableWebSecurity
    static class TestApp {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            // one-argument constructor = documentation closed
            return new SessionTokenSecurityConfig("/api/v1").sessionTokenSecurityFilterChain(http);
        }

        @Bean
        Probe probe() {
            return new Probe();
        }
    }

    @RestController
    static class Probe {
        @RequestMapping({"/api/v1/businesses", "/actuator/health", "/v3/api-docs", "/swagger-ui/index.html", "/swagger-ui.html"})
        String ok() {
            return "ok";
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
    }

    @Test
    void theApiDocumentationIsClosed() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isForbidden());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isForbidden());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isForbidden());
    }

    @Test
    void theApiAndTheProbesStayOpen() throws Exception {
        mvc.perform(get("/api/v1/businesses")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void theSpringConstructorTakesTheFlag() {
        assertThat(new SessionTokenSecurityConfig("/api/v1", false)).isNotNull();
    }
}
