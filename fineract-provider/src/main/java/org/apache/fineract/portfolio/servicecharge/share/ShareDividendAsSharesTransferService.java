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

import org.apache.fineract.portfolio.shareproducts.domain.ShareProduct;
import org.apache.fineract.portfolio.shareproducts.domain.ShareProductDividendPayOutDetails;
import org.joda.time.LocalDate;

public interface ShareDividendAsSharesTransferService {

    void validateIfDividendCanBeTransferredAsShares(final ShareProduct shareProduct, BigDecimal dividendAmount, final LocalDate startDate);

    void validateIfDividendCanBeTransferredAsShares(final ShareProduct shareProduct, BigDecimal dividendAmount,
            final boolean fetchInActiveAccounts, final LocalDate startDate);

    ShareProductDividendPayOutDetails transferSharesAndReturnRemainingDividend(String cashOrShare, Long productId, ShareProductDividendPayOutDetails dividendPayOutDetails);

    BigDecimal validateDividendTransferAsShares(ShareProduct shareProduct, BigDecimal dividendAmount);

}
