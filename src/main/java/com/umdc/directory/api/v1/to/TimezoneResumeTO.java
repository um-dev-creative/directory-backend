package com.umdc.directory.api.v1.to;

import java.util.UUID;

public record TimezoneResumeTO(
        UUID id,
        String name,
        String abbreviation
) {

}
