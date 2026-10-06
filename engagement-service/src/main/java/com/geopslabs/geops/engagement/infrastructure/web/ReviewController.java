package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.application.usecases.ReviewCommandUseCase;
import com.geopslabs.geops.engagement.application.usecases.ReviewQueryUseCase;
import com.geopslabs.geops.engagement.domain.models.queries.GetReviewsByBusinessQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Reviews", description = "Reviews of businesses")
@RestController
@RequestMapping(value = "/api/v1/reviews", produces = APPLICATION_JSON_VALUE)
public class ReviewController {
    private final ReviewCommandUseCase reviewCommandService;
    private final ReviewQueryUseCase reviewQueryService;

    public ReviewController(ReviewCommandUseCase reviewCommandService, ReviewQueryUseCase reviewQueryService) {
        this.reviewCommandService = reviewCommandService;
        this.reviewQueryService = reviewQueryService;
    }

    @Operation(summary = "Publish a review of a business")
    @PostMapping
    public ResponseEntity<ReviewResource> create(@RequestBody CreateReviewResource resource) {
        var command = CreateReviewCommandFromResourceAssembler.toCommandFromResource(resource);
        var review = reviewCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReviewResourceFromEntityAssembler.toResourceFromEntity(review));
    }

    @Operation(summary = "Get the reviews of a business, newest first")
    @GetMapping
    public List<ReviewResource> getReviews(@Parameter(description = "Business ID") @RequestParam Long businessId) {
        return reviewQueryService.handle(new GetReviewsByBusinessQuery(businessId)).stream()
                .map(ReviewResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}
