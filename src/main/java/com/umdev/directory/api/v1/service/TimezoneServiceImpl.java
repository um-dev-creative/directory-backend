package com.umdev.directory.api.v1.service;

import com.umdev.directory.api.v1.to.GetTimezoneCollectionResponse;
import com.umdev.directory.api.v1.to.TimezoneTO;
import com.umdev.directory.jpa.entity.TimezoneEntity;
import com.umdev.directory.jpa.repository.TimezoneRepository;
import com.umdev.directory.mapper.TimezoneMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Implementation of TimezoneService to provide supported timezones.
 */
@Service
public class TimezoneServiceImpl implements TimezoneService {

    private final TimezoneRepository timezoneRepository;
    private final TimezoneMapper timezoneMapper;

    @Autowired
    public TimezoneServiceImpl(TimezoneRepository timezoneRepository, TimezoneMapper timezoneMapper) {
        this.timezoneRepository = timezoneRepository;
        this.timezoneMapper = timezoneMapper;
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
     */
    @Override
    public GetTimezoneCollectionResponse findAll() {
        var timezoneList = timezoneRepository.findAll().stream().map(timezoneMapper::toTimezoneResume).toList();
        return  new GetTimezoneCollectionResponse(timezoneList, timezoneList.size());
    }
}
