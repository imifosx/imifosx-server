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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.servicecharge.constants.QuarterDateRange;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeLoanProductSummary;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeLoanDetailsReadPlatformServiceImpl;

/**
 * Factory pattern to get an object of the type of ServiceChargeLoanProductSummary This class holds a list of all the
 * objects created. If there is a request for a duplicate object then the existing object from the current map is
 * returned else a new object is created and added to the list
 *
 */
public class ServiceChargeLoanSummaryFactory {

	Map<Long, ServiceChargeLoanProductSummary> loanSummaryObjectMap;

	/**
	 * Type param is used to decide on the implementing type of the class that needs to be returned
	 * 
	 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.ServiceChargeLoanProductSummary
	 * @param type
	 */
	public ServiceChargeLoanSummaryFactory() {
		loanSummaryObjectMap = new HashMap<Long, ServiceChargeLoanProductSummary>();
	}

	public ServiceChargeLoanProductSummary getLoanSummaryObject(
			ServiceChargeLoanDetailsReadPlatformServiceImpl loanReadService, LoanAccountData loanAccData,
			LoanProductData loanProduct) {
		Long loanId = loanAccData.getId();
		if (loanSummaryObjectMap.containsKey(loanId)) {
			return loanSummaryObjectMap.get(loanId);
		}
		LoanSummaryQuarterly loanSummary = new LoanSummaryQuarterly();
		loanSummary.populateOutstandingAndRepaymentAmounts(loanReadService, loanProduct, loanAccData);
		loanSummaryObjectMap.put(loanId, loanSummary);

		return loanSummary;
	}

	public List<BigDecimal> getMonthWiseOutstandingAmount(boolean isDemandLoan) {
		List<BigDecimal> result = new LinkedList<>();
		BigDecimal monthOne, monthTwo, monthThree;
		monthOne = monthTwo = monthThree = BigDecimal.ZERO;
		for (Long identifier : loanSummaryObjectMap.keySet()) {
			ServiceChargeLoanProductSummary summaryObj = loanSummaryObjectMap.get(identifier);
			if (summaryObj.isDemandLaon() ^ isDemandLoan) {
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

	private class LoanSummaryQuarterly implements ServiceChargeLoanProductSummary {

		private List<BigDecimal> periodicOutstanding;
		private List<BigDecimal> periodicRepayments;
		private boolean isDemandLaon;

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
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data. ServiceChargeLoanProductSummary#getTotalRepayments()
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
		 * Given the calendar this method returns the date that would be the first day of the month
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
			QuarterDateRange quarter = QuarterDateRange.getCurrentQuarter();
			Date lastDayOfMonth = quarter.getToDateForCurrentYear(); // Last day of the quarter
			// Start with the last outstanding amount
			BigDecimal loanOutstandingAmount = loanAccData.getTotalOutstandingAmount();
			int doesLoanHaveOutstandingAmount = loanAccData.getTotalOutstandingAmount().compareTo(BigDecimal.ZERO);
			Date dateDisbursement = loanAccData.repaymentScheduleRelatedData().disbursementDate().toDate();

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(lastDayOfMonth);
			for (int j = 0; j < 3; j++) {
				// Get to the first day of the current month
				Date firstDayOfMonth = getFirstDateOfCurrentMonth(calendar);
				// Loan will be considered only if the disbursement date is before the date under consideration
				if (dateDisbursement.compareTo(lastDayOfMonth) < 0) {
					// Retrieve the transaction between the given dates for the loan
					final Collection<LoanTransactionData> currentLoanRepayments = scLoanDetailsReadPlatform
							.retrieveLoanTransactionsMonthlyPayments(loanAccData.getId(),
									new SimpleDateFormat("yyyy-MM-dd").format(firstDayOfMonth),
									new SimpleDateFormat("yyyy-MM-dd").format(lastDayOfMonth));

					if (currentLoanRepayments.isEmpty() && doesLoanHaveOutstandingAmount != 0) {
						// There are no repayments and so the outstanding amount remains the same
						outstanding.add(loanOutstandingAmount);
					} else if (!currentLoanRepayments.isEmpty()) {
						// Whatever is the current outstanding is the outstanding for the current month
						outstanding.add(loanOutstandingAmount);
						// There are some repayments so add them back to the outstanding amount
						BigDecimal repaymentAmount = BigDecimal.ZERO;
						for (LoanTransactionData loanTransactionData : currentLoanRepayments) {
							repaymentAmount = repaymentAmount.add(loanTransactionData.getAmount());
						}
						addPeriodicRepayments(repaymentAmount);
						loanOutstandingAmount = loanOutstandingAmount.add(repaymentAmount);
					}
				}
				lastDayOfMonth = getLastDateOfPreviousMonth(calendar, firstDayOfMonth);
			}
			addPeriodicOutstandingReversed(outstanding);
			return loanOutstandingAmount;
		}

		private void addPeriodicOutstandingReversed(List<BigDecimal> outstanding) {
			for (int iCount = outstanding.size() - 1; iCount >= 0; iCount--) {
				BigDecimal amount = outstanding.get(iCount);
				addPeriodicOutstanding(amount);
			}
		}

	}

}
