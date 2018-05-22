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
package org.apache.fineract.portfolio.servicecharge.share;

import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.portfolio.accounts.constants.ShareAccountApiConstants;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccount;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccountTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShareLimitingServiceImpl implements ShareLimitingService {

    private final ConfigurationDomainService configurationDomainService;

    @Autowired
    public ShareLimitingServiceImpl(final ConfigurationDomainService configurationDomainService) {
        this.configurationDomainService = configurationDomainService;
    }

    @Override
    public void validateSharesRedemptionWithPaidupCapital(final ShareAccount account, final ShareAccountTransaction transaction,
            final DataValidatorBuilder baseDataValidator) {

        // If the feature of limiting share withdrawal is not enabled then skip
        // further processing
        if (!configurationDomainService.isShareWithdrawalLimitEnabled()) { return; }

        Long requested = new Long(0);
        if (transaction.isActive() && transaction.isPendingForApprovalTransaction()) {
            requested += transaction.getTotalShares();
        }

        // Get the share withdrawal limit percentage
        int shareLimitPercent = configurationDomainService.retrieveShareWithdrawalLimitPercent();
        if (shareLimitPercent > 100) {
            baseDataValidator.reset().parameter(SHARE_WITHDRAW_LIMIT_CONFIG_STR).value(requested)
                    .failWithCodeNoParameterAddedToErrorCode("shares.requested.can.not.be.approved.exceeding.totalshares.issuable");
        }
    }

    @Override
    public void validateSharesSubscriptionWithPaidupCapital(final ShareAccount account, final ShareAccountTransaction transaction,
            final DataValidatorBuilder baseDataValidator) {

        // If the feature of limiting share subscription is not enabled then
        // skip further processing
        if (!configurationDomainService.isShareBuyingLimitEnabled()) { return; }

        Long requested = new Long(0);
        if (transaction.isActive() && transaction.isPendingForApprovalTransaction()) {
            requested += transaction.getTotalShares();
        }
        // Number of shares that the client has = requested + current
        Long approvedShares = account.getTotalApprovedShares();
        if (approvedShares == null) {
            approvedShares = new Long(0);
        }
        Long totalShares = requested + approvedShares;

        // Now get the product details of subscribed and issuable shares
        Long totalSharesIssuable = account.getShareProduct().getSharesIssued();
        if (totalSharesIssuable == null) totalSharesIssuable = account.getShareProduct().getTotalShares();

        // Get the share withdrawal limit percentage
        int shareLimitPercent = configurationDomainService.retrieveShareBuyingLimitPercent();
        if (shareLimitPercent > 100) {
            baseDataValidator.reset().parameter(SHARE_BUYING_LIMIT_CONFIG_STR).value(requested)
                    .failWithCodeNoParameterAddedToErrorCode("shares.limit.percent.configured.is.greater.than.hundred");
            return; // Cannot process further with this error
        }

        // Find the number of shares one can subscribe
        Long subscribableShareLimit = (long)(((float)shareLimitPercent / 100) * totalSharesIssuable);
        if (totalShares > subscribableShareLimit) {
            baseDataValidator.reset().parameter(SHARE_BUYING_LIMIT_CONFIG_STR).value(requested)
                    .failWithCodeNoParameterAddedToErrorCode("shares.subscribed.exceeds.configured.share.limit");
        }
    }

}
