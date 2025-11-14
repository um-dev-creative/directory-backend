package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.CampaignService;
import com.prx.directory.api.v1.to.CampaignListResponse;
import com.prx.directory.api.v1.to.CampaignTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/campaigns", "/api/campaigns"})
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
    public ResponseEntity<CampaignListResponse> listCampaigns(Integer page, Integer perPage, String sort, Map<String, String> filters) {
        return campaignService.list(page, perPage, sort, filters);
    }
}
