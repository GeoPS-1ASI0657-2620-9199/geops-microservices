package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.UploadOfferImageUseCase;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidOfferImageException;
import com.geopslabs.geops.catalog.shared.web.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * Photos of the offers. The upload sits under /campaigns because only the owner who publishes may upload (same rule
 * as the campaigns); reading sits under /offers because the photo is public like the offer card that shows it.
 */
@Tag(name = "Offer images", description = "Photos uploaded by the business for its offers")
@RestController
public class OfferImagesController {
    static final String UPLOAD_PATH = "/api/v1/campaigns/images";
    static final String READ_PATH = "/api/v1/offers/images/{imageId}";
    private static final Duration ONE_YEAR = Duration.ofDays(365);

    private final UploadOfferImageUseCase images;

    public OfferImagesController(UploadOfferImageUseCase images) {
        this.images = images;
    }

    public record UploadedImageResponse(UUID imageId, String path) {
    }

    @Operation(summary = "Upload the photo of an offer",
            description = "Requires ROLE_BUSINESS_OWNER. JPEG, PNG or WebP up to 2 MB, detected from its bytes. "
                    + "The answer gives the path to put in imageUrl of the offer.")
    @ApiResponse(responseCode = "201", description = "Stored")
    @ApiResponse(responseCode = "400", description = "Empty file or format other than JPEG, PNG or WebP (INVALID_IMAGE)")
    @ApiResponse(responseCode = "413", description = "Larger than 2 MB (IMAGE_TOO_LARGE)")
    @PostMapping(value = UPLOAD_PATH, consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<UploadedImageResponse> upload(@AuthenticationPrincipal Jwt jwt,
                                                        @RequestPart("file") MultipartFile file) throws IOException {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        var image = images.upload(businessId, file.getBytes());
        var path = READ_PATH.replace("{imageId}", image.imageId().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadedImageResponse(image.imageId(), path));
    }

    @Operation(summary = "Read the photo of an offer", description = "Public, like the offer card. Cached for a year: "
            + "a photo never changes, a new one gets a new id.")
    @ApiResponse(responseCode = "200", description = "The image bytes")
    @ApiResponse(responseCode = "404", description = "Unknown id")
    @GetMapping(READ_PATH)
    public ResponseEntity<byte[]> read(@PathVariable UUID imageId) {
        return images.find(imageId)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.contentType()))
                        .cacheControl(CacheControl.maxAge(ONE_YEAR).cachePublic().immutable())
                        .body(image.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @RestControllerAdvice(assignableTypes = OfferImagesController.class)
    static class Errors {
        private static final Logger LOGGER = LoggerFactory.getLogger(Errors.class);

        @ExceptionHandler(InvalidOfferImageException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        ErrorResponse handleInvalidImage(InvalidOfferImageException exception) {
            LOGGER.info("offer.image.rejected code={}", exception.getCode());
            return new ErrorResponse(exception.getCode(), exception.getMessage());
        }
    }
}
