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
package org.apache.fineract.portfolio.servicecharge.util;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeLoanProductSummary;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeLoanDetailsReadPlatformService;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeLoanDetailsReadPlatformServiceImpl;
import org.apache.fineract.portfolio.servicecharge.util.ServiceChargeDateUtils.DateIterator;
import org.apache.fineract.portfolio.servicecharge.util.daterange.ServiceChargeDateRange;
import org.apache.fineract.portfolio.servicecharge.util.daterange.ServiceChargeDateRangeFactory;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory pattern to get an object of the type of
 * ServiceChargeLoanProductSummary This class holds a list of all the objects
 * created. If there is a request for a duplicate object then the existing
 * object from the current map is returned else a new object is created and
 * added to the list
 *
 */
public class ServiceChargeLoanSummaryFactory {

	private final static Logger logger = LoggerFactory.getLogger(ServiceChargeLoanSummaryFactory.class);

	Map<Long, ServiceChargeLoanProductSummary> loanSummaryObjectMap;

	/**
	 * Type param is used to decide on the implementing type of the class that needs
	 * to be returned
	 * 
	 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.ServiceChargeLoanProductSummary
	 * @param type
	 */
	public ServiceChargeLoanSummaryFactory() {
		loanSummaryObjectMap = new HashMap<Long, ServiceChargeLoanProductSummary>();
	}

	public ServiceChargeLoanProductSummary getLoanSummaryObject(
			ServiceChargeLoanDetailsReadPlatformService loanReadService, LoanAccountData loanAccData,
			LoanProductData loanProduct) {
		Long loanId = loanAccData.getId();
		if (loanSummaryObjectMap.containsKey(loanId)) {
			return loanSummaryObjectMap.get(loanId);
		}
		LoanSummaryDaily loanSummary = new LoanSummaryDaily();
		loanSummary.populateOutstandingAndRepaymentAmounts(loanReadService, loanProduct, loanAccData);
		loanSummaryObjectMap.put(loanId, loanSummary);

		return loanSummary;
	}

	public List<BigDecimal> getMonthWiseOutstandingAmount(boolean isDemandLoan) {
		// TODO: Code too much dependent on size and position - change to streams
		List<BigDecimal> result = new LinkedList<>();
		BigDecimal monthOne, monthTwo, monthThree;
		monthOne = monthTwo = monthThree = BigDecimal.ZERO;
		for (Long identifier : loanSummaryObjectMap.keySet()) {
			ServiceChargeLoanProductSummary summaryObj = loanSummaryObjectMap.get(identifier);
			// The condition in the if is the xnor condition by which
			// the expression becomes true only if both the boolean values are the same
			if (!(summaryObj.isDemandLaon() ^ isDemandLoan)) {
				List<BigDecimal> outstanding = summaryObj.getPeriodicOutstanding();
				int size = outstanding.size();
				if (size > 0) {
					monthOne = monthOne.add(outstanding.get(0));
					if (size > 1) {
						monthTwo = monthTwo.add(outstanding.get(1));
						if (size > 2) {
							monthThree = monthThree.add(outstanding.get(2));
						}
					}
				}
			}
		}
		result.add(monthOne);
		result.add(monthTwo);
		result.add(monthThree);
		return result;
	}

	@SuppressWarnings("unused")
	private class LoanSummaryQuarterly implements ServiceChargeLoanProductSummary {

		private List<BigDecimal> periodicOutstanding;
		private List<BigDecimal> periodicRepayments;
		private boolean isDemandLaon;
		private Date disbursmentDate;

		LoanSummaryQuarterly() {
			periodicOutstanding = new LinkedList<>();
			periodicRepayments = new LinkedList<>();
		}

		public boolean isDemandLaon() {
			return isDemandLaon;
		}

		private void setDemandLaon(boolean isDemandLaon) {
			this.isDemandLaon = isDemandLaon;
		}

		public Date getDisbursmentDate() {
			return disbursmentDate;
		}

