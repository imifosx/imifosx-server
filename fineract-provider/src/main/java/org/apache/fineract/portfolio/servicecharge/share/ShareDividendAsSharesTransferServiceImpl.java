package org.apache.fineract.portfolio.servicecharge.share;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.fineract.portfolio.shareaccounts.domain.PurchasedSharesStatusType;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccountStatusType;
import org.apache.fineract.portfolio.shareproducts.domain.ShareProduct;
import org.apache.fineract.portfolio.shareproducts.domain.ShareProductRepository;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShareDividendAsSharesTransferServiceImpl implements ShareDividendAsSharesTransferService {

    private final ShareProductRepository shareProductRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    @Autowired
    public ShareDividendAsSharesTransferServiceImpl(final ShareProductRepository shareProductRepository, final JdbcTemplate jdbcTemplate) {
        this.shareProductRepository = shareProductRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void validateIfDividendCanBeTransferredAsShares(final Long productId, BigDecimal dividendAmount,
            final boolean fetchInActiveAccounts, final LocalDate startDate) {
        ShareProduct product = this.shareProductRepository.findOne(productId);
        validateIfDividendCanBeTransferredAsShares(product, dividendAmount, fetchInActiveAccounts, startDate);
    }

    @Override
    public void validateIfDividendCanBeTransferredAsShares(final ShareProduct shareProduct, BigDecimal dividendAmount,
            final boolean fetchInActiveAccounts, final LocalDate startDate) {
        Long numberOfShareAccounts = fetchCountOfShareAccountsForShareProduct(shareProduct.getId(), fetchInActiveAccounts, startDate);
        BigDecimal unitPrice = shareProduct.getUnitPrice();
        BigDecimal numberOfSharesForDividendAmount = dividendAmount.divide(unitPrice);
        if (numberOfSharesForDividendAmount.doubleValue() < 0) {
            // Need to throw exception here saying that dividend amount cannot
            // be divided over the shares
        }
        BigDecimal numberOfSharesPerClient = numberOfSharesForDividendAmount.divide(new BigDecimal(numberOfShareAccounts));
        if (numberOfSharesPerClient.doubleValue() < 0) {
            // Need to throw exception here saying that the number of shares is
            // less than the needed count
        }
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
