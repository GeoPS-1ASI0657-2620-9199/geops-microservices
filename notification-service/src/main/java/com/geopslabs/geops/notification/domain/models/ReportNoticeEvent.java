package com.geopslabs.geops.notification.domain.models;

public record ReportNoticeEvent(String eventType, Long reportId, Long consumerId, Long businessId, String reservationCode, String offerTitle, String resolution) {}
