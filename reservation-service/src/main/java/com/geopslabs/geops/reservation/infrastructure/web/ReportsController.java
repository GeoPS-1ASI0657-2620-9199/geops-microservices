package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.application.usecases.AnswerReportUseCase;
import com.geopslabs.geops.reservation.application.usecases.GetReportUseCase;
import com.geopslabs.geops.reservation.application.usecases.ListReportsUseCase;
import com.geopslabs.geops.reservation.application.usecases.OpenReportUseCase;
import com.geopslabs.geops.reservation.application.usecases.ResolveReportUseCase;
import com.geopslabs.geops.reservation.domain.models.ReportResolution;
import com.geopslabs.geops.reservation.domain.models.ReportStatus;
import com.geopslabs.geops.reservation.domain.models.commands.AnswerReportCommand;
import com.geopslabs.geops.reservation.domain.models.commands.OpenReportCommand;
import com.geopslabs.geops.reservation.domain.models.commands.ResolveReportCommand;
import com.geopslabs.geops.reservation.domain.models.exceptions.InvalidReportStatusChangeException;
import com.geopslabs.geops.reservation.domain.models.queries.GetReportQuery;
import com.geopslabs.geops.reservation.domain.models.queries.ListReportsQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Reports", description = "Reports of offers that the business did not honor")
@RestController
@RequestMapping(value = "/api/v1", produces = APPLICATION_JSON_VALUE)
public class ReportsController {
    private static final String REPORTS_PATH = "/api/v1/reports/";
    private static final String OPEN_EXAMPLE = """
            {"reportId": 31, "reservationId": 5013, "reservationCode": "Q4ZT8K2M",
             "offerTitle": "2x1 en almuerzos ejecutivos", "reason": "No me aplicaron el 2x1 y me cobraron los dos platos.",
             "status": "OPEN", "openedAt": "2026-10-28T19:10:00Z", "responseDeadline": "2026-10-30T19:10:00Z",
             "response": null, "resolution": null, "resolvedAt": null}""";
    private static final String REVIEW_EXAMPLE = """
            {"reportId": 31, "reservationId": 5013, "reservationCode": "Q4ZT8K2M",
             "offerTitle": "2x1 en almuerzos ejecutivos", "reason": "No me aplicaron el 2x1 y me cobraron los dos platos.",
             "status": "IN_REVIEW", "openedAt": "2026-10-28T19:10:00Z", "responseDeadline": "2026-10-30T19:10:00Z",
             "response": {"text": "El 2x1 aplicaba solo de lunes a jueves; igual le devolvimos la diferencia.",
                          "respondedAt": "2026-10-29T12:00:00Z"}, "resolution": null, "resolvedAt": null}""";
    private static final String CLOSED_EXAMPLE = """
            {"reportId": 31, "reservationId": 5013, "reservationCode": "Q4ZT8K2M",
             "offerTitle": "2x1 en almuerzos ejecutivos", "reason": "No me aplicaron el 2x1 y me cobraron los dos platos.",
             "status": "CLOSED", "openedAt": "2026-10-28T19:10:00Z", "responseDeadline": "2026-10-30T19:10:00Z",
             "response": null, "resolution": "CONSUMER_FAVOR", "resolvedAt": "2026-10-29T15:30:00Z"}""";
    private static final String UNAUTHORIZED_EXAMPLE = """
            {"code": "UNAUTHORIZED", "message": "A valid token is required"}""";
    private static final String NOT_FOUND_EXAMPLE = """
            {"code": "REPORT_NOT_FOUND", "message": "Report 9999 was not found"}""";
    private static final String OTHER_PARTY_EXAMPLE = """
            {"code": "FORBIDDEN", "message": "Report 31 belongs to another account"}""";

    private final OpenReportUseCase openReport;
    private final ListReportsUseCase listReports;
    private final GetReportUseCase getReport;
    private final AnswerReportUseCase answerReport;
    private final ResolveReportUseCase resolveReport;

    public ReportsController(OpenReportUseCase openReport, ListReportsUseCase listReports, GetReportUseCase getReport,
                             AnswerReportUseCase answerReport, ResolveReportUseCase resolveReport) {
        this.openReport = openReport;
        this.listReports = listReports;
        this.getReport = getReport;
        this.answerReport = answerReport;
        this.resolveReport = resolveReport;
    }

