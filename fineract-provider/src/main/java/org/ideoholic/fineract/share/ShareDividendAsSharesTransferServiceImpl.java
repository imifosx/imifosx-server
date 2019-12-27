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
package org.ideoholic.fineract.share;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.portfolio.shareaccounts.domain.PurchasedSharesStatusType;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccountDividendDetails;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccountStatusType;
import org.apache.fineract.portfolio.shareaccounts.service.ShareAccountWritePlatformService;
import org.apache.fineract.portfolio.shareproducts.domain.ShareProduct;
import org.apache.fineract.portfolio.shareproducts.domain.ShareProductDividendPayOutDetails;
import org.apache.fineract.portfolio.shareproducts.domain.ShareProductRepository;
import org.apache.fineract.portfolio.shareproducts.exception.DividentProcessingException;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@Service
public class ShareDividendAsSharesTransferServiceImpl implements ShareDividendAsSharesTransferService {

    private final ShareProductRepository shareProductRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    private final ShareAccountWritePlatformService shareAccountWritePlatformService;

    @Autowired
    public ShareDividendAsSharesTransferServiceImpl(final RoutingDataSource dataSource, final ShareProductRepository shareProductRepository,
            final ShareAccountWritePlatformService shareAccountWritePlatformService) {
        this.shareProductRepository = shareProductRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.shareAccountWritePlatformService = shareAccountWritePlatformService;
    }

    @Override
    public void validateIfDividendCanBeTransferredAsShares(final ShareProduct shareProduct, BigDecimal dividendAmount,
            final LocalDate startDate) {

        validateIfDividendCanBeTransferredAsShares(shareProduct, dividendAmount,
                shareProduct.getAllowDividendCalculationForInactiveClients(), startDate);
    }

    @Override
    public void validateIfDividendCanBeTransferredAsShares(final ShareProduct shareProduct, BigDecimal dividendAmount,
            final boolean fetchInActiveAccounts, final LocalDate startDate) {
        Long numberOfShareAccounts = fetchCountOfShareAccountsForShareProduct(shareProduct.getId(), fetchInActiveAccounts, startDate);
        BigDecimal unitPrice = shareProduct.getUnitPrice();
        BigDecimal numberOfSharesForDividendAmount = dividendAmount.divide(unitPrice);
        if (numberOfSharesForDividendAmount.doubleValue() < 0) {

        throw new DividentProcessingException("dividend.amount.cannot.divide", "Dividend amount cannot be divided over the shares"); }
        BigDecimal numberOfSharesPerClient = numberOfSharesForDividendAmount.divide(new BigDecimal(numberOfShareAccounts));
        if (numberOfSharesPerClient.doubleValue() < 0) {

        throw new DividentProcessingException("shares.number.less.count", "The number of shares is less than the needed count"); }
    }

    @Override
    public BigDecimal validateDividendTransferAsShares(final ShareProduct shareProduct, BigDecimal dividendAmount) {

        if (dividendAmount.compareTo(BigDecimal.ZERO) == 0) { return BigDecimal.ZERO; }

        BigDecimal unitPrice = shareProduct.getUnitPrice();
        BigDecimal numberOfSharesForDividendAmount = dividendAmount.divide(unitPrice);
        if (numberOfSharesForDividendAmount.doubleValue() < 1.000d) { throw new DividentProcessingException("dividend.amount.cannot.divide",
                "dividend amount cannot be divided over the shares"); }
        return numberOfSharesForDividendAmount;
    }

