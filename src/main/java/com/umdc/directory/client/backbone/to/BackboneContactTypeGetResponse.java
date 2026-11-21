package com.umdc.directory.client.backbone.to;

import java.util.UUID;

public record BackboneContactTypeGetResponse(
        UUID id,
        String name,
        String description,
        Boolean active
) {
}

