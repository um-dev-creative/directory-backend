package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.DigitalContactService;
import com.prx.directory.api.v1.to.DigitalContactTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class DigitalContactControllerTest {

    @Mock
    private DigitalContactService digitalContactService;

    @InjectMocks
    private DigitalContactController controller;

    @Test
    @DisplayName("getAllDigitalContacts returns OK with page content")
    void getAllDigitalContacts_ok() {
        Page<DigitalContactTO> page = new PageImpl<>(List.of(sampleTO()));
        when(digitalContactService.getAllDigitalContacts(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<DigitalContactTO>> resp = controller.getAllDigitalContacts(0, 10);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().getTotalElements());
    }

    @Test
    @DisplayName("getDigitalContactById returns OK when found")
    void getDigitalContactById_found() {
        UUID id = UUID.randomUUID();
        when(digitalContactService.getDigitalContactById(id)).thenReturn(Optional.of(sampleTO()));

        ResponseEntity<DigitalContactTO> resp = controller.getDigitalContactById(id);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("getDigitalContactById returns NOT_FOUND when empty")
    void getDigitalContactById_notFound() {
        UUID id = UUID.randomUUID();
        when(digitalContactService.getDigitalContactById(id)).thenReturn(Optional.empty());

        ResponseEntity<DigitalContactTO> resp = controller.getDigitalContactById(id);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    @DisplayName("getDigitalContactById returns BAD_REQUEST on IllegalArgumentException")
    void getDigitalContactById_badRequest() {
        UUID id = UUID.randomUUID();
        when(digitalContactService.getDigitalContactById(id)).thenThrow(new IllegalArgumentException("bad"));

        ResponseEntity<DigitalContactTO> resp = controller.getDigitalContactById(id);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @DisplayName("getDigitalContactsByBusinessId returns OK when list not null")
    void getDigitalContactsByBusinessId_ok() {
        UUID businessId = UUID.randomUUID();
        when(digitalContactService.getDigitalContactsByBusinessId(businessId)).thenReturn(List.of(sampleTO()));

        ResponseEntity<List<DigitalContactTO>> resp = controller.getDigitalContactsByBusinessId(businessId);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    @DisplayName("getDigitalContactsByBusinessId returns NOT_FOUND when null")
    void getDigitalContactsByBusinessId_null() {
        UUID businessId = UUID.randomUUID();
        when(digitalContactService.getDigitalContactsByBusinessId(businessId)).thenReturn(null);

        ResponseEntity<List<DigitalContactTO>> resp = controller.getDigitalContactsByBusinessId(businessId);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    private static DigitalContactTO sampleTO() {
        return new DigitalContactTO(UUID.randomUUID(), "val", LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
