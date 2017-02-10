package org.ideoholic.imifosx.portfolio.servicecharge.exception;

import org.ideoholic.imifosx.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class ServiceChargeNotFoundException extends AbstractPlatformResourceNotFoundException {

    public ServiceChargeNotFoundException(final Long id) {
        super("error.msg.charge.id.invalid", "Service Charge with identifier " + id + " does not exist", id);
    }
}
