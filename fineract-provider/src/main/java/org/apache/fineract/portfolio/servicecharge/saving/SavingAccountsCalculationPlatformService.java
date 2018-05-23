package org.apache.fineract.portfolio.servicecharge.saving;

import java.math.BigDecimal;

public interface SavingAccountsCalculationPlatformService {

    /**
     * Saving Accounts average savings calculation method for a given period
     * 
     * @return Average savings
     */
    BigDecimal calculateAverageSavings();

    void validateDepositUpperLimit(BigDecimal accountBalance, Long savingsId);

}
