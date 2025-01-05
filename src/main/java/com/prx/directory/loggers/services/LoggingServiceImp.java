/*
 *  @(#)LoggingServiceImp.java
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

package com.prx.directory.loggers.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the LoggingService interface.
 * Provides methods to log HTTP requests and responses.
 *
 * @version 1.0.0, 09-05-2022
 * @since 11
 */
@Service
public class LoggingServiceImp implements LoggingService {

    @Value("${prx.logging.trace.enabled}")
    private boolean isTraceEnabled;

    private static final Logger logger = LoggerFactory.getLogger(LoggingServiceImp.class);

    /**
     * Default constructor
     */
    public LoggingServiceImp() {
        // Default constructor
    }

    @Override
    public void displayRequest(HttpServletRequest request, Object body) {
        if(isTraceEnabled) {
            StringBuilder reqMessage = new StringBuilder(32);
            Map<String, String> parameters = getParameters(request);
            reqMessage.append("REQUEST method = [")
                    .append(request.getMethod())
                    .append("] path = [")
                    .append(request.getRequestURI())
                    .append(']');
            if(!parameters.isEmpty()) {
                reqMessage.append(" parameters = [").append(body).append(']');
            }
            if(!Objects.isNull(body)) {
                reqMessage.append(" body = [").append(body).append(']');
            }
            logger.info("log request: {}", reqMessage);
        }
    }

    @Override
    public void displayResponse(HttpServletRequest request, HttpServletResponse response, Object body) {
        if(isTraceEnabled) {
            StringBuilder respMessage = new StringBuilder(39);
            Map<String, String> headers = getHeaders(response);
            respMessage.append("RESPONSE method = [").append(request.getMethod()).append(']');
            if (!headers.isEmpty()) {
                respMessage.append(" ResponseHeaders = [").append(headers).append(']');
            }
            respMessage.append(" responseBody = [").append(body).append(']');
            logger.info("logResponse: {}", respMessage);
        }
    }

    private Map<String, String> getHeaders(HttpServletResponse response) {
        Map<String, String> headers = new ConcurrentHashMap<>();
        Collection<String> headerMap =  response.getHeaderNames();
        headerMap.forEach(s -> headers.put(s, response.getHeader(s)));
        return headers;
    }

    private Map<String,String> getParameters(HttpServletRequest request) {
        Map<String,String> parameters = new ConcurrentHashMap<>();
        Enumeration<String> params = request.getParameterNames();
        while(params.hasMoreElements()) {
            var paramName = params.nextElement();
            parameters.put(paramName,request.getParameter(paramName));
        }
        return parameters;
    }
}
