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
package org.ideoholic.fineract.commands;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.fineract.accounting.journalentry.command.SingleDebitOrCreditEntryCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.ideoholic.fineract.servicechargejournalentry.serialization.ServiceChargeJournalEntryJsonInputParams;
import org.joda.time.LocalDate;

/**
 * JSON Object mapper Java class that maps to the request of JournalEntry
 * request JSON. This is needed to convert the JSON to object, perform required
 * manipulation before converting back to JSON. Hence got JSON can be directly
 * used to post JournalEntry
 * 
 * @author ideoholic
 * @see org.apache.fineract.accounting.journalentry.command.JournalEntryCommand
 */
public class ServiceChargeJournalEntryCommand {

	private final Locale locale;
	private final String dateFormat;
	private final Long officeId;
	private final LocalDate transactionDate;
	private final String currencyCode;
	private final String comments;
	private final String referenceNumber;
	private final Long accountingRuleId;
	private final BigDecimal amount;
	private final Long paymentTypeId;
	private final String accountNumber;
	private final String checkNumber;
	private final String receiptNumber;
	private final String bankNumber;
	private final String routingCode;
	private final Float mobilization;
	private final Float servicing;
	private final Float investment;
	private final Float overheads;

	private final SingleDebitOrCreditEntryCommand[] credits;
	private final SingleDebitOrCreditEntryCommand[] debits;

	public ServiceChargeJournalEntryCommand(final Locale locale, final String dateFormat, final Long officeId,
			final String currencyCode, final LocalDate transactionDate, final String comments,
			final SingleDebitOrCreditEntryCommand[] credits, final SingleDebitOrCreditEntryCommand[] debits,
			final String referenceNumber, final Long accountingRuleId, final BigDecimal amount,
			final Long paymentTypeId, final String accountNumber, final String checkNumber, final String receiptNumber,
			final String bankNumber, final String routingCode, final Float mobilization, final Float servicing,
			final Float investment, final Float overheads) {
		this.locale = locale;
		this.dateFormat = dateFormat;
		this.officeId = officeId;
		this.currencyCode = currencyCode;
		this.transactionDate = transactionDate;
		this.comments = comments;
		this.credits = credits;
		this.debits = debits;
		this.referenceNumber = referenceNumber;
		this.accountingRuleId = accountingRuleId;
		this.amount = amount;
		this.paymentTypeId = paymentTypeId;
		this.accountNumber = accountNumber;
		this.checkNumber = checkNumber;
		this.receiptNumber = receiptNumber;
		this.bankNumber = bankNumber;
		this.routingCode = routingCode;
		this.mobilization = mobilization;
		this.servicing = servicing;
		this.investment = investment;
		this.overheads = overheads;
	}

