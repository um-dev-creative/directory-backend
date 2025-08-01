package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.DigitalContactService;
import com.prx.directory.api.v1.to.DigitalContactTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/digital-contact")
public class DigitalContactController implements DigitalContactApi {
    private final DigitalContactService digitalContactService;

    @Autowired
    public DigitalContactController(DigitalContactService digitalContactService) {
        this.digitalContactService = digitalContactService;
    }

    @Override
    public ResponseEntity<Page<DigitalContactTO>> getAllDigitalContacts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DigitalContactTO> contacts = digitalContactService.getAllDigitalContacts(pageable);
        return ResponseEntity.ok(contacts);
    }

    @Override
    public ResponseEntity<DigitalContactTO> getDigitalContactById(UUID id) {
        try {
            Optional<DigitalContactTO> contactOpt = digitalContactService.getDigitalContactById(id);
            return contactOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Override
    public ResponseEntity<List<DigitalContactTO>> getDigitalContactsByBusinessId(UUID businessId) {
        var result = digitalContactService.getDigitalContactsByBusinessId(businessId);
        if (Objects.nonNull(result)) {
            return  ResponseEntity.ok(result);
        }
        return  ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}

