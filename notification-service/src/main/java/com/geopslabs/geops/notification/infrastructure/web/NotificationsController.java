package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.application.usecases.CountUnreadNotificationsUseCase;
import com.geopslabs.geops.notification.application.usecases.InboxNotificationView;
import com.geopslabs.geops.notification.application.usecases.ListMyNotificationsUseCase;
import com.geopslabs.geops.notification.application.usecases.MarkNotificationsReadUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Notifications", description = "Notices of the signed-in user shown in the bell of the web app")
@RestController
@RequestMapping(value = NotificationsController.NOTIFICATIONS_PATH, produces = APPLICATION_JSON_VALUE)
public class NotificationsController {
    static final String NOTIFICATIONS_PATH = "/api/v1/notifications";
    private static final String DEFAULT_SIZE = "20";

    private static final String UNAUTHORIZED_EXAMPLE = """
            {"code": "UNAUTHORIZED", "message": "A valid token is required"}""";
    private static final String FORBIDDEN_EXAMPLE = """
            {"code": "FORBIDDEN", "message": "Your account is not allowed to perform this operation"}""";

    private final ListMyNotificationsUseCase listNotifications;
    private final CountUnreadNotificationsUseCase countUnread;
    private final MarkNotificationsReadUseCase markRead;

    public NotificationsController(ListMyNotificationsUseCase listNotifications, CountUnreadNotificationsUseCase countUnread,
                                   MarkNotificationsReadUseCase markRead) {
        this.listNotifications = listNotifications;
        this.countUnread = countUnread;
        this.markRead = markRead;
    }

    @Operation(summary = "List my latest notices, newest first",
            description = "Only the notices GeoPS decided to send (sent or pending) to the signed-in user. offerId is filled only for offer notices.")
    @ApiResponse(responseCode = "200", description = "Latest notices", content = @Content(examples = @ExampleObject(value = """
            [{"id": 14, "type": "NEARBY_OFFER", "title": "Oferta cerca de ti",
              "message": "Cevichería Brisa del Morro: Ceviche clásico con chicha, a 546 m, válida hasta el 22/01.",
              "offerId": 30073, "createdAt": "2026-11-23T14:03:17Z", "read": false}]""")))
    @ApiResponse(responseCode = "400", description = "size outside 1 to 50", content = @Content(examples = @ExampleObject(value = """
            {"code": "INVALID_REQUEST", "message": "La cantidad de avisos debe estar entre 1 y 50."}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without a consumer or business owner role", content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @GetMapping
    public List<InboxNotificationView> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = DEFAULT_SIZE) int size) {
        return listNotifications.list(AuthenticatedUser.from(jwt).consumerId(), size);
    }

    @Operation(summary = "Count my unread notices")
    @ApiResponse(responseCode = "200", description = "Unread notices", content = @Content(examples = @ExampleObject(value = "{\"unread\": 3}")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without a consumer or business owner role", content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @GetMapping("/unread-count")
    public UnreadCountResponse unread(@AuthenticationPrincipal Jwt jwt) {
        return new UnreadCountResponse(countUnread.count(AuthenticatedUser.from(jwt).consumerId()));
    }

    @Operation(summary = "Mark all my notices as read")
    @ApiResponse(responseCode = "204", description = "Notices marked as read")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without a consumer or business owner role", content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal Jwt jwt) {
        markRead.markAllRead(AuthenticatedUser.from(jwt).consumerId());
    }

    public record UnreadCountResponse(long unread) { }
}