	public void validateForCreate() {

		final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

		final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
				.resource("GLJournalEntry");

		baseDataValidator.reset().parameter("transactionDate").value(this.transactionDate).notBlank();

		baseDataValidator.reset().parameter("officeId").value(this.officeId).notNull().integerGreaterThanZero();

		baseDataValidator.reset().parameter(ServiceChargeJournalEntryJsonInputParams.CURRENCY_CODE.getValue())
				.value(this.currencyCode).notBlank();

		baseDataValidator.reset().parameter("comments").value(this.comments).ignoreIfNull().notExceedingLengthOf(500);

		baseDataValidator.reset().parameter("referenceNumber").value(this.referenceNumber).ignoreIfNull()
				.notExceedingLengthOf(100);

		baseDataValidator.reset().parameter("accountingRule").value(this.accountingRuleId).ignoreIfNull()
				.longGreaterThanZero();

		baseDataValidator.reset().parameter("paymentTypeId").value(this.paymentTypeId).ignoreIfNull()
				.longGreaterThanZero();

		// validation for credit array elements
		if (this.credits != null) {
			if (this.credits.length == 0) {
				validateSingleDebitOrCredit(baseDataValidator, "credits", 0,
						new SingleDebitOrCreditEntryCommand(null, null, null, null));
			} else {
				int i = 0;
				for (final SingleDebitOrCreditEntryCommand credit : this.credits) {
					validateSingleDebitOrCredit(baseDataValidator, "credits", i, credit);
					i++;
				}
			}
		}

		// validation for debit array elements
		if (this.debits != null) {
			if (this.debits.length == 0) {
				validateSingleDebitOrCredit(baseDataValidator, "debits", 0,
						new SingleDebitOrCreditEntryCommand(null, null, null, null));
			} else {
				int i = 0;
				for (final SingleDebitOrCreditEntryCommand debit : this.debits) {
					validateSingleDebitOrCredit(baseDataValidator, "debits", i, debit);
					i++;
				}
			}
		}
		baseDataValidator.reset().parameter("amount").value(this.amount).ignoreIfNull().zeroOrPositiveAmount();

		if (!dataValidationErrors.isEmpty()) {
			throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
					"Validation errors exist.", dataValidationErrors);
		}
	}

	/**
	 * @param baseDataValidator
	 * @param i
	 * @param credit
	 */
	private void validateSingleDebitOrCredit(final DataValidatorBuilder baseDataValidator, final String paramSuffix,
			final int arrayPos, final SingleDebitOrCreditEntryCommand credit) {
		baseDataValidator.reset().parameter(paramSuffix + "[" + arrayPos + "].glAccountId")
				.value(credit.getGlAccountId()).notNull().integerGreaterThanZero();
		baseDataValidator.reset().parameter(paramSuffix + "[" + arrayPos + "].amount").value(credit.getAmount())
				.notNull().zeroOrPositiveAmount();
	}

	public String getLocale() {
		return locale.getLanguage();
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public Long getOfficeId() {
		return this.officeId;
	}

	public LocalDate getTransactionDate() {
		return this.transactionDate;
	}

	public String getComments() {
		return this.comments;
	}

	public SingleDebitOrCreditEntryCommand[] getCredits() {
		return this.credits;
	}

	public SingleDebitOrCreditEntryCommand[] getDebits() {
		return this.debits;
	}

	public String getReferenceNumber() {
		return this.referenceNumber;
	}

	public Long getAccountingRuleId() {
		return this.accountingRuleId;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Long getPaymentTypeId() {
		return paymentTypeId;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public String getCheckNumber() {
		return checkNumber;
	}

	public String getReceiptNumber() {
		return receiptNumber;
	}

	public String getBankNumber() {
		return bankNumber;
	}

	public String getRoutingCode() {
		return routingCode;
	}

	public Float getMobilization() {
		return mobilization;
	}

	public Float getServicing() {
		return servicing;
	}

	public Float getInvestment() {
		return investment;
	}

	public Float getOverheads() {
		return overheads;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("locale:").append(this.getLocale());
		sb.append(" dateFormat:").append(this.getDateFormat());
		sb.append(" officeId:").append(this.getOfficeId());
		sb.append(", currencyCode:").append(this.getCurrencyCode());
		sb.append(", referenceNumber:").append(this.getReferenceNumber());
		sb.append(", transactionDate:").append(this.getTransactionDate());
		sb.append(", amount:").append(this.getAmount());
		sb.append(", paymentTypeId:").append(this.getPaymentTypeId());
		sb.append(", accountNumber:").append(this.getAccountNumber());
		sb.append(", checkNumber:").append(this.getCheckNumber());
		sb.append(", receiptNumber:").append(this.getReceiptNumber());
		sb.append(", bankNumber:").append(this.getBankNumber());
		sb.append(", routingCode:").append(this.getRoutingCode());
		sb.append(", comments:").append(this.getComments());
		sb.append(", mobilization:").append(this.getMobilization());
		sb.append(", servicing:").append(this.getServicing());
		sb.append(", investment:").append(this.getInvestment());
		sb.append(", overheads:").append(this.getOverheads());
		return sb.toString();
	}

}