package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.TimezoneService;
import com.prx.directory.api.v1.to.GetTimezoneCollectionResponse;
import com.prx.directory.api.v1.to.TimezoneResumeTO;
import com.prx.directory.api.v1.to.TimezoneTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class TimezoneControllerTest {

    @Mock
    private TimezoneService service;

    @InjectMocks
    private TimezoneController controller;

    @Test
    @DisplayName("getAllTimezones returns OK")
    void all_ok() {
        var resp = new GetTimezoneCollectionResponse(List.of(new TimezoneResumeTO(UUID.randomUUID(), "UTC", "UTC")), 1);
        when(service.findAll()).thenReturn(resp);
        var out = controller.getAllTimezones();
        assertEquals(HttpStatus.OK, out.getStatusCode());
        Assertions.assertNotNull(out.getBody());
        assertEquals(1, out.getBody().timezones().size());
    }

    @Test
    @DisplayName("getTimezones pageable returns OK")
    void pageable_ok() {
        var uuid = UUID.randomUUID();
        var t = new TimezoneTO(uuid, "UTC", Duration.ZERO, "ac", LocalDateTime.now(), LocalDateTime.now());
        Page<TimezoneTO> page = new PageImpl<>(List.of(t));
        when(service.getTimezonesPageable(any())).thenReturn(page);
        var out = controller.getTimezones(PageRequest.of(0, 10));
        assertEquals(HttpStatus.OK, out.getStatusCode());
        Assertions.assertNotNull(out.getBody());
        assertEquals(1, out.getBody().getTotalElements());
    }
}

