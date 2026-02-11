package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.DigitalContactTO;
import com.prx.directory.jpa.entity.DigitalContactEntity;
import com.prx.directory.jpa.repository.DigitalContactRepository;
import com.prx.directory.mapper.DigitalContactMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DigitalContactServiceImplTest {

    DigitalContactRepository repository;
    DigitalContactMapper mapper;
    DigitalContactServiceImpl service;

    @BeforeEach
    void setup() {
        repository = mock(DigitalContactRepository.class);
        mapper = Mappers.getMapper(DigitalContactMapper.class);
        service = new DigitalContactServiceImpl(repository, mapper);
    }

    @Test
    void getAllDigitalContacts_mapsEntities() {
        var ent = new DigitalContactEntity();
        ent.setId(UUID.randomUUID());
        ent.setContent("c");
        ent.setCreatedDate(LocalDateTime.now());
        ent.setLastUpdate(LocalDateTime.now());
        var page = new PageImpl<>(List.of(ent));
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        Page<DigitalContactTO> result = service.getAllDigitalContacts(PageRequest.of(0,10));
        assertEquals(1, result.getTotalElements());
        assertNotNull(result.getContent().get(0));
    }

    @Test
    void getDigitalContactById_presentOrEmpty() {
        var ent = new DigitalContactEntity();
        ent.setId(UUID.randomUUID());
        ent.setContent("c");
        ent.setCreatedDate(LocalDateTime.now());
        ent.setLastUpdate(LocalDateTime.now());
        UUID id = ent.getId();
        when(repository.findById(id)).thenReturn(Optional.of(ent));

        Optional<DigitalContactTO> ok = service.getDigitalContactById(id);
        assertTrue(ok.isPresent());

        when(repository.findById(id)).thenReturn(Optional.empty());
        assertTrue(service.getDigitalContactById(id).isEmpty());
    }
}

