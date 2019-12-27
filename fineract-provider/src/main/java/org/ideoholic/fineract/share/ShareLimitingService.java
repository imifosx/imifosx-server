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

import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccount;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccountTransaction;

public interface ShareLimitingService {

    String SHARE_BUYING_LIMIT_CONFIG_STR = "Share-Buying-Limit";
    String SHARE_WITHDRAW_LIMIT_CONFIG_STR = "Share-Withdrawal-Limit";

    void validateSharesRedemptionWithPaidupCapital(ShareAccount account, ShareAccountTransaction transaction,
            DataValidatorBuilder baseDataValidator);

    void validateSharesSubscriptionWithPaidupCapital(ShareAccount account, ShareAccountTransaction transaction,
            DataValidatorBuilder baseDataValidator);
}
