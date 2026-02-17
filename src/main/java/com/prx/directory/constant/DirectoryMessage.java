package com.prx.directory.constant;

import com.prx.commons.constants.httpstatus.type.MessageType;

public enum DirectoryMessage  implements MessageType {

    DATABASE_CERTIFICATE_ERROR(1001, "Database Certificate Error"),;

    private final int code;
    private final String status;

    DirectoryMessage(int code, String status) {
        this.code = code;
        this.status = status;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getCodeToString() {
        return String.valueOf(code);
    }

    @Override
    public String getStatus() {
        return status;
    }
}
