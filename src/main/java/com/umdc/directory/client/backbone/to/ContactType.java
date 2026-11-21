package com.umdc.directory.client.backbone.to;

import java.util.UUID;

public record ContactType(UUID id, String name, String description, boolean active) {
}
