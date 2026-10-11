package com.geopslabs.geops.identity.infrastructure.sunat;

/**
 * A load that did not replace the registry. The previous copy stays in sunat_taxpayers.
 */
public class SunatRegistryLoadException extends RuntimeException {

    public SunatRegistryLoadException(String message) {
        super(message);
    }

    public SunatRegistryLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
