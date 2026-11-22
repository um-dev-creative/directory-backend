package com.umdc.directory.jpa.spec;

import java.util.UUID;

/// Filters of the business search; every field is optional and the ones that are set are combined with AND.
///
/// @param cityId     only businesses with a location in this city of the catalogue
/// @param city       only businesses with a location in the city of this name (case-insensitive); ignored when `cityId` is set
/// @param categoryId only businesses of this category or of one of its sub-categories
/// @param text       case-insensitive text found in the name or the description
public record BusinessSearchCriteria(UUID cityId, String city, UUID categoryId, String text) {
}
