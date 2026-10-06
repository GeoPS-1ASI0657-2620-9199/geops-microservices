package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.GetCampaignByIdUseCase;
import com.geopslabs.geops.catalog.application.usecases.ListBusinessCampaignsUseCase;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignByIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignsByBusinessIdQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Campaigns", description = "Advertising campaigns of the businesses")
@RestController
@RequestMapping(value = "/api/v1/campaigns", produces = APPLICATION_JSON_VALUE)
public class CampaignController {
    private final GetCampaignByIdUseCase getCampaignById;
    private final ListBusinessCampaignsUseCase listBusinessCampaigns;

    public CampaignController(GetCampaignByIdUseCase getCampaignById,
                              ListBusinessCampaignsUseCase listBusinessCampaigns) {
        this.getCampaignById = getCampaignById;
        this.listBusinessCampaigns = listBusinessCampaigns;
    }

    @Operation(summary = "Gets a campaign by its id")
    @ApiResponse(responseCode = "200", description = "Campaign found")
    @ApiResponse(responseCode = "404", description = "Campaign not found")
    @GetMapping("/{id}")
    public CampaignResource getById(@Parameter(description = "Campaign unique identifier") @PathVariable Long id) {
        return CampaignResourceFromEntityAssembler.toResourceFromEntity(
                getCampaignById.getById(new GetCampaignByIdQuery(id)));
    }

    @Operation(summary = "Gets all the campaigns of a business")
    @ApiResponse(responseCode = "200", description = "Campaigns of the business")
    @GetMapping("/business/{businessId}/campaigns")
    public List<CampaignResource> getCampaignsByBusinessId(
            @Parameter(description = "Business unique identifier") @PathVariable Long businessId) {
        return listBusinessCampaigns.list(new GetCampaignsByBusinessIdQuery(businessId)).stream()
                .map(CampaignResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}
