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
	private final ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService;
	private final BigDecimal lsCostOnACBf = HUNDRED;

	@Autowired
	public ServiceChargeJournalDetailsReadPlatformServiceImpl(JournalEntryReadPlatformService journalEntryReadPlatformService,
			GLAccountReadPlatformService glAccountReadPlatformService, ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService) {
		// Initialize the class level final autowired variables
		this.journalEntryReadPlatformService = journalEntryReadPlatformService;
		this.glAccountReadPlatformService = glAccountReadPlatformService;
		this.scLoanDetailsReadPlatformService = scLoanDetailsReadPlatformService;
	}

	@Override
	public Map<String, List<BigDecimal>> readJournalEntriesForGivenQuarter() {
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
		Map<GLExpenseTagsForServiceCharge, BigDecimal> resultDataHolder = new HashMap<>();
		resultDataHolder.put(GLExpenseTagsForServiceCharge.MOBILIZATION, totalMobilizationAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.SERVICING, totalServicingAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.INVESTMENT, totalInvestmentAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.OVERHEADS, totalOverHeadsAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.PROVISIONS, totalProvisionsAmount);
		resultDataHolder.put(GLExpenseTagsForServiceCharge.BFSERVICING, totalBFServicingAmount);
		return generateFinalTableOfJournalEntries(resultDataHolder);
	}

	private boolean checkGLAccountDataCode(GLAccountData glAccount, GLExpenseTagsForServiceCharge expenseTag) {
		if (glAccount != null && glAccount.getTagId() != null && glAccount.getTagId().getName() != null) {
			return glAccount.getTagId().getName().equals(expenseTag.getCode());
		}
		return false;
	}

	@Override
	public Map<String, List<BigDecimal>> computeFinalCalculations(Map<String, List<BigDecimal>> journalEntriest) {
		Map<String, List<BigDecimal>> result = new LinkedHashMap<>();
		BigDecimal mobilizationCostPercent, avgDLRePm, lsCostPa, lsCostPerLoan, totalNoDlLoans, reForPeriod, reCostPer100;
		try {
			List<BigDecimal> columnEntry = new ArrayList<>(1);
			columnEntry.add(lsCostOnACBf);
			// TODO: Need to store and read this value
			result.put("LS Cost on A/c BF", columnEntry);

			columnEntry = new ArrayList<>(1);
			mobilizationCostPercent = journalEntriest.get(row4Header).get(1);
			mobilizationCostPercent = mobilizationCostPercent.multiply(new BigDecimal("4"));
			columnEntry.add(mobilizationCostPercent);
			result.put("Total Mobilisation Cost p.a.", columnEntry);

			columnEntry = new ArrayList<>(1);
			avgDLRePm = HUNDRED;
			columnEntry.add(avgDLRePm);
			result.put("Average OS DL Re.Months", columnEntry);

			columnEntry = new ArrayList<>(1);
			mobilizationCostPercent = mobilizationCostPercent.divide(avgDLRePm, RoundingMode.HALF_UP).multiply(HUNDRED);
			columnEntry.add(mobilizationCostPercent);
			result.put("Mobilisation Cost (%)", columnEntry);

			columnEntry = new ArrayList<>(1);
			lsCostPa = journalEntriest.get(row3Header).get(1);
			lsCostPa = lsCostPa.multiply(new BigDecimal("4"));
			columnEntry.add(lsCostPa);
			result.put("Loan Servicing Cost p.a.", columnEntry);

			columnEntry = new ArrayList<>(1);
			totalNoDlLoans = scLoanDetailsReadPlatformService.getTotalLoansForCurrentQuarter();
			columnEntry.add(totalNoDlLoans);
			result.put("Total No.of DL Loans for the Period", columnEntry);

			columnEntry = new ArrayList<>(1);
			lsCostPerLoan = lsCostPa.divide(totalNoDlLoans, RoundingMode.HALF_UP);
			columnEntry.add(lsCostPerLoan);
			result.put("Loan Servicing Cost per Loan", columnEntry);

			columnEntry = new ArrayList<>(1);
			columnEntry.add(lsCostOnACBf);
			result.put("LS Cost on A/c BF", columnEntry);

			columnEntry = new ArrayList<>(1);
			reForPeriod = scLoanDetailsReadPlatformService.getAllLoansRepaymentData();
			columnEntry.add(reForPeriod);
			result.put("Total Repyament for the Period", columnEntry);

			columnEntry = new ArrayList<>(1);
			reCostPer100 = lsCostOnACBf.divide(reForPeriod, RoundingMode.HALF_UP);
			columnEntry.add(reCostPer100);
			result.put("Repayment Cost per 100 Rupee of Repayment", columnEntry);

			columnEntry = new ArrayList<>(1);
			columnEntry.add(mobilizationCostPercent);
			result.put("Equivalent Annualized Cost (%) - I", columnEntry);

			columnEntry = new ArrayList<>(1);
			BigDecimal eac2 = lsCostPa.divide(avgDLRePm, RoundingMode.HALF_UP).multiply(HUNDRED);
			columnEntry.add(eac2);
			result.put("Equivalent Annualized Cost (%) - II", columnEntry);

			columnEntry = new ArrayList<>(1);
			BigDecimal eac3 = lsCostOnACBf.divide(avgDLRePm, RoundingMode.HALF_UP).multiply(HUNDRED);
			columnEntry.add(eac3);
			result.put("Equivalent Annualized Cost (%) - III", columnEntry);

			columnEntry = new ArrayList<>(1);
			BigDecimal total = mobilizationCostPercent.add(eac2).add(eac3);
			columnEntry.add(total);
			result.put("Equivalent Annualized Cost (%) - Total", columnEntry);

		} catch (Exception ex) {
			// Any exception means that input data is wrong so ignoring it
			logger.error(ex.getMessage(), ex);
		}
		return result;
	}

	private Map<String, List<BigDecimal>> generateFinalTableOfJournalEntries(Map<GLExpenseTagsForServiceCharge, BigDecimal> resultDataHolder) {
		Map<String, List<BigDecimal>> result = new LinkedHashMap<>();
		Map<GLExpenseTagsForServiceCharge, BigDecimal> resultList = null;

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

		BigDecimal multiplicand = BigDecimal.ONE.multiply(dlAmount);
		if (!outstandingLoanAmount.equals(BigDecimal.ZERO)) {
			multiplicand = multiplicand.divide(outstandingLoanAmount, RoundingMode.HALF_UP);
		}

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

	private String row1Header = "Sub Total";
	private String row2Header = "Allocation-I (Overheads)";
	private String row3Header = "Sub Total after Overheads Allocation";
	private String row4Header = "Allocation-II (Mobilization Cost)";
	private String row5Header = "Total Activity-wise Segrigated Cost";

	private Map<String, List<BigDecimal>> populateResultMapRow(Map<String, List<BigDecimal>> resultMap, int rowNum, Map<GLExpenseTagsForServiceCharge, BigDecimal> dataMap) {
		List<BigDecimal> columnEntries = new ArrayList<>(5);
		BigDecimal totalAmount = BigDecimal.ZERO;

		BigDecimal mobilizationAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.MOBILIZATION);
		BigDecimal servicingAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.SERVICING);
		BigDecimal investmentAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.INVESTMENT);
		BigDecimal overHeadsAmount = readValueFromMap(dataMap, GLExpenseTagsForServiceCharge.OVERHEADS);
		switch (rowNum) {
		case 2: // Populate total values fetched from the DB
			totalAmount = totalAmount.add(mobilizationAmount).add(servicingAmount).add(investmentAmount).add(overHeadsAmount);
			columnEntries.add(mobilizationAmount);
			columnEntries.add(servicingAmount);
			columnEntries.add(investmentAmount);
			columnEntries.add(overHeadsAmount);
			columnEntries.add(totalAmount);
			resultMap.put(row1Header, columnEntries);
			break;
		case 3: // Populate overheads apportioned values
			totalAmount = totalAmount.add(mobilizationAmount).add(servicingAmount).add(investmentAmount);
			columnEntries.add(mobilizationAmount);
			columnEntries.add(servicingAmount);
			columnEntries.add(investmentAmount);
			columnEntries.add(null);
			columnEntries.add(totalAmount);
			resultMap.put(row2Header, columnEntries);
			break;
		case 4: // Populate overheads apportioned + original total values
			totalAmount = totalAmount.add(mobilizationAmount).add(servicingAmount).add(investmentAmount);
			columnEntries.add(mobilizationAmount);
			columnEntries.add(servicingAmount);
			columnEntries.add(investmentAmount);
			columnEntries.add(null);
			columnEntries.add(totalAmount);
			resultMap.put(row3Header, columnEntries);
			break;
		case 5: // Populate mobilization apportioned values
			totalAmount = totalAmount.add(servicingAmount).add(investmentAmount);
			columnEntries.add(null);
			columnEntries.add(servicingAmount);
			columnEntries.add(investmentAmount);
			columnEntries.add(null);
			columnEntries.add(totalAmount);
			resultMap.put(row4Header, columnEntries);
			break;
		case 6: // Populate mobilization apportioned + original total values
			totalAmount = totalAmount.add(servicingAmount).add(investmentAmount);
			columnEntries.add(null);
			columnEntries.add(servicingAmount);
			columnEntries.add(investmentAmount);
			columnEntries.add(null);
			columnEntries.add(totalAmount);
			resultMap.put(row5Header, columnEntries);
			break;
		}
		return resultMap;
	}

}
