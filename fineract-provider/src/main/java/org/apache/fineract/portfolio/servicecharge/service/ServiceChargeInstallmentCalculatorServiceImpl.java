package org.apache.fineract.portfolio.servicecharge.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.charge.exception.LoanChargeNotFoundException;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInstallmentCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallmentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleTransactionProcessorFactory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionComparator;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.servicecharge.exception.ServiceChargeException;
import org.apache.fineract.portfolio.servicecharge.exception.ServiceChargeException.SERVICE_CHARGE_EXCEPTION_REASON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeInstallmentCalculatorServiceImpl implements ServiceChargeInstallmentCalculatorService {

    private final LoanAssembler loanAssembler;
    private final LoanChargeRepository loanChargeRepository;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final ServiceChargeCalculationPlatformService serviceChargeCalculationService;
    private final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;

    @Autowired
    public ServiceChargeInstallmentCalculatorServiceImpl(final LoanAssembler loanAssembler,
            final ServiceChargeCalculationPlatformService serviceChargeCalculationService,
            final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory,
            LoanChargeRepository loanChargeRepository, LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository,
            LoanRepositoryWrapper loanRepositoryWrapper) {
        this.loanAssembler = loanAssembler;
        this.loanChargeRepository = loanChargeRepository;
        this.loanRepositoryWrapper = loanRepositoryWrapper;
        this.serviceChargeCalculationService = serviceChargeCalculationService;
        this.repaymentScheduleInstallmentRepository = repaymentScheduleInstallmentRepository;
        this.loanRepaymentScheduleTransactionProcessorFactory = loanRepaymentScheduleTransactionProcessorFactory;
    }

    @Override
    public void recalculateServiceChargeForGivenLoan(Long loanId, Long loanChargeId) {
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        List<LoanRepaymentScheduleInstallment> installments = recalculateServiceChargeForGivenLoan(loan, loanChargeId);
        saveLoanWithDataIntegrityViolationChecks(loan, installments);
    }

    @Override
    public List<LoanRepaymentScheduleInstallment> recalculateServiceChargeForGivenLoan(Loan loan, Long loanChargeId) {
        BigDecimal serviceChargeForLoan = serviceChargeCalculationService.calculateServiceChargeForLoan(loan.getId());
        final LoanCharge loanCharge = retrieveLoanChargeBy(loan.getId(), loanChargeId);
        final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retreiveListOfTransactionsPostDisbursement(loan);
        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();

        // The generated total service charge needs to be divided over the total
        // number of pending installments
        int loanInstallmentCount = getPendingLoanInstallmentCount(installments, allNonContraTransactionsPostDisbursement);
        serviceChargeForLoan = serviceChargeForLoan.divide(new BigDecimal(loanInstallmentCount));
        loanCharge.updateAmountOrPercentage(serviceChargeForLoan);

        generatePendingLoanInstallmentCharges(loanCharge, installments, loanInstallmentCount, loan.getCurrency());

        Set<LoanCharge> loanCharges = new HashSet<>();
        loanCharges.add(loanCharge);
        
        /*final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = loanRepaymentScheduleTransactionProcessorFactory
                .determineProcessor(loan.transactionProcessingStrategy());

        loanRepaymentScheduleTransactionProcessor.handleTransaction(loan.getDisbursementDate(), allNonContraTransactionsPostDisbursement,
                loan.getCurrency(), installments, loanCharges);*/
        return installments;
    }

    private int getPendingLoanInstallmentCount(List<LoanRepaymentScheduleInstallment> installments,
            List<LoanTransaction> loanTransactions) {
        int paidTransactions = loanTransactions.size();
        int totalTransactions = installments.size();
        return totalTransactions - paidTransactions;
    }

    /**
     * This method is to generate a new set of charge for the given set of loan
     * installments. It returns a the total amount of the charge calculated in
     * case this is a service charge calculation, null otherwise
     * 
     * @param command
     * @param installmentAmount
     * @param loan
     * @param loanInstallmentCharge
     * @return BigDecimal - The total amount of the charge in case this is
     *         service charge calculation else null
     * 
     */
    private final BigDecimal generatePendingLoanInstallmentCharges(final LoanCharge loanCharge,
            final List<LoanRepaymentScheduleInstallment> installments, final int pendingInstallmentsCount,
            final MonetaryCurrency loanCurrency) {
        Set<LoanInstallmentCharge> loanInstallmentCharge = loanCharge.installmentCharges();
        final Collection<LoanInstallmentCharge> remove = new HashSet<>();
        final List<LoanInstallmentCharge> newChargeInstallments = generateInstallmentLoanCharges(loanCharge, installments,
                pendingInstallmentsCount, loanCurrency);
        if (loanInstallmentCharge != null && loanInstallmentCharge.isEmpty()) {
            loanInstallmentCharge.addAll(newChargeInstallments);
        } else {
            int index = 0;
            final List<LoanInstallmentCharge> oldChargeInstallments = new ArrayList<>();
            if (loanInstallmentCharge != null && !loanInstallmentCharge.isEmpty()) {
                oldChargeInstallments.addAll(loanInstallmentCharge);
            } else {
                loanInstallmentCharge = new HashSet<>();
            }
            Collections.sort(oldChargeInstallments);
            final LoanInstallmentCharge[] loanChargePerInstallmentArray = newChargeInstallments
                    .toArray(new LoanInstallmentCharge[newChargeInstallments.size()]);
            for (final LoanInstallmentCharge chargePerInstallment : oldChargeInstallments) {
                if (index == loanChargePerInstallmentArray.length) {
                    remove.add(chargePerInstallment);
                    chargePerInstallment.updateInstallment(null);
                } else {
                    chargePerInstallment.copyFrom(loanChargePerInstallmentArray[index++]);
                }
            }
            loanInstallmentCharge.removeAll(remove);
            while (index < loanChargePerInstallmentArray.length) {
                loanInstallmentCharge.add(loanChargePerInstallmentArray[index++]);
            }
        }
        Money amount = Money.zero(loanCurrency);
        for (LoanInstallmentCharge charge : loanInstallmentCharge) {
            amount = amount.plus(charge.getAmount());
        }
        return amount.getAmount();
    }

    private List<LoanInstallmentCharge> generateInstallmentLoanCharges(final LoanCharge loanCharge,
            final List<LoanRepaymentScheduleInstallment> installments, final int pendingInstallmentsCount,
            final MonetaryCurrency loanCurrency) {
        final List<LoanInstallmentCharge> loanChargePerInstallments = new ArrayList<>();
        if (loanCharge.isInstalmentFee()) {
            int iCounter = 0;
            int numOfInstallmentsToSkip = installments.size() - pendingInstallmentsCount;
            for (final LoanRepaymentScheduleInstallment installment : installments) {
                if (installment.isRecalculatedInterestComponent() || numOfInstallmentsToSkip > iCounter) {
                    iCounter++;
                    continue;
                }
                BigDecimal amount = BigDecimal.ZERO;
                if (loanCharge.getChargeCalculation().isFlat()) {
                    amount = calculateInstallmentChargeAmount(loanCharge.amountOrPercentage(), loanCurrency).getAmount();
                } else {
                    // This means that the Service Charge has not been
                    // configured as a flat charge,
                    throw new ServiceChargeException(SERVICE_CHARGE_EXCEPTION_REASON.SC_INSTALLMENT_ADJUSTMENT_EXCEPTION,
                            loanCharge.getId());
                }
                final LoanInstallmentCharge loanInstallmentCharge = new LoanInstallmentCharge(amount, loanCharge, installment);
                loanChargePerInstallments.add(loanInstallmentCharge);
                Money installAmount = loanInstallmentCharge.getAmount(loanCurrency);
                installment.updateChargePortion(installAmount, installAmount, installAmount, installAmount, installAmount, installAmount);
            }
        }
        return loanChargePerInstallments;
    }

    private Money calculateInstallmentChargeAmount(final BigDecimal chargeAmount, final MonetaryCurrency currency) {
        Money amount = Money.zero(currency);
        amount = amount.plus(chargeAmount);
        return amount;
    }

    private List<LoanTransaction> retreiveListOfTransactionsPostDisbursement(final Loan loan) {
        final List<LoanTransaction> repaymentsOrWaivers = new ArrayList<>();
        List<LoanTransaction> trans = loan.getLoanTransactions();
        for (final LoanTransaction transaction : trans) {
            if (transaction.isNotReversed() && !(transaction.isDisbursement() || transaction.isNonMonetaryTransaction())) {
                repaymentsOrWaivers.add(transaction);
            }
        }
        final LoanTransactionComparator transactionComparator = new LoanTransactionComparator();
        Collections.sort(repaymentsOrWaivers, transactionComparator);
        return repaymentsOrWaivers;
    }

    private LoanCharge retrieveLoanChargeBy(final Long loanId, final Long loanChargeId) {
        final LoanCharge loanCharge = this.loanChargeRepository.findOne(loanChargeId);
        if (loanCharge == null) { throw new LoanChargeNotFoundException(loanChargeId); }

        if (loanCharge.hasNotLoanIdentifiedBy(loanId)) { throw new LoanChargeNotFoundException(loanChargeId, loanId); }
        return loanCharge;
    }

    private void saveLoanWithDataIntegrityViolationChecks(final Loan loan, List<LoanRepaymentScheduleInstallment> installments) {
        try {
            // List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
            for (LoanRepaymentScheduleInstallment installment : installments) {
                this.repaymentScheduleInstallmentRepository.save(installment);
            }
            this.loanRepositoryWrapper.save(loan);
        } catch (final DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.transaction");
            if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
                baseDataValidator.reset().parameter("externalId").failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                    "Validation errors exist.", dataValidationErrors); }
        }
    }
}
