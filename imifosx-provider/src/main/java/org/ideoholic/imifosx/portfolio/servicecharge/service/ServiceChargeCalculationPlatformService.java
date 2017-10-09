package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.math.BigDecimal;

public interface ServiceChargeCalculationPlatformService {
	/**
	 * Service charge calculation method for a given loan, with the loan being already saved and is currently active and functional
	 * 
	 * @param loanId
	 * @return Service Charge
	 */
	BigDecimal calculateServiceChargeForLoan(Long loanId);

	/**
	 * Service charge calculation method for a given loan, when the loan is yet being disbursed and hence only the principal amount is known. It
	 * assumes that the loan is disbursed, no repayments yet made and principal amount is the outstanding amount of the loan.
	 * 
	 * @param principal
	 * @return Service Charge
	 */
	BigDecimal calculateServiceChargeForPrincipal(BigDecimal principal);
}
