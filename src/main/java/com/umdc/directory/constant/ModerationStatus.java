package com.umdc.directory.constant;

/// Moderation state of a business. Only `APPROVED` businesses are public when `umdc.moderation.enabled=true`.
public enum ModerationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
