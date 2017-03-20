
package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;

import org.ideoholic.imifosx.infrastructure.core.service.Page;
import org.ideoholic.imifosx.infrastructure.core.service.SearchParameters;
import org.ideoholic.imifosx.portfolio.loanaccount.data.LoanAccountData;
import org.ideoholic.imifosx.portfolio.loanaccount.data.LoanChargeData;
import org.ideoholic.imifosx.portfolio.loanaccount.data.LoanTransactionData;
import org.ideoholic.imifosx.portfolio.loanaccount.service.LoanChargeReadPlatformService;
import org.ideoholic.imifosx.portfolio.loanaccount.service.LoanReadPlatformService;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.QuarterDateRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeLoanDetailsReadPlatformServiceImpl implements ServiceChargeLoanDetailsReadPlatformService {

	private final static Logger logger = LoggerFactory.getLogger(ServiceChargeLoanDetailsReadPlatformServiceImpl.class);

	private final LoanReadPlatformService loanReadPlatformService;
	private final LoanChargeReadPlatformService loanChargeReadPlatformService;

	@Autowired
	public ServiceChargeLoanDetailsReadPlatformServiceImpl(LoanReadPlatformService loanReadPlatformService, LoanChargeReadPlatformService loanChargeReadPlatformService) {
		this.loanReadPlatformService = loanReadPlatformService;
		this.loanChargeReadPlatformService = loanChargeReadPlatformService;
	}

	public BigDecimal getTotalLoans() {
		BigDecimal totalLoans = BigDecimal.ZERO;
		final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null, null);
		Page<LoanAccountData> loanAccountData = null;

		loanAccountData = loanReadPlatformService.retrieveAll(searchParameters);

		if (loanAccountData != null) {
			int totalNumberLoans = loanAccountData.getPageItems().size();
			totalLoans = new BigDecimal(totalNumberLoans);
		}

		return totalLoans;
	}

	public BigDecimal getAllLoansRepaymentData() throws Exception {
		logger.debug("entered into getAllLoansRepaymentData");

		BigDecimal totalRepayment = BigDecimal.ZERO;

		// create MathContext object with 4 precision
		MathContext mc = new MathContext(4);

		// Get the dates
		QuarterDateRange quarter = QuarterDateRange.getCurrentQuarter();
		String startDate = quarter.getFormattedFromDateString();
		String endDate = quarter.getFormattedToDateString();

		final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null, null);
		Page<LoanAccountData> loanAccountData = null;
		try {
			loanAccountData = loanReadPlatformService.retrieveAll(searchParameters);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < loanAccountData.getPageItems().size(); i++) {
			logger.debug("Total number of accounts" + loanAccountData.getPageItems().size());
			logger.debug("Monthly Payments");
			try {
				System.out.println("The loan id is " + loanAccountData.getPageItems().get(i).getId());
				final Collection<LoanTransactionData> currentLoanRepayments = this.loanReadPlatformService
						.retrieveLoanTransactionsMonthlyPayments(loanAccountData.getPageItems().get(i).getId(), startDate, endDate);

				for (LoanTransactionData loanTransactionData : currentLoanRepayments) {
					logger.debug("Date = " + loanTransactionData.dateOf() + "  Repayment Amount = " + loanTransactionData.getAmount());

					// perform add operation on bg1 with augend bg2 and context mc
					totalRepayment = totalRepayment.add(loanTransactionData.getAmount(), mc);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return totalRepayment;
	}

	public String getLoanChargeName(Long loanId) {
		String loanChargeData = null;
		final Collection<LoanChargeData> loanCharges = this.loanChargeReadPlatformService.retrieveLoanCharges(loanId);

		for (LoanChargeData loanChargeDataIT : loanCharges) {
			// loanChargeData = loanChargeDataIT.getAmount();
			loanChargeData = loanChargeDataIT.getName();
		}

		return loanChargeData;
	}

}