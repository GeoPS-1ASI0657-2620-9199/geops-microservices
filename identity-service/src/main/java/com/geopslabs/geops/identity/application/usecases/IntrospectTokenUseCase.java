package com.geopslabs.geops.identity.application.usecases;

public interface IntrospectTokenUseCase {
    /** token may be null or blank: the answer is then inactive. */
    TokenIntrospection introspect(String token);
}
