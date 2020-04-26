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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.fineract.accounting.journalentry.command.SingleDebitOrCreditEntryCommand;
import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.useradministration.domain.AppUser;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.ideoholic.fineract.commands.JECommand;
import org.ideoholic.fineract.commands.JEDebitCreditEntryCommand;
import org.ideoholic.fineract.commands.ServiceChargeJournalEntryCommand;
import org.ideoholic.fineract.servicecharge.constants.ServiceChargeApiConstants;
import org.ideoholic.fineract.servicechargejournalentry.domain.ServiceChargeJournalEntry;
import org.ideoholic.fineract.servicechargejournalentry.domain.ServiceChargeJournalEntryRepository;
import org.ideoholic.fineract.servicechargejournalentry.serialization.ServiceChargeJournalEntryCommandFromApiJsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonElement;

@Service
public class ServiceChargeJournalEntryCreationPlatformServiceImpl
		implements ServiceChargeJournalEntryCreationPlatformService, ServiceChargeApiConstants {
	private final static Logger logger = LoggerFactory
			.getLogger(ServiceChargeJournalEntryCreationPlatformServiceImpl.class);

	private final FromJsonHelper fromApiJsonHelper;
	private final JournalEntryRepository journalEntryRepository;
	private final JournalEntryWritePlatformService jeWritePlatformService;
	private final DefaultToApiJsonSerializer<Object> apiJsonSerializerService;
	private final ServiceChargeJournalEntryRepository serviceChargeJERepository;
	private final ServiceChargeJournalEntryCommandFromApiJsonDeserializer fromApiJsonDeserializer;

	@Autowired
	public ServiceChargeJournalEntryCreationPlatformServiceImpl(final FromJsonHelper fromApiJsonHelper,
			final JournalEntryRepository journalEntryRepository,
			final JournalEntryWritePlatformService jeWritePlatformService,
			final ServiceChargeJournalEntryRepository serviceChargeJERepository,
			final DefaultToApiJsonSerializer<Object> toApiJsonSerializer,
			final ServiceChargeJournalEntryCommandFromApiJsonDeserializer fromApiJsonDeserializer) {
		this.fromApiJsonHelper = fromApiJsonHelper;
		this.jeWritePlatformService = jeWritePlatformService;
		this.journalEntryRepository = journalEntryRepository;
		this.serviceChargeJERepository = serviceChargeJERepository;
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
					"ServiceChargeJournalEntryCreationPlatformServiceImpl.createServiceChargeJournalEntry:: ServiceChargeJournalEntryCommand:"
							+ scJECommand);
			// Validate before creation for any error in data that has been sent
			scJECommand.validateForCreate();

			// From the transfer command get our own JECommand which is a map of
			// JournalEntry command
			jeCommand = getJECommandFromServiceChargeJECommand(scJECommand);
			// ObjectMapper is needed to convert object to JSON and JSON to object
			jeJson = new ObjectMapper().writeValueAsString(jeCommand);

			final JsonElement parsedCommand = this.fromApiJsonHelper.parse(jeJson);
			logger.debug(
					"ServiceChargeJournalEntryCreationPlatformServiceImpl.createServiceChargeJournalEntry:: parsed Command:"
							+ parsedCommand);

			result = createJournalEntryUsingJSON(parsedCommand, jeJson);
			final String transactionID = result.getTransactionId();
			logger.debug(
					"ServiceChargeJournalEntryCreationPlatformServiceImpl.createServiceChargeJournalEntry:: transactionID:"
							+ transactionID);
			final List<JournalEntry> jeList = journalEntryRepository
					.findUnReversedManualJournalEntriesByTransactionId(transactionID);

			final JournalEntry singleJE = jeList.get(0);

			ServiceChargeJournalEntry singleSCJE = createServiceChargeJournalEntryFromJournalEntry(scJECommand,
					singleJE);

			serviceChargeJERepository.saveAndFlush(singleSCJE);

		} catch (JsonParseException | JsonMappingException e) {
			logger.error(
					"ServiceChargeJournalEntryCreationPlatformServiceImpl.createServiceChargeJournalEntry:: JsonParseException | JsonMappingException:"
							+ e);
			logger.error("JsonParseException | JsonMappingException:", e);
			e.printStackTrace();
			error = true;
		} catch (IOException | NullPointerException e) {
			logger.error(
					"ServiceChargeJournalEntryCreationPlatformServiceImpl.createServiceChargeJournalEntry:: IOException | NullPointerException:"
							+ e);
			logger.error("IOException:", e);
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

	private ServiceChargeJournalEntry createServiceChargeJournalEntryFromJournalEntry(
			final ServiceChargeJournalEntryCommand serviceChargeJournalEntry, final JournalEntry journalEntry) {
		final Office office = journalEntry.getOffice();
		final Date transactionDate = journalEntry.getTransactionDate();
		final AppUser createdBy = journalEntry.getCreatedBy();
		final BigDecimal amount = journalEntry.getAmount();

		final BigDecimal mobilizationPercent = serviceChargeJournalEntry.getMobilization();
		final BigDecimal servicingPercent = serviceChargeJournalEntry.getServicing();
		final BigDecimal investmentPercent = serviceChargeJournalEntry.getInvestment();
		final BigDecimal overheadsPercent = serviceChargeJournalEntry.getOverheads();
		// Formula to find divided-amount is (Part-Percentage * Amount) / 100
		final BigDecimal mobilizationAmount = mobilizationPercent.multiply(amount).divide(HUNDRED);
		final BigDecimal servicingAmount = servicingPercent.multiply(amount).divide(HUNDRED);
		final BigDecimal investmentAmount = investmentPercent.multiply(amount).divide(HUNDRED);
		final BigDecimal overheadsAmount = overheadsPercent.multiply(amount).divide(HUNDRED);

		ServiceChargeJournalEntry singleSCJE = ServiceChargeJournalEntry.createNew(journalEntry, office,
				transactionDate, mobilizationPercent, servicingPercent, investmentPercent, overheadsPercent,
				mobilizationAmount, servicingAmount, investmentAmount, overheadsAmount, createdBy);

		return singleSCJE;
	}

	private CommandProcessingResult createJournalEntryUsingJSON(final JsonElement parsedCommand, final String jeJson) {
		// Constants values defined here to understand the name of value being passed
		final Long loanId = null;
		final Long savingsId = null;
		final Long productId = null;
		final Long clientId = null;
		final String entityName = "JOURNALENTRY";
		final String href = "/servicechargejournalentries";

		System.out.println(
				"ServiceChargeJournalEntryCreationPlatformServiceImpl.createJournalEntryUsingJSON:: final JE Json that will be passed to create JsonCommand:"
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
	private JECommand getJECommandFromServiceChargeJECommand(ServiceChargeJournalEntryCommand scJECommand) {
		System.out.println(
				"ServiceChargeJournalEntryCreationPlatformServiceImpl.getJECommandFromServiceChargeJECommand::scJECommand:"
						+ scJECommand);
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

		for (SingleDebitOrCreditEntryCommand debitCommand : scJECommand.getDebits()) {
			JEDebitCreditEntryCommand jeDebitCommand = new JEDebitCreditEntryCommand();
			jeDebitCommand.setGlAccountId(debitCommand.getGlAccountId());
			jeDebitCommand.setAmount(debitCommand.getAmount());
			jeDebitCommand.setComments(debitCommand.getComments());
			jeDebits.add(jeDebitCommand);
		}

		for (SingleDebitOrCreditEntryCommand creditCommand : scJECommand.getCredits()) {
			JEDebitCreditEntryCommand jeCreditCommand = new JEDebitCreditEntryCommand();
			jeCreditCommand.setGlAccountId(creditCommand.getGlAccountId());
			jeCreditCommand.setAmount(creditCommand.getAmount());
			jeCreditCommand.setComments(creditCommand.getComments());
			jeCredits.add(jeCreditCommand);
		}

		jeCommand.setDebits(jeDebits.toArray(new JEDebitCreditEntryCommand[jeDebits.size()]));
		jeCommand.setCredits(jeCredits.toArray(new JEDebitCreditEntryCommand[jeCredits.size()]));

		return jeCommand;
	}

}
