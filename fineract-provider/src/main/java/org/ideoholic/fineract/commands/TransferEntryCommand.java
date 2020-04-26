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

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.accounting.journalentry.exception.JournalEntryInvalidException;
import org.apache.fineract.accounting.journalentry.exception.JournalEntryInvalidException.GL_JOURNAL_ENTRY_INVALID_REASON;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ideoholic.fineract.servicecharge.constants.ServiceChargeApiConstants;
import org.ideoholic.fineract.servicechargejournalentry.serialization.ServiceChargeJournalEntryJsonInputParams;

public class TransferEntryCommand {

	@JsonProperty("locale")
	private String locale;
	@JsonProperty("dateFormat")
	private String dateFormat;
	@JsonProperty("officeId")
	private Long officeId;
	@JsonProperty("currencyCode")
	private String currencyCode;
	// 1-For member and 2-For accounting header
	@JsonProperty("toWhomToTransfer")
	private Integer toWhomToTransfer;
	@JsonProperty("transactionDate")
	private String transactionDate;
	@JsonProperty("toClientId")
	private Long toClientId;
	@JsonProperty("toAccountType")
	private Integer toAccountType;
	@JsonProperty("toAccountId")
	private Long toAccountId;
	@JsonProperty("amount")
	private BigDecimal amount;
	@JsonProperty("paymentTypeId")
	private Long paymentTypeId;
	@JsonProperty("accountNumber")
	private String accountNumber;
	@JsonProperty("checkNumber")
	private String checkNumber;
	@JsonProperty("routingCode")
	private String routingCode;
	@JsonProperty("receiptNumber")
	private String receiptNumber;
	@JsonProperty("bankNumber")
	private String bankNumber;
	@JsonProperty("mobilization")
	private BigDecimal mobilization;
	@JsonProperty("servicing")
	private BigDecimal servicing;
	@JsonProperty("overheads")
	private BigDecimal overheads;
	@JsonProperty("investment")
	private BigDecimal investment;
	@JsonProperty("transferDescription")
	private String transferDescription;
	// Array of accounting heads that are affected by this transfer action
	@JsonProperty("accountingHeaders")
	private TransferDebitCreditEntryCommand[] accountingHeaders;

