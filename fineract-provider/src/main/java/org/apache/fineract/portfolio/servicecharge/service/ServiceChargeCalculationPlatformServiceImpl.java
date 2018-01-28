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
package org.apache.fineract.portfolio.servicecharge.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Collection;

import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.servicecharge.constants.QuarterDateRange;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeApiConstants;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeData;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeFinalSheetData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeCalculationPlatformServiceImpl implements ServiceChargeCalculationPlatformService {

	private final static Logger logger = LoggerFactory.getLogger(ServiceChargeCalculationPlatformServiceImpl.class);

	private final ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService;
	private final ServiceChargeReadPlatformService scChargeReadPlatformService;
	@Autowired
	private ApplicationContext appContext;
	
	@Autowired
	public ServiceChargeCalculationPlatformServiceImpl(final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService,
			final ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService,
			ServiceChargeReadPlatformService scChargeReadPlatformService) {
		this.scLoanDetailsReadPlatformService = scLoanDetailsReadPlatformService;
		this.scChargeReadPlatformService = scChargeReadPlatformService;
	}

	@Override
	public BigDecimal calculateServiceChargeForLoan(Long loanId) {
		boolean isDisbursed = scLoanDetailsReadPlatformService.findIfLoanDisbursedInCurrentQuarter(loanId);
		BigDecimal totalRepaymensts = scLoanDetailsReadPlatformService.getTotalRepaymentsForCurrentQuarter(loanId);
		BigDecimal totalOutstanding = scLoanDetailsReadPlatformService.getTotalOutstandingAmountForCurrentQuarter(loanId);

		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeForLoan:isDisbursed=" + isDisbursed);
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeForLoan:totalRepaymensts=" + totalRepaymensts);
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeForLoan:totalOutstanding=" + totalOutstanding);

		ServiceChargeFinalSheetData finalSheetData = (ServiceChargeFinalSheetData)appContext.getBean("serviceChargeFinalSheetData");
		return serviceChargeCalculationLogic(finalSheetData, isDisbursed, totalRepaymensts, totalOutstanding);
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
	private BigDecimal serviceChargeCalculationLogic(ServiceChargeFinalSheetData finalSheetData, boolean isDisbursed, BigDecimal totalRepaymensts, BigDecimal totalOutstanding) {
		QuarterDateRange quarter = QuarterDateRange.getPreviousQuarter();
		int year = Calendar.getInstance().get(Calendar.YEAR);
		if (QuarterDateRange.Q4.equals(quarter)) {
			year--;
		}
		Collection<ServiceChargeData> retrivedSCList = scChargeReadPlatformService.retrieveCharge(quarter, year);
		if (retrivedSCList == null || retrivedSCList.isEmpty()) {
			return calculateServiceChargeForCurrentQuarter(finalSheetData, isDisbursed, totalRepaymensts, totalOutstanding);
		}
		return calculateServiceChargeFromDBValues(isDisbursed, totalRepaymensts, totalOutstanding, retrivedSCList);
	}

	private BigDecimal calculateServiceChargeFromDBValues(boolean isDisbursed, BigDecimal totalRepaymensts, BigDecimal totalOutstanding,
			Collection<ServiceChargeData> retrivedSCList) {
		BigDecimal repaymentCostPerRupee = BigDecimal.ZERO;
		BigDecimal annualizedCost = BigDecimal.ZERO;
		BigDecimal serviceCostPerLoan = BigDecimal.ZERO;
		for (ServiceChargeData data : retrivedSCList) {
			switch (data.getHeader()) {
			case REPAYMENT_PER_100:
				repaymentCostPerRupee = data.getAmount();
				break;
			case ANNUALIZED_COST_I:
				annualizedCost = data.getAmount();
				break;
			case LOAN_SERVICING_PER_LOAN:
				serviceCostPerLoan = data.getAmount();
				break;
			}
		}

		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeFromDBValues: repaymentCostPerRupee="
				+ repaymentCostPerRupee.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeFromDBValues: annualizedCost="
				+ annualizedCost.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeFromDBValues: serviceCostPerLoan/disbursement="
				+ serviceCostPerLoan.toPlainString());

		BigDecimal serviceCharge = serviceCalculationLogic(isDisbursed, totalRepaymensts, totalOutstanding, repaymentCostPerRupee, annualizedCost,
				serviceCostPerLoan);

		return serviceCharge;
	}

	private BigDecimal calculateServiceChargeForCurrentQuarter(ServiceChargeFinalSheetData finalSheetData, boolean isDisbursed, BigDecimal totalRepaymensts, BigDecimal totalOutstanding) {
		BigDecimal repaymentCostPerRupee = finalSheetData.getColumnValue(ServiceChargeReportTableHeaders.REPAYMENT_PER_100, 0);
		BigDecimal annualizedCost = finalSheetData.getColumnValue(ServiceChargeReportTableHeaders.ANNUALIZED_COST_I, 0);
		BigDecimal serviceCostPerLoan = finalSheetData.getColumnValue(ServiceChargeReportTableHeaders.LOAN_SERVICING_PER_LOAN, 0);

		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceChargeCalculationLogic:repaymentCostPerRupee="
				+ repaymentCostPerRupee.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceChargeCalculationLogic:annualizedCost=" + annualizedCost.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceChargeCalculationLogic:serviceCostPerLoan/disbursement="
				+ serviceCostPerLoan.toPlainString());

		BigDecimal serviceCharge = serviceCalculationLogic(isDisbursed, totalRepaymensts, totalOutstanding, repaymentCostPerRupee, annualizedCost,
				serviceCostPerLoan);
		return serviceCharge;
	}

	private BigDecimal serviceCalculationLogic(boolean isDisbursed, BigDecimal totalRepaymensts, BigDecimal totalOutstanding,
			BigDecimal repaymentCostPerRupee, BigDecimal annualizedCost, BigDecimal serviceCostPerLoan) {
		final RoundingMode roundingMode = MoneyHelper.getRoundingMode();

		// Adding disbursement charge in case it was disbursed in the current quarter
		BigDecimal serviceCharge = isDisbursed ? serviceCostPerLoan : BigDecimal.ZERO;

		BigDecimal mobilization = totalOutstanding.multiply(repaymentCostPerRupee);
		mobilization = mobilization.divide(ServiceChargeApiConstants.ONE_THOUSAND_TWO_HUNDRED, roundingMode);

		BigDecimal repayment = totalRepaymensts.multiply(annualizedCost);
		repayment = repayment.divide(ServiceChargeApiConstants.HUNDRED, roundingMode);

		serviceCharge = serviceCharge.add(mobilization);
		serviceCharge = serviceCharge.add(repayment);

		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceCalculationLogic: mobilization=" + mobilization.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceCalculationLogic: repayment=" + repayment.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceCalculationLogic: serviceCharge=" + serviceCharge.toPlainString());
		return serviceCharge;
	}

	@Override
	public BigDecimal calculateServiceChargeForPrincipal(BigDecimal principal) {
		boolean isDisbursed = true; // Assuming that it will be disbursed
		BigDecimal totalRepaymensts = BigDecimal.ZERO; // Assuming that on loan-payout, there is no repayments yet
		BigDecimal totalOutstanding = principal; // The current amount being disbursed is the outstanding loan amount
		ServiceChargeFinalSheetData finalSheetData = (ServiceChargeFinalSheetData)appContext.getBean("serviceChargeFinalSheetData");
		return serviceChargeCalculationLogic(finalSheetData, isDisbursed, totalRepaymensts, totalOutstanding);
	}

}