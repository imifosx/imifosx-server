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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.tax.data.TaxGroupData;
import org.joda.time.MonthDay;

/**
 * Data object for service charge data - Contains ChargeData
 * Will need to decide if we need to extend ChargeData
 */
public class ServiceChargeData {
	
	private ChargeData chargeData;

	protected ServiceChargeData(final Long id, final String name, final BigDecimal amount, final CurrencyData currency, final EnumOptionData chargeTimeType,
			final EnumOptionData chargeAppliesTo, final EnumOptionData chargeCalculationType, final EnumOptionData chargePaymentMode, final boolean penalty, final boolean active,
			final TaxGroupData taxGroup, final Collection<CurrencyData> currencyOptions, final List<EnumOptionData> chargeCalculationTypeOptions,
			final List<EnumOptionData> chargeAppliesToOptions, final List<EnumOptionData> chargeTimeTypeOptions, final List<EnumOptionData> chargePaymentModeOptions,
			final List<EnumOptionData> loansChargeCalculationTypeOptions, final List<EnumOptionData> loansChargeTimeTypeOptions,
			final List<EnumOptionData> savingsChargeCalculationTypeOptions, final List<EnumOptionData> savingsChargeTimeTypeOptions,
			final List<EnumOptionData> clientChargeCalculationTypeOptions, final List<EnumOptionData> clientChargeTimeTypeOptions, final MonthDay feeOnMonthDay,
			final Integer feeInterval, final BigDecimal minCap, final BigDecimal maxCap, final EnumOptionData feeFrequency, final List<EnumOptionData> feeFrequencyOptions,
			final GLAccountData account, final Map<String, List<GLAccountData>> incomeOrLiabilityAccountOptions, Collection<TaxGroupData> taxGroupOptions) {

	}

}