	public void validateForCreate() {

		// Check for debit credit amount to match along with the parallel transaction
		// that is to be take place on the client account
		checkDebitAndCreditAmounts(getAccountingHeaders(), getAmount());

		final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

		final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
				.resource("ServiceChargeJournalEntry");

		baseDataValidator.reset().parameter("transactionDate").value(this.transactionDate).notBlank();

		baseDataValidator.reset().parameter("officeId").value(this.officeId).notNull().integerGreaterThanZero();

		baseDataValidator.reset().parameter(ServiceChargeJournalEntryJsonInputParams.CURRENCY_CODE.getValue())
				.value(this.currencyCode).notBlank();

		baseDataValidator.reset().parameter("paymentTypeId").value(this.paymentTypeId).ignoreIfNull()
				.longGreaterThanZero();

		baseDataValidator.reset().parameter("amount").value(this.amount).ignoreIfNull().zeroOrPositiveAmount();

		validateForServiceChargeDivision(dataValidationErrors);

		if (!dataValidationErrors.isEmpty()) {
			throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
					"Validation errors exist.", dataValidationErrors);
		}
	}

	private void validateForServiceChargeDivision(List<ApiParameterError> dataValidationErrors) {
		// Check only if either of the value of service charge is not zero
		if (!getInvestment().equals(BigDecimal.ZERO) || !getMobilization().equals(BigDecimal.ZERO)
				|| !getOverheads().equals(BigDecimal.ZERO) || !getServicing().equals(BigDecimal.ZERO)) {
			BigDecimal sum = getInvestment().add(getMobilization()).add(getOverheads()).add(getServicing());

			if (!sum.equals(ServiceChargeApiConstants.HUNDRED)) {
				StringBuffer message = new StringBuffer();
				StringBuffer params = new StringBuffer();
				params.append(getInvestment()).append(',');
				params.append(getMobilization()).append(',');
				params.append(getOverheads()).append(',');
				params.append(getServicing());
				message.append("The sum of ").append(params);
				message.append(" - servicing charge division entities must be equal to 100");
				ApiParameterError error = ApiParameterError.parameterError(
						"validation.msg.servicecharge.division.not.hundered", message.toString(), params.toString(),
						new Object[] { getInvestment(), getMobilization(), getOverheads(), getServicing() });
				dataValidationErrors.add(error);
			}
		}
	}

	/**
	 * Function to verify that all credit and debit entries match along with the
	 * amount that is going to create an additional debit/credit entry for
	 * completing the transaction.
	 * 
	 * @param debitCreditEntry List of {@link TransferDebitCreditEntryCommand} that
	 *                         needs to be verified
	 * @param amount           BigDecimal amount that is being transferred
	 */
	private void checkDebitAndCreditAmounts(final TransferDebitCreditEntryCommand[] debitCreditEntry,
			BigDecimal amount) {
		// In case that there are no rows for debit-credit or the amount of transfer is
		// zero then throwing exception
		if (debitCreditEntry == null || debitCreditEntry.length == 0 || (amount.compareTo(BigDecimal.ZERO) == 0)) {
			throw new JournalEntryInvalidException(GL_JOURNAL_ENTRY_INVALID_REASON.DEBIT_CREDIT_ACCOUNT_OR_AMOUNT_EMPTY,
					null, null, null);
		}

		BigDecimal creditsSum = BigDecimal.ZERO;
		BigDecimal debitsSum = BigDecimal.ZERO;
		for (final TransferDebitCreditEntryCommand debitCreditEntryCommand : debitCreditEntry) {
			if (debitCreditEntryCommand.getAmount() == null || debitCreditEntryCommand.getGlAccountId() == null) {
				throw new JournalEntryInvalidException(
						GL_JOURNAL_ENTRY_INVALID_REASON.DEBIT_CREDIT_ACCOUNT_OR_AMOUNT_EMPTY, null, null, null);
			}
			switch (debitCreditEntryCommand.getTypeEnum()) {
			case DEBIT:
				debitsSum = debitsSum.add(debitCreditEntryCommand.getAmount());
				break;
			case CREDIT:
				creditsSum = creditsSum.add(debitCreditEntryCommand.getAmount());
				break;
			}

		}
		// 1-For member and 2-For accounting header
		// So if 1 then there will be additional credit accounting header added else for
		// 2 a debit entry. Add the amount under corresponding amount bucket and any
		// other value is excpetion
		switch (getToWhomToTransfer()) {
		case 2:
			debitsSum = debitsSum.add(getAmount());
			break;
		case 1:
			creditsSum = creditsSum.add(getAmount());
			break;
		default:
			throw new JournalEntryInvalidException(GL_JOURNAL_ENTRY_INVALID_REASON.INVALID_DEBIT_OR_CREDIT_ACCOUNTS,
					null, null, null);
		}
		// sum of all debits must be = sum of all credits
		if (creditsSum.compareTo(debitsSum) != 0) {
			throw new JournalEntryInvalidException(GL_JOURNAL_ENTRY_INVALID_REASON.DEBIT_CREDIT_SUM_MISMATCH, null,
					null, null);
		}
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

	public String getCurrencyCode() {
		return currencyCode;
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	public Integer getToWhomToTransfer() {
		return toWhomToTransfer;
	}

	public void setToWhomToTransfer(Integer toWhomToTransfer) {
		this.toWhomToTransfer = toWhomToTransfer;
	}

	public String getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(String transactionDate) {
		this.transactionDate = transactionDate;
	}

	public Long getToClientId() {
		return toClientId;
	}

	public void setToClientId(Long toClientId) {
		this.toClientId = toClientId;
	}

	public Integer getToAccountType() {
		return toAccountType;
	}

	public void setToAccountType(Integer toAccountType) {
		this.toAccountType = toAccountType;
	}

	public Long getToAccountId() {
		return toAccountId;
	}

	public void setToAccountId(Long toAccountId) {
		this.toAccountId = toAccountId;
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

	public String getRoutingCode() {
		return routingCode;
	}

	public void setRoutingCode(String routingCode) {
		this.routingCode = routingCode;
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

	public BigDecimal getMobilization() {
		return mobilization;
	}

	public void setMobilization(BigDecimal mobilization) {
		this.mobilization = mobilization;
	}

	public BigDecimal getServicing() {
		return servicing;
	}

	public void setServicing(BigDecimal servicing) {
		this.servicing = servicing;
	}

	public BigDecimal getOverheads() {
		return overheads;
	}

	public void setOverheads(BigDecimal overheads) {
		this.overheads = overheads;
	}

	public BigDecimal getInvestment() {
		return investment;
	}

	public void setInvestment(BigDecimal investment) {
		this.investment = investment;
	}

	public String getTransferDescription() {
		return transferDescription;
	}

	public void setTransferDescription(String transferDescription) {
		this.transferDescription = transferDescription;
	}

	public TransferDebitCreditEntryCommand[] getAccountingHeaders() {
		return accountingHeaders;
	}

	public void setAccountingHeaders(TransferDebitCreditEntryCommand[] accountingHeaders) {
		this.accountingHeaders = accountingHeaders;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		sb.append("officeId:").append(this.officeId);
		sb.append(", locale:").append(this.locale);
		sb.append(", dateFormat:").append(this.dateFormat);
		sb.append(", currencyCode:").append(this.currencyCode);
		sb.append(", comments:").append(this.transferDescription);
		sb.append(", transactionDate:").append(this.transactionDate);

		sb.append(", amount:").append(this.amount);
		sb.append(", paymentTypeId:").append(this.paymentTypeId);
		sb.append(", accountNumber:").append(this.accountNumber);
		sb.append(", checkNumber:").append(this.checkNumber);
		sb.append(", routingCode:").append(this.routingCode);
		sb.append(", receiptNumber:").append(this.receiptNumber);
		sb.append(", bankNumber:").append(this.bankNumber);
		sb.append(", mobilization:").append(this.mobilization);
		sb.append(", servicing:").append(this.servicing);
		sb.append(", overheads:").append(this.overheads);
		sb.append(", investment:").append(this.investment);
		
		sb.append(", useAccountingRule:").append(StringUtils.EMPTY);
		// sb.append(", credits:").append(this.getCredits());
		// sb.append(", debits:").append(this.getDebits());
		
		return sb.toString();
	}

}
