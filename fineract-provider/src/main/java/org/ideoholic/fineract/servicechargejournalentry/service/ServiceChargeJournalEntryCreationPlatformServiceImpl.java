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
package org.ideoholic.fineract.servicechargejournalentry.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountDomainService;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.ideoholic.fineract.commands.JECommand;
import org.ideoholic.fineract.commands.JEDebitCreditEntryCommand;
import org.ideoholic.fineract.commands.ServiceChargeJournalEntryCommand;
import org.ideoholic.fineract.servicechargejournalentry.serialization.ServiceChargeJournalEntryCommandFromApiJsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonElement;

@Service
public class ServiceChargeJournalEntryCreationPlatformServiceImpl
		implements ServiceChargeJournalEntryCreationPlatformService {
	private final static Logger logger = LoggerFactory
			.getLogger(ServiceChargeJournalEntryCreationPlatformServiceImpl.class);

	private final FromJsonHelper fromApiJsonHelper;
	private final JournalEntryWritePlatformService jeWritePlatformService;
	private final DefaultToApiJsonSerializer<Object> apiJsonSerializerService;
	 private final ServiceChargeJournalEntryCommandFromApiJsonDeserializer fromApiJsonDeserializer;

	@Autowired
	public ServiceChargeJournalEntryCreationPlatformServiceImpl(final FromJsonHelper fromApiJsonHelper,
			final SavingsAccountAssembler savingsAccountAssembler,
			final JournalEntryRepository glJournalEntryRepository,
			final SavingsAccountDomainService savingsAccountDomainService,
			final JournalEntryWritePlatformService jeWritePlatformService,
			final DefaultToApiJsonSerializer<Object> toApiJsonSerializer,
			final ServiceChargeJournalEntryCommandFromApiJsonDeserializer fromApiJsonDeserializer) {
		this.fromApiJsonHelper = fromApiJsonHelper;
		this.jeWritePlatformService = jeWritePlatformService;
		this.apiJsonSerializerService = toApiJsonSerializer;
		this.fromApiJsonDeserializer = fromApiJsonDeserializer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ideoholic.fineract.servicechargejournalentry.service.
	 * ServiceChargeWritePlatformService#createServiceChargeJournalEntry()
	 */
	@Override
	public String createServiceChargeJournalEntry(String json) {
		boolean error = false;
		String jeJson = null;
		JECommand jeCommand = null;
		CommandProcessingResult result = null;
		try {

			logger.debug(
					"ServiceChargeJournalEntryCreationPlatformServiceImpl.createServiceChargeJournalEntry:: passed "
							+ "jsonCommand:" + json);

			// Convert passed JSON into our own TransferEntryCommand
			final ServiceChargeJournalEntryCommand scJECommand = fromApiJsonDeserializer.commandFromApiJson(json);
			logger.debug(
					"ServiceChargeJournalEntryCreationPlatformServiceImpl.createServiceChargeJournalEntry:: transferCommand:"
							+ scJECommand);
			// Validate before creation for any error in data that has been sent
			scJECommand.validateForCreate();

			// From the transfer command get our own JECommand which is a map of
			// JournalEntry command
			jeCommand = getJECommandFromServiceChargeJECommand(scJECommand, null);
			// ObjectMapper is needed to convert object to JSON and JSON to object
			jeJson = new ObjectMapper().writeValueAsString(jeCommand);

			final JsonElement parsedCommand = this.fromApiJsonHelper.parse(jeJson);
			logger.debug("ServiceChargeJournalEntryCreationPlatformServiceImpl.createTransferEntry:: parsed Command:"
					+ parsedCommand);

			transferAmountWithAccountingHeader(parsedCommand, jeJson);

		} catch (JsonParseException | JsonMappingException e) {
			logger.error(
					"ServiceChargeJournalEntryCreationPlatformServiceImpl.createServiceChargeJournalEntry:: JsonParseException | JsonMappingException:"
							+ e);
			e.printStackTrace();
			error = true;
		} catch (IOException | NullPointerException e) {
			logger.error(
					"ServiceChargeJournalEntryCreationPlatformServiceImpl.createServiceChargeJournalEntry:: IOException | NullPointerException:"
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
					"Invalid JSON request format has been passed for passing Service Charge Journal Entry request");
		}
		return this.apiJsonSerializerService.serialize(result);
	}

	private CommandProcessingResult transferAmountWithAccountingHeader(final JsonElement parsedCommand,
			final String jeJson) {
		// Constants values defined here to understand the name of value being passed
		final Long loanId = null;
		final Long savingsId = null;
		final Long productId = null;
		final Long clientId = null;
		final String entityName = "JOURNALENTRY";
		final String href = "/servicechargejournalentries";

		System.out.println(
				"ServiceChargeJournalEntryCreationPlatformServiceImpl.createTransferEntry:: final JE Json that will be passed to create JsonCommand:"
						+ jeJson);

		final JsonCommand command = JsonCommand.from(jeJson, parsedCommand, this.fromApiJsonHelper, entityName, null,
				null, null, clientId, loanId, savingsId, null, href, productId, null, null);
		final CommandProcessingResult result = jeWritePlatformService.createJournalEntry(command);
		return result;
	}

	/**
	 * Method to generate JECommand using TransferEntryCommand. In case of
	 * additional JournalEntry is passed then the same is added to the JECommand but
	 * reversed. i.e. if the additionalJe is a credit then it is added as a debit
	 * entry and vice-versa
	 * 
	 * @param scJECommand  TransferEntryCommand that is used as basis for JECommand
	 * @param additionalJe JournalEntry which if passed then will be added to
	 *                     reversed to TransferEntryCommand
	 * @return JECommand
	 */
	private JECommand getJECommandFromServiceChargeJECommand(ServiceChargeJournalEntryCommand scJECommand,
			JournalEntry additionalJe) {
		JECommand jeCommand = new JECommand();
		jeCommand.setLocale(scJECommand.getLocale());
		jeCommand.setDateFormat(scJECommand.getDateFormat());
		jeCommand.setOfficeId(scJECommand.getOfficeId());
		jeCommand.setTransactionDate(scJECommand.getTransactionDate());
		jeCommand.setCurrencyCode(scJECommand.getCurrencyCode());
		jeCommand.setComments(scJECommand.getComments());
		jeCommand.setReferenceNumber(scJECommand.getReceiptNumber());
		jeCommand.setAmount(scJECommand.getAmount());
		jeCommand.setPaymentTypeId(scJECommand.getPaymentTypeId());
		jeCommand.setAccountNumber(scJECommand.getAccountNumber());
		jeCommand.setCheckNumber(scJECommand.getCheckNumber());
		jeCommand.setReceiptNumber(scJECommand.getReceiptNumber());
		jeCommand.setBankNumber(scJECommand.getBankNumber());
		jeCommand.setRoutingCode(scJECommand.getRoutingCode());

		List<JEDebitCreditEntryCommand> jeDebits = new ArrayList<JEDebitCreditEntryCommand>();
		List<JEDebitCreditEntryCommand> jeCredits = new ArrayList<JEDebitCreditEntryCommand>();

		for (JEDebitCreditEntryCommand debitCommand : scJECommand.getDebits()) {
			jeDebits.add(debitCommand);
		}

		for (JEDebitCreditEntryCommand creditCommand : scJECommand.getCredits()) {
			jeDebits.add(creditCommand);
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