    @Override
    public ShareProductDividendPayOutDetails transferSharesAndReturnRemainingDividend(String cashOrShare, Long productId,
            ShareProductDividendPayOutDetails dividendPayOutDetails) {

        if ("cash".equalsIgnoreCase(cashOrShare)) { return dividendPayOutDetails; }

        ShareProduct product = this.shareProductRepository.findOne(productId);
        Map<Long, JsonCommand> applyAdditionalShareMap = new HashMap<Long, JsonCommand>();
        int totalSharesAsDividend = 0;

        for (ShareAccountDividendDetails shareAccountDividendDetails : dividendPayOutDetails.getAccountDividendDetails()) {
            BigDecimal numberOfSharesForDividendAmount = validateDividendTransferAsShares(product, shareAccountDividendDetails.getAmount());
            int noOfShares = numberOfSharesForDividendAmount.intValue();
            totalSharesAsDividend = totalSharesAsDividend + noOfShares;

            // Add shares
            LocalDate todayDate = new LocalDate();
            DateTimeFormatter fmt = DateTimeFormat.forPattern("dd MMMM yyyy");
            String requestedDate = todayDate.toString(fmt);
            JsonElement jsonElement = new JsonParser().parse("{\"unitPrice\":" + product.getUnitPrice() + ",\"requestedDate\":\""
                    + requestedDate + "\",\"requestedShares\":\"" + noOfShares + "\",\"locale\":\"en\",\"dateFormat\":\"dd MMMM yyyy\"}");
            JsonCommand jsonCommand = JsonCommand.from(
                    "{\"unitPrice\":" + product.getUnitPrice() + ",\"requestedDate\":\"" + requestedDate + "\",\"requestedShares\":\""
                            + noOfShares + "\",\"locale\":\"en\",\"dateFormat\":\"dd MMMM yyyy\"}",
                    jsonElement, null, null, null, null, null, null, null, null, null, null, null, null, null);
            applyAdditionalShareMap.put(shareAccountDividendDetails.getShareAccountId(), jsonCommand);

            double remainingAmount = numberOfSharesForDividendAmount.doubleValue() - noOfShares;
            BigDecimal amountToDeposit = new BigDecimal(remainingAmount).multiply(product.getUnitPrice());
            shareAccountDividendDetails.setAmount(amountToDeposit);
        }

        // Now get the product details of subscribed and issuable shares
        Long totalSharesIssuable = product.getSharesIssued();
        if (totalSharesIssuable == null) {
            totalSharesIssuable = product.getTotalShares();
        } else {
            totalSharesIssuable = product.getTotalShares() - totalSharesIssuable;
        }

        if (totalSharesAsDividend > totalSharesIssuable) { throw new DividentProcessingException("shares.not.enough",
                "Not enough shares to be divided among all the client"); }

        applyAdditionalShareMap.forEach((key, value) -> shareAccountWritePlatformService.applyAddtionalShares(key, value));

        return dividendPayOutDetails;
    }

    private Long fetchCountOfShareAccountsForShareProduct(final Long productId, final boolean fetchInActiveAccounts,
            final LocalDate startDate) {

        StringBuilder query = new StringBuilder("select ");
        query.append("count(*) ");
        // sb.append(purchasedSharesDataRowMapper.schema());
        query.append(" from m_share_account sa ");
        query.append(" join m_client c ON c.id = sa.client_id ");
        query.append(" join m_share_account_transactions saps ON saps.account_id = sa.id ");
        query.append(" where sa.product_id = ? ");

        List<Object> params = new ArrayList<>(3);
        params.add(productId);
        params.add(ShareAccountStatusType.ACTIVE.getValue());

        if (fetchInActiveAccounts) {
            query.append(" and (sa.status_enum = ? or (sa.status_enum = ? ");
            query.append(" and sa.closed_date >  ?)) ");
            params.add(ShareAccountStatusType.CLOSED.getValue());
            params.add(formatter.print(startDate));
        } else {
            query.append(" and sa.status_enum = ? ");
        }
        query.append(" and saps.status_enum = ?");
        params.add(PurchasedSharesStatusType.APPROVED.getValue());
        Object[] whereClauseItems = params.toArray();

        @SuppressWarnings("deprecation")
        final long count = this.jdbcTemplate.queryForLong(query.toString(), whereClauseItems);
        return count;
    }

}
