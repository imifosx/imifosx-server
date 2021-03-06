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
package org.apache.fineract.portfolio.servicecharge.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeApiConstants;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.apache.fineract.portfolio.servicecharge.exception.ServiceChargeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServiceChargeFinalSheetData implements ServiceChargeApiConstants {

    private final static Logger logger = LoggerFactory.getLogger(ServiceChargeFinalSheetData.class);

    private Map<ServiceChargeReportTableHeaders, List<BigDecimal>> resultsDataMap;
    private StringBuffer resultDataAsHTMLTableString;

    private BigDecimal subTotalMobilization;
    private BigDecimal subTotalServicing;
    private BigDecimal subTotalInvestment;
    private BigDecimal subTotalOverHeads;
    private BigDecimal subTotalBFServicing;

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

    // Total Activity-wise Segregated Cost
    private BigDecimal finalTotalLoanServicing;
    private BigDecimal finalTotalInvestment;

    // DL & NDL loan outstanding amount
    private BigDecimal dlOutstandingAmount;
    private BigDecimal nDlOutstandingAmount;
    private BigDecimal totalLoanRepaymentAmount;

    // Total Number of Demand Loans
    private int noOfDemandLoans;

    private void init() {
        resultsDataMap = new HashMap<ServiceChargeReportTableHeaders, List<BigDecimal>>();
        resultDataAsHTMLTableString = new StringBuffer();
        for (ServiceChargeReportTableHeaders header : ServiceChargeReportTableHeaders.values()) {
            resultsDataMap.put(header, new ArrayList<BigDecimal>());
        }
        totalLoanRepaymentAmount = BigDecimal.ZERO;
    }

    public ServiceChargeFinalSheetData() {
        init();
    }

    public BigDecimal getSubTotalMobilization() {
        return subTotalMobilization;
    }

    public BigDecimal getSubTotalServicing() {
        return subTotalServicing;
    }

    public BigDecimal getSubTotalInvestment() {
        return subTotalInvestment;
    }

    public BigDecimal getSubTotalOverHeads() {
        return subTotalOverHeads;
    }

    public BigDecimal getTotalBFServicing() {
        return subTotalBFServicing;
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

    public BigDecimal getTotalLoanRepaymentAmount() {
        return totalLoanRepaymentAmount;
    }

    public void addTotalLoanRepaymentAmount(BigDecimal amount) {
        // logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.addTotalLoanRepaymentAmount::totalLoanRepaymentAmount
        // before:" + totalLoanRepaymentAmount);
        // logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.addTotalLoanRepaymentAmount
        // addTotalLoanRepaymentAmount::amount to add:" + amount);
        logger.debug("ServiceChargeFinalSheetData.addTotalLoanRepaymentAmount::" + amount.toEngineeringString());
        totalLoanRepaymentAmount = totalLoanRepaymentAmount.add(amount);
        // logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.addTotalLoanRepaymentAmount::totalLoanRepaymentAmount
        // after:" + totalLoanRepaymentAmount);
    }

    public BigDecimal getNDlOutstandingAmount() {
        return nDlOutstandingAmount;
    }

    public void setColumnValue(ServiceChargeReportTableHeaders header, List<BigDecimal> dataList) {
        List<BigDecimal> roundedDataList = roundOffBigDecimalValues(dataList, header);
        getResultsDataMap().put(header, roundedDataList);
    }

    private List<BigDecimal> roundOffBigDecimalValues(List<BigDecimal> dataList, ServiceChargeReportTableHeaders header) {
        List<BigDecimal> roundedDataList = new ArrayList<BigDecimal>();
        RoundingMode roundingMode = MoneyHelper.getRoundingMode();
        int roundOffDigits = header.getRoundOff();
        logger.debug("ServiceChargeFinalSheetData.roundOffBigDecimalValues:: rounding off digits" + roundOffDigits + " for header:"
                + header.name());
        for (BigDecimal data : dataList) {
            if (data != null) {
                BigDecimal value = data.setScale(roundOffDigits, roundingMode);
                roundedDataList.add(value);
            } else {
                roundedDataList.add(null);
            }
        }
        return roundedDataList;
    }

    public BigDecimal getColumnValue(ServiceChargeReportTableHeaders header, int columnNumber) {
        List<BigDecimal> dataList = getResultsDataMap().get(header);
        if (columnNumber >= dataList.size()) { throw new ServiceChargeException(header.getValue().longValue()); }
        return dataList.get(columnNumber);
    }

    public String generateResultAsHTMLTable(boolean forceRecreate) {
        StringBuffer result = getResultDataAsHTMLTableString();
        if (result.length() == 0 || forceRecreate) {
            generateInitialCalculationSheet();
            generateFinalCalculationSheet();
        }
        logger.debug("ServiceChargeFinalSheetData.addTotalLoanRepaymentAmount:: Final Display Result:" + result);
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
                    if (element != null) {
                        String elementNumberAsString = String.format("%." + header.getRoundOff() + "f", element);
                        logger.debug("ServiceChargeFinalSheetData.addTotalLoanRepaymentAmount:: element value:" + elementNumberAsString);
                        result.append(elementNumberAsString);
                    } else {
                        result.append(StringUtils.EMPTY);
                    }
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
            sb.append("<th>");
            sb.append("Expenses Allocation Categaories");
            sb.append("</th>");
            sb.append("<th>");
            sb.append("Mobilisation");
            sb.append("</th>");
            sb.append("<th>");
            sb.append("Loan Servicing");
            sb.append("</th>");
            sb.append("<th>");
            sb.append("Investment");
            sb.append("</th>");
            sb.append("<th>");
            sb.append("Overheads");
            sb.append("</th>");
            sb.append("<th>");
            sb.append("Total");
            sb.append("</th>");
            sb.append("</tr>");
        } else if (tableNumber == 2) {
            sb.append("<table table style=\"width:100%\" border=5pt>");
            sb.append("<tr>");
            sb.append("<th>");
            sb.append("Particulars");
            sb.append("</th>");
            sb.append("<th>");
            sb.append("Value");
            sb.append("</th>");
            sb.append("</tr>");
        }
        return sb;
    }

    private void setBFAmountRow() {
        List<BigDecimal> bfColumnEntries = new ArrayList<>(1);
        bfColumnEntries.add(subTotalBFServicing);
        setColumnValue(ServiceChargeReportTableHeaders.LSCOST_ON_ACCOUNT_BF, bfColumnEntries);
    }

    private void setSubTotalRow() {
        List<BigDecimal> columnEntries = new ArrayList<>(5);
        BigDecimal totalAmount = BigDecimal.ZERO;
        totalAmount = totalAmount.add(subTotalMobilization).add(subTotalServicing).add(subTotalInvestment).add(subTotalOverHeads);
        columnEntries.add(subTotalMobilization);
        columnEntries.add(subTotalServicing);
        columnEntries.add(subTotalInvestment);
        columnEntries.add(subTotalOverHeads);
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
        setColumnValue(ServiceChargeReportTableHeaders.ALLOCATION_I_OVERHEADS, columnEntries);
        subTotalAfterOverheadsAllocationMobilization = subTotalMobilization.add(overheadsApportionedMobilization);
        subTotalAfterOverheadsAllocationServicing = subTotalServicing.add(overheadsApportionedServicing);
        subTotalAfterOverheadsAllocationInvestment = subTotalInvestment.add(overheadsApportionedlInvestment);
    }

    private void setTotalAfterOverheadsApportionedRow() {
        List<BigDecimal> columnEntries = new ArrayList<>(5);
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Populate overheads apportioned + original total values
        totalAmount = totalAmount.add(subTotalAfterOverheadsAllocationMobilization).add(subTotalAfterOverheadsAllocationServicing)
                .add(subTotalAfterOverheadsAllocationInvestment);
        columnEntries.add(subTotalAfterOverheadsAllocationMobilization);
        columnEntries.add(subTotalAfterOverheadsAllocationServicing);
        columnEntries.add(subTotalAfterOverheadsAllocationInvestment);
        columnEntries.add(null);
        columnEntries.add(totalAmount);
        setColumnValue(ServiceChargeReportTableHeaders.SUBTOTAL_ALLOCATION, columnEntries);

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
        setColumnValue(ServiceChargeReportTableHeaders.ALLOCATION_II_MOBILIZATION, columnEntries);
    }

    private void setTotalAfterMobilizationApportionedRow() {
        List<BigDecimal> columnEntries = new ArrayList<>(5);
        BigDecimal totalAmount = BigDecimal.ZERO;

        this.finalTotalLoanServicing = this.subTotalAfterOverheadsAllocationServicing.add(mobilizationApportionedServicing);
        this.finalTotalInvestment = this.subTotalAfterOverheadsAllocationInvestment.add(mobilizationApportionedlInvestment);

        // Populate mobilization apportioned + original total values
        totalAmount = totalAmount.add(finalTotalLoanServicing).add(finalTotalInvestment);
        columnEntries.add(null);
        columnEntries.add(finalTotalLoanServicing);
        columnEntries.add(finalTotalInvestment);
        columnEntries.add(null);
        columnEntries.add(totalAmount);
        setColumnValue(ServiceChargeReportTableHeaders.TOTAL_SEGREGATION_COST, columnEntries);

    }

    public void setJounEntriesData(BigDecimal totalMobilizationAmount, BigDecimal totalServicingAmount, BigDecimal totalInvestmentAmount,
            BigDecimal totalOverHeadsAmount, BigDecimal totalProvisionsAmount, BigDecimal totalBFServicingAmount) {
        this.subTotalMobilization = totalMobilizationAmount;
        this.subTotalServicing = totalServicingAmount;
        this.subTotalInvestment = totalInvestmentAmount;
        this.subTotalOverHeads = totalOverHeadsAmount.add(totalProvisionsAmount);
        this.subTotalBFServicing = totalBFServicingAmount;

        setBFAmountRow();
        setSubTotalRow();
    }

    public void setOverheadsApportionedValues(BigDecimal mobilizationAmount, BigDecimal servicingAmount, BigDecimal investmentAmount) {
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

    public void setLoanOutstandingAmount(BigDecimal dLoanOutstandingAmount, BigDecimal nDloanOutstandingAmount) {
        this.dlOutstandingAmount = dLoanOutstandingAmount;
        this.nDlOutstandingAmount = nDloanOutstandingAmount;
    }

    public int getNoOfDemandLoans() {
        return noOfDemandLoans;
    }

    public void setNoOfDemandLoans(int noOfDemandLoans) {
        this.noOfDemandLoans = noOfDemandLoans;
    }
}
