package com.umdev.directory.api.v1.service;

import com.umdev.directory.api.v1.to.DigitalContactTO;
import com.umdev.directory.jpa.repository.DigitalContactRepository;
import com.umdev.directory.mapper.DigitalContactMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DigitalContactServiceImpl implements DigitalContactService {
    private final DigitalContactRepository digitalContactRepository;
    private final DigitalContactMapper digitalContactMapper;

    public DigitalContactServiceImpl(DigitalContactRepository digitalContactRepository, DigitalContactMapper digitalContactMapper) {
        this.digitalContactRepository = digitalContactRepository;
        this.digitalContactMapper = digitalContactMapper;
    }

    @Override
    public Page<DigitalContactTO> getAllDigitalContacts(Pageable pageable) {
        return digitalContactRepository.findAll(pageable).map(digitalContactMapper::toTO);
    }

    @Override
    public Optional<DigitalContactTO> getDigitalContactById(UUID id) {
        return digitalContactRepository.findById(id).map(digitalContactMapper::toTO);
    }

    @Override
    public List<DigitalContactTO> getDigitalContactsByBusinessId(UUID businessId) {
        return digitalContactRepository.findByBusinessId(businessId)
                .stream()
                .map(digitalContactMapper::toTO)
                .toList();
    }
}
