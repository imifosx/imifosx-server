package org.ideoholic.imifosx.portfolio.servicecharge.constants;

import java.math.BigDecimal;
import java.math.MathContext;

public interface ServiceChargeApiConstants {

	String serviceChargeRESTName = "servicecharge";
	String SLASH = "/";
	String SERVICE_CHARGE_REST_CALL = SLASH + serviceChargeRESTName;
	BigDecimal ONE_THOUSAND_TWO_HUNDRED = new BigDecimal("1200");
	BigDecimal HUNDRED = new BigDecimal("100");
	BigDecimal _365 = new BigDecimal("365");
	String SERVICE_CHARGE_NAME = "Service Charge";

}
