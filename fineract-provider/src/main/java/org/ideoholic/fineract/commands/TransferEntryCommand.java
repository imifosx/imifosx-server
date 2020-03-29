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

import org.apache.fineract.accounting.journalentry.exception.JournalEntryInvalidException;
import org.apache.fineract.accounting.journalentry.exception.JournalEntryInvalidException.GL_JOURNAL_ENTRY_INVALID_REASON;
import org.codehaus.jackson.annotate.JsonProperty;

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
	@JsonProperty("transferDescription")
	private String transferDescription;
	// Array of accounting heads that are affected by this transfer action
	@JsonProperty("accountingHeaders")
	private TransferDebitCreditEntryCommand[] accountingHeaders;

	public void validateForCreate() {
		// Check for debit credit amount to match along with the parallel transaction
		// that is to be take place on the client account
		checkDebitAndCreditAmounts(getAccountingHeaders(), getAmount());
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
		sb.append("officeId:").append(this.officeId);
		sb.append(", currencyCode:").append(this.currencyCode);
		sb.append(", toWhomToTransfer:").append(this.toWhomToTransfer);
		sb.append(", transactionDate:").append(this.transactionDate);
		sb.append(", toClientId:").append(this.toClientId);
		sb.append(", toAccountType:").append(this.toAccountType);
		sb.append(", toAccountId:").append(this.toAccountId);
		sb.append(", amount:").append(this.amount);
		sb.append(", paymentTypeId:").append(this.paymentTypeId);
		sb.append(", accountNumber:").append(this.accountNumber);
		sb.append(", checkNumber:").append(this.checkNumber);
		sb.append(", routingCode:").append(this.routingCode);
		sb.append(", receiptNumber:").append(this.receiptNumber);
		sb.append(", bankNumber:").append(this.bankNumber);
		sb.append(", transferDescription:").append(this.transferDescription);
		return sb.toString();
	}

}
