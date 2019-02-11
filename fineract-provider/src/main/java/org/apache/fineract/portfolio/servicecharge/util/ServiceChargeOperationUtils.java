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
package org.apache.fineract.portfolio.servicecharge.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeApiConstants;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeData;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeReadPlatformService;
import org.apache.fineract.portfolio.servicecharge.utils.daterange.DateRangeFactory;

public class ServiceChargeOperationUtils implements ServiceChargeApiConstants {

	public static BigDecimal divideAndMultiplyNonZeroValues(BigDecimal operand, BigDecimal divisor,
			BigDecimal multiplicand) {
		final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
		if (operand == null) {
			return BigDecimal.ONE;
		}
		if (divisor != null && !divisor.equals(BigDecimal.ZERO)) {
			operand = operand.divide(divisor, roundingMode);
		}
		if (multiplicand != null) {
			operand = operand.multiply(multiplicand);
		}
		return operand;
	}

	public static BigDecimal divideNonZeroValues(BigDecimal operand, BigDecimal divisor) {
		if (operand == null) {
			return BigDecimal.ONE;
		}
		if (divisor != null && (divisor.compareTo(BigDecimal.ZERO) != 0)) {
			final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
			operand = operand.divide(divisor, roundingMode);
		}
		return operand;
	}

    public static String convertMapToHTMLTable(Map<String, List<String>> map) {
        StringBuffer sb = new StringBuffer();
        sb.append("<table table style=\"width:100%\" border=5pt>");
        for (String key : map.keySet()) {
            sb.append("<tr>");
            sb.append("<td>");
            sb.append(key);
            sb.append("</td>");
            for (String element : map.get(key)) {
                sb.append("<td>");
                sb.append(element);
                sb.append("</td>");
            }
            sb.append("</tr>");

        }
        sb.append("</table>");
        return sb.toString();
    }

    public static String convertMapToHTMLTable(Map<String, List<BigDecimal>> map, StringBuffer appendHTML) {
        StringBuffer sb = new StringBuffer();
        if (null == appendHTML) {
            sb.append("<table table style=\"width:100%\" border=5pt>");
        }
        for (String key : map.keySet()) {
            sb.append("<tr>");
            sb.append("<td>");
            sb.append(key);
            sb.append("</td>");
            for (BigDecimal element : map.get(key)) {
                sb.append("<td>");
                if (element != null)
                    sb.append(element.toPlainString());
                else
                    sb.append(StringUtils.EMPTY);
                sb.append("</td>");
            }
            sb.append("</tr>");

        }
        sb.append("</table>");
        return sb.toString();
    }

    /**
     * This method checks if the given loan product is of type
     * "Demand-Loan"<br/>
     * By meaning of type "Demand-Loan" it means that any loan product
     * associated with the "Serice Charge" charge, it is considered to be of
     * this type.
     * 
     * @param loanProduct
     *            - LoanProduct to be verified
     * @return boolean - If the given loan product is associated with this
     *         charge it returns true, false otherwise
     */
    public static boolean checkDemandLaon(LoanProductData loanProduct) {
        if (loanProduct != null) {
            // Get all the charges associated with the loan product
            Collection<ChargeData> chargeData = loanProduct.charges();
            // Iterate over the list of charges
            for (ChargeData chargeDataName : chargeData) {
                if (chargeDataName != null) {
                    String chargeName = chargeDataName.getName();
                    // If the charge name is Service Charge then it is
                    // of Demand-Loan type
                    if (StringUtils.equalsIgnoreCase(SERVICE_CHARGE_NAME, chargeName)) { return true; }
                }
            } // End-Of-For
        }
        return false;
    }

    /**
     * This method gets the Service Charge from the table for the current
     * quarter. Quarter identification is done based on the current date.
     * 
     * @param ServiceChargeReadPlatformService
     * @return ServiceChargeData - Service Charge data for the current quarter
     *         if exists, null otherwise
     */
    public static ServiceChargeData getServiceChargeForCurrentQuarter(final ServiceChargeReadPlatformService scChargeReadPlatformService) {
        DateRangeFactory quarter = DateRangeFactory.getCurrentDateRange();
        int year = Calendar.getInstance().get(Calendar.YEAR);

        Collection<ServiceChargeData> retrivedSCList = scChargeReadPlatformService.retrieveCharge(quarter, year);
        if (retrivedSCList == null || retrivedSCList.isEmpty()) { return null; }
        // Returning the first element
        return retrivedSCList.iterator().next();
    }
}
