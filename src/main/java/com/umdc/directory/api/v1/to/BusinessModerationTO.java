package com.umdc.directory.api.v1.to;

import com.umdc.directory.constant.ModerationStatus;

import java.util.UUID;

/// Moderation state of a business: the status, and whether the owner proved the ownership with a verification code.
public record BusinessModerationTO(UUID businessId, ModerationStatus status, boolean verified) {
}
