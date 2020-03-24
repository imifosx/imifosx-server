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
package org.ideoholic.fineract.accountingtransfer.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentType;
import org.apache.fineract.portfolio.savings.SavingsTransactionBooleanValues;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountDomainService;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.ideoholic.fineract.accountingtransfer.command.JECommand;
import org.ideoholic.fineract.commands.JEDebitCreditEntryCommand;
import org.ideoholic.fineract.commands.TransferDebitCreditEntryCommand;
import org.ideoholic.fineract.commands.TransferEntryCommand;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonElement;

@Service
public class ClientToAccountingHeaderTransferServiceImpl implements ClientToAccountingHeaderTransferService {
	private final static Logger logger = LoggerFactory.getLogger(ClientToAccountingHeaderTransferServiceImpl.class);

	private final FromJsonHelper fromApiJsonHelper;
	private final SavingsAccountAssembler savingsAccountAssembler;
	private final JournalEntryRepository glJournalEntryRepository;
	private final SavingsAccountDomainService savingsAccountDomainService;
	private final JournalEntryWritePlatformService jeWritePlatformService;
	private final DefaultToApiJsonSerializer<Object> apiJsonSerializerService;

	@Autowired
	public ClientToAccountingHeaderTransferServiceImpl(final FromJsonHelper fromApiJsonHelper,
			final SavingsAccountAssembler savingsAccountAssembler,
			final JournalEntryRepository glJournalEntryRepository,
			final SavingsAccountDomainService savingsAccountDomainService,
			final JournalEntryWritePlatformService jeWritePlatformService,
			final DefaultToApiJsonSerializer<Object> toApiJsonSerializer) {
		this.fromApiJsonHelper = fromApiJsonHelper;
		this.savingsAccountAssembler = savingsAccountAssembler;
		this.glJournalEntryRepository = glJournalEntryRepository;
		this.savingsAccountDomainService = savingsAccountDomainService;
		this.jeWritePlatformService = jeWritePlatformService;
		this.apiJsonSerializerService = toApiJsonSerializer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ideoholic.fineract.accounting.service.
	 * MemberToAccountingHeaderTransferService#createTransferEntry()
	 */
	@Override
	public String createTransferEntry(String json) {
		boolean error = false;
		String jeJson = null;
		JECommand jeCommand = null;
		CommandProcessingResult result = null;
		try {

			logger.debug("ClientToAccountingHeaderTransferServiceImpl.createTransferEntry:: passed " + "jsonCommand:"
					+ json);

			// ObjectMapper is needed to convert object to JSON and JSON to object
			ObjectMapper mapper = new ObjectMapper();

			// Convert passed JSON into our own TransferEntryCommand
			final TransferEntryCommand transferCommand = mapper.readValue(json, TransferEntryCommand.class);
			logger.debug("ClientToAccountingHeaderTransferServiceImpl.createTransferEntry:: transferCommand:"
					+ transferCommand);
			// Validate before creation for any error in data that has been sent
			transferCommand.validateForCreate();

			// Currently not supporting loan transfer feature
			if (transferCommand.getToAccountType() == 1) {
				throw new GeneralPlatformDomainRuleException("error.msg.accounttransfer.loan.to.loan.not.supported",
						"Account transfer from/to a loan to/from ledger head is not supported");
			}

			// From the transfer command get our own JECommand which is a map of
			// JournalEntry command
			jeCommand = getJECommandFromTransferCommand(transferCommand, null);
			jeJson = mapper.writeValueAsString(jeCommand);

			final JsonElement parsedCommand = this.fromApiJsonHelper.parse(jeJson);
			logger.debug("ClientToAccountingHeaderTransferServiceImpl.createTransferEntry:: parsed Command:"
					+ parsedCommand);

			/* This is the first step in transfer, deposit or withdraw from client */
			final SavingsAccountTransaction transaction = transferAmountWithClientAccount(transferCommand, jeJson,
					parsedCommand);

			final JournalEntry pendingTransfer = getIntermediateJEWhereClientTransferAmountIsParked(transaction);

			System.out
					.println("ClientToAccountingHeaderTransferServiceImpl.createTransferEntry:: pending journal entry:"
							+ getJournalEntryAsString(pendingTransfer));

			// Get the JECommand that also includes the pending transfer liability account
			jeCommand = getJECommandFromTransferCommand(transferCommand, pendingTransfer);
			jeJson = mapper.writeValueAsString(jeCommand);

			/* The second step in transfer, credit or debit appropriate journal ledger */
			result = transferAmountWithAccountingHeader(transferCommand, parsedCommand, jeJson, transaction);
		} catch (JsonParseException | JsonMappingException e) {
			logger.error(
					"ClientToAccountingHeaderTransferServiceImpl.createTransferEntry:: JsonParseException | JsonMappingException:"
							+ e);
			e.printStackTrace();
			error = true;
		} catch (IOException | NullPointerException e) {
			logger.error(
					"ClientToAccountingHeaderTransferServiceImpl.createTransferEntry:: IOException | NullPointerException:"
							+ e);
			logger.error("IOException:" + e);
			e.printStackTrace();
			error = true;
		}
		// error variable will be true in case there has been exception due to invalid
		// JSON or any NullPointerException
		if (error) {
			// In such error case throw a general exception with response message
			throw new GeneralPlatformDomainRuleException("error.msg.accounttransfer.invalid.request.json.passed",
					"Invalid JSON request format has been passed for Member-Accounting transfer request");
		}
		return this.apiJsonSerializerService.serialize(result);
	}

	/**
	 * This method transfer the amount from the client savings account to accounting
	 * header that has been mapped in Financial Activity Mappings as Liability
	 * Transfer account. For this purpose it uses the existing savings transfer
	 * feature.
	 * 
	 * @param jeJson
	 * @param parsedCommand
	 * 
	 * @return transaction ID generated for the transfer transaction
	 */
	private SavingsAccountTransaction transferAmountWithClientAccount(TransferEntryCommand transferCommand,
			String jeJson, JsonElement parsedCommand) {
		// Constants values defined here to understand the name of value being passed
		final Long loanId = null;
		final String entityName = "ACCOUNTTRANSFER";
		final String href = "/clienttoheader";
		final String transactionDateParamName = "transactionDate";
		final boolean isRegularTransaction = true;
		final boolean isInterestTransfer = false;
		final boolean isAccountTransfer = true;
		final boolean isWithdrawBalance = false;

		// Get the savings account that needs to be transacted upon
		final Long savingsId = transferCommand.getToAccountId();
		final SavingsAccount savingsAccount = this.savingsAccountAssembler.assembleFrom(savingsId);
		final Long productId = savingsAccount.productId();

		final JsonCommand command = JsonCommand.from(jeJson, parsedCommand, this.fromApiJsonHelper, entityName, null,
				null, null, transferCommand.getToClientId(), loanId, savingsId, null, href, productId, null, null);
		// Parameters needed for doing savings account transaction
		final LocalDate transactionDate = command.localDateValueOfParameterNamed(transactionDateParamName);
		final PaymentDetail paymentDetail = generatePaymentDetailUsingTransferEntryCommand(transferCommand);
		final BigDecimal transactionAmount = transferCommand.getAmount();
		final Locale locale = command.extractLocale();
		final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);

		SavingsAccountTransaction transaction = null;
		// If the transfer is to the client then perform deposit in the account
		if (transferCommand.getToWhomToTransfer() == 1) {
			final SavingsAccountTransaction deposit = this.savingsAccountDomainService.handleDeposit(savingsAccount,
					fmt, transactionDate, transactionAmount, paymentDetail, isAccountTransfer, isRegularTransaction);
			transaction = deposit;

		} else {
			// If otherwise then the amount is the be withdrawan from the client account
			// which will later be transferred to accounting head
			final SavingsTransactionBooleanValues transactionBooleanValues = new SavingsTransactionBooleanValues(
					isAccountTransfer, isRegularTransaction, savingsAccount.isWithdrawalFeeApplicableForTransfer(),
					isInterestTransfer, isWithdrawBalance);

			final SavingsAccountTransaction withdrawal = this.savingsAccountDomainService.handleWithdrawal(
					savingsAccount, fmt, transactionDate, transactionAmount, paymentDetail, transactionBooleanValues);
			transaction = withdrawal;
		}
		return transaction;
	}

