package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.domain.models.RucVerification;

/**
 * QAS13: every answer of the verification tells the owner what happened and what to do next, in the words of the
 * panel. The reason code stays in the response for the web app.
 */
final class VerificationMessages {
    private static final String APPROVED = "Tu comercio quedó verificado. El sello aparecerá junto a tus ofertas.";
    private static final String RUC_NOT_FOUND = "No encontramos el RUC %s en el padrón de SUNAT de Lima y Callao. "
            + "Revisa que sea el RUC de tu negocio; si está mal registrado, escríbenos para corregirlo.";
    private static final String TAXPAYER_NOT_ACTIVE = "SUNAT muestra tu RUC con estado \"%s\". Regulariza el "
            + "estado en SUNAT y vuelve a solicitar la verificación.";
    private static final String TAXPAYER_NOT_HABIDO = "SUNAT muestra tu domicilio fiscal como \"%s\". Actualízalo "
            + "en SUNAT y vuelve a solicitar la verificación.";
    private static final String ADDRESS_NOT_GEOCODABLE = "No ubicamos \"%s\" en el mapa de Lima y Callao. Corrige "
            + "la dirección (vía, número y distrito) y vuelve a intentarlo; lo demás ya está validado.";
    private static final String GEOCODER_UNAVAILABLE = "El servicio de mapas no respondió. Tu verificación quedó "
            + "pendiente y la reintentaremos en unos minutos; no tienes que hacer nada.";

    private VerificationMessages() {
    }

    static String of(RucVerification verification, String checkedAddress) {
        if (verification.reason() == null) {
            return APPROVED;
        }
        return switch (verification.reason()) {
            case RUC_NOT_FOUND -> RUC_NOT_FOUND.formatted(verification.ruc().number());
            case TAXPAYER_NOT_ACTIVE -> TAXPAYER_NOT_ACTIVE.formatted(verification.taxpayerStatus());
            case TAXPAYER_NOT_HABIDO -> TAXPAYER_NOT_HABIDO.formatted(verification.taxpayerCondition());
            case ADDRESS_NOT_GEOCODABLE -> ADDRESS_NOT_GEOCODABLE.formatted(checkedAddress);
            case GEOCODER_UNAVAILABLE -> GEOCODER_UNAVAILABLE;
        };
    }
}
