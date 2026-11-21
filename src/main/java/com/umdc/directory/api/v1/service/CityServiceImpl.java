package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.CityTO;
import com.umdc.directory.jpa.repository.CityRepository;
import com.umdc.directory.jpa.spec.CitySpecifications;
import com.umdc.directory.mapper.CityMapper;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;

    public CityServiceImpl(CityRepository cityRepository, CityMapper cityMapper) {
        this.cityRepository = cityRepository;
        this.cityMapper = cityMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<CityTO>> list(String country, String text) {
        var cities = cityRepository.findAll(CitySpecifications.from(country, text), Sort.by("name").ascending().and(Sort.by("province")))
                .stream()
                .map(cityMapper::toTO)
                .toList();
        return ResponseEntity.ok(cities);
    }
}
