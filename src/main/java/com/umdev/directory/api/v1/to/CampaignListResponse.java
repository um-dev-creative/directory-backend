package com.umdev.directory.api.v1.to;

import java.util.List;

/**
 * Response wrapper for campaign listings with pagination metadata.
 */
public record CampaignListResponse(
        long total_count,
        int page,
        int per_page,
        int total_pages,
        long actives,
        long inactives,
        long expired,
        List<CampaignResumeTO> items
) {}
