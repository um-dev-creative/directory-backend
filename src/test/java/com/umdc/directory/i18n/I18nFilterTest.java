package com.umdc.directory.i18n;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nFilterTest {

    private final I18nFilter filter = new I18nFilter(new MessageTranslator());

    private MockHttpServletResponse run(String acceptLanguage, String headerName, String message, AtomicReference<Locale> seen,
                                        MockHttpServletRequest request) throws Exception {
        if (acceptLanguage != null) {
            request.addHeader("Accept-Language", acceptLanguage);
        }
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void service(jakarta.servlet.http.HttpServletRequest req, HttpServletResponse res) {
                seen.set(LocaleContextHolder.getLocale());
                res.setHeader(headerName, message);
                res.addHeader("X-Other", "Business not found.");
            }
        });
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void frenchRequestTranslatesMessageErrorAndSetsContentLanguage() throws Exception {
        var seen = new AtomicReference<Locale>();
        var request = new MockHttpServletRequest();
        var response = run("fr-CA,fr;q=0.9", "message-error", "Business not found.", seen, request);
        assertEquals("Entreprise introuvable.", response.getHeader("message-error"));
        assertEquals("fr-CA", response.getHeader("Content-Language"));
        assertEquals(SupportedLocales.FR_CA, seen.get());
        assertEquals(SupportedLocales.FR_CA, request.getAttribute(SupportedLocales.REQUEST_ATTRIBUTE));
        assertEquals("Business not found.", response.getHeader("X-Other"));
    }

    @Test
    void spanishTranslatesMessageHeaderWithAddHeader() throws Exception {
        var response = run("es", "MESSAGE", "User not found.", new AtomicReference<>(), new MockHttpServletRequest());
        assertEquals("Usuario no encontrado.", response.getHeader("message"));
        assertEquals("es", response.getHeader("Content-Language"));
    }

    @Test
    void noHeaderIsEnglishAndMessageUntouched() throws Exception {
        var response = run(null, "message-error", "Business not found.", new AtomicReference<>(), new MockHttpServletRequest());
        assertEquals("Business not found.", response.getHeader("message-error"));
        assertEquals("en", response.getHeader("Content-Language"));
    }

    @Test
    void unknownLanguageFallsBackToEnglishAndContextIsRestored() throws Exception {
        LocaleContextHolder.setLocale(Locale.JAPAN);
        try {
            var response = run("de", "message-error", "Business not found.", new AtomicReference<>(), new MockHttpServletRequest());
            assertEquals("Business not found.", response.getHeader("message-error"));
            assertEquals("en", response.getHeader("Content-Language"));
            assertEquals(Locale.JAPAN, LocaleContextHolder.getLocale());
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void unknownMessagePassesThrough() throws Exception {
        var response = run("fr", "message-error", "Brand new message", new AtomicReference<>(), new MockHttpServletRequest());
        assertEquals("Brand new message", response.getHeader("message-error"));
    }

    @Test
    void configRegistersTheFilterAheadOfRateLimit() {
        FilterRegistrationBean<I18nFilter> registration = new I18nConfig().i18nFilterRegistration(new MessageTranslator());
        assertTrue(registration.getOrder() < org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10);
        assertTrue(registration.getFilter() instanceof I18nFilter);
    }
}
