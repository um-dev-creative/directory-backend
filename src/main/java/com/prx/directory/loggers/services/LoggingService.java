/*
 *  @(#)LoggingService.java
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

/**
 * LogginService.
 *
 * @author Luis Antonio Mata
 * @version 1.0.0, 09-05-2022
 * @since 11
 */
public interface LoggingService {

    void displayRequest(HttpServletRequest httpServletRequest, Object body);

    void displayResponse(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object body);
}