	private JournalEntry getIntermediateJEWhereClientTransferAmountIsParked(SavingsAccountTransaction transaction) {
		final Integer entityType = 2;
		JournalEntry pendingTransferJE = null;
		final String transactionId = "S" + transaction.getId();
		// 1. Fetch the journal entries passed for the current savings transfer
		// transaction
		final List<JournalEntry> journalEntries = glJournalEntryRepository.findJournalEntries(transactionId,
				entityType);
		// 2. From this list find the transaction corresponding to the savings, the
		// ledger where the amount is currently parked

		for (JournalEntry journalEntry : journalEntries) {
			// 3. If savings transaction is credit then look of debit journal entry and
			// vice-versa
			if (transaction.isCredit() && journalEntry.isDebitEntry()) {
				pendingTransferJE = journalEntry;
				break;
			} else if (transaction.isDebit() && !journalEntry.isDebitEntry()) {
				pendingTransferJE = journalEntry;
				break;
			}
			// 4. There can be only sinlge savings transaction and so break on first find
		}
		return pendingTransferJE;
	}

	private CommandProcessingResult transferAmountWithAccountingHeader(final TransferEntryCommand transferCommand,
			final JsonElement parsedCommand, final String jeJson, final SavingsAccountTransaction transaction) {
		// Constants values defined here to understand the name of value being passed
		final Long loanId = null;
		final String entityName = "JOURNALENTRY";
		final String href = "/clienttoheader";
		final Long savingsId = transaction.getSavingsAccount().getId();
		final Long productId = transaction.getSavingsAccount().productId();

		System.out.println(
				"ClientToAccountingHeaderTransferServiceImpl.createTransferEntry:: final JE Json that will be passed to create JsonCommand:"
						+ jeJson);

		final JsonCommand command = JsonCommand.from(jeJson, parsedCommand, this.fromApiJsonHelper, entityName, null,
				null, null, transferCommand.getToClientId(), loanId, savingsId, null, href, productId, null, null);
		final CommandProcessingResult result = jeWritePlatformService.createJournalEntry(command);
		return result;
	}

