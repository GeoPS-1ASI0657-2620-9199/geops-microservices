package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.queries.GetReviewsByBusinessQuery;
import com.geopslabs.geops.engagement.application.usecases.ReviewCommandUseCase;
import com.geopslabs.geops.engagement.application.usecases.ReviewQueryUseCase;
import com.geopslabs.geops.engagement.infrastructure.web.CreateReviewResource;
import com.geopslabs.geops.engagement.infrastructure.web.ReviewResource;
import com.geopslabs.geops.engagement.infrastructure.web.CreateReviewCommandFromResourceAssembler;
import com.geopslabs.geops.engagement.infrastructure.web.ReviewResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Reviews", description = "Review operations and management")
@RestController
@RequestMapping(value = "/api/v1/reviews", produces = APPLICATION_JSON_VALUE)
public class ReviewController {
    private final ReviewCommandUseCase reviewCommandService;
    private final ReviewQueryUseCase reviewQueryService;

    public ReviewController(ReviewCommandUseCase reviewCommandService,
                           ReviewQueryUseCase reviewQueryService) {
        this.reviewCommandService = reviewCommandService;
        this.reviewQueryService = reviewQueryService;
    }

    @Operation(summary = "Create new review")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Review created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ReviewResource> create(@RequestBody CreateReviewResource resource) {
        var command = CreateReviewCommandFromResourceAssembler.toCommandFromResource(resource);
        var review = reviewCommandService.handle(command);

        if (review.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var reviewResource = ReviewResourceFromEntityAssembler.toResourceFromEntity(review.get());
        return new ResponseEntity<>(reviewResource, CREATED);
    }

    @Operation(summary = "Get the reviews of a business, newest first")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<ReviewResource>> getReviews(
            @Parameter(description = "Business ID") @RequestParam Long businessId) {
        var reviews = reviewQueryService.handle(new GetReviewsByBusinessQuery(businessId));
        return ResponseEntity.ok(reviews.stream()
                .map(ReviewResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }
}
