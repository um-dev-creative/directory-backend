package com.prx.directory.constant;

public final class DirectoryAppConstants {
    public static final String ENTITY_PACKAGE = "com.prx.directory.jpa.entity";
    public static final String REPOSITORY_PACKAGE = "com.prx.directory.jpa.repository";
    public static final String MESSAGE_ERROR_HEADER = "message-error";

    private DirectoryAppConstants() {
        throw new IllegalStateException("Utility class");
    }
}
