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
package org.ideoholic.fineract.servicecharge.service;

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
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.charge.exception.LoanChargeNotFoundException;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInstallmentCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallmentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionComparator;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.ideoholic.fineract.exception.ServiceChargeException;
import org.ideoholic.fineract.exception.ServiceChargeException.SERVICE_CHARGE_EXCEPTION_REASON;
import org.ideoholic.fineract.servicecharge.constants.ServiceChargeApiConstants;
import org.ideoholic.fineract.servicecharge.data.SCLoanAccountData;
import org.ideoholic.fineract.util.ServiceChargeOperationUtils;
import org.ideoholic.fineract.util.daterange.ServiceChargeDateRangeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeInstallmentCalculatorServiceImpl implements ServiceChargeInstallmentCalculatorService {

	private final static Logger logger = LoggerFactory.getLogger(ServiceChargeInstallmentCalculatorServiceImpl.class);

	private final LoanAssembler loanAssembler;
	private final LoanChargeRepository loanChargeRepository;
	private final LoanRepositoryWrapper loanRepositoryWrapper;
	private final LoanProductReadPlatformService loanProductReadPlatformService;
	private final ServiceChargeCalculationPlatformService serviceChargeCalculationService;
	private final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository;
	private final ServiceChargeLoanDetailsReadPlatformService serviceChargeLoanDetailsReadPlatformService;

	@Autowired
	public ServiceChargeInstallmentCalculatorServiceImpl(final LoanAssembler loanAssembler,
			final LoanChargeRepository loanChargeRepository, final LoanRepositoryWrapper loanRepositoryWrapper,
			final LoanProductReadPlatformService loanProductReadPlatformService,
			final ServiceChargeCalculationPlatformService serviceChargeCalculationService,
			final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository,
			final ServiceChargeLoanDetailsReadPlatformService serviceChargeLoanDetailsReadPlatformService) {
		this.loanAssembler = loanAssembler;
		this.loanChargeRepository = loanChargeRepository;
		this.loanRepositoryWrapper = loanRepositoryWrapper;
		this.loanProductReadPlatformService = loanProductReadPlatformService;
		this.serviceChargeCalculationService = serviceChargeCalculationService;
		this.repaymentScheduleInstallmentRepository = repaymentScheduleInstallmentRepository;
		this.serviceChargeLoanDetailsReadPlatformService = serviceChargeLoanDetailsReadPlatformService;
	}

	@Override
	public void recalculateServiceChargeForAllLoans(final boolean useDirectJournalEntries) {
		final Page<SCLoanAccountData> currentQuarterLoans = serviceChargeLoanDetailsReadPlatformService
				.retrieveLoansToBeConsideredForTheCurrentQuarter();
		// If no loans to be processed for the current quarter then return
		if (currentQuarterLoans == null || currentQuarterLoans.getPageItems().isEmpty()) {
			return;
		}
		for (int i = 0; i < currentQuarterLoans.getPageItems().size(); i++) {
			SCLoanAccountData loanAccData = currentQuarterLoans.getPageItems().get(i);
			LoanProductData loanProduct = loanProductReadPlatformService
					.retrieveLoanProduct(loanAccData.getLoanProductId());
			// Only if it is demand loan
			if (ServiceChargeOperationUtils.checkDemandLaon(loanProduct)) {
				Long loanId = loanAccData.getId();
				logger.debug(
						"ServiceChargeInstallmentCalculatorServiceImpl:recalculateServiceChargeForAllLoans::Demand Loan-"
								+ loanId);
				// Re-calculate charge based on Service Charge calculation
				recalculateServiceChargeForGivenLoan(loanId, ServiceChargeApiConstants.ASSUMED_SERVICE_CHARGE_ID,
						useDirectJournalEntries);
			}
		}
	}

	@Override
	public void recalculateServiceChargeForGivenLoan(final Long loanId, final Long loanChargeId,
			final boolean useDirectJournalEntries) {
		final Loan loan = this.loanAssembler.assembleFrom(loanId);
		List<LoanRepaymentScheduleInstallment> installments = recalculateServiceChargeForGivenLoan(loan, loanChargeId,
				useDirectJournalEntries);
		// If it is open loan then the transactions can be updated
		if (checkIfLoanAmountCanBeUpdated(loan)) {
			loan.updateLoanSummaryDerivedFields();
			saveLoanWithDataIntegrityViolationChecks(loan, installments);
		} else {
			// If the loan is closed
		}
	}

	private boolean checkIfLoanAmountCanBeUpdated(Loan loan) {
		if (loan.isClosed() || loan.isClosedWrittenOff()) {
			return false;
		}
		return true;
	}

	public List<LoanRepaymentScheduleInstallment> recalculateServiceChargeForGivenLoan(final Loan loan,
			final Long loanChargeId, final boolean useDirectJournalEntries) {
		BigDecimal serviceChargeForLoan = serviceChargeCalculationService.calculateServiceChargeForLoan(loan.getId(),
				useDirectJournalEntries);
		final LoanCharge loanCharge = retrieveLoanChargeBy(loan.getId(), loanChargeId);
		final List<LoanTransaction> allNonContraTransactionsPostDisbursement = retreiveListOfTransactionsPostDisbursement(
				loan);
		List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();

		// The generated total service charge needs to be divided over the total
		// number of pending installments
		int loanInstallmentCount = getCountOfLoanTransactionsForChargeUpdation(installments,
				allNonContraTransactionsPostDisbursement);
		Money amount = Money.zero(loan.getCurrency());
		amount = amount.plus(serviceChargeForLoan).dividedBy(loanInstallmentCount, MoneyHelper.getRoundingMode());
		loanCharge.updateAmountOrPercentage(amount.getAmount());

		reGenerateLoanInstallmentServiceCharge(loanCharge, installments, loanInstallmentCount, loan.getCurrency());

		Set<LoanCharge> loanCharges = new HashSet<>();
		loanCharges.add(loanCharge);
		return installments;
	}

	private int getCountOfLoanTransactionsForChargeUpdation(List<LoanRepaymentScheduleInstallment> installments,
			List<LoanTransaction> loanTransactions) {
		int transactionsToBeSkipped = 0;
		for (LoanTransaction transaction : loanTransactions) {
			LoanTransactionType type = transaction.getTypeOf();
			if (type.isRepayment() && !ifTransactionLiesInTheCurrentQuarter(transaction)) {
				transactionsToBeSkipped++;
			}
		}
		int totalTransactions = installments.size();
		return totalTransactions - transactionsToBeSkipped;
	}

	private boolean ifTransactionLiesInTheCurrentQuarter(LoanTransaction transaction) {
		// If the transaction is within the quarter date range, then return true
		if (ServiceChargeDateRangeFactory.checkIfGivenDateIsInCurrentQuarter(transaction.getDateOf())) {
			return true;
		}
		return false; // else false
	}

	/**
	 * This method is to generate a new set of charge for the given set of loan
	 * installments. It returns a the total amount of the charge calculated in case
	 * this is a service charge calculation, null otherwise
	 * 
	 * @param command
	 * @param installmentAmount
	 * @param loan
	 * @param loanInstallmentCharge
	 * @return BigDecimal - The total amount of the charge in case this is service
	 *         charge calculation else null
	 * 
	 */
	private final BigDecimal reGenerateLoanInstallmentServiceCharge(final LoanCharge loanCharge,
			final List<LoanRepaymentScheduleInstallment> installments, final int pendingInstallmentsCount,
			final MonetaryCurrency loanCurrency) {
		Set<LoanInstallmentCharge> loanInstallmentCharge = loanCharge.installmentCharges();
		final Collection<LoanInstallmentCharge> remove = new HashSet<>();
		final List<LoanInstallmentCharge> newChargeInstallments = generateInstallmentLoanCharges(loanCharge,
				installments, pendingInstallmentsCount, loanCurrency);
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
					amount = calculateInstallmentChargeAmount(loanCharge.amountOrPercentage(), loanCurrency)
							.getAmount();
				} else {
					// This means that the Service Charge has not been
					// configured as a flat charge,
					throw new ServiceChargeException(
							SERVICE_CHARGE_EXCEPTION_REASON.SC_INSTALLMENT_ADJUSTMENT_EXCEPTION, loanCharge.getId());
				}
				final LoanInstallmentCharge loanInstallmentCharge = new LoanInstallmentCharge(amount, loanCharge,
						installment);
				loanChargePerInstallments.add(loanInstallmentCharge);
				Money installAmount = loanInstallmentCharge.getAmount(loanCurrency);
				Money zero = Money.zero(loanCurrency);
				installment.updateChargePortion(installAmount, zero, zero, zero, zero, zero);
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
			if (transaction.isNotReversed()
					&& !(transaction.isDisbursement() || transaction.isNonMonetaryTransaction())) {
				repaymentsOrWaivers.add(transaction);
			}
		}
		final LoanTransactionComparator transactionComparator = new LoanTransactionComparator();
		Collections.sort(repaymentsOrWaivers, transactionComparator);
		return repaymentsOrWaivers;
	}

	private LoanCharge retrieveLoanChargeBy(final Long loanId, final Long loanChargeId) {
		final LoanCharge loanCharge = this.loanChargeRepository.findOne(loanChargeId);
		if (loanCharge == null) {
			throw new LoanChargeNotFoundException(loanChargeId);
		}

		if (loanCharge.hasNotLoanIdentifiedBy(loanId)) {
			throw new LoanChargeNotFoundException(loanChargeId, loanId);
		}
		return loanCharge;
	}

	private void saveLoanWithDataIntegrityViolationChecks(final Loan loan,
			List<LoanRepaymentScheduleInstallment> installments) {
		try {
			for (LoanRepaymentScheduleInstallment installment : installments) {
				this.repaymentScheduleInstallmentRepository.save(installment);
			}
			this.loanRepositoryWrapper.save(loan);
		} catch (final DataIntegrityViolationException e) {
			final Throwable realCause = e.getCause();
			final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
			final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
					.resource("loan.transaction");
			if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
				baseDataValidator.reset().parameter("externalId").failWithCode("value.must.be.unique");
			}
			if (!dataValidationErrors.isEmpty()) {
				throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
						"Validation errors exist.", dataValidationErrors);
			}
		}
	}
}
