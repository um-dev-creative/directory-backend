package com.umdev.directory.api.v1.to;

import java.util.List;

public record GetTimezoneCollectionResponse(List<TimezoneResumeTO>  timezones, int total) {
}
