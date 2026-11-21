package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.GetTimezoneCollectionResponse;
import com.prx.directory.api.v1.to.TimezoneResumeTO;
import com.prx.directory.api.v1.to.TimezoneTO;
import com.prx.directory.jpa.entity.TimezoneEntity;
import com.prx.directory.jpa.repository.TimezoneRepository;
import com.prx.directory.mapper.TimezoneMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of TimezoneService to provide supported timezones.
 * Read operations delegate to {@link TimezoneCacheService} to serve results
 * from Redis when available and reduce load on PostgreSQL.
 */
@Service
public class TimezoneServiceImpl implements TimezoneService {

    private final TimezoneRepository timezoneRepository;
    private final TimezoneMapper timezoneMapper;
    private final TimezoneCacheService timezoneCacheService;

    public TimezoneServiceImpl(
            TimezoneRepository timezoneRepository,
            TimezoneMapper timezoneMapper,
            TimezoneCacheService timezoneCacheService) {
        this.timezoneRepository = timezoneRepository;
        this.timezoneMapper = timezoneMapper;
        this.timezoneCacheService = timezoneCacheService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<TimezoneTO> getTimezonesPageable(Pageable pageable) {
        Page<TimezoneEntity> timezoneEntities = timezoneRepository.findAll(pageable);
        return timezoneEntities.map(timezoneMapper::toTimezoneTO);
    }

    /**
     * {@inheritDoc}
     * Delegates to {@link TimezoneCacheService#getAllTimezones()} so the result is
     * served from Redis on cache hits.
     */
    @Override
    public GetTimezoneCollectionResponse findAll() {
        List<TimezoneResumeTO> timezoneList = timezoneCacheService.getAllTimezones();
        return new GetTimezoneCollectionResponse(timezoneList, timezoneList.size());
    }
}
