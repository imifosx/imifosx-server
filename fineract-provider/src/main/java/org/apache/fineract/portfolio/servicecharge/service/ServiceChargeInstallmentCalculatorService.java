package org.apache.fineract.portfolio.servicecharge.service;

import java.util.List;

import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;

public interface ServiceChargeInstallmentCalculatorService {

    void recalculateServiceChargeForGivenLoan(Long loanId, Long loanChargeId);

    List<LoanRepaymentScheduleInstallment> recalculateServiceChargeForGivenLoan(Loan loan, Long loanChargeId);

    void recalculateServiceChargeForAllLoans();

}
