package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.CampaignService;
import com.umdev.directory.api.v1.to.CampaignListResponse;
import com.umdev.directory.api.v1.to.CampaignTO;
import com.umdev.directory.api.v1.to.CampaignUpdateRequest;
import com.umdev.directory.api.v1.to.CampaignUpdateResponse;
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
    public ResponseEntity<CampaignTO> create(CampaignTO campaignTO) {
        return campaignService.create(campaignTO);
    }

    @Override
    public ResponseEntity<CampaignTO> getById(UUID id) {
        return campaignService.find(id);
    }

    @Override
    public ResponseEntity<CampaignListResponse> listCampaigns(Integer page, Integer perPage, String sort, Map<String, String> filters) {
        return campaignService.list(page, perPage, sort, filters);
    }

    @Override
    public ResponseEntity<CampaignUpdateResponse> update(UUID id, CampaignUpdateRequest request) {
        return campaignService.update(id, request);
    }
}
