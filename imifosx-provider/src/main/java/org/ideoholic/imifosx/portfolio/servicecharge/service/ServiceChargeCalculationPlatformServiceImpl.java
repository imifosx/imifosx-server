package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.math.BigDecimal;

import org.ideoholic.imifosx.portfolio.servicecharge.constants.ServiceChargeApiConstants;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.ideoholic.imifosx.portfolio.servicecharge.data.ServiceChargeFinalSheetData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeCalculationPlatformServiceImpl implements ServiceChargeCalculationPlatformService {

	private final static Logger logger = LoggerFactory.getLogger(ServiceChargeCalculationPlatformServiceImpl.class);

	private final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService;
	private final ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService;

	@Autowired
	public ServiceChargeCalculationPlatformServiceImpl(final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService,
			final ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService) {
		this.scJournalDetailsReadPlatformService = scJournalDetailsReadPlatformService;
		this.scLoanDetailsReadPlatformService = scLoanDetailsReadPlatformService;
	}

	@Override
	public BigDecimal calculateServiceChargeForLoan(Long loanId) {
		boolean isDisbursed = scLoanDetailsReadPlatformService.findIfLoanDisbursedInCurrentQuarter(loanId);
		BigDecimal totalRepaymensts = scLoanDetailsReadPlatformService.getTotalRepaymentsForCurrentQuarter(loanId);
		BigDecimal totalOutstanding = scLoanDetailsReadPlatformService.getTotalOutstandingAmountForCurrentQuarter(loanId);

		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeForLoan:isDisbursed=" + isDisbursed);
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeForLoan:totalRepaymensts=" + totalRepaymensts);
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeForLoan:totalOutstanding=" + totalOutstanding);

		return serviceChargeCalculationLogic(isDisbursed, totalRepaymensts, totalOutstanding);
	}

	/**
	 * All the service charge calculation logic should be put here. The same method should be used for service charge calculation from other methods.
	 * This method is private so that the internal logic is kept insulated for any future changes. For accessing this method, need to write
	 * appropriate wrappers that will talk to the external world
	 * 
	 * @param isDisbursed
	 * @param totalRepaymensts
	 * @param totalOutstanding
	 * @return
	 */
	private BigDecimal serviceChargeCalculationLogic(boolean isDisbursed, BigDecimal totalRepaymensts, BigDecimal totalOutstanding) {
		ServiceChargeFinalSheetData finalSheetData = scJournalDetailsReadPlatformService.generatefinalSheetData();
		BigDecimal repaymentCostPerRupee = finalSheetData.getColumnValue(ServiceChargeReportTableHeaders.REPAYMENT_PER_100, 0);
		BigDecimal annualizedCost = finalSheetData.getColumnValue(ServiceChargeReportTableHeaders.ANNUALIZED_COST_I, 0);
		BigDecimal serviceCostPerLoan = finalSheetData.getColumnValue(ServiceChargeReportTableHeaders.LOAN_SERVICING_PER_LOAN, 0);

		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceChargeCalculationLogic:repaymentCostPerRupee="
				+ repaymentCostPerRupee.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceChargeCalculationLogic:annualizedCost=" + annualizedCost.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceChargeCalculationLogic:serviceCostPerLoan/disbursement="
				+ serviceCostPerLoan.toPlainString());

		// Adding disbursement charge in case it was disbursed in the current quarter
		BigDecimal serviceCharge = isDisbursed ? serviceCostPerLoan : BigDecimal.ZERO;

		BigDecimal mobilization = totalOutstanding.multiply(repaymentCostPerRupee);
		mobilization = mobilization.divide(ServiceChargeApiConstants.ONE_THOUSAND_TWO_HUNDRED);

		BigDecimal repayment = totalRepaymensts.multiply(annualizedCost);
		repayment = repayment.divide(ServiceChargeApiConstants.HUNDRED);

		serviceCharge = serviceCharge.add(mobilization);
		serviceCharge = serviceCharge.add(repayment);

		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceChargeCalculationLogic:mobilization=" + mobilization.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceChargeCalculationLogic:repayment=" + repayment.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceChargeCalculationLogic:serviceCharge=" + serviceCharge.toPlainString());
		return serviceCharge;
	}

	@Override
	public BigDecimal calculateServiceChargeForPrincipal(BigDecimal principal) {
		boolean isDisbursed = true; // Assuming that it will be disbursed
		BigDecimal totalRepaymensts = BigDecimal.ZERO; // Assuming that on loan-payout, there is no repayment yet
		BigDecimal totalOutstanding = principal; // The current amount being disbursed is the outstanding loan amount
		return serviceChargeCalculationLogic(isDisbursed, totalRepaymensts, totalOutstanding);
	}

}