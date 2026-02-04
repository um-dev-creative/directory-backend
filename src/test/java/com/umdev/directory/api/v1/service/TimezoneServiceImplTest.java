package com.umdev.directory.api.v1.service;

import com.umdev.directory.api.v1.to.GetTimezoneCollectionResponse;
import com.umdev.directory.api.v1.to.TimezoneTO;
import com.umdev.directory.jpa.entity.TimezoneEntity;
import com.umdev.directory.jpa.repository.TimezoneRepository;
import com.umdev.directory.mapper.TimezoneMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TimezoneServiceImplTest {

    TimezoneRepository repository;
    TimezoneMapper mapper;
    TimezoneServiceImpl service;

    @BeforeEach
    void setup() {
        repository = mock(TimezoneRepository.class);
        mapper = Mappers.getMapper(TimezoneMapper.class);
        service = new TimezoneServiceImpl(repository, mapper);
    }

    private static TimezoneEntity sampleEntity() {
        TimezoneEntity entity = new TimezoneEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("UTC");
        entity.setAbbreviation("UTC");
        entity.setUtcOffset(Duration.ZERO);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setLastUpdated(LocalDateTime.now());
        return entity;
    }

    @Test
    void getTimezonesPageable_mapsEntities() {
        Page<TimezoneEntity> page = new PageImpl<>(List.of(sampleEntity()));
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        Page<TimezoneTO> result = service.getTimezonesPageable(PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findAll_returnsCollectionResponse() {
        when(repository.findAll()).thenReturn(List.of(sampleEntity()));

        GetTimezoneCollectionResponse resp = service.findAll();
        assertEquals(1, resp.total());
        assertEquals(1, resp.timezones().size());
    }
}
