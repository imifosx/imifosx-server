package org.apache.fineract.portfolio.servicecharge.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.apache.fineract.portfolio.servicecharge.exception.ServiceChargeNotFoundException;

public class ServiceChargeFinalSheetData {
	private Map<ServiceChargeReportTableHeaders, List<BigDecimal>> resultsDataMap;
	private StringBuffer resultDataAsHTMLTableString;

	private void init() {
		resultsDataMap = new HashMap<ServiceChargeReportTableHeaders, List<BigDecimal>>();
		resultDataAsHTMLTableString = new StringBuffer();
		for (ServiceChargeReportTableHeaders header : ServiceChargeReportTableHeaders
				.values()) {
			resultsDataMap.put(header, new ArrayList<BigDecimal>());
		}
	}

	public ServiceChargeFinalSheetData() {
		init();
	}

	public Map<ServiceChargeReportTableHeaders, List<BigDecimal>> getResultsDataMap() {
		return resultsDataMap;
	}

	public StringBuffer getResultDataAsHTMLTableString() {
		return resultDataAsHTMLTableString;
	}

	public void setColumnValue(ServiceChargeReportTableHeaders header,
			List<BigDecimal> dataList) {
		getResultsDataMap().put(header, dataList);
	}

	public BigDecimal getColumnValue(
			ServiceChargeReportTableHeaders header, int columnNumber) {
		List<BigDecimal> dataList = getResultsDataMap().get(header);
		if(columnNumber >= dataList.size()){
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

	private void populateTableRowsWithEntriesinResultsMap(int start, int end,
			boolean endTable) {
		StringBuffer result = getResultDataAsHTMLTableString();
		for (int iCounter = start; iCounter <= end; iCounter++) {
			ServiceChargeReportTableHeaders header = ServiceChargeReportTableHeaders
					.fromInt(iCounter);
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
}