	private PaymentDetail generatePaymentDetailUsingTransferEntryCommand(TransferEntryCommand transferCommand) {
		final PaymentType paymentType = null;
		final String accountNumber = transferCommand.getAccountNumber();
		final String checkNumber = transferCommand.getCheckNumber();
		final String routingCode = transferCommand.getRoutingCode();
		final String receiptNumber = transferCommand.getReceiptNumber();
		final String bankNumber = transferCommand.getBankNumber();
		PaymentDetail pde = PaymentDetail.instance(paymentType, accountNumber, checkNumber, routingCode, receiptNumber,
				bankNumber);
		// Returning null for now until figuring out how to pass correct paymentType
		return null;
	}

	/**
	 * Method to generate JECommand using TransferEntryCommand. In case of
	 * additional JournalEntry is passed then the same is added to the JECommand but
	 * reversed. i.e. if the additionalJe is a credit then it is added as a debit
	 * entry and vice-versa
	 * 
	 * @param transferCommand TransferEntryCommand that is used as basis for
	 *                        JECommand
	 * @param additionalJe    JournalEntry which if passed then will be added to
	 *                        reversed to TransferEntryCommand
	 * @return JECommand
	 */
	private JECommand getJECommandFromTransferCommand(TransferEntryCommand transferCommand, JournalEntry additionalJe) {
		JECommand jeCommand = new JECommand();
		jeCommand.setLocale(transferCommand.getLocale());
		jeCommand.setDateFormat(transferCommand.getDateFormat());
		jeCommand.setOfficeId(transferCommand.getOfficeId());
		jeCommand.setTransactionDate(transferCommand.getTransactionDate());
		jeCommand.setCurrencyCode(transferCommand.getCurrencyCode());
		jeCommand.setComments(transferCommand.getTransferDescription());
		jeCommand.setReferenceNumber(transferCommand.getReceiptNumber());
		jeCommand.setAmount(transferCommand.getAmount());
		jeCommand.setPaymentTypeId(transferCommand.getPaymentTypeId());
		jeCommand.setAccountNumber(transferCommand.getAccountNumber());
		jeCommand.setCheckNumber(transferCommand.getCheckNumber());
		jeCommand.setReceiptNumber(transferCommand.getReceiptNumber());
		jeCommand.setBankNumber(transferCommand.getBankNumber());
		jeCommand.setRoutingCode(transferCommand.getRoutingCode());

		TransferDebitCreditEntryCommand[] accountingHeaders = transferCommand.getAccountingHeaders();

		List<JEDebitCreditEntryCommand> jeDebits = new ArrayList<JEDebitCreditEntryCommand>();
		List<JEDebitCreditEntryCommand> jeCredits = new ArrayList<JEDebitCreditEntryCommand>();
		for (TransferDebitCreditEntryCommand debitCreditEntryCommand : accountingHeaders) {
			JEDebitCreditEntryCommand jeCdDb = new JEDebitCreditEntryCommand();
			jeCdDb.setAmount(debitCreditEntryCommand.getAmount());
			jeCdDb.setGlAccountId(debitCreditEntryCommand.getGlAccountId());

			switch (debitCreditEntryCommand.getTypeEnum()) {
			case CREDIT:
				jeCredits.add(jeCdDb);
				break;
			case DEBIT:
				jeDebits.add(jeCdDb);
				break;
			}
		}
		if (additionalJe != null) {
			JEDebitCreditEntryCommand jeCdDb = new JEDebitCreditEntryCommand();
			jeCdDb.setAmount(additionalJe.getAmount());
			jeCdDb.setGlAccountId(additionalJe.getGlAccount().getId());

			if (additionalJe.isDebitEntry()) {
				jeCredits.add(jeCdDb);
			} else {
				jeDebits.add(jeCdDb);
			}
		}
		jeCommand.setDebits(jeDebits.toArray(new JEDebitCreditEntryCommand[jeDebits.size()]));
		jeCommand.setCredits(jeCredits.toArray(new JEDebitCreditEntryCommand[jeCredits.size()]));

		return jeCommand;
	}

