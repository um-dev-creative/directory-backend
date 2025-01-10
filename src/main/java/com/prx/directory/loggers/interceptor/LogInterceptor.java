/*
 *  @(#)LogInterceptor.java
 *
 *  Copyright (c) Luis Antonio Mata Mata. All rights reserved.
 *
 *   All rights to this product are owned by Luis Antonio Mata Mata and may only
 *  be used under the terms of its associated license document. You may NOT
 *  copy, modify, sublicense, or distribute this source file or portions of
 *  it unless previously authorized in writing by Luis Antonio Mata Mata.
 *  In any event, this notice and the above copyright must always be included
 *  verbatim with this file.
 */

package com.prx.directory.loggers.interceptor;

import com.prx.directory.loggers.services.LoggingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * InterceptorLog.
 *
 * @author Luis Antonio Mata
 * @version 1.0.0, 15-05-2022
 * @since 11
 */
@Component
public class LogInterceptor implements HandlerInterceptor {

    private final LoggingService loggingService;

    public LogInterceptor(LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        if (request.getMethod().equals(HttpMethod.GET.name())
                ||request.getMethod().equals(HttpMethod.POST.name())
                ||request.getMethod().equals(HttpMethod.DELETE.name())
                ||request.getMethod().equals(HttpMethod.PUT.name())) {
            loggingService.displayRequest(request, null);
        }
        return true;
    }
}
