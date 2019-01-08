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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.portfolio.servicecharge.util.Pair;
import org.apache.fineract.portfolio.servicecharge.util.ServiceChargeDateUtils;
import org.apache.fineract.portfolio.shareaccounts.data.ShareAccountTransactionData;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccount;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccountTransaction;
import org.apache.fineract.portfolio.shareaccounts.service.SharesEnumerations;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;


@Service
public class ShareLimitingServiceImpl implements ShareLimitingService {

    private final ConfigurationDomainService configurationDomainService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ShareLimitingServiceImpl(final RoutingDataSource dataSource, final ConfigurationDomainService configurationDomainService) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.configurationDomainService = configurationDomainService;
    }

    @Override
    public void validateSharesSubscriptionWithPaidupCapital(final ShareAccount account, final ShareAccountTransaction transaction,
            final DataValidatorBuilder baseDataValidator) {

        // If the feature of limiting share subscription is not enabled then
        // skip further processing
        if (!configurationDomainService.isShareBuyingLimitEnabled()) { return; }

        // Get the share withdrawal limit percentage
        int shareLimitPercent = configurationDomainService.retrieveShareBuyingLimitPercent();
        if (shareLimitPercent > 100) {
            baseDataValidator.reset().parameter(SHARE_BUYING_LIMIT_CONFIG_STR).value(shareLimitPercent)
                    .failWithCodeNoParameterAddedToErrorCode("shares.subscription.limit.percent.configured.is.greater.than.hundred");
            return; // Cannot process further with this error
        }

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

        // Find the number of shares one can subscribe
        Long subscribableShareLimit = (long) (((float) shareLimitPercent / 100) * totalSharesIssuable);
        if (totalShares > subscribableShareLimit) {
            baseDataValidator.reset().parameter(SHARE_BUYING_LIMIT_CONFIG_STR).value(requested)
                    .failWithCodeNoParameterAddedToErrorCode("shares.subscribed.exceeds.configured.share.limit");
        }
    }

    @Override
    public void validateSharesRedemptionWithPaidupCapital(final ShareAccount account, final ShareAccountTransaction transaction,
            final DataValidatorBuilder baseDataValidator) {

        // If the feature of limiting share withdrawal is not enabled then skip
        // further processing
        if (!configurationDomainService.isShareWithdrawalLimitEnabled()) { return; }

        // Get the share withdrawal limit percentage
        int shareLimitPercent = configurationDomainService.retrieveShareWithdrawalLimitPercent();
        if (shareLimitPercent > 100) {
            baseDataValidator.reset().parameter(SHARE_WITHDRAW_LIMIT_CONFIG_STR).value(shareLimitPercent)
                    .failWithCodeNoParameterAddedToErrorCode("shares.redemption.limit.percent.configured.is.greater.than.hundred");
        }

        // Loop over the share transactions
        Collection<ShareAccountTransactionData> shareProductTransactions = retrieveShareProductAccountRedeemTransactionDataForCurrentFinancialYear(
                account.getShareProduct().getId());
        // Now that we have the total share withdrawal transactions, get the sum
        // of the values
        Long redeemedShares = new Long(0);
        for (ShareAccountTransactionData shareAccountTransaction : shareProductTransactions) {
            redeemedShares += shareAccountTransaction.getNumberOfShares();
        }

        // Final amount on which to check is also includes currently redeem
        // requested shares
        Long requested = new Long(0);
        if (transaction.isActive() && transaction.isRedeemTransaction()) {
            requested += transaction.getTotalShares();
            redeemedShares += requested;
        }

        // Now get the product details of subscribed and issuable shares
        Long totalSharesIssuable = account.getShareProduct().getSharesIssued();
        if (totalSharesIssuable == null) totalSharesIssuable = account.getShareProduct().getTotalShares();
        // Find the number of shares one can subscribe
        Long redeemableShareLimit = (long) (((float) shareLimitPercent / 100) * totalSharesIssuable);
        if (redeemedShares > redeemableShareLimit) {
            baseDataValidator.reset().parameter(SHARE_BUYING_LIMIT_CONFIG_STR).value(requested)
                    .failWithCodeNoParameterAddedToErrorCode("shares.subscribed.exceeds.configured.share.limit");
        }
    }

    private Collection<ShareAccountTransactionData> retrieveShareProductAccountRedeemTransactionData(Long productId) {
        ShareProductAccountTransactionDataRowMapper mapper = new ShareProductAccountTransactionDataRowMapper();
        // To get only active active transactions is_active = 1 and active
        // accounts status_enum = 300. See {@link ShareAccountStatusType}
        final String sql = "select " + mapper.schema()
                + " where sapr.id = ? and saps.is_active = 1 and sacc.status_enum = 300 and saps.type_enum = 600";
        return this.jdbcTemplate.query(sql, mapper, new Object[] { productId });
    }

    /**
     * Based on the product ID returns the share account transactions only for
     * the current financial year.<br/>
     * The conditions are:<br/>
     * Transaction should be active (is_active = 1) <br/>
     * Account should be active (status_enum = 300) <br/>
     * Share transaction should be approved (type_enum = 600) <br/>
     * 
     * <b>Note</b>: The current financial year here implies from 1st April to
     * 31st March
     * 
     * @param productId
     * @return Collection of ShareAccountTransactionData
     */
    private Collection<ShareAccountTransactionData> retrieveShareProductAccountRedeemTransactionDataForCurrentFinancialYear(
            Long productId) {
        Pair<String, String> currentFinancialYearDatePair = ServiceChargeDateUtils.getCurrentFinancialYearDatePair();
        ShareProductAccountTransactionDataRowMapper mapper = new ShareProductAccountTransactionDataRowMapper();
        // To get only active active transactions is_active = 1 and active
        // accounts status_enum = 300. See {@link ShareAccountStatusType}
        final String sql = "select " + mapper.schema()
                + " where sapr.id = ? and saps.is_active = 1 and sacc.status_enum = 300 and saps.type_enum = 600"
                + " and (transaction_date between '" + currentFinancialYearDatePair.getFirst() + "' and '"
                + currentFinancialYearDatePair.getSecond() + "')";
        return this.jdbcTemplate.query(sql, mapper, new Object[] { productId });
    }

    private final static class ShareProductAccountTransactionDataRowMapper implements RowMapper<ShareAccountTransactionData> {

        private final String schema;

        public ShareProductAccountTransactionDataRowMapper() {
            StringBuffer buff = new StringBuffer()
                    .append("saps.id, saps.account_id, saps.transaction_date, saps.total_shares, saps.unit_price, ")
                    .append("saps.status_enum, saps.type_enum, saps.amount, saps.charge_amount as chargeamount, ")
                    .append("saps.amount_paid as amountPaid").append(" from m_share_account_transactions saps ")
                    .append("INNER JOIN m_share_account sacc ON saps.account_id = sacc.id ")
                    .append("INNER JOIN m_share_product sapr ON sacc.product_id = sapr.id");
            schema = buff.toString();
        }

        @Override
        public ShareAccountTransactionData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long accountId = rs.getLong("account_id");
            final LocalDate purchasedDate = new LocalDate(rs.getDate("transaction_date"));
            final Long numberOfShares = JdbcSupport.getLong(rs, "total_shares");
            final BigDecimal purchasedPrice = rs.getBigDecimal("unit_price");
            final Integer status = rs.getInt("status_enum");
            final EnumOptionData statusEnum = SharesEnumerations.purchasedSharesEnum(status);
            final Integer type = rs.getInt("type_enum");
            final EnumOptionData typeEnum = SharesEnumerations.purchasedSharesEnum(type);
            final BigDecimal amount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "amount");
            final BigDecimal chargeAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "chargeamount");
            final BigDecimal amountPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "amountPaid");

            return new ShareAccountTransactionData(id, accountId, purchasedDate, numberOfShares, purchasedPrice, statusEnum, typeEnum,
                    amount, chargeAmount, amountPaid);
        }

        public String schema() {
            return this.schema;
        }
    }

}
