package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.application.usecases.ReplyToReviewUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/reviews/{reviewId}/replies")
@Tag(name = "Reviews")
public class ReviewRepliesController {
    private final ReplyToReviewUseCase replies;
    public ReviewRepliesController(ReplyToReviewUseCase replies) { this.replies = replies; }

    @Operation(summary = "Reply once to a review of your own business")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = @ExampleObject(value = "{\"text\":\"Gracias por venir.\"}")))
    @ApiResponse(responseCode = "201", description = "Reply published", content = @Content(examples = @ExampleObject(value = "{\"reviewId\":3,\"replyId\":1,\"text\":\"Gracias por venir.\",\"repliedAt\":\"2026-10-29T10:00:00Z\"}")))
    @ApiResponse(responseCode = "400", description = "Text must have 1 to 2000 characters", content = @Content(examples = @ExampleObject(value = "{\"code\":\"INVALID_REQUEST\",\"message\":\"Revisa estos datos: text.\"}")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"A valid token is required\"}")))
    @ApiResponse(responseCode = "403", description = "Only the owner of the reviewed business can reply", content = @Content(examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"Solo el comercio del comentario puede responder.\"}")))
    @ApiResponse(responseCode = "404", description = "Review not found", content = @Content(examples = @ExampleObject(value = "{\"code\":\"REVIEW_NOT_FOUND\",\"message\":\"No encontramos ese comentario.\"}")))
    @ApiResponse(responseCode = "409", description = "Already replied", content = @Content(examples = @ExampleObject(value = "{\"code\":\"REPLY_ALREADY_EXISTS\",\"message\":\"Este comentario ya tiene una respuesta.\"}")))
    @PostMapping
    public ResponseEntity<ReplyResponse> reply(@AuthenticationPrincipal Jwt jwt, @PathVariable Long reviewId,
                                              @Valid @RequestBody ReplyRequest request) {
        Number businessId = jwt.getClaim("businessId");
        if (businessId == null) throw new TokenClaimsException();
        var reply = replies.reply(reviewId, businessId.longValue(), request.text());
        return ResponseEntity.status(201).body(new ReplyResponse(reviewId, reply.id(), reply.text(), reply.repliedAt()));
    }

    public record ReplyResponse(Long reviewId, Long replyId, String text, Instant repliedAt) {}
}
