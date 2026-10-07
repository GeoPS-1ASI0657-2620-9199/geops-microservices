package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.application.usecases.CreateReviewUseCase;
import com.geopslabs.geops.engagement.application.usecases.ListBusinessReviewsUseCase;
import com.geopslabs.geops.engagement.domain.models.queries.GetReviewsByBusinessQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Reviews", description = "Reviews of a business published after a verified redemption")
@RestController
@RequestMapping(value = ReviewsController.REVIEWS_PATH, produces = APPLICATION_JSON_VALUE)
public class ReviewsController {
    static final String REVIEWS_PATH = "/api/v1/reviews";

    private static final String REVIEW_EXAMPLE = """
            {"reviewId": 3, "businessId": 1, "rating": 5, "text": "Respetaron el precio y me atendieron rápido.",
             "verifiedRedemption": true, "createdAt": "2026-10-08T13:05:00Z"}""";
    private static final String UNAUTHORIZED_EXAMPLE = """
            {"code": "UNAUTHORIZED", "message": "A valid token is required"}""";

    private final CreateReviewUseCase createReview;
    private final ListBusinessReviewsUseCase listBusinessReviews;

    public ReviewsController(CreateReviewUseCase createReview, ListBusinessReviewsUseCase listBusinessReviews) {
        this.createReview = createReview;
        this.listBusinessReviews = listBusinessReviews;
    }

    @Operation(summary = "Publish a review of a business after a verified redemption",
            description = "Uses the oldest redeemed reservation of the consumer in that business that has no review yet.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "review", value = """
                    {"businessId": 1, "rating": 5, "text": "Respetaron el precio y me atendieron rápido."}""")))
    @ApiResponse(responseCode = "201", description = "Review published",
            content = @Content(examples = @ExampleObject(value = REVIEW_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Rating out of 1 to 5 or empty text",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "Revisa estos datos: rating."}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_CONSUMER or no redemption in that business",
            content = @Content(examples = {
                    @ExampleObject(name = "redemptionRequired", value = """
                            {"code": "REDEMPTION_REQUIRED",
                             "message": "Puedes comentar un comercio después de canjear una reserva en él."}"""),
                    @ExampleObject(name = "forbidden", value = """
                            {"code": "FORBIDDEN", "message": "Your account is not allowed to perform this operation"}""")}))
    @ApiResponse(responseCode = "409", description = "Every redemption in that business already has a review",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "REVIEW_ALREADY_EXISTS",
                     "message": "Ya comentaste cada canje que hiciste en este comercio."}""")))
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<ReviewResponse> createReview(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReviewRequest request) {
        var consumerId = AuthenticatedUser.from(jwt).consumerId();
        var review = createReview.create(request.toCommand(consumerId));
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewResponseAssembler.toResponse(review));
    }

    @Operation(summary = "List the reviews of a business, newest first")
    @ApiResponse(responseCode = "200", description = "Reviews of the business",
            content = @Content(examples = @ExampleObject(value = "[" + REVIEW_EXAMPLE + "]")))
    @ApiResponse(responseCode = "400", description = "Missing or invalid businessId",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "Revisa estos datos: businessId."}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @GetMapping
    public List<ReviewResponse> listReviews(
            @Parameter(description = "Business id", example = "1") @RequestParam Long businessId) {
        return listBusinessReviews.list(new GetReviewsByBusinessQuery(businessId)).stream()
                .map(ReviewResponseAssembler::toResponse)
                .toList();
    }
}
