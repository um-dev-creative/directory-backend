package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.CampaignService;
import com.umdc.directory.api.v1.to.CampaignListResponse;
import com.umdc.directory.api.v1.to.CampaignTO;
import com.umdc.directory.api.v1.to.CampaignUpdateRequest;
import com.umdc.directory.api.v1.to.CampaignUpdateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController implements CampaignApi {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @Override
    public ResponseEntity<CampaignTO> create(CampaignTO campaignTO, String sessionToken) {
        return campaignService.create(campaignTO, sessionToken);
    }

    @Override
    public ResponseEntity<CampaignTO> getById(UUID id, String sessionToken) {
        return campaignService.find(id, sessionToken);
    }

    @Override
    public ResponseEntity<CampaignListResponse> listCampaigns(Integer page, Integer perPage, String sort,
                                                              Map<String, String> filters, String sessionToken) {
        return campaignService.list(page, perPage, sort, filters, sessionToken);
    }

    @Override
    public ResponseEntity<CampaignUpdateResponse> update(UUID id, CampaignUpdateRequest request, String sessionToken) {
        return campaignService.update(id, request, sessionToken);
    }

    @Override
    public ResponseEntity<CampaignTO> deactivate(UUID id, String sessionToken) {
        return campaignService.deactivate(id, sessionToken);
    }

    @Override
    public ResponseEntity<CampaignTO> reactivate(UUID id, String sessionToken) {
        return campaignService.reactivate(id, sessionToken);
    }

    @Override
    public ResponseEntity<CampaignTO> publish(UUID id, String sessionToken) {
        return campaignService.publish(id, sessionToken);
    }
}
