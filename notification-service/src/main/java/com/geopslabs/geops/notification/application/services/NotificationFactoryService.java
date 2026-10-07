package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.Channel;
import com.geopslabs.geops.notification.domain.models.Notification;
import com.geopslabs.geops.notification.domain.models.NotificationType;

import java.util.List;
import java.util.Map;

public class NotificationFactoryService {
    public static final String RECIPIENT_ID = "recipientId";
    public static final String CHANNEL = "channel";
    public static final String RELATED_ENTITY_ID = "relatedEntityId";
    public static final String BUSINESS_NAME = "businessName";
    public static final String OFFER_TITLE = "offerTitle";
    public static final String RESERVATION_CODE = "reservationCode";
    public static final String VERIFICATION_RESULT = "verificationResult";
    private static final String MISSING_DATA_MESSAGE = "The %s notice needs %s";
    private static final Map<NotificationType, Template> TEMPLATES = Map.of(
            NotificationType.NEARBY_OFFER, new Template("Oferta cerca de ti",
                    "%s tiene una oferta cerca de donde estuviste: %s.", List.of(BUSINESS_NAME, OFFER_TITLE)),
            NotificationType.FOLLOWED_BUSINESS_OFFER, new Template("Nueva oferta de un comercio que sigues",
                    "%s publicó una oferta: %s.", List.of(BUSINESS_NAME, OFFER_TITLE)),
            NotificationType.REPORT_RECEIVED, new Template("Recibiste un reporte",
                    "Un consumidor reportó la reserva %s. Responde dentro del plazo.", List.of(RESERVATION_CODE)),
            NotificationType.REPORT_ANSWERED, new Template("Respondieron tu reporte",
                    "%s respondió tu reporte de la reserva %s.", List.of(BUSINESS_NAME, RESERVATION_CODE)),
            NotificationType.VERIFICATION_RESULT, new Template("Resultado de la verificación de tu negocio",
                    "La verificación de tu RUC terminó: %s.", List.of(VERIFICATION_RESULT)));

    public Notification create(NotificationType type, Map<String, String> data) {
        var template = TEMPLATES.get(type);
        var values = template.keys().stream().map(key -> required(type, data, key)).toArray();
        return Notification.draft(Long.valueOf(required(type, data, RECIPIENT_ID)), type,
                Channel.valueOf(required(type, data, CHANNEL)), template.title(),
                template.message().formatted(values), data.get(RELATED_ENTITY_ID));
    }

    private static String required(NotificationType type, Map<String, String> data, String key) {
        var value = data.get(key);
        if (value == null) {
            throw new IllegalArgumentException(MISSING_DATA_MESSAGE.formatted(type, key));
        }
        return value;
    }

    private record Template(String title, String message, List<String> keys) {
    }
}
