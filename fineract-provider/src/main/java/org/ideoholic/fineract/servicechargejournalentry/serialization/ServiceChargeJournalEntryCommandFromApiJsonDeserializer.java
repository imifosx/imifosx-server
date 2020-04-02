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
package org.ideoholic.fineract.servicechargejournalentry.serialization;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.accounting.journalentry.command.JournalEntryCommand;
import org.apache.fineract.accounting.journalentry.command.SingleDebitOrCreditEntryCommand;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.serialization.AbstractFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.serialization.FromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.ideoholic.fineract.commands.ServiceChargeJournalEntryCommand;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * Implementation of {@link FromApiJsonDeserializer} for
 * {@link JournalEntryCommand}'s.
 */
@Component
public final class ServiceChargeJournalEntryCommandFromApiJsonDeserializer
		extends AbstractFromApiJsonDeserializer<ServiceChargeJournalEntryCommand> {

	private final FromJsonHelper fromApiJsonHelper;

	@Autowired
	public ServiceChargeJournalEntryCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonfromApiJsonHelper) {
		this.fromApiJsonHelper = fromApiJsonfromApiJsonHelper;
	}

	@Override
    public ServiceChargeJournalEntryCommand commandFromApiJson(final String json) {
        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        final Set<String> supportedParameters = ServiceChargeJournalEntryJsonInputParams.getAllValues();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, supportedParameters);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final Long officeId = this.fromApiJsonHelper.extractLongNamed(ServiceChargeJournalEntryJsonInputParams.OFFICE_ID.getValue(), element);
        final String currencyCode = this.fromApiJsonHelper
                .extractStringNamed(ServiceChargeJournalEntryJsonInputParams.CURRENCY_CODE.getValue(), element);
        final String comments = this.fromApiJsonHelper.extractStringNamed(ServiceChargeJournalEntryJsonInputParams.COMMENTS.getValue(), element);
        final LocalDate transactionDate = this.fromApiJsonHelper.extractLocalDateNamed(
                ServiceChargeJournalEntryJsonInputParams.TRANSACTION_DATE.getValue(), element);
        final String referenceNumber = this.fromApiJsonHelper.extractStringNamed(ServiceChargeJournalEntryJsonInputParams.REFERENCE_NUMBER.getValue(),
                element);
        final Long accountingRuleId = this.fromApiJsonHelper.extractLongNamed(ServiceChargeJournalEntryJsonInputParams.ACCOUNTING_RULE.getValue(),
                element);
        final JsonObject topLevelJsonElement = element.getAsJsonObject();
        final Locale locale = this.fromApiJsonHelper.extractLocaleParameter(topLevelJsonElement);

        final BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalNamed(ServiceChargeJournalEntryJsonInputParams.AMOUNT.getValue(), element,
                locale);
        final Long paymentTypeId = this.fromApiJsonHelper.extractLongNamed(ServiceChargeJournalEntryJsonInputParams.PAYMENT_TYPE_ID.getValue(), element);
        final String accountNumber = this.fromApiJsonHelper.extractStringNamed(ServiceChargeJournalEntryJsonInputParams.ACCOUNT_NUMBER.getValue(),
                element);
        final String checkNumber = this.fromApiJsonHelper.extractStringNamed(ServiceChargeJournalEntryJsonInputParams.CHECK_NUMBER.getValue(), element);
        final String receiptNumber = this.fromApiJsonHelper.extractStringNamed(ServiceChargeJournalEntryJsonInputParams.RECEIPT_NUMBER.getValue(),
                element);
        final String bankNumber = this.fromApiJsonHelper.extractStringNamed(ServiceChargeJournalEntryJsonInputParams.BANK_NUMBER.getValue(), element);
        final String routingCode = this.fromApiJsonHelper.extractStringNamed(ServiceChargeJournalEntryJsonInputParams.ROUTING_CODE.getValue(), element);

        SingleDebitOrCreditEntryCommand[] credits = null;
        SingleDebitOrCreditEntryCommand[] debits = null;
        if (element.isJsonObject()) {
            if (topLevelJsonElement.has(ServiceChargeJournalEntryJsonInputParams.CREDITS.getValue())
                    && topLevelJsonElement.get(ServiceChargeJournalEntryJsonInputParams.CREDITS.getValue()).isJsonArray()) {
                credits = populateCreditsOrDebitsArray(topLevelJsonElement, locale, credits, ServiceChargeJournalEntryJsonInputParams.CREDITS.getValue());
            }
            if (topLevelJsonElement.has(ServiceChargeJournalEntryJsonInputParams.DEBITS.getValue())
                    && topLevelJsonElement.get(ServiceChargeJournalEntryJsonInputParams.DEBITS.getValue()).isJsonArray()) {
                debits = populateCreditsOrDebitsArray(topLevelJsonElement, locale, debits, ServiceChargeJournalEntryJsonInputParams.DEBITS.getValue());
            }
        }
        final Float mobilization = this.fromApiJsonHelper.extractBigDecimalNamed(ServiceChargeJournalEntryJsonInputParams.MOBILIZATION.getValue(), element,
                locale).floatValue();
        final Float servicing = this.fromApiJsonHelper.extractBigDecimalNamed(ServiceChargeJournalEntryJsonInputParams.SERVICING.getValue(), element,
                locale).floatValue();
        final Float investment = this.fromApiJsonHelper.extractBigDecimalNamed(ServiceChargeJournalEntryJsonInputParams.INVESTMENT.getValue(), element,
                locale).floatValue();
        final Float overheads = this.fromApiJsonHelper.extractBigDecimalNamed(ServiceChargeJournalEntryJsonInputParams.OVERHEADS.getValue(), element,
                locale).floatValue();
        return new ServiceChargeJournalEntryCommand(officeId, currencyCode, transactionDate, comments, credits, debits, referenceNumber,
                accountingRuleId, amount, paymentTypeId, accountNumber, checkNumber, receiptNumber, bankNumber, routingCode, mobilization,
                servicing, investment, overheads);
    }

	/**
	 * @param comments
	 * @param topLevelJsonElement
	 * @param locale
	 */
	private SingleDebitOrCreditEntryCommand[] populateCreditsOrDebitsArray(final JsonObject topLevelJsonElement,
			final Locale locale, SingleDebitOrCreditEntryCommand[] debitOrCredits, final String paramName) {
		final JsonArray array = topLevelJsonElement.get(paramName).getAsJsonArray();
		debitOrCredits = new SingleDebitOrCreditEntryCommand[array.size()];
		for (int i = 0; i < array.size(); i++) {

			final JsonObject creditElement = array.get(i).getAsJsonObject();
			final Set<String> parametersPassedInForCreditsCommand = new HashSet<>();

			final Long glAccountId = this.fromApiJsonHelper.extractLongNamed("glAccountId", creditElement);
			final String comments = this.fromApiJsonHelper.extractStringNamed("comments", creditElement);
			final BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalNamed("amount", creditElement, locale);

			debitOrCredits[i] = new SingleDebitOrCreditEntryCommand(parametersPassedInForCreditsCommand, glAccountId,
					amount, comments);
		}
		return debitOrCredits;
	}
}