		public void setDisbursmentDate(Date disbursmentDate) {
			this.disbursmentDate = disbursmentDate;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getPeriodicOutstanding()
		 */
		@Override
		public List<BigDecimal> getPeriodicOutstanding() {
			return periodicOutstanding;
		}

		private void addPeriodicOutstanding(BigDecimal amount) {
			getPeriodicOutstanding().add(amount);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getPeriodicRepayments()
		 */
		@Override
		public List<BigDecimal> getPeriodicRepayments() {
			return periodicRepayments;
		}

		private void addPeriodicRepayments(BigDecimal amount) {
			getPeriodicRepayments().add(amount);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getTotalOutstanding()
		 */
		@Override
		public BigDecimal getTotalOutstanding() {
			// Start with zero
			BigDecimal sum = BigDecimal.ZERO;
			// If there are values
			if (!getPeriodicOutstanding().isEmpty()) {
				// Iterate over the list of values and add them to sum
				for (BigDecimal repayment : getPeriodicOutstanding()) {
					sum = sum.add(repayment);
				}
			}
			// Return the calculated sum (or zero)
			return sum;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getTotalRepayments()
		 */
		@Override
		public BigDecimal getTotalRepayments() {
			// Start with zero
			BigDecimal sum = BigDecimal.ZERO;
			// If there are values
			if (!getPeriodicRepayments().isEmpty()) {
				// Iterate over the list of values and add them to sum
				for (BigDecimal repayment : getPeriodicRepayments()) {
					sum = sum.add(repayment);
				}
			}
			// Return the calculated sum (or zero)
			return sum;
		}

		/**
		 * Given the calendar this method returns the date that would be the first day
		 * of the month
		 * 
		 * @param calendar
		 * @return Date - Date should be 1 of the the month
		 */
		private Date getFirstDateOfCurrentMonth(Calendar calendar) {
			calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
			return calendar.getTime();
		}

		private Date getLastDateOfPreviousMonth(Calendar calendar, Date date) {
			calendar.add(Calendar.MONTH, -1);
			calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
			return calendar.getTime();
		}

		BigDecimal populateOutstandingAndRepaymentAmounts(
				ServiceChargeLoanDetailsReadPlatformServiceImpl scLoanDetailsReadPlatform, LoanProductData loanProduct,
				LoanAccountData loanAccData) {
			List<BigDecimal> outstanding = new LinkedList<>();
			// Set the demand loan type
			setDemandLaon(ServiceChargeOperationUtils.checkDemandLaon(loanProduct));
			// Date details to iterate over
			ServiceChargeDateRange quarter = ServiceChargeDateRangeFactory.getCurrentDateRange();
			Date lastDayOfMonth = quarter.getToDateForCurrentYear(); // Last day of the quarter
			// Start with the last outstanding amount
			BigDecimal loanOutstandingAmount = loanAccData.getTotalOutstandingAmount();
			// int doesLoanHaveOutstandingAmount =
			// loanAccData.getTotalOutstandingAmount().compareTo(BigDecimal.ZERO);
			setDisbursmentDate(loanAccData.repaymentScheduleRelatedData().disbursementDate().toDate());

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(lastDayOfMonth);
			for (int i = 1; i <= 3; i++) {
				// Repayments total - start from zero for every iteration
				BigDecimal repaymentAmount = BigDecimal.ZERO;
				// Get to the first day of the current calendar
				Date firstDayOfMonth = getFirstDateOfCurrentMonth(calendar);
				// Loan will be considered only if the disbursement date is before the date
				// under consideration
				if (getDisbursmentDate().compareTo(lastDayOfMonth) < 0) {
					// Whatever is the current outstanding is the outstanding for the current month
					outstanding.add(loanOutstandingAmount);

					/*
					 * Retrieve the transaction between the given dates for the loan. If there are
					 * repayments then update the outstanding Updated outstanding will be added to
					 * the list in the next iteration
					 */
					final Collection<LoanTransactionData> currentLoanRepayments = scLoanDetailsReadPlatform
							.retrieveLoanTransactionsMonthlyPayments(loanAccData.getId(),
									DateUtils.formatToSqlDate(firstDayOfMonth),
									DateUtils.formatToSqlDate(lastDayOfMonth));
					if (!currentLoanRepayments.isEmpty()) {
						// There are some repayments so add them back to the outstanding amount
						for (LoanTransactionData loanTransactionData : currentLoanRepayments) {
							// Add only those transactions that are for repayment, reject others
							if (isRepaymentTransaction(loanTransactionData)) {
								repaymentAmount = repaymentAmount.add(loanTransactionData.getAmount());
							}
						}
						loanOutstandingAmount = loanOutstandingAmount.add(repaymentAmount);
					}
				} else {
					// Adding Zero to make sure that each list contains the same number of elements
					outstanding.add(BigDecimal.ZERO);
				}
				addPeriodicRepayments(repaymentAmount);
				lastDayOfMonth = getLastDateOfPreviousMonth(calendar, firstDayOfMonth);
			}
			addPeriodicOutstandingReversed(outstanding);
			return loanOutstandingAmount;
		}

		/**
		 * Method to validate if the loan transaction entry is a repayments entry
		 * Current check is for repayment or repayment at disbursement, similar other
		 * checks can be placed here
		 * 
		 * @param LoanTransactionData
		 * @return true - if transaction is of repayment type false - otherwise
		 */
		private boolean isRepaymentTransaction(LoanTransactionData loanTransactionData) {
			boolean result = loanTransactionData.getType().isRepayment()
					|| loanTransactionData.getType().isRepaymentAtDisbursement();
			return result;
		}

		private void addPeriodicOutstandingReversed(List<BigDecimal> outstanding) {
			// System.out.println("ServiceChargeLoanSummaryFactory.LoanSummaryQuarterly:addPeriodicOutstandingReversed::");
			for (int iCount = outstanding.size() - 1; iCount >= 0; iCount--) {
				BigDecimal amount = outstanding.get(iCount);
				System.out.print(amount + ", ");
				addPeriodicOutstanding(amount);
			}
			// System.out.println();
		}

	}

	@SuppressWarnings("unused")
	private class LoanSummaryDaily implements ServiceChargeLoanProductSummary {

		private List<BigDecimal> periodicOutstanding;
		private List<BigDecimal> periodicRepayments;
		private boolean isDemandLaon;
		private Date disbursmentDate;

		LoanSummaryDaily() {
			periodicOutstanding = new LinkedList<>();
			periodicRepayments = new LinkedList<>();
		}

		public boolean isDemandLaon() {
			return isDemandLaon;
		}

		private void setDemandLaon(boolean isDemandLaon) {
			this.isDemandLaon = isDemandLaon;
		}

		public Date getDisbursmentDate() {
			return disbursmentDate;
		}

		public void setDisbursmentDate(Date disbursmentDate) {
			this.disbursmentDate = disbursmentDate;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getPeriodicOutstanding()
		 */
		@Override
		public List<BigDecimal> getPeriodicOutstanding() {
			return periodicOutstanding;
		}

		private void addPeriodicOutstanding(BigDecimal amount) {
			getPeriodicOutstanding().add(amount);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getPeriodicRepayments()
		 */
		@Override
		public List<BigDecimal> getPeriodicRepayments() {
			return periodicRepayments;
		}

		private void addPeriodicRepayments(BigDecimal amount) {
			logger.debug(
					"ServiceChargeLoanSummaryFactory.LoanSummaryDaily.addPeriodicRepayments()::Total Monthly repayment:"
							+ amount);
			getPeriodicRepayments().add(amount);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getTotalOutstanding()
		 */
		@Override
		public BigDecimal getTotalOutstanding() {
			// Start with zero
			BigDecimal sum = BigDecimal.ZERO;
			// If there are values
			if (!getPeriodicOutstanding().isEmpty()) {
				// Iterate over the list of values and add them to sum
				for (BigDecimal repayment : getPeriodicOutstanding()) {
					sum = sum.add(repayment);
				}
			}
			// Return the calculated sum (or zero)
			return sum;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getTotalRepayments()
		 */
		@Override
		public BigDecimal getTotalRepayments() {
			// Start with zero
			BigDecimal sum = BigDecimal.ZERO;
			// If there are values
			if (!getPeriodicRepayments().isEmpty()) {
				// Iterate over the list of values and add them to sum
				for (BigDecimal repayment : getPeriodicRepayments()) {
					sum = sum.add(repayment);
				}
			}
			// Return the calculated sum (or zero)
			return sum;
		}

		/**
		 * Given the calendar this method returns the date that would be the first day
		 * of the month
		 * 
		 * @param calendar
		 * @return Date - Date should be 1 of the the month
		 */
		private Date getFirstDateOfCurrentMonth(Calendar calendar) {
			calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
			return calendar.getTime();
		}

		private Date getLastDateOfPreviousMonth(Calendar calendar, Date date) {
			calendar.add(Calendar.MONTH, -1);
			calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
			return calendar.getTime();
		}

		BigDecimal populateOutstandingAndRepaymentAmounts(
				ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatform, LoanProductData loanProduct,
				LoanAccountData loanAccData) {
			List<BigDecimal> outstanding = new LinkedList<>();
			// Set the demand loan type
			setDemandLaon(ServiceChargeOperationUtils.checkDemandLaon(loanProduct));
			// Date details to iterate over
			ServiceChargeDateRange quarter = ServiceChargeDateRangeFactory.getCurrentDateRange();
			Date lastDayOfMonth = quarter.getToDateForCurrentYear(); // Last day of the quarter
			// Start with the last outstanding amount
			BigDecimal loanOutstandingAmount = loanAccData.getTotalOutstandingAmount();
			setDisbursmentDate(loanAccData.repaymentScheduleRelatedData().disbursementDate().toDate());
			printLoanDetailsInLogger(loanAccData);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(lastDayOfMonth);
			for (int i = 1; i <= 3; i++) {
				// Repayments total - start from zero for every iteration
				BigDecimal monthlyTotalRepaymentAmount = BigDecimal.ZERO;
				// Get to the first day of the current calendar
				Date firstDayOfMonth = getFirstDateOfCurrentMonth(calendar);
				// Loan will be considered only if the disbursement date is before the date
				// under consideration
				if (getDisbursmentDate().compareTo(lastDayOfMonth) < 0) {
					// Retrieve the transaction between the given dates for the loan
					final Collection<LoanTransactionData> currentLoanRepayments = scLoanDetailsReadPlatform
							.retrieveLoanTransactionsMonthlyPayments(loanAccData.getId(),
									DateUtils.formatToSqlDate(firstDayOfMonth),
									DateUtils.formatToSqlDate(lastDayOfMonth));

					Map<Date, BigDecimal> repaymentDateAmountMap = new HashMap<>();
					if (!currentLoanRepayments.isEmpty()) {
						// There are some repayments so add them back to the map of date and amount
						monthlyTotalRepaymentAmount = populateRepaymentsIntoMap(repaymentDateAmountMap,
								currentLoanRepayments);
					}
					Iterator<Date> dateIterator = new DateIterator(lastDayOfMonth, firstDayOfMonth, true);
					BigDecimal summationOfDailyOutstanding = BigDecimal.ZERO;
					Date dateMarker = lastDayOfMonth;
					Date curDate = lastDayOfMonth;
					while (dateIterator.hasNext()) {
						// If there are repayments then update the outstanding
						curDate = dateIterator.next();
						if (repaymentDateAmountMap.containsKey(curDate)) {
							BigDecimal amount = repaymentDateAmountMap.get(curDate);
							updateLoanOutstandingAndSummationValues(curDate, dateMarker, summationOfDailyOutstanding,
									loanOutstandingAmount, amount);
						}
						// For a less efficient calculation uncomment this and comment the
						// summationOfDailyOutstanding calculation in method
						// updateLoanOutstandingAndSummationValues
						// summationOfDailyOutstanding =
						// summationOfDailyOutstanding.add(loanOutstandingAmount);
					}
					updateLoanOutstandingAndSummationValues(curDate, dateMarker, summationOfDailyOutstanding,
							loanOutstandingAmount, null);
					// Add to the list the current outstanding for the current month
					outstanding.add(summationOfDailyOutstanding);
				}
				addPeriodicRepayments(monthlyTotalRepaymentAmount);
				lastDayOfMonth = getLastDateOfPreviousMonth(calendar, firstDayOfMonth);
			}
			addPeriodicOutstandingReversed(outstanding);
			calendar.clear(); // clearing all sets before returning
			return loanOutstandingAmount;
		}

		private void printLoanDetailsInLogger(LoanAccountData loanAccData) {
			logger.debug("###########################################################");
			logger.debug("ServiceChargeLoanSummaryFactory.LoanSummaryDaily.printLoanDetailsInLogger()");
			logger.debug("Client ID:" + loanAccData.clientId());
			logger.debug("Loan ID:" + loanAccData.getId());
			logger.debug("Loan current outstanding:" + loanAccData.getTotalOutstandingAmount());
			logger.debug(
					"Loan Disbursment Date:" + loanAccData.repaymentScheduleRelatedData().disbursementDate().toDate());
		}

		private BigDecimal populateRepaymentsIntoMap(Map<Date, BigDecimal> repaymentDateAmountMap,
				Collection<LoanTransactionData> currentLoanRepayments) {
			BigDecimal totalRepaymentAmount = BigDecimal.ZERO;
			for (LoanTransactionData loanTransactionData : currentLoanRepayments) {
				// Add only those transactions that are for repayment will be considered, reject
				// others
				if (isRepaymentTransaction(loanTransactionData)) {
					BigDecimal repaymentAmount = loanTransactionData.getAmount();
					LocalDate transactionDate = loanTransactionData.dateOf();
					logger.debug(
							"ServiceChargeLoanSummaryFactory.LoanSummaryDaily.populateRepaymentsIntoMap()::Loan repayment-> amount\t"
									+ repaymentAmount + "\tdate\t" + transactionDate);
					Date dateKey = ServiceChargeDateUtils.getDateFromLocaleDate(transactionDate);
					// If there has already been a repayment on the same day
					if (repaymentDateAmountMap.containsKey(dateKey)) {
						// Then add the values of both the repayments
						repaymentAmount = repaymentAmount.add(repaymentDateAmountMap.get(dateKey));
						// removing to ensure one key has only one value
						repaymentDateAmountMap.remove(dateKey);
					}
					repaymentDateAmountMap.put(dateKey, repaymentAmount);
					totalRepaymentAmount = totalRepaymentAmount.add(repaymentAmount);
				}
			}
			return totalRepaymentAmount;
		}

		private void updateLoanOutstandingAndSummationValues(Date curDate, Date dateMarker,
				BigDecimal summationOfDailyOutstanding, BigDecimal loanOutstandingAmount, BigDecimal amount) {
			BigDecimal periodBetweenDates = new BigDecimal(
					ServiceChargeDateUtils.getDiffBetweenDates(curDate, dateMarker, 1));
			BigDecimal outstandingForPeriod = loanOutstandingAmount.multiply(periodBetweenDates);
			summationOfDailyOutstanding = summationOfDailyOutstanding.add(outstandingForPeriod);
			if (amount != null) {
				loanOutstandingAmount = loanOutstandingAmount.add(amount);
			}
			logger.debug(
					"ServiceChargeLoanSummaryFactory.LoanSummaryDaily.updateLoanOutstandingAndSummationValues()::Daily Outstanding summation:"
							+ summationOfDailyOutstanding);
		}

		/**
		 * Method to validate if the loan transaction entry is a repayments entry
		 * Current check is for repayment or repayment at disbursement, similar other
		 * checks can be placed here
		 * 
		 * @param LoanTransactionData
		 * @return true - if transaction is of repayment type false - otherwise
		 */
		private boolean isRepaymentTransaction(LoanTransactionData loanTransactionData) {
			boolean result = loanTransactionData.getType().isRepayment()
					|| loanTransactionData.getType().isRepaymentAtDisbursement();
			return result;
		}

		private void addPeriodicOutstandingReversed(List<BigDecimal> outstanding) {
			logger.debug("ServiceChargeLoanSummaryFactory.LoanSummaryQuarterly:addPeriodicOutstandingReversed::");
			for (int iCount = outstanding.size() - 1; iCount >= 0; iCount--) {
				BigDecimal amount = outstanding.get(iCount);
				logger.debug(amount.toEngineeringString());
				addPeriodicOutstanding(amount);
			}
			// System.out.println();
		}

	}
}
