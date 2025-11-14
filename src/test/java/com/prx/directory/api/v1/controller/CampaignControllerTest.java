package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.CampaignService;
import com.prx.directory.api.v1.to.CampaignListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;

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
        CampaignListResponse response = new CampaignListResponse(0, 1, 20, 0, List.of());
        when(campaignService.list(eq(1), eq(20), eq("-start_date"), any())).thenReturn(ResponseEntity.ok(response));

        ResponseEntity<CampaignListResponse> result = campaignController.listCampaigns(1, 20, "-start_date", Collections.emptyMap());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }
}
