package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.GetTimezoneCollectionResponse;
import com.prx.directory.api.v1.to.TimezoneTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * TimezoneService provides methods to retrieve timezone information.
 */
public interface TimezoneService {

    /**
     * Retrieves a list of all supported timezones.
     *
     * @return List of TimezoneTO objects.
     */
    default List<TimezoneTO> getTimezonesPageable() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Retrieves a paginated list of all supported timezones.
     *
     * @param pageable Pageable object for pagination and sorting
     * @return Page of TimezoneTO objects.
     */
    default Page<TimezoneTO> getTimezonesPageable(Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Retrieves a list of all available timezones.
     *
     * @return a list of TimezoneTO objects representing all supported timezones.
     */
    default GetTimezoneCollectionResponse findAll() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
