package org.apache.fineract.portfolio.servicecharge.share;

import java.math.BigDecimal;

import org.apache.fineract.portfolio.shareproducts.domain.ShareProduct;
import org.joda.time.LocalDate;

public interface ShareDividendAsSharesTransferService {

    void validateIfDividendCanBeTransferredAsShares(final Long productId, BigDecimal dividendAmount, final boolean fetchInActiveAccounts,
            final LocalDate startDate);

    void validateIfDividendCanBeTransferredAsShares(final ShareProduct shareProduct, BigDecimal dividendAmount,
            final boolean fetchInActiveAccounts, final LocalDate startDate);
}