    @Operation(summary = "Report that the business did not honor the offer",
            description = "Only the consumer who owns an ACTIVE reservation before its deadline. Opens the report with "
                    + "a 48 hour response deadline and moves the reservation to REPORTED.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "report", value = """
                    {"reason": "No me aplicaron el 2x1 y me cobraron los dos platos."}""")))
    @ApiResponse(responseCode = "201", description = "Report opened",
            content = @Content(examples = @ExampleObject(value = OPEN_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Reason shorter than 10 or longer than 500 characters",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "Cuéntanos el motivo en 10 a 500 caracteres."}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Not the consumer of the reservation",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "FORBIDDEN", "message": "Reservation 5013 belongs to another account"}""")))
    @ApiResponse(responseCode = "404", description = "Unknown reservation",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "RESERVATION_NOT_FOUND", "message": "Reservation 9999 was not found"}""")))
    @ApiResponse(responseCode = "409", description = "Already reported, or redeemed or expired",
            content = @Content(examples = {
                    @ExampleObject(name = "Already reported", value = """
                            {"code": "REPORT_ALREADY_EXISTS", "message": "Reservation 5013 already has a report"}"""),
                    @ExampleObject(name = "Not reportable", value = """
                            {"code": "RESERVATION_NOT_REPORTABLE", "message": "Reservation 5013 can no longer be reported"}""")}))
    @ApiResponse(responseCode = "410", description = "The reservation deadline passed",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "RESERVATION_EXPIRED", "message": "Reservation 5013 is past its deadline"}""")))
    @PostMapping(value = "/reservations/{id}/reports", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportResponse> openReport(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Reservation id", example = "5013") @PathVariable Long id,
            @Valid @RequestBody OpenReportRequest request) {
        var consumerId = AuthenticatedUser.from(jwt).consumerId();
        var view = openReport.open(new OpenReportCommand(id, consumerId, request.reason()));
        var body = ReportResponseAssembler.toResponse(view);
        return ResponseEntity.created(URI.create(REPORTS_PATH + body.reportId())).body(body);
    }

    @Operation(summary = "List the reports of the consumer or of the business in the token, newest first",
            description = "A consumer sees the reports they opened and a business owner those of their business. "
                    + "Without status it returns all of them.")
    @ApiResponse(responseCode = "200", description = "Reports of the caller",
            content = @Content(examples = @ExampleObject(value = "[" + OPEN_EXAMPLE + "]")))
    @ApiResponse(responseCode = "400", description = "Unknown status",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "status has an invalid value"}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @GetMapping("/reports")
    public List<ReportResponse> listReports(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Only reports in this status", example = "OPEN")
            @RequestParam(required = false) ReportStatus status) {
        var user = AuthenticatedUser.from(jwt);
        var query = user.businessId() != null ? ListReportsQuery.ofBusiness(user.businessId(), status)
                : ListReportsQuery.ofConsumer(user.consumerId(), status);
        return listReports.list(query).stream().map(ReportResponseAssembler::toResponse).toList();
    }

    @Operation(summary = "Get one report of the consumer or of the business in the token")
    @ApiResponse(responseCode = "200", description = "Report found",
            content = @Content(examples = @ExampleObject(value = REVIEW_EXAMPLE)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "The report belongs to another consumer or business",
            content = @Content(examples = @ExampleObject(value = OTHER_PARTY_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "Unknown report",
            content = @Content(examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @GetMapping("/reports/{id}")
    public ReportResponse getReport(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Report id", example = "31") @PathVariable Long id) {
        var user = AuthenticatedUser.from(jwt);
        return ReportResponseAssembler.toResponse(getReport.get(new GetReportQuery(id,
                user.businessId() == null ? user.consumerId() : null, user.businessId())));
    }

    @Operation(summary = "Answer a report before its deadline",
            description = "Only the owner of the business of the report, once, while it is OPEN and before its "
                    + "response deadline. The report moves to IN_REVIEW and the consumer is notified.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "answer", value = """
                    {"text": "El 2x1 aplicaba solo de lunes a jueves; igual le devolvimos la diferencia."}""")))
    @ApiResponse(responseCode = "201", description = "Answer stored",
            content = @Content(examples = @ExampleObject(value = REVIEW_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Text shorter than 10 or longer than 2000 characters",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "Escribe tu respuesta en 10 a 2000 caracteres."}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Not a business owner, or the report is of another business",
            content = @Content(examples = @ExampleObject(value = OTHER_PARTY_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "Unknown report",
            content = @Content(examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @ApiResponse(responseCode = "409", description = "The report is already in review or closed",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "REPORT_NOT_OPEN", "message": "Report 31 is IN_REVIEW and cannot be answered"}""")))
    @ApiResponse(responseCode = "410", description = "The response deadline passed",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "REPORT_RESPONSE_DEADLINE_PASSED", "message": "The deadline to answer report 31 has passed"}""")))
    @PostMapping(value = "/reports/{id}/responses", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportResponse> answer(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Report id", example = "31") @PathVariable Long id,
            @Valid @RequestBody AnswerReportRequest request) {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        var view = answerReport.answer(new AnswerReportCommand(id, businessId, request.text()));
        return ResponseEntity.created(URI.create(REPORTS_PATH + id)).body(ReportResponseAssembler.toResponse(view));
    }

    @Operation(summary = "Resolve a report in favor of the consumer",
            description = "Only the owner of the business of the report, from OPEN or IN_REVIEW. The report is "
                    + "closed and stops counting against the business. Resolving in favor of the business belongs "
                    + "to the administrator.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "resolution", value = """
                    {"status": "CLOSED", "resolution": "CONSUMER_FAVOR"}""")))
    @ApiResponse(responseCode = "200", description = "Report closed",
            content = @Content(examples = @ExampleObject(value = CLOSED_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "status is not CLOSED or resolution is missing",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "status must be CLOSED and resolution is required"}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Another business, or BUSINESS_FAVOR",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "FORBIDDEN", "message": "Only the administrator can resolve a report in favor of the business"}""")))
    @ApiResponse(responseCode = "404", description = "Unknown report",
            content = @Content(examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @ApiResponse(responseCode = "409", description = "The report is already closed",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "REPORT_ALREADY_CLOSED", "message": "Report 31 is already closed"}""")))
    @PatchMapping(value = "/reports/{id}/status", consumes = APPLICATION_JSON_VALUE)
    public ReportResponse changeStatus(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Report id", example = "31") @PathVariable Long id,
            @Valid @RequestBody ChangeReportStatusRequest request) {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        if (!"CLOSED".equals(request.status()) || request.resolution() == null) {
            throw new InvalidReportStatusChangeException();
        }
        ReportResolution resolution;
        try {
            resolution = ReportResolution.valueOf(request.resolution());
        } catch (IllegalArgumentException unknown) {
            throw new InvalidReportStatusChangeException();
        }
        return ReportResponseAssembler.toResponse(
                resolveReport.resolve(new ResolveReportCommand(id, businessId, resolution)));
    }
}
