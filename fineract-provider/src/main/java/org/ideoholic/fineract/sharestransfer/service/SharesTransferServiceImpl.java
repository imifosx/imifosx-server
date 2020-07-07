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
package org.ideoholic.fineract.sharestransfer.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormatRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.portfolio.accounts.constants.ShareAccountApiConstants;
import org.apache.fineract.portfolio.client.domain.AccountNumberGenerator;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentType;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentTypeRepositoryWrapper;
import org.apache.fineract.portfolio.savings.SavingsTransactionBooleanValues;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountDomainService;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccount;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccountRepositoryWrapper;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccountTransaction;
import org.apache.fineract.portfolio.shareaccounts.serialization.ShareAccountDataSerializer;
import org.apache.fineract.portfolio.shareproducts.domain.ShareProduct;
import org.apache.fineract.portfolio.shareproducts.domain.ShareProductRepositoryWrapper;
import org.apache.fineract.useradministration.domain.AppUser;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.ideoholic.fineract.commands.JECommand;
import org.ideoholic.fineract.commands.JEDebitCreditEntryCommand;
import org.ideoholic.fineract.commands.TransferDebitCreditEntryCommand;
import org.ideoholic.fineract.commands.TransferEntryCommand;
import org.ideoholic.fineract.servicecharge.constants.ServiceChargeApiConstants;
import org.ideoholic.fineract.servicechargejournalentry.domain.ServiceChargeJournalEntry;
import org.ideoholic.fineract.servicechargejournalentry.domain.ServiceChargeJournalEntryRepository;
import org.ideoholic.fineract.sharestransfer.constants.SharesTransferApiConstants;
import org.ideoholic.fineract.sharestransfer.domain.ShareAccountCertificateRepositoryWrapper;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.google.gson.JsonElement;

@Service
public class SharesTransferServiceImpl
		implements SharesTransferService, SharesTransferApiConstants {
	private final static Logger logger = LoggerFactory.getLogger(SharesTransferServiceImpl.class);

	    private final ShareAccountCertificateRepositoryWrapper shareAccountCertificateRepositoryWrapper;


	@Autowired
	public SharesTransferServiceImpl(final ShareAccountCertificateRepositoryWrapper shareAccountCertificateRepositoryWrapper) {
		this.shareAccountCertificateRepositoryWrapper = shareAccountCertificateRepositoryWrapper;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ideoholic.fineract.share.service.
	 * ShareTransferService#createTransferEntry()
	 */
	@Override
	public String updateSharesCertificate(String json) {
		
		Long shareAccountIdFrom = 0l;
		Long shareAccountIdTo = 0l;
		this.shareAccountCertificateRepositoryWrapper.update(shareAccountIdFrom,shareAccountIdTo);
		/*
        try {
        	
        	logger.debug("SharesTransferServiceImpl.createSharesTransferEntry:: passed " + "jsonCommand:"
					+ json);
        	
            ShareAccount account = this.shareAccountRepository.findOneWithNotFoundDetection(accountId);
            Map<String, Object> changes = this.accountDataSerializer.validateAndRedeemShares(jsonCommand, account);
            if (!changes.isEmpty()) {
                this.shareAccountRepository.save(account);
                ShareAccountTransaction transaction = (ShareAccountTransaction) changes
                        .get(ShareAccountApiConstants.requestedshares_paramname);
             // after saving, entity will have different object. So need to retrieve the entity object
                transaction = account.getShareAccountTransaction(transaction); 
                Long redeemShares = transaction.getTotalShares() ;
                ShareProduct shareProduct = account.getShareProduct() ;
                //remove the redeem shares from total subscribed shares 
                shareProduct.removeSubscribedShares(redeemShares); 
                this.shareProductRepository.save(shareProduct);
                
                Set<ShareAccountTransaction> transactions = new HashSet<>();
                transactions.add(transaction);
                //this.journalEntryWritePlatformService.createJournalEntriesForShares(populateJournalEntries(account, transactions));
                changes.clear();
                changes.put(ShareAccountApiConstants.requestedshares_paramname, transaction.getId());

            }
            return new CommandProcessingResultBuilder() //
                    .withCommandId(jsonCommand.commandId()) //
                    .withEntityId(accountId) //
                    .with(changes) //
                    .build();
        } catch (DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(jsonCommand, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        }
    */return null;}

}
