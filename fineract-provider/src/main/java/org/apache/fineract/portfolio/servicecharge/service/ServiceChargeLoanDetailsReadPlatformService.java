package org.apache.fineract.portfolio.servicecharge.service;

import java.math.BigDecimal;

public interface ServiceChargeLoanDetailsReadPlatformService {
	// Add Loan related Service Charge methods here 
	
	BigDecimal getTotalLoansForCurrentQuarter();
	BigDecimal getAllLoansRepaymentData() throws Exception;
}
