package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.GetTimezoneCollectionResponse;
import com.prx.directory.api.v1.to.TimezoneResumeTO;
import com.prx.directory.api.v1.to.TimezoneTO;
import com.prx.directory.jpa.entity.TimezoneEntity;
import com.prx.directory.jpa.repository.TimezoneRepository;
import com.prx.directory.mapper.TimezoneMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    TimezoneCacheService cacheService;
    TimezoneServiceImpl service;

    @BeforeEach
    void setup() {
        repository = mock(TimezoneRepository.class);
        mapper = Mappers.getMapper(TimezoneMapper.class);
        cacheService = mock(TimezoneCacheService.class);
        service = new TimezoneServiceImpl(repository, mapper, cacheService);
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
    @DisplayName("TimezoneServiceImpl: pageable mapping returns expected total elements")
    void getTimezonesPageable_mapsEntities() {
        Page<TimezoneEntity> page = new PageImpl<>(List.of(sampleEntity()));
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        Page<TimezoneTO> result = service.getTimezonesPageable(PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("TimezoneServiceImpl: findAll delegates to TimezoneCacheService and wraps response")
    void findAll_delegatesToCacheService() {
        UUID id = UUID.randomUUID();
        TimezoneResumeTO resumeTO = new TimezoneResumeTO(id, "UTC", "UTC");
        when(cacheService.getAllTimezones()).thenReturn(List.of(resumeTO));

        GetTimezoneCollectionResponse resp = service.findAll();

        assertEquals(1, resp.total());
        assertEquals(1, resp.timezones().size());
        verify(cacheService, times(1)).getAllTimezones();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("TimezoneServiceImpl: findAll returns empty collection when cache is empty")
    void findAll_returnsEmptyWhenCacheEmpty() {
        when(cacheService.getAllTimezones()).thenReturn(List.of());

        GetTimezoneCollectionResponse resp = service.findAll();

        assertEquals(0, resp.total());
        assertTrue(resp.timezones().isEmpty());
    }
}

