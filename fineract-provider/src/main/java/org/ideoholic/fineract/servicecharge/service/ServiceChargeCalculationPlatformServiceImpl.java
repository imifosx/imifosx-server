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
package org.ideoholic.fineract.servicecharge.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Collection;

import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.ideoholic.fineract.servicecharge.constants.ServiceChargeApiConstants;
import org.ideoholic.fineract.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.ideoholic.fineract.servicecharge.data.ServiceChargeData;
import org.ideoholic.fineract.servicecharge.data.ServiceChargeFinalSheetData;
import org.ideoholic.fineract.util.ServiceChargeDateUtils;
import org.ideoholic.fineract.util.daterange.ServiceChargeDateRange;
import org.ideoholic.fineract.util.daterange.ServiceChargeDateRangeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeCalculationPlatformServiceImpl implements ServiceChargeCalculationPlatformService {

	private final static Logger logger = LoggerFactory.getLogger(ServiceChargeCalculationPlatformServiceImpl.class);

	private final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService;
	private final ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService;
	private final ServiceChargeReadPlatformService scChargeReadPlatformService;
	@Autowired
	private ApplicationContext appContext;

	@Autowired
	public ServiceChargeCalculationPlatformServiceImpl(
			final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService,
			final ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService,
			ServiceChargeReadPlatformService scChargeReadPlatformService) {
		this.scJournalDetailsReadPlatformService = scJournalDetailsReadPlatformService;
		this.scLoanDetailsReadPlatformService = scLoanDetailsReadPlatformService;
		this.scChargeReadPlatformService = scChargeReadPlatformService;
	}

	@Override
	public BigDecimal calculateServiceChargeForLoan(Long loanId) {
		ServiceChargeDateRange quarter = ServiceChargeDateRangeFactory.getCurrentDateRange();
		int year = Calendar.getInstance().get(Calendar.YEAR);

		return calculateServiceChargeForLoan(loanId, quarter, year);
	}

	@Override
	public BigDecimal calculateServiceChargeForLoan(Long loanId, ServiceChargeDateRange quarter, int year) {
		boolean isDisbursed = scLoanDetailsReadPlatformService.findIfLoanDisbursedInCurrentQuarter(loanId);
		BigDecimal totalRepaymensts = scLoanDetailsReadPlatformService.getTotalRepaymentsForCurrentQuarter(loanId);
		BigDecimal totalOutstanding = scLoanDetailsReadPlatformService
				.getTotalOutstandingAmountForCurrentQuarter(loanId);

		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeForLoan:isDisbursed="
				+ isDisbursed);
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeForLoan:totalRepaymensts="
				+ totalRepaymensts);
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeForLoan:totalOutstanding="
				+ totalOutstanding);

		return serviceChargeCalculationLogic(isDisbursed, totalRepaymensts, totalOutstanding, quarter, year);
	}

	private BigDecimal serviceChargeCalculationLogic(boolean isDisbursed, BigDecimal totalRepaymensts,
			BigDecimal totalOutstanding) {
		ServiceChargeDateRange quarter = ServiceChargeDateRangeFactory.getCurrentDateRange();
		int year = Calendar.getInstance().get(Calendar.YEAR);

		return serviceChargeCalculationLogic(isDisbursed, totalRepaymensts, totalOutstanding, quarter, year);
	}

	/**
	 * All the service charge calculation logic should be put here. The same method
	 * should be used for service charge calculation from other methods. This method
	 * is private so that the internal logic is kept insulated for any future
	 * changes. For accessing this method, need to write appropriate wrappers that
	 * will talk to the external world
	 * 
	 * @param isDisbursed
	 * @param totalRepaymensts
	 * @param totalOutstanding
	 * @param quarter
	 * @param year
	 * @return
	 */
	private BigDecimal serviceChargeCalculationLogic(boolean isDisbursed, BigDecimal totalRepaymensts,
			BigDecimal totalOutstanding, ServiceChargeDateRange quarter, int year) {
		Collection<ServiceChargeData> retrivedSCList = scChargeReadPlatformService.retrieveCharge(quarter, year);
		if (retrivedSCList == null || retrivedSCList.isEmpty()) {
			return calculateServiceChargeForGivenQuarter(isDisbursed, totalRepaymensts, totalOutstanding, quarter);
		}
		return calculateServiceChargeFromDBValues(isDisbursed, totalRepaymensts, totalOutstanding, retrivedSCList,
				quarter);
	}

	private BigDecimal calculateServiceChargeFromDBValues(boolean isDisbursed, BigDecimal totalRepaymensts,
			BigDecimal totalOutstanding, Collection<ServiceChargeData> retrivedSCList, ServiceChargeDateRange quarter) {
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
			default:
				logger.debug(
						"ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeFromDBValues: header being skipped="
								+ data.getHeader());
				break;
			}
		}

		logger.debug(
				"ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeFromDBValues: repaymentCostPerRupee="
						+ repaymentCostPerRupee.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeFromDBValues: annualizedCost="
				+ annualizedCost.toPlainString());
		logger.debug(
				"ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeFromDBValues: serviceCostPerLoan/disbursement="
						+ serviceCostPerLoan.toPlainString());

		BigDecimal serviceCharge = serviceCalculationLogic(isDisbursed, totalRepaymensts, totalOutstanding,
				repaymentCostPerRupee, annualizedCost, serviceCostPerLoan, quarter);

		return serviceCharge;
	}

	private BigDecimal calculateServiceChargeForGivenQuarter(boolean isDisbursed, BigDecimal totalRepaymensts,
			BigDecimal totalOutstanding, ServiceChargeDateRange quarter) {
		ServiceChargeFinalSheetData finalSheetData = (ServiceChargeFinalSheetData) appContext
				.getBean("serviceChargeFinalSheetData");
		scJournalDetailsReadPlatformService.generatefinalSheetData(finalSheetData);

		BigDecimal repaymentCostPerRupee = finalSheetData
				.getColumnValue(ServiceChargeReportTableHeaders.REPAYMENT_PER_100, 0);
		BigDecimal annualizedCost = finalSheetData.getColumnValue(ServiceChargeReportTableHeaders.ANNUALIZED_COST_I, 0);
		BigDecimal serviceCostPerLoan = finalSheetData
				.getColumnValue(ServiceChargeReportTableHeaders.LOAN_SERVICING_PER_LOAN, 0);

		logger.debug(
				"ServiceChargeCalculationPlatformServiceImpl:: calculateServiceChargeForCurrentQuarter: repaymentCostPerRupee="
						+ repaymentCostPerRupee.toPlainString());
		logger.debug(
				"ServiceChargeCalculationPlatformServiceImpl:: calculateServiceChargeForCurrentQuarter: annualizedCost="
						+ annualizedCost.toPlainString());
		logger.debug(
				"ServiceChargeCalculationPlatformServiceImpl:: calculateServiceChargeForCurrentQuarter: serviceCostPerLoan/disbursement="
						+ serviceCostPerLoan.toPlainString());

		BigDecimal serviceCharge = serviceCalculationLogic(isDisbursed, totalRepaymensts, totalOutstanding,
				repaymentCostPerRupee, annualizedCost, serviceCostPerLoan, quarter);
		return serviceCharge;
	}

	private BigDecimal serviceCalculationLogic(boolean isDisbursed, BigDecimal totalRepaymensts,
			BigDecimal totalOutstanding, BigDecimal repaymentCostPerRupee, BigDecimal annualizedCost,
			BigDecimal serviceCostPerLoan, ServiceChargeDateRange quarter) {
		final RoundingMode roundingMode = MoneyHelper.getRoundingMode();

		// Adding disbursement charge in case it was disbursed in the current quarter
		BigDecimal serviceCharge = isDisbursed ? serviceCostPerLoan : BigDecimal.ZERO;

		// For a daily calculation the mobilization cost would be:
		// ((outstanding * repayments cost per rupee) * number of days in quarter)/36500
		BigDecimal mobilization = totalOutstanding.multiply(repaymentCostPerRupee);
		int numOfDays = ServiceChargeDateUtils.getDiffBetweenDates(quarter.getFromDateForCurrentYear(),
				quarter.getToDateForCurrentYear(), 1);
		mobilization = mobilization.divide(new BigDecimal(numOfDays), roundingMode);
		mobilization = mobilization.divide(ServiceChargeApiConstants.THREE_SIXTY_FIVE_HUNDRED, roundingMode);

		BigDecimal repayment = totalRepaymensts.multiply(annualizedCost);
		repayment = repayment.divide(ServiceChargeApiConstants.HUNDRED, roundingMode);

		serviceCharge = serviceCharge.add(mobilization);
		serviceCharge = serviceCharge.add(repayment);

		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceCalculationLogic: mobilization="
				+ mobilization.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceCalculationLogic: repayment="
				+ repayment.toPlainString());
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::serviceCalculationLogic: serviceCharge="
				+ serviceCharge.toPlainString());
		return serviceCharge;
	}

	@Override
	public BigDecimal calculateServiceChargeForPrincipal(BigDecimal principal, Integer numberOfRepayments) {
		// Configured rounding mode to be used for division
		final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
		// Assuming that it will be disbursed
		boolean isDisbursed = true;
		// The current amount being disbursed is the outstanding loan amount
		BigDecimal totalOutstanding = principal;
		logger.debug("ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeForPrincipal: principal="
				+ principal.toPlainString());
		// QuarterDateRange.setQuarterAndYear("Q1", 2018);
		BigDecimal numberOfRepaymentsBigDecimal = new BigDecimal(numberOfRepayments);
		BigDecimal totalServiceChargeAmount = serviceChargeCalculationLogic(isDisbursed, BigDecimal.ZERO,
				totalOutstanding);
		logger.debug(
				"ServiceChargeCalculationPlatformServiceImpl::calculateServiceChargeForPrincipal: serviceChargeAmount="
						+ totalServiceChargeAmount.toPlainString());
		BigDecimal serviceChargeAmount = totalServiceChargeAmount.divide(numberOfRepaymentsBigDecimal, roundingMode);
		return serviceChargeAmount;
	}

}
