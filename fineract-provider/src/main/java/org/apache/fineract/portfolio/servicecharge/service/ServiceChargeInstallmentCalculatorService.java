package org.apache.fineract.portfolio.servicecharge.service;

public interface ServiceChargeInstallmentCalculatorService {

    void recalculateServiceChargeForGivenLoan(Long loanId, Long loanChargeId);

    void recalculateServiceChargeForAllLoans();

}
