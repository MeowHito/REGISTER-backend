package com.actionth.membership.service;

import com.actionth.membership.model.dto.AppErrorLogResponse;
import com.actionth.membership.model.request.AppErrorLogRequest;

import javax.servlet.http.HttpServletRequest;

public interface AppErrorLogService {

    AppErrorLogResponse saveErrorLog(AppErrorLogRequest request, HttpServletRequest httpRequest);

    /** Log a backend error without HttpServletRequest (for internal service use) */
    void logBackendError(String level, String context, String message, String stack);
}
