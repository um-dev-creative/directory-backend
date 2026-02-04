package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.CampaignService;
import com.umdev.directory.api.v1.to.CampaignListResponse;
import com.umdev.directory.api.v1.to.CampaignResumeTO;
import com.umdev.directory.api.v1.to.CampaignTO;
import com.umdev.directory.api.v1.to.CampaignUpdateRequest;
import com.umdev.directory.api.v1.to.CampaignUpdateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class CampaignControllerTest {

    @Mock
    private CampaignService campaignService;

    @InjectMocks
    private CampaignController campaignController;

    @Test
    @DisplayName("listCampaigns: returns 200 with response body")
    void listCampaignsReturnsOk() {
        CampaignResumeTO resume = new CampaignResumeTO(UUID.randomUUID(), "t", null, null, "Automotive", null, "ACTIVE");
        CampaignListResponse response = new CampaignListResponse(0, 1, 20, 0, 0L, 0L, 0L, List.of(resume));
        when(campaignService.list(eq(1), eq(20), eq("-start_date"), any())).thenReturn(ResponseEntity.ok(response));

        ResponseEntity<CampaignListResponse> result = campaignController.listCampaigns(1, 20, "-start_date", Collections.emptyMap());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("getById: delegates to service and returns result")
    void getByIdDelegates() {
        UUID id = UUID.randomUUID();
        CampaignTO to = new CampaignTO(id, "n", null, null, null, null, null, null, null, null, true);
        when(campaignService.find(id)).thenReturn(ResponseEntity.ok(to));

        ResponseEntity<CampaignTO> resp = campaignController.getById(id);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(to, resp.getBody());
    }

    @Test
    @DisplayName("update: delegates to service and returns result")
    void updateDelegates() {
        UUID id = UUID.randomUUID();
        CampaignUpdateRequest request = new CampaignUpdateRequest("Updated Name", null, null, null, null, null, null, null, null, null);
        LocalDateTime lastUpdate = LocalDateTime.now();
        CampaignUpdateResponse updatedResp = new CampaignUpdateResponse(id, lastUpdate);

        when(campaignService.update(eq(id), eq(request))).thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).body(updatedResp));

        ResponseEntity<CampaignUpdateResponse> resp = campaignController.update(id, request);
        assertEquals(HttpStatus.ACCEPTED, resp.getStatusCode());
        assertEquals(updatedResp, resp.getBody());
    }
}
