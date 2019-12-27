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
package org.ideoholic.fineract.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;

public class ServiceChargeException extends AbstractPlatformDomainRuleException {

	/**
	 * Auto-generated serialVersionUID for this class
	 */
	private static final long serialVersionUID = -4127334063513633243L;

	public static enum SERVICE_CHARGE_EXCEPTION_REASON {
		SC_CALCULATION_EXCEPTION, SC_INSTALLMENT_ADJUSTMENT_EXCEPTION, SC_INVALID_CALCULATION_PARAM,
		SC_INVALID_MONTH_CODE;

		public String errorMessage() {
			if (name().toString().equalsIgnoreCase("SC_CALCULATION_EXCEPTION")) {
				return "Service Charge Exception occurred when calculating Service Charge Final Sheet values";
			} else if (name().toString().equalsIgnoreCase("SC_INSTALLMENT_ADJUSTMENT_EXCEPTION")) {
				return "This loan charge has already been waived";
			} else if (name().toString().equalsIgnoreCase("SC_INVALID_CALCULATION_PARAM")) {
				return "Invalid Service Charge calculation method set for current tenant.\n"
						+ "Check the system parameter value for servicecharge.calculation.method.<tenant-identifier>";
			} else if (name().toString().equalsIgnoreCase("SC_INVALID_MONTH_CODE")) {
				return "Invalid Service Charge month code passed. Month should be one of:\n"
						+ "Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec";
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
		super("error.msg.servicecharge.id.generic.exception",
				"Generic Exception used when calculating Service Charge values", id);
	}

	public ServiceChargeException(final SERVICE_CHARGE_EXCEPTION_REASON reason, final Long loanChargeId) {
		super(reason.errorCode(), reason.errorMessage(), loanChargeId);
	}
}
