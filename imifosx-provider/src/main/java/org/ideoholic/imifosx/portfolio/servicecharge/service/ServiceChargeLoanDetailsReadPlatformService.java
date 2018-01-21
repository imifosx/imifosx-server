package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.math.BigDecimal;
import java.util.Collection;

import org.ideoholic.imifosx.infrastructure.core.service.SearchParameters;
import org.ideoholic.imifosx.portfolio.loanaccount.data.LoanAccountData;
import org.ideoholic.imifosx.portfolio.loanaccount.data.LoanTransactionData;
import org.ideoholic.imifosx.portfolio.servicecharge.data.ServiceChargeFinalSheetData;

public interface ServiceChargeLoanDetailsReadPlatformService {
	// Add Loan related Service Charge methods here

	BigDecimal getTotalLoansForCurrentQuarter();

	BigDecimal getAllLoansRepaymentData() throws Exception;

	void getLoansOutstandingAmount(ServiceChargeFinalSheetData sheetData) throws Exception;

	boolean findIfLoanDisbursedInCurrentQuarter(Long loanId);

	BigDecimal getTotalRepaymentsForCurrentQuarter(Long loanId);

	BigDecimal getTotalOutstandingAmountForCurrentQuarter(Long loanId);

	Collection<LoanTransactionData> retrieveLoanTransactionsOutstandingPayments(Long loanId, String startDate, String endDate);

	LoanAccountData checkLoanStatus(SearchParameters searchParameters, Long loanId);

	Collection<LoanTransactionData> retrieveLoanTransactionsOutstandingPaymentsPreviuosData(Long loanId);

}
