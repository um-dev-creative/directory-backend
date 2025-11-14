package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.CampaignService;
import com.prx.directory.api.v1.to.CampaignTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class CampaignControllerTest {

    @Mock
    private CampaignService campaignService;

    @InjectMocks
    private CampaignController controller;

    @Test
    @DisplayName("create returns CREATED on success")
    void create_created() {
        CampaignTO req = new CampaignTO(null, "N", null, Instant.now(), Instant.now().plusSeconds(60), UUID.randomUUID(), UUID.randomUUID(), null, null, true);
        CampaignTO res = new CampaignTO(UUID.randomUUID(), "N", null, req.startDate(), req.endDate(), req.categoryId(), req.businessId(), Instant.now(), Instant.now(), true);
        when(campaignService.create(any(CampaignTO.class))).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(res));

        ResponseEntity<CampaignTO> resp = controller.create(req);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(res, resp.getBody());
    }

    @Test
    @DisplayName("create returns BAD_REQUEST on validation error")
    void create_badRequest() {
        CampaignTO req = new CampaignTO(null, null, null, Instant.now(), Instant.now(), null, null, null, null, null);
        when(campaignService.create(any(CampaignTO.class))).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        ResponseEntity<CampaignTO> resp = controller.create(req);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }
}

