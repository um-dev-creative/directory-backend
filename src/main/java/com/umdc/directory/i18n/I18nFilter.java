package com.umdc.directory.i18n;

import com.umdc.directory.constant.DirectoryAppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/// Resolves the request locale from `Accept-Language` (`en` default, `es`, `fr-CA`), keeps it in Spring's
/// {@link LocaleContextHolder} and in a request attribute for the rest of the request, answers it in `Content-Language`, and
/// translates the `message` and `message-error` response headers with {@link MessageTranslator}.
///
/// It runs before the rate limiter so that the `429` message is translated too.
public class I18nFilter extends OncePerRequestFilter {

    private final MessageTranslator translator;

    /// @param translator translator of the message headers
    public I18nFilter(MessageTranslator translator) {
        super();
        this.translator = translator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Locale locale = SupportedLocales.resolve(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE));
        request.setAttribute(SupportedLocales.REQUEST_ATTRIBUTE, locale);
        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(locale);
        response.setHeader(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag());
        try {
            chain.doFilter(request, new TranslatingResponse(response, translator, locale));
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    /// Response that translates the message headers as they are written.
    static final class TranslatingResponse extends HttpServletResponseWrapper {

        private final MessageTranslator translator;
        private final Locale locale;

        TranslatingResponse(HttpServletResponse response, MessageTranslator translator, Locale locale) {
            super(response);
            this.translator = translator;
            this.locale = locale;
        }

        @Override
        public void setHeader(String name, String value) {
            super.setHeader(name, translated(name, value));
        }

        @Override
        public void addHeader(String name, String value) {
            super.addHeader(name, translated(name, value));
        }

        private String translated(String name, String value) {
            if (DirectoryAppConstants.MESSAGE_ERROR_HEADER.equalsIgnoreCase(name)
                    || DirectoryAppConstants.MESSAGE_HEADER.equalsIgnoreCase(name)) {
                return translator.translate(value, locale);
            }
            return value;
        }
    }
}
