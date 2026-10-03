package com.geopslabs.geops.identity.shared;

public abstract class RuleViolationException extends DomainException {
    protected RuleViolationException(String code, String message) {
        super(code, message);
    }
}
