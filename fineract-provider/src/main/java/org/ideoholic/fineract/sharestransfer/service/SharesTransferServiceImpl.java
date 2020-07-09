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

import org.ideoholic.fineract.sharestransfer.constants.SharesTransferApiConstants;
import org.ideoholic.fineract.sharestransfer.domain.ShareAccountCertificateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SharesTransferServiceImpl
		implements SharesTransferService, SharesTransferApiConstants {
	private final static Logger logger = LoggerFactory.getLogger(SharesTransferServiceImpl.class);

	private final ShareAccountCertificateRepository shareAccountCertificateRepository;


	@Autowired
	public SharesTransferServiceImpl(final ShareAccountCertificateRepository shareAccountCertificateRepository) {
		this.shareAccountCertificateRepository = shareAccountCertificateRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ideoholic.fineract.share.service.
	 * ShareTransferService#createTransferEntry()
	 */
	@Override
	public String updateSharesCertificate(String json) {
		
		Long shareAccountIdFrom = Long.parseLong("18");
		Long shareAccountIdTo = Long.parseLong("16");
		this.shareAccountCertificateRepository.update(shareAccountIdFrom,shareAccountIdTo);
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
