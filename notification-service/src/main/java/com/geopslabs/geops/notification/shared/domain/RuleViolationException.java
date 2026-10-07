package com.geopslabs.geops.notification.shared.domain;

public abstract class RuleViolationException extends DomainException {
    protected RuleViolationException(String code, String message) {
        super(code, message);
    }
}
