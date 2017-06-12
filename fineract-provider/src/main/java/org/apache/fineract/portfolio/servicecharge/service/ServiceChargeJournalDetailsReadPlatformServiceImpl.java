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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.accounting.glaccount.domain.GLAccountType;
import org.apache.fineract.accounting.glaccount.service.GLAccountReadPlatformService;
import org.apache.fineract.accounting.journalentry.data.JournalEntryAssociationParametersData;
import org.apache.fineract.accounting.journalentry.data.JournalEntryData;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryType;
import org.apache.fineract.accounting.journalentry.service.JournalEntryReadPlatformService;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.portfolio.servicecharge.constants.GLExpenseTagsForServiceCharge;
import org.apache.fineract.portfolio.servicecharge.constants.QuarterDateRange;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeFinalSheetData;
import org.apache.fineract.portfolio.servicecharge.util.ServiceChargeOperationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeJournalDetailsReadPlatformServiceImpl implements ServiceChargeJournalDetailsReadPlatformService {

	private final static Logger logger = LoggerFactory.getLogger(ServiceChargeJournalDetailsReadPlatformServiceImpl.class);

	private final JournalEntryReadPlatformService journalEntryReadPlatformService;
	private final GLAccountReadPlatformService glAccountReadPlatformService;
	private final ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService;

	@Autowired
	public ServiceChargeJournalDetailsReadPlatformServiceImpl(JournalEntryReadPlatformService journalEntryReadPlatformService,
			GLAccountReadPlatformService glAccountReadPlatformService, ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService) {
		// Initialize the class level final autowired variables
		this.journalEntryReadPlatformService = journalEntryReadPlatformService;
		this.glAccountReadPlatformService = glAccountReadPlatformService;
		this.scLoanDetailsReadPlatformService = scLoanDetailsReadPlatformService;
	}
	
	public ServiceChargeFinalSheetData generatefinalSheetData() {
		ServiceChargeFinalSheetData finalSheetData = new ServiceChargeFinalSheetData();
		Map<GLExpenseTagsForServiceCharge, BigDecimal> resultDataHolder = generateExpenseTagsData();
		generateFinalTableOfJournalEntries(resultDataHolder, finalSheetData);
		computeFinalCalculations(finalSheetData);
		return finalSheetData;
	}
	
	private Map<GLExpenseTagsForServiceCharge, BigDecimal> generateExpenseTagsData(){
		Map<GLExpenseTagsForServiceCharge, BigDecimal> resultDataHolder = new HashMap<>();
		List<GLAccountData> glAccountData = glAccountReadPlatformService.retrieveAllEnabledDetailGLAccounts(GLAccountType.EXPENSE);

		Map<GLExpenseTagsForServiceCharge, List<GLAccountData>> filteredGLAccountMap = new HashMap<>();
		for (GLExpenseTagsForServiceCharge serviceChargeTag : GLExpenseTagsForServiceCharge.values()) {
			filteredGLAccountMap.put(serviceChargeTag, new ArrayList<GLAccountData>());
		}
		for (GLAccountData glAccount : glAccountData) {
			if (checkGLAccountDataCode(glAccount, GLExpenseTagsForServiceCharge.MOBILIZATION)) {
				List<GLAccountData> filteredGLAccountList = filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.MOBILIZATION);
				filteredGLAccountList.add(glAccount);
			} else if (checkGLAccountDataCode(glAccount, GLExpenseTagsForServiceCharge.SERVICING)) {
				List<GLAccountData> filteredGLAccountList = filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.SERVICING);
				filteredGLAccountList.add(glAccount);
			} else if (checkGLAccountDataCode(glAccount, GLExpenseTagsForServiceCharge.INVESTMENT)) {
				List<GLAccountData> filteredGLAccountList = filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.INVESTMENT);
				filteredGLAccountList.add(glAccount);
			} else if (checkGLAccountDataCode(glAccount, GLExpenseTagsForServiceCharge.OVERHEADS)) {
				List<GLAccountData> filteredGLAccountList = filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.OVERHEADS);
				filteredGLAccountList.add(glAccount);
			} else if (checkGLAccountDataCode(glAccount, GLExpenseTagsForServiceCharge.PROVISIONS)) {
				List<GLAccountData> filteredGLAccountList = filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.PROVISIONS);
				filteredGLAccountList.add(glAccount);
			} else if (checkGLAccountDataCode(glAccount, GLExpenseTagsForServiceCharge.BFSERVICING)) {
				List<GLAccountData> filteredGLAccountList = filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.BFSERVICING);
				filteredGLAccountList.add(glAccount);
			}
		}
		
		BigDecimal totalMobilizationAmount = calculateTotalAmountForJournalEntriesOfGivenListOfGLs(filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.MOBILIZATION));
		BigDecimal totalServicingAmount = calculateTotalAmountForJournalEntriesOfGivenListOfGLs(filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.SERVICING));
		BigDecimal totalInvestmentAmount = calculateTotalAmountForJournalEntriesOfGivenListOfGLs(filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.INVESTMENT));
		BigDecimal totalOverHeadsAmount = calculateTotalAmountForJournalEntriesOfGivenListOfGLs(filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.OVERHEADS));
		BigDecimal totalProvisionsAmount = calculateTotalAmountForJournalEntriesOfGivenListOfGLs(filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.PROVISIONS));
		BigDecimal totalBFServicingAmount = calculateTotalAmountForJournalEntriesOfGivenListOfGLs(filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.BFSERVICING));
		
		resultDataHolder.put(GLExpenseTagsForServiceCharge.MOBILIZATION, totalMobilizationAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.SERVICING, totalServicingAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.INVESTMENT, totalInvestmentAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.OVERHEADS, totalOverHeadsAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.PROVISIONS, totalProvisionsAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.BFSERVICING, totalBFServicingAmount);
		return resultDataHolder;
	}

	private boolean checkGLAccountDataCode(GLAccountData glAccount, GLExpenseTagsForServiceCharge expenseTag) {
		if (glAccount != null && glAccount.getTagId() != null && glAccount.getTagId().getName() != null) {
			return glAccount.getTagId().getName().equals(expenseTag.getCode());
		}
		return false;
	}

	public void computeFinalCalculations(ServiceChargeFinalSheetData sheetData) {
		BigDecimal mobilizationCostPercent, avgDLRePm, lsCostPa, lsCostPerLoan, totalNoDlLoans, reForPeriod, reCostPer100;
		try {
			List<BigDecimal> columnEntry;
			BigDecimal lsCostOnACBf = sheetData.getColumnValue(ServiceChargeReportTableHeaders.LSCOST_ON_ACCOUNT_BF, 0);

			columnEntry = new ArrayList<>(1);
			mobilizationCostPercent = sheetData.getColumnValue(ServiceChargeReportTableHeaders.ALLOCATION_MOBILIZATION, 1);
			mobilizationCostPercent = mobilizationCostPercent.multiply(new BigDecimal("4"));
			columnEntry.add(mobilizationCostPercent);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.TOTAL_MOBILIZATION, columnEntry);

			columnEntry = new ArrayList<>(1);
			avgDLRePm = scLoanDetailsReadPlatformService.getAllLoansRepaymentData();
			columnEntry.add(avgDLRePm);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.AVG_REPAYMENT, columnEntry);

			columnEntry = new ArrayList<>(1);
			mobilizationCostPercent = ServiceChargeOperationUtils.divideNonZeroValues(mobilizationCostPercent, avgDLRePm).multiply(HUNDRED);
			columnEntry.add(mobilizationCostPercent);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.MOBILIZATION_PERCENT, columnEntry);

			columnEntry = new ArrayList<>(1);
			lsCostPa = sheetData.getColumnValue(ServiceChargeReportTableHeaders.ALLOCATION_SUBTOTAL, 1);
			lsCostPa = lsCostPa.multiply(new BigDecimal("4"));
			columnEntry.add(lsCostPa);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.LOAN_SERVICING_PA, columnEntry);

			columnEntry = new ArrayList<>(1);
			totalNoDlLoans = scLoanDetailsReadPlatformService.getTotalLoansForCurrentQuarter();
			columnEntry.add(totalNoDlLoans);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.TOTAL_LOANS, columnEntry);

			columnEntry = new ArrayList<>(1);
			lsCostPerLoan = ServiceChargeOperationUtils.divideNonZeroValues(lsCostPa, totalNoDlLoans);
			columnEntry.add(lsCostPerLoan);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.LOAN_SERVICING_PER_LOAN, columnEntry);

			columnEntry = new ArrayList<>(1);
			reForPeriod = scLoanDetailsReadPlatformService.getAllLoansRepaymentData();
			columnEntry.add(reForPeriod);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.TOTAL_REPAYMENT, columnEntry);

			columnEntry = new ArrayList<>(1);
			reCostPer100 = ServiceChargeOperationUtils.divideNonZeroValues(lsCostOnACBf, reForPeriod);
			columnEntry.add(reCostPer100);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.REPAYMENT_PER_100, columnEntry);

			columnEntry = new ArrayList<>(1);
			columnEntry.add(mobilizationCostPercent);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.ANNUALIZED_COST_I, columnEntry);

			columnEntry = new ArrayList<>(1);
			BigDecimal eac2 = ServiceChargeOperationUtils.divideNonZeroValues(lsCostPa, avgDLRePm).multiply(HUNDRED);
			columnEntry.add(eac2);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.ANNUALIZED_COST_II, columnEntry);

			columnEntry = new ArrayList<>(1);
			BigDecimal eac3 = ServiceChargeOperationUtils.divideNonZeroValues(lsCostOnACBf, avgDLRePm).multiply(HUNDRED);
			columnEntry.add(eac3);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.ANNUALIZED_COST_III, columnEntry);

			columnEntry = new ArrayList<>(1);
			BigDecimal total = mobilizationCostPercent.add(eac2).add(eac3);
			columnEntry.add(total);
			sheetData.setColumnValue(ServiceChargeReportTableHeaders.ANNUALIZED_COST_TOTAL, columnEntry);

		} catch (Exception ex) {
			// Any exception means that input data is wrong so ignoring it
			logger.error(ex.getMessage(), ex);
		}
	}

	private ServiceChargeFinalSheetData generateFinalTableOfJournalEntries(Map<GLExpenseTagsForServiceCharge, BigDecimal> resultDataHolder,
			ServiceChargeFinalSheetData finalSheetData) {
		Map<GLExpenseTagsForServiceCharge, BigDecimal> resultList = null;

		populateResultMapRow(finalSheetData, 1, resultDataHolder);

		populateResultMapRow(finalSheetData, 2, resultDataHolder);

		/*
		 * TODO: Check if rounding is necessary, and if necessary then what type
		 * of rounding needs to be done
		 */

		resultList = apportionOverHeads(resultDataHolder);
		populateResultMapRow(finalSheetData, 3, resultList);

		resultDataHolder = addSingleRowToMapEntries(resultDataHolder, resultList);
		populateResultMapRow(finalSheetData, 4, resultDataHolder);

		resultList = apportionMobilization(resultDataHolder);
		populateResultMapRow(finalSheetData, 5, resultList);

		resultDataHolder = addSingleRowToMapEntries(resultDataHolder, resultList);
		populateResultMapRow(finalSheetData, 6, resultDataHolder);

		return finalSheetData;
	}

	private Map<GLExpenseTagsForServiceCharge, BigDecimal> apportionOverHeads(Map<GLExpenseTagsForServiceCharge, BigDecimal> dataMap) {
		Map<GLExpenseTagsForServiceCharge, BigDecimal> resultMap = new HashMap<>();
		BigDecimal mobilizationAmount = new BigDecimal(dataMap.get(GLExpenseTagsForServiceCharge.MOBILIZATION).toPlainString());
		BigDecimal servicingAmount = new BigDecimal(dataMap.get(GLExpenseTagsForServiceCharge.SERVICING).toPlainString());
		BigDecimal investmentAmount = new BigDecimal(dataMap.get(GLExpenseTagsForServiceCharge.INVESTMENT).toPlainString());
		BigDecimal overHeadsAmount = new BigDecimal(dataMap.get(GLExpenseTagsForServiceCharge.OVERHEADS).toPlainString());
		BigDecimal totalAmount = new BigDecimal(0);
		totalAmount = totalAmount.add(mobilizationAmount).add(servicingAmount).add(investmentAmount);

		mobilizationAmount = ServiceChargeOperationUtils.divideAndMultiplyNonZeroValues(mobilizationAmount, totalAmount, overHeadsAmount);
		servicingAmount = ServiceChargeOperationUtils.divideAndMultiplyNonZeroValues(servicingAmount, totalAmount, overHeadsAmount);
		investmentAmount = ServiceChargeOperationUtils.divideAndMultiplyNonZeroValues(investmentAmount, totalAmount, overHeadsAmount);

		resultMap.put(GLExpenseTagsForServiceCharge.MOBILIZATION, mobilizationAmount);
		resultMap.put(GLExpenseTagsForServiceCharge.SERVICING, servicingAmount);
		resultMap.put(GLExpenseTagsForServiceCharge.INVESTMENT, investmentAmount);

		return resultMap;
	}

	private Map<GLExpenseTagsForServiceCharge, BigDecimal> apportionMobilization(Map<GLExpenseTagsForServiceCharge, BigDecimal> dataMap) {
		Map<GLExpenseTagsForServiceCharge, BigDecimal> resultMap = new HashMap<>();
		BigDecimal mobilizationAmount = new BigDecimal(dataMap.get(GLExpenseTagsForServiceCharge.MOBILIZATION).toPlainString());
		BigDecimal servicingAmount = new BigDecimal(dataMap.get(GLExpenseTagsForServiceCharge.SERVICING).toPlainString());
		BigDecimal investmentAmount = new BigDecimal(dataMap.get(GLExpenseTagsForServiceCharge.INVESTMENT).toPlainString());
		BigDecimal dlAmount = BigDecimal.ONE;
		BigDecimal outstandingLoanAmount = BigDecimal.ONE;

		BigDecimal multiplicand = BigDecimal.ONE.multiply(dlAmount);
		multiplicand = ServiceChargeOperationUtils.divideNonZeroValues(multiplicand, outstandingLoanAmount);

		servicingAmount = mobilizationAmount.multiply(multiplicand);
		investmentAmount = mobilizationAmount.subtract(servicingAmount);

		resultMap.put(GLExpenseTagsForServiceCharge.SERVICING, servicingAmount);
		resultMap.put(GLExpenseTagsForServiceCharge.INVESTMENT, investmentAmount);

		return resultMap;
	}

	private BigDecimal calculateTotalAmountForJournalEntriesOfGivenListOfGLs(List<GLAccountData> glAccountData) {
		BigDecimal finalAmount = new BigDecimal(0);

		if (glAccountData != null && !glAccountData.isEmpty()) {
			for (GLAccountData glAccount : glAccountData) {
				// Start from 0 entries and get all entries by passing limit=-1
				final SearchParameters searchParameters = SearchParameters.forJournalEntries(null, 0, -1, null, null, null, null);
				final JournalEntryAssociationParametersData associationParametersData = new JournalEntryAssociationParametersData(false, false);

				final Date fromDateParam = QuarterDateRange.getCurrentQuarter().getFromDateForCurrentYear();
				final Date toDateParam = QuarterDateRange.getCurrentQuarter().getToDateForCurrentYear();

				Page<JournalEntryData> journalEntryDataPage = journalEntryReadPlatformService.retrieveAll(searchParameters, glAccount.getId(), true, fromDateParam, toDateParam,
						StringUtils.EMPTY, 0, associationParametersData);
				for (JournalEntryData journalData : journalEntryDataPage.getPageItems()) {
					JournalEntryType entryType = JournalEntryType.fromInt(journalData.getEntryType().getId().intValue());
					switch (entryType) {
					case DEBIT:
						finalAmount = finalAmount.add(journalData.getAmount());
						break;
					case CREDIT:
						finalAmount = finalAmount.subtract(journalData.getAmount());
						break;
					}
				}
			} // End of for-loop
		} // End of outer if-condition
		return finalAmount;
	}

	private Map<GLExpenseTagsForServiceCharge, BigDecimal> addSingleRowToMapEntries(Map<GLExpenseTagsForServiceCharge, BigDecimal> fullDataMap,
			Map<GLExpenseTagsForServiceCharge, BigDecimal> singleRow) {
		BigDecimal totalMobilizationAmount = readValueFromMap(fullDataMap, GLExpenseTagsForServiceCharge.MOBILIZATION);
		BigDecimal totalServicingAmount = readValueFromMap(fullDataMap, GLExpenseTagsForServiceCharge.SERVICING);
		BigDecimal totalInvestmentAmount = readValueFromMap(fullDataMap, GLExpenseTagsForServiceCharge.INVESTMENT);
		BigDecimal totalOverHeadsAmount = readValueFromMap(fullDataMap, GLExpenseTagsForServiceCharge.OVERHEADS);

		totalMobilizationAmount = totalMobilizationAmount.add(readValueFromMap(singleRow, GLExpenseTagsForServiceCharge.MOBILIZATION));
		fullDataMap.put(GLExpenseTagsForServiceCharge.MOBILIZATION, totalMobilizationAmount);

		totalServicingAmount = totalServicingAmount.add(readValueFromMap(singleRow, GLExpenseTagsForServiceCharge.SERVICING));
		fullDataMap.put(GLExpenseTagsForServiceCharge.SERVICING, totalServicingAmount);

		totalInvestmentAmount = totalInvestmentAmount.add(readValueFromMap(singleRow, GLExpenseTagsForServiceCharge.INVESTMENT));
		fullDataMap.put(GLExpenseTagsForServiceCharge.INVESTMENT, totalInvestmentAmount);

		totalOverHeadsAmount = totalInvestmentAmount.add(readValueFromMap(singleRow, GLExpenseTagsForServiceCharge.INVESTMENT));
		fullDataMap.put(GLExpenseTagsForServiceCharge.INVESTMENT, totalOverHeadsAmount);

		return fullDataMap;
	}

	private BigDecimal readValueFromMap(Map<GLExpenseTagsForServiceCharge, BigDecimal> dataMap, GLExpenseTagsForServiceCharge key) {
		BigDecimal value = dataMap.get(key);
		if (value == null) {
			return BigDecimal.ZERO;
		}
		return value;
	}

	private void populateResultMapRow(ServiceChargeFinalSheetData finalSheetData, int rowNum, Map<GLExpenseTagsForServiceCharge, BigDecimal> dataMap) {
		List<BigDecimal> columnEntries = new ArrayList<>(5);
		BigDecimal totalAmount = BigDecimal.ZERO;

		BigDecimal mobilizationAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.MOBILIZATION);
		BigDecimal servicingAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.SERVICING);
		BigDecimal investmentAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.INVESTMENT);
		BigDecimal overHeadsAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.OVERHEADS);
		BigDecimal bfServicing = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.BFSERVICING);
		switch (rowNum) {
		case 1:
			List<BigDecimal> bfColumnEntries = new ArrayList<>(1);
			bfColumnEntries.add(bfServicing);
			finalSheetData.setColumnValue(ServiceChargeReportTableHeaders.LSCOST_ON_ACCOUNT_BF, bfColumnEntries);
			break;
		case 2: // Populate total values fetched from the DB
			totalAmount = totalAmount.add(mobilizationAmount).add(servicingAmount).add(investmentAmount).add(overHeadsAmount);
			columnEntries.add(mobilizationAmount);
			columnEntries.add(servicingAmount);
			columnEntries.add(investmentAmount);
			columnEntries.add(overHeadsAmount);
			columnEntries.add(totalAmount);
			finalSheetData.setColumnValue(ServiceChargeReportTableHeaders.SUBTOTAL, columnEntries);
			break;
		case 3: // Populate overheads apportioned values
			totalAmount = totalAmount.add(mobilizationAmount).add(servicingAmount).add(investmentAmount);
			columnEntries.add(mobilizationAmount);
			columnEntries.add(servicingAmount);
			columnEntries.add(investmentAmount);
			columnEntries.add(null);
			finalSheetData.setColumnValue(ServiceChargeReportTableHeaders.ALLOCATION_OVERHEADS, columnEntries);
			break;
		case 4: // Populate overheads apportioned + original total values
			totalAmount = totalAmount.add(mobilizationAmount).add(servicingAmount).add(investmentAmount);
			columnEntries.add(mobilizationAmount);
			columnEntries.add(servicingAmount);
			columnEntries.add(investmentAmount);
			columnEntries.add(null);
			columnEntries.add(totalAmount);
			finalSheetData.setColumnValue(ServiceChargeReportTableHeaders.ALLOCATION_SUBTOTAL, columnEntries);
			break;
		case 5: // Populate mobilization apportioned values
			totalAmount = totalAmount.add(servicingAmount).add(investmentAmount);
			columnEntries.add(null);
			columnEntries.add(servicingAmount);
			columnEntries.add(investmentAmount);
			columnEntries.add(null);
			columnEntries.add(totalAmount);
			finalSheetData.setColumnValue(ServiceChargeReportTableHeaders.ALLOCATION_MOBILIZATION, columnEntries);
			break;
		case 6: // Populate mobilization apportioned + original total values
			totalAmount = totalAmount.add(servicingAmount).add(investmentAmount);
			columnEntries.add(null);
			columnEntries.add(servicingAmount);
			columnEntries.add(investmentAmount);
			columnEntries.add(null);
			columnEntries.add(totalAmount);
			finalSheetData.setColumnValue(ServiceChargeReportTableHeaders.TOTAL_SEGREGATION_COST, columnEntries);
			break;
		}
	}

}
