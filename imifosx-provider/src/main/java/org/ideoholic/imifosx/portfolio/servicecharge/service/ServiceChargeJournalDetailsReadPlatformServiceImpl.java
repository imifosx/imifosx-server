package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ideoholic.imifosx.accounting.glaccount.data.GLAccountData;
import org.ideoholic.imifosx.accounting.glaccount.domain.GLAccountType;
import org.ideoholic.imifosx.accounting.glaccount.service.GLAccountReadPlatformService;
import org.ideoholic.imifosx.accounting.journalentry.data.JournalEntryAssociationParametersData;
import org.ideoholic.imifosx.accounting.journalentry.data.JournalEntryData;
import org.ideoholic.imifosx.accounting.journalentry.domain.JournalEntryType;
import org.ideoholic.imifosx.accounting.journalentry.service.JournalEntryReadPlatformService;
import org.ideoholic.imifosx.infrastructure.core.service.Page;
import org.ideoholic.imifosx.infrastructure.core.service.SearchParameters;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.GLExpenseTagsForServiceCharge;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.QuarterDateRange;
import org.ideoholic.imifosx.portfolio.servicecharge.util.ServiceChargeOperationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeJournalDetailsReadPlatformServiceImpl implements ServiceChargeJournalDetailsReadPlatformService {

	private final static Logger logger = LoggerFactory.getLogger(ServiceChargeJournalDetailsReadPlatformServiceImpl.class);

	private final JournalEntryReadPlatformService journalEntryReadPlatformService;
	private final GLAccountReadPlatformService glAccountReadPlatformService;

	@Autowired
	public ServiceChargeJournalDetailsReadPlatformServiceImpl(JournalEntryReadPlatformService journalEntryReadPlatformService,
			GLAccountReadPlatformService glAccountReadPlatformService) {
		// Initialize the class level final autowired variables
		this.journalEntryReadPlatformService = journalEntryReadPlatformService;
		this.glAccountReadPlatformService = glAccountReadPlatformService;
	}

	@Override
	public Map<String, List<String>> readJournalEntriesForGivenQuarter() {
		List<GLAccountData> glAccountData = glAccountReadPlatformService.retrieveAllEnabledDetailGLAccounts(GLAccountType.EXPENSE);

		Map<GLExpenseTagsForServiceCharge, List<GLAccountData>> filteredGLAccountMap = new HashMap<>();
		for (GLExpenseTagsForServiceCharge serviceChargeTag : GLExpenseTagsForServiceCharge.values()) {
			filteredGLAccountMap.put(serviceChargeTag, new ArrayList<GLAccountData>());
		}
		for (GLAccountData glAccount : glAccountData) {
			if (glAccount.getTagId().getName().equals(GLExpenseTagsForServiceCharge.MOBILIZATION.getCode())) {
				List<GLAccountData> filteredGLAccountList = filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.MOBILIZATION);
				filteredGLAccountList.add(glAccount);
			} else if (glAccount.getTagId().getName().equals(GLExpenseTagsForServiceCharge.SERVICING.getCode())) {
				List<GLAccountData> filteredGLAccountList = filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.SERVICING);
				filteredGLAccountList.add(glAccount);
			} else if (glAccount.getTagId().getName().equals(GLExpenseTagsForServiceCharge.INVESTMENT.getCode())) {
				List<GLAccountData> filteredGLAccountList = filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.INVESTMENT);
				filteredGLAccountList.add(glAccount);
			} else if (glAccount.getTagId().getName().equals(GLExpenseTagsForServiceCharge.OVERHEADS.getCode())) {
				List<GLAccountData> filteredGLAccountList = filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.OVERHEADS);
				filteredGLAccountList.add(glAccount);
			} else if (glAccount.getTagId().getName().equals(GLExpenseTagsForServiceCharge.PROVISIONS.getCode())) {
				List<GLAccountData> filteredGLAccountList = filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.PROVISIONS);
				filteredGLAccountList.add(glAccount);
			}
		}

		BigDecimal totalMobilizationAmount = calculateTotalAmountForJournalEntriesOfGivenListOfGLs(filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.MOBILIZATION));
		BigDecimal totalServicingAmount = calculateTotalAmountForJournalEntriesOfGivenListOfGLs(filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.SERVICING));
		BigDecimal totalInvestmentAmount = calculateTotalAmountForJournalEntriesOfGivenListOfGLs(filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.INVESTMENT));
		BigDecimal totalOverHeadsAmount = calculateTotalAmountForJournalEntriesOfGivenListOfGLs(filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.OVERHEADS));
		BigDecimal totalProvisionsAmount = calculateTotalAmountForJournalEntriesOfGivenListOfGLs(filteredGLAccountMap.get(GLExpenseTagsForServiceCharge.PROVISIONS));
		Map<GLExpenseTagsForServiceCharge, BigDecimal> resultDataHolder = new HashMap<>();
		resultDataHolder.put(GLExpenseTagsForServiceCharge.MOBILIZATION, totalMobilizationAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.SERVICING, totalServicingAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.INVESTMENT, totalInvestmentAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.OVERHEADS, totalOverHeadsAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.PROVISIONS, totalProvisionsAmount);
		return generateFinalTableOfJournalEntries(resultDataHolder);
	}

	public Map<String, List<String>> generateFinalTableOfJournalEntries(Map<GLExpenseTagsForServiceCharge, BigDecimal> resultDataHolder) {
		Map<String, List<String>> result = new LinkedHashMap<>();
		Map<GLExpenseTagsForServiceCharge, BigDecimal> resultList = null;

		result = populateResultMapRow(result, 1, resultDataHolder);
		result = populateResultMapRow(result, 2, resultDataHolder);

		/*
		 * TODO: Check if rounding is necessary, and if necessary then what type
		 * of rounding needs to be done
		 */

		resultList = apportionOverHeads(resultDataHolder);
		result = populateResultMapRow(result, 3, resultList);
		
		resultDataHolder = addSingleRowToMapEntries(resultDataHolder, resultList);
		result = populateResultMapRow(result, 4, resultDataHolder);


		resultList = apportionMobilization(resultDataHolder);
		result = populateResultMapRow(result, 5, resultList);
		
		resultDataHolder = addSingleRowToMapEntries(resultDataHolder, resultList);
		result = populateResultMapRow(result, 6, resultDataHolder);

		return result;
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

		BigDecimal multiplicand = BigDecimal.ONE.multiply(dlAmount).divide(outstandingLoanAmount, RoundingMode.HALF_UP);

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
					case CREDIT:
						finalAmount = finalAmount.add(journalData.getAmount());
						break;
					case DEBIT:
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

	private Map<String, List<String>> populateResultMapRow(Map<String, List<String>> resultMap, int rowNum, Map<GLExpenseTagsForServiceCharge, BigDecimal> dataMap) {
		List<String> columnEntries = new ArrayList<>(5);
		BigDecimal totalAmount = BigDecimal.ZERO;

		BigDecimal mobilizationAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.MOBILIZATION);
		BigDecimal servicingAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.SERVICING);
		BigDecimal investmentAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.INVESTMENT);
		BigDecimal overHeadsAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.OVERHEADS);
		switch (rowNum) {
		case 1: // Populate the header of the table
			columnEntries.add("Mobilisation");
			columnEntries.add("Loan Servicing");
			columnEntries.add("Investment");
			columnEntries.add("Overheads");
			columnEntries.add("Total");
			resultMap.put("Expenses Allocation Categaories", columnEntries);
			break;
		case 2: // Populate total values fetched from the DB
			totalAmount = totalAmount.add(mobilizationAmount).add(servicingAmount).add(investmentAmount).add(overHeadsAmount);
			columnEntries.add(mobilizationAmount.toPlainString());
			columnEntries.add(servicingAmount.toPlainString());
			columnEntries.add(investmentAmount.toPlainString());
			columnEntries.add(overHeadsAmount.toPlainString());
			columnEntries.add(totalAmount.toPlainString());
			resultMap.put("Sub Total", columnEntries);
			break;
		case 3: // Populate overheads apportioned values
			totalAmount = totalAmount.add(mobilizationAmount).add(servicingAmount).add(investmentAmount);
			columnEntries.add(mobilizationAmount.toPlainString());
			columnEntries.add(servicingAmount.toPlainString());
			columnEntries.add(investmentAmount.toPlainString());
			columnEntries.add(StringUtils.EMPTY);
			columnEntries.add(totalAmount.toPlainString());
			resultMap.put("Allocation-I (Overheads)", columnEntries);
			break;
		case 4: // Populate overheads apportioned + original total values
			totalAmount = totalAmount.add(mobilizationAmount).add(servicingAmount).add(investmentAmount);
			columnEntries.add(mobilizationAmount.toPlainString());
			columnEntries.add(servicingAmount.toPlainString());
			columnEntries.add(investmentAmount.toPlainString());
			columnEntries.add(StringUtils.EMPTY);
			columnEntries.add(totalAmount.toPlainString());
			resultMap.put("Sub Total after Overheads Allocation", columnEntries);
			break;
		case 5: // Populate mobilization apportioned values
			totalAmount = totalAmount.add(servicingAmount).add(investmentAmount);
			columnEntries.add(StringUtils.EMPTY);
			columnEntries.add(servicingAmount.toPlainString());
			columnEntries.add(investmentAmount.toPlainString());
			columnEntries.add(StringUtils.EMPTY);
			columnEntries.add(totalAmount.toPlainString());
			resultMap.put("Allocation-II (Mobilization Cost)", columnEntries);
			break;
		case 6: // Populate mobilization apportioned + original total values
			totalAmount = totalAmount.add(servicingAmount).add(investmentAmount);
			columnEntries.add(StringUtils.EMPTY);
			columnEntries.add(servicingAmount.toPlainString());
			columnEntries.add(investmentAmount.toPlainString());
			columnEntries.add(StringUtils.EMPTY);
			columnEntries.add(totalAmount.toPlainString());
			resultMap.put("Total Activity-wise Segrigated Cost", columnEntries);
			break;
		}
		return resultMap;
	}

}
