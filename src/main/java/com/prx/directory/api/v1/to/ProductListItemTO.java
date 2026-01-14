package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ProductListItemTO is a record that represents a minimal data structure for a product listed in a category.
 * This class encapsulates details about the product, including its identity, description,
 * parent category, timestamps for creation and updates, and its active status.
 *
 * Fields:
 * - id: The unique identifier of the product.
 * - name: The name of the product.
 * - description: A brief description of the product.
 * - categoryParentId: The unique identifier of the parent category to which the product belongs.
 * - lastUpdate: The timestamp of the last update made to the product's details, formatted in a specific pattern.
 * - createdDate: The timestamp of when the product was created, formatted in a specific pattern.
 * - active: Indicates whether the product is active or not.
 */
public record ProductListItemTO(
        UUID id,
        String name,
        String description,
        UUID categoryParentId,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_MIL) LocalDateTime lastUpdate,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_MIL) LocalDateTime createdDate,
        Boolean active
) {}

