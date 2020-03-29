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

import org.apache.fineract.accounting.journalentry.api.JournalEntryJsonInputParams;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * JSON Object mapper Java class that maps to the request of JournalEntry
 * request JSON. This is needed to convert the JSON to object, perform required
 * manipulation before converting back to JSON. Hence got JSON can be directly
 * used to post JournalEntry
 * 
 * @author ideoholic
 * @see org.apache.fineract.accounting.journalentry.command.JournalEntryCommand
 */
public class JECommand {

	@JsonProperty("locale")
	private String locale;
	@JsonProperty("dateFormat")
	private String dateFormat;
	@JsonProperty("officeId")
	private Long officeId;
	@JsonProperty("transactionDate")
	private String transactionDate;
	@JsonProperty("currencyCode")
	private String currencyCode;
	@JsonProperty("comments")
	private String comments;
	@JsonProperty("referenceNumber")
	private String referenceNumber;
	@JsonProperty("amount")
	private BigDecimal amount;
	@JsonProperty("paymentTypeId")
	private Long paymentTypeId;
	@JsonProperty("accountNumber")
	private String accountNumber;
	@JsonProperty("checkNumber")
	private String checkNumber;
	@JsonProperty("receiptNumber")
	private String receiptNumber;
	@JsonProperty("bankNumber")
	private String bankNumber;
	@JsonProperty("routingCode")
	private String routingCode;

	@JsonProperty("credits")
	private JEDebitCreditEntryCommand[] credits;
	@JsonProperty("debits")
	private JEDebitCreditEntryCommand[] debits;

	public void validateForCreate() {

		final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

		final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
				.resource("GLJournalEntry");

		baseDataValidator.reset().parameter("transactionDate").value(this.transactionDate).notBlank();

		baseDataValidator.reset().parameter("officeId").value(this.officeId).notNull().integerGreaterThanZero();

		baseDataValidator.reset().parameter(JournalEntryJsonInputParams.CURRENCY_CODE.getValue())
				.value(this.currencyCode).notBlank();

		baseDataValidator.reset().parameter("comments").value(this.comments).ignoreIfNull().notExceedingLengthOf(500);

		baseDataValidator.reset().parameter("referenceNumber").value(this.referenceNumber).ignoreIfNull()
				.notExceedingLengthOf(100);

		baseDataValidator.reset().parameter("paymentTypeId").value(this.paymentTypeId).ignoreIfNull()
				.longGreaterThanZero();

		// validation for credit array elements
		if (this.credits != null) {
			if (this.credits.length == 0) {
				validateSingleDebitOrCredit(baseDataValidator, "credits", 0, new JEDebitCreditEntryCommand());
			} else {
				int i = 0;
				for (final JEDebitCreditEntryCommand credit : this.credits) {
					validateSingleDebitOrCredit(baseDataValidator, "credits", i, credit);
					i++;
				}
			}
		}

		// validation for debit array elements
		if (this.debits != null) {
			if (this.debits.length == 0) {
				validateSingleDebitOrCredit(baseDataValidator, "debits", 0, new JEDebitCreditEntryCommand());
			} else {
				int i = 0;
				for (final JEDebitCreditEntryCommand debit : this.debits) {
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
			final int arrayPos, final JEDebitCreditEntryCommand credit) {
		baseDataValidator.reset().parameter(paramSuffix + "[" + arrayPos + "].glAccountId")
				.value(credit.getGlAccountId()).notNull().integerGreaterThanZero();
		baseDataValidator.reset().parameter(paramSuffix + "[" + arrayPos + "].amount").value(credit.getAmount())
				.notNull().zeroOrPositiveAmount();
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public Long getOfficeId() {
		return officeId;
	}

	public void setOfficeId(Long officeId) {
		this.officeId = officeId;
	}

	public String getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(String transactionDate) {
		this.transactionDate = transactionDate;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getReferenceNumber() {
		return referenceNumber;
	}

	public void setReferenceNumber(String referenceNumber) {
		this.referenceNumber = referenceNumber;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Long getPaymentTypeId() {
		return paymentTypeId;
	}

	public void setPaymentTypeId(Long paymentTypeId) {
		this.paymentTypeId = paymentTypeId;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getCheckNumber() {
		return checkNumber;
	}

	public void setCheckNumber(String checkNumber) {
		this.checkNumber = checkNumber;
	}

	public String getReceiptNumber() {
		return receiptNumber;
	}

	public void setReceiptNumber(String receiptNumber) {
		this.receiptNumber = receiptNumber;
	}

	public String getBankNumber() {
		return bankNumber;
	}

	public void setBankNumber(String bankNumber) {
		this.bankNumber = bankNumber;
	}

	public String getRoutingCode() {
		return routingCode;
	}

	public void setRoutingCode(String routingCode) {
		this.routingCode = routingCode;
	}

	public JEDebitCreditEntryCommand[] getCredits() {
		return credits;
	}

	public void setCredits(JEDebitCreditEntryCommand[] credits) {
		this.credits = credits;
	}

	public JEDebitCreditEntryCommand[] getDebits() {
		return debits;
	}

	public void setDebits(JEDebitCreditEntryCommand[] debits) {
		this.debits = debits;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("officeId:").append(this.getOfficeId());
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
		return sb.toString();
	}

}