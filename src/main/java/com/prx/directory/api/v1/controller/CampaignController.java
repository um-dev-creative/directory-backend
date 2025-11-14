package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.CampaignService;
import com.prx.directory.api.v1.to.CampaignTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}

