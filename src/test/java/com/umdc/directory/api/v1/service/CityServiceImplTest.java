package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.CityTO;
import com.umdc.directory.jpa.entity.CityEntity;
import com.umdc.directory.jpa.repository.CityRepository;
import com.umdc.directory.mapper.CityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CityServiceImplTest {

    @Mock
    private CityRepository cityRepository;
    @Mock
    private CityMapper cityMapper;

    @Test
    void listMapsTheCitiesOrderedByName() {
        var entity = new CityEntity();
        var to = new CityTO(UUID.randomUUID(), "Toronto", "Ontario", "CA");
        when(cityRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(entity));
        when(cityMapper.toTO(entity)).thenReturn(to);

        var response = new CityServiceImpl(cityRepository, cityMapper).list("ca", "tor");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(to), response.getBody());
        verify(cityRepository).findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(Sort.by("name").ascending().and(Sort.by("province"))));
    }
}
