package com.umdc.directory.api.v1.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/// `umdc.location.bounds.enforced` (default `true`): reject pins outside the bounding box of Canada. Turning it off is meant for
/// tests and for a future multi-country release; the (0, 0) point stays rejected either way.
@Component
@ConfigurationProperties(prefix = "umdc.location.bounds")
public class LocationBoundsProperties {

    private boolean enforced = true;

    public LocationBoundsProperties() {
        // bound by Spring from umdc.location.bounds.*
    }

    public boolean isEnforced() {
        return enforced;
    }

    public void setEnforced(boolean enforced) {
        this.enforced = enforced;
        LocationValidator.setBoundsEnforced(enforced);
    }
}
