package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.TimezoneService;
import com.umdev.directory.api.v1.to.GetTimezoneCollectionResponse;
import com.umdev.directory.api.v1.to.TimezoneTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * TimezoneController implements the TimezoneApi interface.
 */
@RestController
public class TimezoneController implements TimezoneApi {

    private final TimezoneService timezoneService;

    @Autowired
    public TimezoneController(TimezoneService timezoneService) {
        this.timezoneService = timezoneService;
    }

    @Override
    public ResponseEntity<GetTimezoneCollectionResponse> getAllTimezones() {
        return ResponseEntity.ok(timezoneService.findAll());
    }

    @Override
    public ResponseEntity<Page<TimezoneTO>> getTimezones(Pageable pageable) {
        return ResponseEntity.ok(timezoneService.getTimezonesPageable(pageable));
    }
}
