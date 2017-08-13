package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.math.BigDecimal;

public interface ServiceChargeLoanDetailsReadPlatformService {
	// Add Loan related Service Charge methods here 
	
	BigDecimal getTotalLoansForCurrentQuarter();
	BigDecimal getAllLoansRepaymentData() throws Exception;
	boolean findIfLoanDisbursedInCurrentQuarter(Long loanId);
	BigDecimal getTotalRepaymentsForCurrentQuarter(Long loanId);
	BigDecimal getTotalOutstandingAmountForCurrentQuarter(Long loanId);
}
