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

public enum ServiceChargeReportTableHeaders {
	
	SUBTOTAL(1, "Sub Total"),
	ALLOCATION_I_OVERHEADS(2, "Allocation-I (Overheads)"),
	SUBTOTAL_ALLOCATION(3, "Sub Total after Overheads Allocation"),
	ALLOCATION_II_MOBILIZATION(4, "Allocation-II (Mobilization Cost)"),
    TOTAL_SEGREGATION_COST(5, "Total Activity-wise Segregated Cost"),
    LSCOST_ON_ACCOUNT_BF(6, "LS Cost on A/c BF"),
    TOTAL_MOBILIZATION(7, "Total Mobilisation Cost p.a."),
	AVG_REPAYMENT(8, "Average OS DL Re.Months"),
	MOBILIZATION_PERCENT(9, "Mobilisation Cost (%)"),
    LOAN_SERVICING_PA(10, "Loan Servicing Cost p.a."),
    TOTAL_LOANS(11, "Total No.of DL Loans for the Period"),
	LOAN_SERVICING_PER_LOAN(12, "Loan Servicing Cost per Loan"),
    TOTAL_REPAYMENT(13, "Total Repayment for the Period"),
    REPAYMENT_PER_100(14, "Repayment Cost per 100 Rupee of Repayment"),
    ANNUALIZED_COST_I(15, "Equivalent Annualized Cost (%) - I"),
    ANNUALIZED_COST_II(16, "Equivalent Annualized Cost (%) - II"),
    ANNUALIZED_COST_III(17, "Equivalent Annualized Cost (%) - III"),
    ANNUALIZED_COST_TOTAL(18, "Equivalent Annualized Cost (%) - Total"),
	INVALID(100, "INVALID HEADER");
	private final Integer value;
	private final String code;

	private ServiceChargeReportTableHeaders(final Integer value,
			final String code) {
		this.value = value;
		this.code = code;
	}

	public Integer getValue() {
		return this.value;
	}

	public String getCode() {
		return this.code;
	}

	private static final Map<Integer, ServiceChargeReportTableHeaders> intToEnumMap = new HashMap<>();
	private static int minValue;
	private static int maxValue;
	static {
		int i = 0;
		for (final ServiceChargeReportTableHeaders type : ServiceChargeReportTableHeaders
				.values()) {
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
		final ServiceChargeReportTableHeaders type = intToEnumMap.get(Integer
				.valueOf(i));
		return type;
	}

	@Override
	public String toString() {
		return name().toString();
	}
}
