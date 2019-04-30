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
package org.apache.fineract.portfolio.servicecharge.constants;

import java.util.HashMap;
import java.util.Map;

public enum ServiceChargeReportTableHeaders  implements ServiceChargeApiConstants {
	
    SUBTOTAL(1, "Sub Total", 2),
    ALLOCATION_I_OVERHEADS(2, "Allocation-I (Overheads)", 2),
    SUBTOTAL_ALLOCATION(3, "Sub Total after Overheads Allocation", 2),
    ALLOCATION_II_MOBILIZATION(4, "Allocation-II (Mobilization Cost)", 2),
    TOTAL_SEGREGATION_COST(5, "Total Activity-wise Segregated Cost", 2),
    LSCOST_ON_ACCOUNT_BF(6, "LS Cost on A/c BF", 2),
    TOTAL_MOBILIZATION(7, "Total Mobilisation Cost p.a.", ROUNDOFF_DIGITS_LIMIT),
    AVG_REPAYMENT(8, "Average OS DL Re.Months", ROUNDOFF_DIGITS_LIMIT),
    DAILY_OS_SUM(8, "Daily Summation OutStanding DL", 1),
    MOBILIZATION_PERCENT(9, "Mobilisation Cost (%)", ROUNDOFF_DIGITS_LIMIT),
    LOAN_SERVICING_PA(10, "Loan Servicing Cost p.a.", 1),
    TOTAL_LOANS(11, "Total No.of DL Loans for the Period", 1),
    LOAN_SERVICING_PER_LOAN(12, "Loan Servicing Cost per Loan", ROUNDOFF_DIGITS_LIMIT),
    TOTAL_REPAYMENT(13, "Total Repayment for the Period", 1),
    REPAYMENT_PER_100(14, "Repayment Cost per 100 Rupee of Repayment", ROUNDOFF_DIGITS_LIMIT),
    ANNUALIZED_COST_I(15, "Equivalent Annualized Cost (%) - I", ROUNDOFF_DIGITS_LIMIT),
    ANNUALIZED_COST_II(16, "Equivalent Annualized Cost (%) - II", ROUNDOFF_DIGITS_LIMIT),
    ANNUALIZED_COST_III(17, "Equivalent Annualized Cost (%) - III", ROUNDOFF_DIGITS_LIMIT),
    ANNUALIZED_COST_TOTAL(18, "Equivalent Annualized Cost (%) - Total", ROUNDOFF_DIGITS_LIMIT),
    INVALID(100, "INVALID HEADER", 0);
    
    private final Integer value;
    private final String code;
    private final int roundOff;

    private ServiceChargeReportTableHeaders(final Integer value, final String code, final int roundOff) {
        this.value = value;
        this.code = code;
        this.roundOff = roundOff;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public int getRoundOff() {
        return this.roundOff;
    }

    private static final Map<Integer, ServiceChargeReportTableHeaders> intToEnumMap = new HashMap<>();
    private static int minValue;
    private static int maxValue;
    static {
        int i = 0;
        for (final ServiceChargeReportTableHeaders type : ServiceChargeReportTableHeaders.values()) {
            if (i == 0) {
                minValue = type.value;
            }
            intToEnumMap.put(type.value, type);
            if (minValue >= type.value) {
                minValue = type.value;
            }
            if (maxValue < type.value) {
                maxValue = type.value;
            }
            i = i + 1;
        }
    }

    public static ServiceChargeReportTableHeaders fromInt(final int i) {
        final ServiceChargeReportTableHeaders type = intToEnumMap.get(Integer.valueOf(i));
        return type;
    }

    @Override
    public String toString() {
        return name().toString();
    }
}
