package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.math.BigDecimal;

public interface ServiceChargeLoanDetailsReadPlatformService {
	// Add Loan related Service Charge methods here 
	
	BigDecimal getTotalLoans();
	BigDecimal getAllLoansRepaymentData() throws Exception;
}