	/**
	 * Helper method to convert data in a journal entry to a string format for
	 * easier printing. This is used for debugging purpose of accounting
	 * information.
	 * 
	 * @param je JournalEntry that needs to be converted
	 * @return JournalEntry data in a string format
	 */
	private String getJournalEntryAsString(JournalEntry je) {
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		sb.append("isDebitEntry: " + je.isDebitEntry());
		sb.append("\n");
		sb.append("getType: " + je.getType());
		sb.append("\n");
		sb.append("getOffice: " + je.getOffice());
		sb.append("\n");
		sb.append("getGlAccount: " + je.getGlAccount());
		sb.append("\n");
		sb.append("getTransactionDate: " + je.getTransactionDate());
		sb.append("\n");
		sb.append("getAmount: " + je.getAmount());
		sb.append("\n");
		sb.append("getReferenceNumber: " + je.getReferenceNumber());
		sb.append("\n");
		sb.append("getCurrencyCode: " + je.getCurrencyCode());
		sb.append("\n");
		if (je.getLoanTransaction() != null) {
			sb.append("getLoanTransaction.id: " + je.getLoanTransaction().getId());
			sb.append("\n");
		}
		if (je.getSavingsTransaction() != null) {
			sb.append("getLoanTransaction.id: " + je.getSavingsTransaction().getId());
			sb.append("\n");
		}

		PaymentDetail pd = je.getPaymentDetails();
		if (pd != null) {
			sb.append("PaymentDetail.getReceiptNumber: " + pd.getReceiptNumber());
			sb.append("\n");
			sb.append("PaymentDetail.getRoutingCode: " + pd.getRoutingCode());
			sb.append("\n");
			sb.append("PaymentDetail.isCashPayment: " + pd.getPaymentType().isCashPayment());
			sb.append("\n");
		}
		sb.append("getTransactionId: " + je.getTransactionId());
		sb.append("\n");
		sb.append("getClientTransaction: " + je.getClientTransaction());
		sb.append("\n");
		sb.append("getEntityId: " + je.getEntityId());
		sb.append("\n");
		sb.append("getEntityType: " + je.getEntityType());
		sb.append("\n");
		sb.append("getShareTransactionId: " + je.getShareTransactionId());
		sb.append("}");
		return sb.toString();
	}

}
