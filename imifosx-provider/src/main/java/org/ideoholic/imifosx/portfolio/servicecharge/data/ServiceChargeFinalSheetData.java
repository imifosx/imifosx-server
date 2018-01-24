package org.ideoholic.imifosx.portfolio.servicecharge.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.ideoholic.imifosx.portfolio.servicecharge.exception.ServiceChargeNotFoundException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServiceChargeFinalSheetData {
	private Map<ServiceChargeReportTableHeaders, List<BigDecimal>> resultsDataMap;
	private StringBuffer resultDataAsHTMLTableString;

	private BigDecimal totalMobilization;
	private BigDecimal totalServicing;
	private BigDecimal totalInvestment;
	private BigDecimal totalOverHeads;
	private BigDecimal totalProvisions;
	private BigDecimal totalBFServicing;

	// Overheads apportioned values
	private BigDecimal overheadsApportionedMobilization;
	private BigDecimal overheadsApportionedServicing;
	private BigDecimal overheadsApportionedlInvestment;
	
	// Sub Total after Overheads Allocation
	private BigDecimal subTotalAfterOverheadsAllocationMobilization;
	private BigDecimal subTotalAfterOverheadsAllocationServicing;
	private BigDecimal subTotalAfterOverheadsAllocationInvestment;

	// Mobilization apportioned values
	private BigDecimal mobilizationApportionedServicing;
	private BigDecimal mobilizationApportionedlInvestment;
	
	// DL & NDL loan outstanding amount
	private BigDecimal dlOutstandingAmount;
	private BigDecimal nDlOutstandingAmount;
	

	private void init() {
		resultsDataMap = new HashMap<ServiceChargeReportTableHeaders, List<BigDecimal>>();
		resultDataAsHTMLTableString = new StringBuffer();
		for (ServiceChargeReportTableHeaders header : ServiceChargeReportTableHeaders.values()) {
			resultsDataMap.put(header, new ArrayList<BigDecimal>());
		}
	}

	public ServiceChargeFinalSheetData() {
		init();
	}

	public BigDecimal getTotalMobilization() {
		return totalMobilization;
	}

	public BigDecimal getTotalServicing() {
		return totalServicing;
	}

	public BigDecimal getTotalInvestment() {
		return totalInvestment;
	}

	public BigDecimal getTotalOverHeads() {
		return totalOverHeads;
	}

	public BigDecimal getTotalProvisions() {
		return totalProvisions;
	}

	public BigDecimal getTotalBFServicing() {
		return totalBFServicing;
	}

	public BigDecimal getOverheadsApportionedMobilization() {
		return overheadsApportionedMobilization;
	}

	public BigDecimal getOverheadsApportionedServicing() {
		return overheadsApportionedServicing;
	}

	public BigDecimal getOverheadsApportionedlInvestment() {
		return overheadsApportionedlInvestment;
	}

	public BigDecimal getSubTotalAfterOverheadsAllocationMobilization() {
		return subTotalAfterOverheadsAllocationMobilization;
	}

	public BigDecimal getSubTotalAfterOverheadsAllocationServicing() {
		return subTotalAfterOverheadsAllocationServicing;
	}

	public BigDecimal getSubTotalAfterOverheadsAllocationInvestment() {
		return subTotalAfterOverheadsAllocationInvestment;
	}

	public BigDecimal getMobilizationApportionedServicing() {
		return mobilizationApportionedServicing;
	}

	public BigDecimal getMobilizationApportionedlInvestment() {
		return mobilizationApportionedlInvestment;
	}

	public Map<ServiceChargeReportTableHeaders, List<BigDecimal>> getResultsDataMap() {
		return resultsDataMap;
	}

	public StringBuffer getResultDataAsHTMLTableString() {
		return resultDataAsHTMLTableString;
	}

	public BigDecimal getDlOutstandingAmount() {
		return dlOutstandingAmount;
	}

	public void setDlOutstandingAmount(BigDecimal dloutstandingAmount) {
		dlOutstandingAmount = dloutstandingAmount;
	}

	public BigDecimal getNDlOutstandingAmount() {
		return nDlOutstandingAmount;
	}

	public void setNDlOutstandingAmount(BigDecimal ndloutstandingAmount) {
		nDlOutstandingAmount = ndloutstandingAmount;
	}

	public void setColumnValue(ServiceChargeReportTableHeaders header, List<BigDecimal> dataList) {
		getResultsDataMap().put(header, dataList);
	}

	public BigDecimal getColumnValue(ServiceChargeReportTableHeaders header, int columnNumber) {
		List<BigDecimal> dataList = getResultsDataMap().get(header);
		if (columnNumber >= dataList.size()) {
			throw new ServiceChargeNotFoundException(header.getValue().longValue());
		}
		return dataList.get(columnNumber);
	}

	public String generateResultAsHTMLTable(boolean forceRecreate) {
		StringBuffer result = getResultDataAsHTMLTableString();
		if (result.length() == 0 || forceRecreate) {
			generateInitialCalculationSheet();
			generateFinalCalculationSheet();
		}
		return result.toString();
	}

	private void generateInitialCalculationSheet() {
		generateTableHeader(1);

		// First five headers goes in the first table
		populateTableRowsWithEntriesinResultsMap(1, 5, true);
	}

	private void generateFinalCalculationSheet() {
		generateTableHeader(2);

		// 6 to 12 headers to be populated first
		populateTableRowsWithEntriesinResultsMap(6, 12, false);
		// One row of 6 to be populated once again
		populateTableRowsWithEntriesinResultsMap(6, 6, false);
		// Continue from 13 till the end
		populateTableRowsWithEntriesinResultsMap(13, 18, true);
	}

	private void populateTableRowsWithEntriesinResultsMap(int start, int end, boolean endTable) {
		StringBuffer result = getResultDataAsHTMLTableString();
		for (int iCounter = start; iCounter <= end; iCounter++) {
			ServiceChargeReportTableHeaders header = ServiceChargeReportTableHeaders.fromInt(iCounter);
			List<BigDecimal> dataList = resultsDataMap.get(header);
			if (dataList != null) {
				result.append("<tr>");
				result.append("<td>");
				result.append(header.getCode());
				result.append("</td>");
				for (BigDecimal element : dataList) {
					result.append("<td>");
					if (element != null)
						result.append(element.toPlainString());
					else
						result.append(StringUtils.EMPTY);
					result.append("</td>");
				}
				result.append("</tr>");
			}
		}
		if (endTable) {
			result.append("</table>");
		}
	}

	private StringBuffer generateTableHeader(int tableNumber) {
		StringBuffer sb = getResultDataAsHTMLTableString();
		if (tableNumber == 1) {
			sb.append("<table table style=\"width:100%\" border=5pt>");
			sb.append("<tr>");
			sb.append("<td>");
			sb.append("Expenses Allocation Categaories");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Mobilisation");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Loan Servicing");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Investment");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Overheads");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Total");
			sb.append("</td>");
			sb.append("</tr>");
		} else if (tableNumber == 2) {
			sb.append("<table table style=\"width:100%\" border=5pt>");
			sb.append("<tr>");
			sb.append("<td>");
			sb.append("Particulars");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Value");
			sb.append("</td>");
			sb.append("</tr>");
		}
		return sb;
	}
	
	private void setBFAmountRow() {
		List<BigDecimal> bfColumnEntries = new ArrayList<>(1);
		bfColumnEntries.add(totalBFServicing);
		setColumnValue(ServiceChargeReportTableHeaders.LSCOST_ON_ACCOUNT_BF, bfColumnEntries);
	}
	
	private void setSubTotalRow() {
		List<BigDecimal> columnEntries = new ArrayList<>(5);
		BigDecimal totalAmount = BigDecimal.ZERO;
		totalAmount = totalAmount.add(totalMobilization).add(totalServicing).add(totalInvestment).add(totalOverHeads);
		columnEntries.add(totalMobilization);
		columnEntries.add(totalServicing);
		columnEntries.add(totalInvestment);
		columnEntries.add(totalOverHeads);
		columnEntries.add(totalAmount);
		setColumnValue(ServiceChargeReportTableHeaders.SUBTOTAL, columnEntries);
	}
	
	private void setOverheadsApportionedRow() {
		List<BigDecimal> columnEntries = new ArrayList<>(5);
		BigDecimal totalAmount = BigDecimal.ZERO;
		// Populate overheads apportioned values
		totalAmount = totalAmount.add(overheadsApportionedMobilization).add(overheadsApportionedServicing)
				.add(overheadsApportionedlInvestment);
		columnEntries.add(overheadsApportionedMobilization);
		columnEntries.add(overheadsApportionedServicing);
		columnEntries.add(overheadsApportionedlInvestment);
		columnEntries.add(null);
		columnEntries.add(totalAmount);
		setColumnValue(ServiceChargeReportTableHeaders.ALLOCATION_OVERHEADS, columnEntries);
		subTotalAfterOverheadsAllocationMobilization = totalMobilization.add(overheadsApportionedMobilization);
		subTotalAfterOverheadsAllocationServicing = totalServicing.add(overheadsApportionedServicing);
		subTotalAfterOverheadsAllocationInvestment = totalInvestment.add(overheadsApportionedlInvestment);
	}
	
	private void setTotalAfterOverheadsApportionedRow() {
		List<BigDecimal> columnEntries = new ArrayList<>(5);
		BigDecimal totalAmount = BigDecimal.ZERO;

		this.totalMobilization = this.totalMobilization.add(overheadsApportionedMobilization);
		this.totalServicing = this.totalServicing.add(overheadsApportionedServicing);
		this.totalInvestment = this.totalInvestment.add(overheadsApportionedlInvestment);

		// Populate overheads apportioned + original total values
		totalAmount = totalAmount.add(totalMobilization).add(totalServicing).add(totalInvestment);
		columnEntries.add(totalMobilization);
		columnEntries.add(totalServicing);
		columnEntries.add(totalInvestment);
		columnEntries.add(null);
		columnEntries.add(totalAmount);
		setColumnValue(ServiceChargeReportTableHeaders.ALLOCATION_SUBTOTAL, columnEntries);

	}
	
	private void setMobilizationApportionedRow() {
		List<BigDecimal> columnEntries = new ArrayList<>(5);
		BigDecimal totalAmount = BigDecimal.ZERO;
		// Populate mobilization apportioned values
		totalAmount = totalAmount.add(mobilizationApportionedServicing).add(mobilizationApportionedlInvestment);
		columnEntries.add(null);
		columnEntries.add(mobilizationApportionedServicing);
		columnEntries.add(mobilizationApportionedlInvestment);
		columnEntries.add(null);
		columnEntries.add(totalAmount);
		setColumnValue(ServiceChargeReportTableHeaders.ALLOCATION_MOBILIZATION, columnEntries);
	}
	
	private void setTotalAfterMobilizationApportionedRow() {
		List<BigDecimal> columnEntries = new ArrayList<>(5);
		BigDecimal totalAmount = BigDecimal.ZERO;

		this.totalServicing = this.totalServicing.add(overheadsApportionedServicing);
		this.totalInvestment = this.totalInvestment.add(overheadsApportionedlInvestment);

		// Populate mobilization apportioned + original total values
		totalAmount = totalAmount.add(totalServicing).add(totalInvestment);
		columnEntries.add(null);
		columnEntries.add(totalServicing);
		columnEntries.add(totalInvestment);
		columnEntries.add(null);
		columnEntries.add(totalAmount);
		setColumnValue(ServiceChargeReportTableHeaders.TOTAL_SEGREGATION_COST, columnEntries);

	}

	public void setJounEntriesData(BigDecimal totalMobilizationAmount, BigDecimal totalServicingAmount,
			BigDecimal totalInvestmentAmount, BigDecimal totalOverHeadsAmount, BigDecimal totalProvisionsAmount,
			BigDecimal totalBFServicingAmount) {
		this.totalMobilization = totalMobilizationAmount;
		this.totalServicing = totalServicingAmount;
		this.totalInvestment = totalInvestmentAmount;
		this.totalOverHeads = totalOverHeadsAmount;
		this.totalProvisions = totalProvisionsAmount;
		this.totalBFServicing = totalBFServicingAmount;

		setBFAmountRow();
		setSubTotalRow();
	}

	public void setOverheadsApportionedValues(BigDecimal mobilizationAmount, BigDecimal servicingAmount,
			BigDecimal investmentAmount) {
		overheadsApportionedMobilization = mobilizationAmount;
		overheadsApportionedlInvestment = investmentAmount;
		overheadsApportionedServicing = servicingAmount;
		setOverheadsApportionedRow();
		setTotalAfterOverheadsApportionedRow();
	}

	public void setMobilizationApportionedValues(BigDecimal servicingAmount, BigDecimal investmentAmount) {
		mobilizationApportionedServicing = servicingAmount;
		mobilizationApportionedlInvestment = investmentAmount;
		setMobilizationApportionedRow();
		setTotalAfterMobilizationApportionedRow();
	}
	
	public void setLoanOutstandingAmount(BigDecimal dLoanOutstandingAmount, BigDecimal NdloanOutstandingAmount){
		dlOutstandingAmount = dLoanOutstandingAmount;
		nDlOutstandingAmount = NdloanOutstandingAmount; 
	}
}
