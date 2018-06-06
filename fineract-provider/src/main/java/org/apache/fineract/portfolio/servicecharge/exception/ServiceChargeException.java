/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.servicecharge.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;

public class ServiceChargeException extends AbstractPlatformDomainRuleException {

    public static enum SERVICE_CHARGE_EXCEPTION_REASON {
        SC_CALCULATION_EXCEPTION, SC_INSTALLMENT_ADJUSTMENT_EXCEPTION;

        public String errorMessage() {
            if (name().toString().equalsIgnoreCase("SC_CALCULATION_EXCEPTION")) {
                return "Service Charge Exception occurred when calculating Service Charge Final Sheet values";
            } else if (name().toString().equalsIgnoreCase("SC_INSTALLMENT_ADJUSTMENT_EXCEPTION")) {
                return "This loan charge has already been waived";
            }
            return name().toString();
        }

        public String errorCode() {
            if (name().toString().equalsIgnoreCase("SC_CALCULATION_EXCEPTION")) {
                return "error.msg.charge.id.invalid";
            } else if (name().toString().equalsIgnoreCase("SC_INSTALLMENT_ADJUSTMENT_EXCEPTION")) {
                return "error.msg.loan.charge.already.waived";
            }
            return name().toString();
        }
    }

    public ServiceChargeException(final Long id) {
        super("error.msg.servicecharge.id.generic.exception", "Generic Exception used when calculating Service Charge values", id);
    }
    
    public ServiceChargeException(final SERVICE_CHARGE_EXCEPTION_REASON reason, final Long loanChargeId) {
        super(reason.errorCode(), reason.errorMessage(), loanChargeId);
    }
}
