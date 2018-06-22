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
package org.apache.fineract.portfolio.servicecharge.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.account.data.AccountTransferData;
import org.apache.fineract.portfolio.accountdetails.service.AccountEnumerations;
import org.apache.fineract.portfolio.calendar.data.CalendarData;
import org.apache.fineract.portfolio.client.domain.ClientEnumerations;
import org.apache.fineract.portfolio.common.service.CommonEnumerations;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanApplicationTimelineData;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanInterestRecalculationData;
import org.apache.fineract.portfolio.loanaccount.data.LoanStatusEnumData;
import org.apache.fineract.portfolio.loanaccount.data.LoanSummaryData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionEnumData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSubStatus;
import org.apache.fineract.portfolio.loanaccount.exception.LoanNotFoundException;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanChargeReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.portfolio.paymentdetail.data.PaymentDetailData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.apache.fineract.portfolio.servicecharge.constants.QuarterDateRange;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeApiConstants;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeFinalSheetData;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeLoanProductSummary;
import org.apache.fineract.portfolio.servicecharge.util.ServiceChargeLoanSummaryFactory;
import org.apache.fineract.portfolio.servicecharge.util.ServiceChargeOperationUtils;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeLoanDetailsReadPlatformServiceImpl implements ServiceChargeLoanDetailsReadPlatformService, ServiceChargeApiConstants {

	private final static Logger logger = LoggerFactory.getLogger(ServiceChargeLoanDetailsReadPlatformServiceImpl.class);

	private final LoanAssembler loanAssembler;
	private final LoanReadPlatformService loanReadPlatformService;
	private final LoanChargeReadPlatformService loanChargeReadPlatformService;
	private final LoanProductReadPlatformService loanProductReadPlatformService;
	private final JdbcTemplate jdbcTemplate;
	private final PlatformSecurityContext context;
	private final LoanMapper loaanLoanMapper = new LoanMapper();
	private final PaginationHelper<LoanAccountData> paginationHelper = new PaginationHelper<>();
	
    @Autowired
    public ServiceChargeLoanDetailsReadPlatformServiceImpl(LoanReadPlatformService loanReadPlatformService,
            LoanChargeReadPlatformService loanChargeReadPlatformService, final LoanProductReadPlatformService readPlatformService,
            final RoutingDataSource dataSource, final PlatformSecurityContext context, final LoanAssembler loanAssembler) {
        this.loanReadPlatformService = loanReadPlatformService;
        this.loanChargeReadPlatformService = loanChargeReadPlatformService;
        this.loanProductReadPlatformService = readPlatformService;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.context = context;
        this.loanAssembler = loanAssembler;
    }

	public BigDecimal getTotalLoansForCurrentQuarter() {
		BigDecimal totalLoans = BigDecimal.ZERO;
		
		// Get the dates
		QuarterDateRange quarter = QuarterDateRange.getCurrentQuarter();
		String startDate = quarter.getFormattedFromDateString();
		String endDate = quarter.getFormattedToDateString();
		
		final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null, null);
		Page<LoanAccountData> loanAccountData = null;

		loanAccountData = retrieveLoansForCurrentQuarter(searchParameters,startDate,endDate);

		if (loanAccountData != null) {
			int totalNumberLoans = loanAccountData.getPageItems().size();
			totalLoans = new BigDecimal(totalNumberLoans);
		}

		return totalLoans;
	}

	public BigDecimal getAllLoansRepaymentData() throws Exception {

		BigDecimal totalRepayment = new BigDecimal("0");
		
		// Get the dates
		QuarterDateRange quarter = QuarterDateRange.getCurrentQuarter();
		String startDate = quarter.getFormattedFromDateString();
		String endDate = quarter.getFormattedToDateString();

		final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null, null);
		Page<LoanAccountData> loanAccountData = null;
		try {
			loanAccountData = loanReadPlatformService.retrieveAll(searchParameters);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int dataListSize = loanAccountData.getPageItems().size();
		logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getAllLoansRepaymentData::Total number of accounts" + dataListSize);
		for (int i = 0; i < dataListSize; i++) {
			
			try {
				Long loanId = loanAccountData.getPageItems().get(i).getId();
				if (!loanAccountData.getPageItems().get(i).isActive()) {
					logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getAllLoansRepaymentData::Loan ID:"  + loanId + " is inactive!");
					continue;
				}
				logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getAllLoansRepaymentData::The loan id is " + loanId);
				final Collection<LoanTransactionData> currentLoanRepayments = retrieveLoanTransactionsMonthlyPayments(loanAccountData.getPageItems().get(i).getId(), startDate, endDate);

				for (LoanTransactionData loanTransactionData : currentLoanRepayments) {
					BigDecimal repaymentAmount = loanTransactionData.getAmount();
					logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getAllLoansRepaymentData::Date = " + loanTransactionData.dateOf() + "  Repayment Amount = " + repaymentAmount);

					// perform add operation on bg1 with augend bg2 and context mc
					totalRepayment = totalRepayment.add(repaymentAmount);
					
					logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getAllLoansRepaymentData::Partial totalRepayment:" + totalRepayment.toPlainString());
				}
				
				/* Get Loan Charge Name - TODO: Need to decide if we need to identify the loan type
				String loanCharges = getLoanChargeName(loanAccountData.getPageItems().get(i).getId());
				logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getAllLoansRepaymentData::Loan Charge Name:" + loanCharges);
				*/
		        	
			} catch (Exception e) {
				// Exception on a particular loan is ignored and continue to calculate 
				e.printStackTrace();
			}
		}
		logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getAllLoansRepaymentData::totalRepayment:" + totalRepayment.toPlainString());
		
		return totalRepayment;
	}

	public String getLoanChargeName(Long loanId) {
		String loanChargeData = null;
		final Collection<LoanChargeData> loanCharges = this.loanChargeReadPlatformService.retrieveLoanCharges(loanId);

		for (LoanChargeData loanChargeDataIT : loanCharges) {
			// loanChargeData = loanChargeDataIT.getAmount();
			loanChargeData = loanChargeDataIT.getName();
		}

		return loanChargeData;
	}
	
	public void populateRepaymentsInSheetData(ServiceChargeFinalSheetData sheetData) {
		logger.debug("entered into ServiceChargeLoanDetailsReadPlatformServiceImpl.populateRepaymentsInSheetData");

		BigDecimal dLtotalOutstandingAmount = BigDecimal.ZERO;
		BigDecimal nDLtotalOutstandingAmount = BigDecimal.ZERO;
		int noOfDL = 0;
		// Get the dates
		QuarterDateRange quarter = QuarterDateRange.getCurrentQuarter();
		String strStartDate = quarter.getFormattedFromDateString();
		String strEndDate = quarter.getFormattedToDateString();
		Date startDate = quarter.getFromDateForCurrentYear();
		Date endDate = quarter.getToDateForCurrentYear();

		final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null, null);
		Page<LoanAccountData> loanAccountDataForOutstandingAmount = null;
		try {
			loanAccountDataForOutstandingAmount = retrieveLoansToBeConsideredForTheQuarter(searchParameters, strStartDate, strEndDate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ServiceChargeLoanSummaryFactory loanSummaryFactory = new ServiceChargeLoanSummaryFactory();
		if (loanAccountDataForOutstandingAmount != null) {
			for (int i = 0; i < loanAccountDataForOutstandingAmount.getPageItems().size(); i++) {
				LoanAccountData loanAccData = loanAccountDataForOutstandingAmount.getPageItems().get(i);
				LoanProductData loanProduct = loanProductReadPlatformService.retrieveLoanProduct(loanAccData.loanProductId());

				// logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.populateRepaymentsInSheetData::Total Outstanding Amount " + loanAccData.getTotalOutstandingAmount());
				logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.populateRepaymentsInSheetData::Account Loan id " + loanAccData.getId());
				// logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.populateRepaymentsInSheetData::Outstanding Amount: " + loanAccData.getTotalOutstandingAmount());

				ServiceChargeLoanProductSummary loanSummary = loanSummaryFactory.getLoanSummaryObject(this, loanAccData, loanProduct);
				if (!loanSummary.isDemandLaon()) {
					nDLtotalOutstandingAmount = nDLtotalOutstandingAmount.add(loanSummary.getTotalOutstanding());
				} else {
					Date dateDisbursement = loanSummary.getDisbursmentDate();
					if (dateDisbursement.compareTo(startDate) >= 0 && dateDisbursement.compareTo(endDate) <= 0) {
						noOfDL++;
					}
					dLtotalOutstandingAmount = dLtotalOutstandingAmount.add(loanSummary.getTotalOutstanding());
				}
				// Add the repayments amount of this loan to sheet data
				sheetData.addTotalLoanRepaymentAmount(loanSummary.getTotalRepayments());
			} // End of for-loop
		} // End of (loanAccountDataForOutstandingAmount != null)
		sheetData.setNoOfDemandLoans(noOfDL);
		nDLtotalOutstandingAmount = nDLtotalOutstandingAmount.divide(new BigDecimal(3), 6, RoundingMode.CEILING);
		dLtotalOutstandingAmount = dLtotalOutstandingAmount.divide(new BigDecimal(3), 6, RoundingMode.CEILING);
		sheetData.setLoanOutstandingAmount(dLtotalOutstandingAmount, nDLtotalOutstandingAmount);
		logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.populateRepaymentsInSheetData::totalOutstanding DL Amount:" + dLtotalOutstandingAmount);
		logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.populateRepaymentsInSheetData::totalOutstanding Non DL Amount:" + nDLtotalOutstandingAmount);
		logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.populateRepaymentsInSheetData:: DL Monthly cost list:" + loanSummaryFactory.getMonthWiseOutstandingAmount(true));
		logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.populateRepaymentsInSheetData:: Non-DL Monthly cost list:" + loanSummaryFactory.getMonthWiseOutstandingAmount(false));
	}
	
	public void getRepaymentsInSheetData(ServiceChargeFinalSheetData sheetData) {
		// logger.debug("entered into ServiceChargeLoanDetailsReadPlatformServiceImpl.getRepaymentsInSheetData");

		BigDecimal dLtotalOutstandingAmount = BigDecimal.ZERO;
		BigDecimal nDLtotalOutstandingAmount = BigDecimal.ZERO;
		int noOfDL = 0;
		// Get the dates
		QuarterDateRange quarter = QuarterDateRange.getCurrentQuarter();
		String strStartDate = quarter.getFormattedFromDateString();
		String strEndDate = quarter.getFormattedToDateString();
		Date startDate = quarter.getFromDateForCurrentYear();
		Date endDate = quarter.getToDateForCurrentYear();

		final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null, null);
		Page<LoanAccountData> loanAccountDataForOutstandingAmount = null;
		loanAccountDataForOutstandingAmount = retrieveLoansToBeConsideredForTheQuarter(searchParameters, strStartDate, strEndDate);

		if (loanAccountDataForOutstandingAmount != null) {
			for (int i = 0; i < loanAccountDataForOutstandingAmount.getPageItems().size(); i++) {
				BigDecimal loanRepaymentAmount = BigDecimal.ZERO;
				LoanAccountData loanAccData = loanAccountDataForOutstandingAmount.getPageItems().get(i);
				LoanProductData loanProduct = loanProductReadPlatformService.retrieveLoanProduct(loanAccData.loanProductId());

				// logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.populateRepaymentsInSheetData::Total Outstanding Amount " + loanAccData.getTotalOutstandingAmount());
				logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getRepaymentsInSheetData::Account Loan id " + loanAccData.getId());
				logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getRepaymentsInSheetData::Outstanding Amount: " + loanAccData.getTotalOutstandingAmount());

				boolean isDemandLaon = ServiceChargeOperationUtils.checkDemandLaon(loanProduct);
				loanRepaymentAmount = getRepaymentAmount(sheetData, loanAccData, startDate, endDate);
				if (!isDemandLaon) {
					nDLtotalOutstandingAmount = nDLtotalOutstandingAmount.add(loanRepaymentAmount);
				} else {
					Date dateDisbursement = loanAccData.repaymentScheduleRelatedData().disbursementDate().toDate();
					if (dateDisbursement.compareTo(startDate) >= 0 && dateDisbursement.compareTo(endDate) <= 0) {
						noOfDL++;
					}
					dLtotalOutstandingAmount = dLtotalOutstandingAmount.add(loanRepaymentAmount);
				}
			} // End of for-loop
		} // End of (loanAccountDataForOutstandingAmount != null)
		sheetData.setNoOfDemandLoans(noOfDL);
		nDLtotalOutstandingAmount = nDLtotalOutstandingAmount.divide(new BigDecimal(3), 6, RoundingMode.CEILING);
		dLtotalOutstandingAmount = dLtotalOutstandingAmount.divide(new BigDecimal(3), 6, RoundingMode.CEILING);
		sheetData.setLoanOutstandingAmount(dLtotalOutstandingAmount, nDLtotalOutstandingAmount);
		logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getRepaymentsInSheetData::totalOutstanding DL Amount:" + dLtotalOutstandingAmount);
		logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getRepaymentsInSheetData::totalOutstanding Non DL Amount:" + nDLtotalOutstandingAmount);
	}

	private BigDecimal getRepaymentAmount(ServiceChargeFinalSheetData sheetData, LoanAccountData loanAccData, Date startDate, Date endDate) {
		// Get the total repayments
		BigDecimal approvedPricipal = loanAccData.getApprovedPrincipal();
		BigDecimal totlaRepayment = loanAccData.getApprovedPrincipal();
		int outstandingAmout = loanAccData.getTotalOutstandingAmount().compareTo(BigDecimal.ZERO);
		Date dateDisbursement = loanAccData.repaymentScheduleRelatedData().disbursementDate().toDate();

		Date date = startDate;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		for (int j = 0; j < 3; j++) {
			// Get to the first day of the next month and then subtract one day
			// This would get the last day of the current month
			calendar.add(Calendar.MONTH, 1);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			calendar.add(Calendar.DATE, -1);
			Date lastDayOfMonth = calendar.getTime();
			// Ignore if the date of disbursement is after then current date under consideration
			if (dateDisbursement.compareTo(date) < 0) {
				// Retrieve the transaction between the given dates for the loan
				final Collection<LoanTransactionData> currentLoanRepayments = retrieveLoanTransactionsMonthlyPayments(loanAccData.getId(),
						new SimpleDateFormat("yyyy-MM-dd").format(date), new SimpleDateFormat("yyyy-MM-dd").format(lastDayOfMonth));

				if (currentLoanRepayments.isEmpty() && outstandingAmout != 0) {
					approvedPricipal = approvedPricipal.subtract(BigDecimal.ZERO);
					totlaRepayment = approvedPricipal.add(totlaRepayment);
				} else if (!currentLoanRepayments.isEmpty()) {
					BigDecimal repaymentAmount = BigDecimal.ZERO;
					for (LoanTransactionData loanTransactionData : currentLoanRepayments) {
						repaymentAmount = repaymentAmount.add(loanTransactionData.getAmount());
						logger.debug("ServiceChargeLoanDetailsReadPlatformServiceImpl.getRepaymentAmount::individual repayment:" + repaymentAmount);
					}
					sheetData.addTotalLoanRepaymentAmount(repaymentAmount);
					approvedPricipal = approvedPricipal.subtract(repaymentAmount);
					totlaRepayment = approvedPricipal.add(totlaRepayment);
				}
			}
			calendar.add(Calendar.MONTH, +1);
			calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
			date = calendar.getTime();
		}
		return totlaRepayment;
	}

	@Override
	public boolean findIfLoanDisbursedInCurrentQuarter(Long loanId) {
		return findIfLoanDisbursedInGivenQuarter(loanId, QuarterDateRange.getCurrentQuarter());
	}

	@Override
	public BigDecimal getTotalRepaymentsForCurrentQuarter(Long loanId) {
		return getTotalRepaymentsForGivenQuarter(loanId, QuarterDateRange.getCurrentQuarter());
	}

	@Override
	public BigDecimal getTotalOutstandingAmountForCurrentQuarter(Long loanId) {
		return getTotalOutstandingAmountForGivenQuarter(loanId, QuarterDateRange.getCurrentQuarter());
	}
	
    private boolean findIfLoanDisbursedInGivenQuarter(Long loanId, QuarterDateRange range) {
        boolean result = false;
        // Get the dates
        String startDate = range.getFormattedFromDateString();
        String endDate = range.getFormattedToDateString();

        final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null, null);

        try {
            LoanAccountData loanAccountData = retrieveOneLoanForCurrentQuarter(searchParameters, loanId, startDate, endDate);
            if (loanAccountData == null) {
                result = false;
            } else {
                result = true;
            }
        } catch (Exception e) {
            // TODO: handle exception
            logger.error(
                    "Exception in ServiceChargeLoanDetailsReadPlatformServiceImpl:findIfLoanDisbursedInGivenQuarter::" + e.getMessage());
        }

        return result;
    }

    private BigDecimal getTotalRepaymentsForGivenQuarter(Long loanId, QuarterDateRange range) {
        final Loan loan = this.loanAssembler.assembleFrom(loanId);
        Money amount = Money.zero(loan.getCurrency());

        // Get the dates
        String startDate = range.getFormattedFromDateString();
        String endDate = range.getFormattedToDateString();

        final Collection<LoanTransactionData> currentLoanRepayments = retrieveLoanTransactionsMonthlyPayments(loanId, startDate, endDate);

        for (LoanTransactionData loanTransactionData : currentLoanRepayments) {
            logger.debug("Date = " + loanTransactionData.dateOf() + "  Repayment Amount = " + loanTransactionData.getAmount());
            amount.plus(loanTransactionData.getAmount());
        }
        return amount.getAmount();
    }

	private BigDecimal getTotalOutstandingAmountForGivenQuarter(Long loanId, QuarterDateRange range) {

		BigDecimal totalOutstandingAmount = BigDecimal.ZERO;
		// create MathContext object with 2 precision
		MathContext mc = new MathContext(2);
		String startDate = range.getFormattedFromDateString();
		String endDate = range.getFormattedToDateString();

		final Collection<LoanTransactionData> currentLoanRepayments = retrieveLoanTransactionsOutstandingPayments(loanId, startDate, endDate);

		if (!currentLoanRepayments.isEmpty()) {
			for (LoanTransactionData loanTransactionData : currentLoanRepayments) {
				logger.debug("Date = " + loanTransactionData.dateOf() + "  Repayment Amount = " + loanTransactionData.getOutstandingLoanBalance());
				// perform add operation on bg1 with augend bg2 and context mc
				totalOutstandingAmount = totalOutstandingAmount.add(loanTransactionData.getOutstandingLoanBalance(), mc);
			}
		} else {
			// check whether the account is closed or not?
			final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null, null);
			LoanAccountData loanAccountData = null;
			try {
				loanAccountData = checkLoanStatus(searchParameters, loanId);
			} catch (Exception E) {
				logger.error(E.getMessage());
			}
			if (loanAccountData != null) {
				// Loan is closed
				logger.info("Loan is closed");
			} else {
				// get the last outstanding amount
				final Collection<LoanTransactionData> loanRepaymentsPreviousData = retrieveLoanTransactionsOutstandingPaymentsPreviuosData(loanId);

				for (LoanTransactionData loanTransactionData : loanRepaymentsPreviousData) {
					logger.debug("Date = " + loanTransactionData.dateOf() + "  Repayment Amount = " + loanTransactionData.getOutstandingLoanBalance());
					// perform add operation on bg1 with augend bg2 and context mc
					totalOutstandingAmount = totalOutstandingAmount.add(loanTransactionData.getOutstandingLoanBalance(), mc);
				}
			}
		}
		logger.debug("Total outstanding amount " + totalOutstandingAmount);
		return totalOutstandingAmount;
	}
	
	@Override
	public Collection<LoanTransactionData> retrieveLoanTransactionsOutstandingPayments(Long loanId, String startDate, String endDate) {
		try {
			final LoanTransactionsMapper rm = new LoanTransactionsMapper();
			// retrieve all loan transactions that are not invalid and have not
			// been 'contra'ed by another transaction
			// repayments at time of disbursement (e.g. charges)

			/***
			 * TODO Vishwas: Remove references to "Contra" from the codebase
			 ***/
			final String sql = "select " + rm.LoanPaymentsSchema()
			// + " where tr.loan_id = ? and tr.transaction_type_enum in (2) and
			// (tr.is_reversed=0 or tr.manually_adjusted_or_reversed = 1) order
			// by tr.transaction_date ASC,id ";
					+ " where tr.loan_id = ? and tr.transaction_type_enum in (1,2) and  (tr.is_reversed=0 or tr.manually_adjusted_or_reversed = 1) and tr.transaction_date between '"
					+ startDate + "' and '" + endDate + "'  order by tr.transaction_date ASC,id";
			return this.jdbcTemplate.query(sql, rm, new Object[] { loanId });
		} catch (final EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	 @Override
	public LoanAccountData checkLoanStatus(final SearchParameters searchParameters, Long loanId) {
		try {
			final AppUser currentUser = this.context.authenticatedUser();
			final String hierarchy = currentUser.getOffice().getHierarchy();
			final String hierarchySearchString = hierarchy + "%";

			final LoanMapper rm = new LoanMapper();

			final StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("select ");
			sqlBuilder.append(rm.loanSchema());
			sqlBuilder.append(" join m_office o on (o.id = c.office_id or o.id = g.office_id) ");
			sqlBuilder.append(" left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");
			sqlBuilder.append(" where l.id=? and ( o.hierarchy like ? or transferToOffice.hierarchy like ?) and l.closedon_date IS NOT NULL");

			return this.jdbcTemplate.queryForObject(sqlBuilder.toString(), rm, new Object[] { loanId, hierarchySearchString, hierarchySearchString });
		} catch (final EmptyResultDataAccessException e) {
			throw new LoanNotFoundException(loanId);
		}
	}
	
	
	 private static final class LoanTransactionsMapper implements RowMapper<LoanTransactionData> {

	        public String LoanPaymentsSchema() {

	            return " tr.id as id, tr.transaction_type_enum as transactionType, tr.transaction_date as `date`, tr.amount as total, "
	                    + " tr.principal_portion_derived as principal, tr.interest_portion_derived as interest, "
	                    + " tr.fee_charges_portion_derived as fees, tr.penalty_charges_portion_derived as penalties, "
	                    + " tr.overpayment_portion_derived as overpayment, tr.outstanding_loan_balance_derived as outstandingLoanBalance, "
	                    + " tr.unrecognized_income_portion as unrecognizedIncome,"
	                    + " tr.submitted_on_date as submittedOnDate, "
	                    + " tr.manually_adjusted_or_reversed as manuallyReversed, "
	                    + " pd.payment_type_id as paymentType,pd.account_number as accountNumber,pd.check_number as checkNumber, "
	                    + " pd.receipt_number as receiptNumber, pd.bank_number as bankNumber,pd.routing_code as routingCode, "
	                    + " l.currency_code as currencyCode, l.currency_digits as currencyDigits, l.currency_multiplesof as inMultiplesOf, rc.`name` as currencyName, "
	                    + " rc.display_symbol as currencyDisplaySymbol, rc.internationalized_name_code as currencyNameCode, "
	                    + " pt.value as paymentTypeName, tr.external_id as externalId, tr.office_id as officeId, office.name as officeName, "
	                    + " fromtran.id as fromTransferId, fromtran.is_reversed as fromTransferReversed,"
	                    + " fromtran.transaction_date as fromTransferDate, fromtran.amount as fromTransferAmount,"
	                    + " fromtran.description as fromTransferDescription,"
	                    + " totran.id as toTransferId, totran.is_reversed as toTransferReversed,"
	                    + " totran.transaction_date as toTransferDate, totran.amount as toTransferAmount,"
	                    + " totran.description as toTransferDescription " + " from m_loan l join m_loan_transaction tr on tr.loan_id = l.id"
	                    + " join m_currency rc on rc.`code` = l.currency_code "
	                    + " left JOIN m_payment_detail pd ON tr.payment_detail_id = pd.id"
	                    + " left join m_payment_type pt on pd.payment_type_id = pt.id" + " left join m_office office on office.id=tr.office_id"
	                    + " left join m_account_transfer_transaction fromtran on fromtran.from_loan_transaction_id = tr.id "
	                    + " left join m_account_transfer_transaction totran on totran.to_loan_transaction_id = tr.id ";
	        }

	        @Override
	        public LoanTransactionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

	            final String currencyCode = rs.getString("currencyCode");
	            final String currencyName = rs.getString("currencyName");
	            final String currencyNameCode = rs.getString("currencyNameCode");
	            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
	            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
	            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
	            final CurrencyData currencyData = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
	                    currencyDisplaySymbol, currencyNameCode);

	            final Long id = rs.getLong("id");
	            final Long officeId = rs.getLong("officeId");
	            final String officeName = rs.getString("officeName");
	            final int transactionTypeInt = JdbcSupport.getInteger(rs, "transactionType");
	            final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(transactionTypeInt);
	            final boolean manuallyReversed = rs.getBoolean("manuallyReversed");

	            PaymentDetailData paymentDetailData = null;

	            if (transactionType.isPaymentOrReceipt()) {
	                final Long paymentTypeId = JdbcSupport.getLong(rs, "paymentType");
	                if (paymentTypeId != null) {
	                    final String typeName = rs.getString("paymentTypeName");
	                    final PaymentTypeData paymentType = PaymentTypeData.instance(paymentTypeId, typeName);
	                    final String accountNumber = rs.getString("accountNumber");
	                    final String checkNumber = rs.getString("checkNumber");
	                    final String routingCode = rs.getString("routingCode");
	                    final String receiptNumber = rs.getString("receiptNumber");
	                    final String bankNumber = rs.getString("bankNumber");
	                    paymentDetailData = new PaymentDetailData(id, paymentType, accountNumber, checkNumber, routingCode, receiptNumber,
	                            bankNumber);
	                }
	            }
	            final LocalDate date = JdbcSupport.getLocalDate(rs, "date");
	            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
	            final BigDecimal totalAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "total");
	            final BigDecimal principalPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principal");
	            final BigDecimal interestPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interest");
	            final BigDecimal feeChargesPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "fees");
	            final BigDecimal penaltyChargesPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penalties");
	            final BigDecimal overPaymentPortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "overpayment");
	            final BigDecimal unrecognizedIncomePortion = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "unrecognizedIncome");
	            final BigDecimal outstandingLoanBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "outstandingLoanBalance");
	            final String externalId = rs.getString("externalId");

	            AccountTransferData transfer = null;
	            final Long fromTransferId = JdbcSupport.getLong(rs, "fromTransferId");
	            final Long toTransferId = JdbcSupport.getLong(rs, "toTransferId");
	            if (fromTransferId != null) {
	                final LocalDate fromTransferDate = JdbcSupport.getLocalDate(rs, "fromTransferDate");
	                final BigDecimal fromTransferAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "fromTransferAmount");
	                final boolean fromTransferReversed = rs.getBoolean("fromTransferReversed");
	                final String fromTransferDescription = rs.getString("fromTransferDescription");

	                transfer = AccountTransferData.transferBasicDetails(fromTransferId, currencyData, fromTransferAmount, fromTransferDate,
	                        fromTransferDescription, fromTransferReversed);
	            } else if (toTransferId != null) {
	                final LocalDate toTransferDate = JdbcSupport.getLocalDate(rs, "toTransferDate");
	                final BigDecimal toTransferAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "toTransferAmount");
	                final boolean toTransferReversed = rs.getBoolean("toTransferReversed");
	                final String toTransferDescription = rs.getString("toTransferDescription");

	                transfer = AccountTransferData.transferBasicDetails(toTransferId, currencyData, toTransferAmount, toTransferDate,
	                        toTransferDescription, toTransferReversed);
	            }
	            return new LoanTransactionData(id, officeId, officeName, transactionType, paymentDetailData, currencyData, date, totalAmount,
	                    principalPortion, interestPortion, feeChargesPortion, penaltyChargesPortion, overPaymentPortion,
	                    unrecognizedIncomePortion, externalId, transfer, null, outstandingLoanBalance, submittedOnDate, manuallyReversed);
	        }
	    }

	 
	 @Override
	public Collection<LoanTransactionData> retrieveLoanTransactionsOutstandingPaymentsPreviuosData(Long loanId) {
		try {
			final LoanTransactionsMapper rm = new LoanTransactionsMapper();

			// retrieve all loan transactions that are not invalid and have not
			// been 'contra'ed by another transaction
			// repayments at time of disbursement (e.g. charges)

			/***
			 * TODO Vishwas: Remove references to "Contra" from the codebase
			 ***/
			final String sql = "select " + rm.LoanPaymentsSchema()
					+ " where tr.loan_id = ? and tr.transaction_type_enum in (1,2) and  (tr.is_reversed=0 or tr.manually_adjusted_or_reversed = 1) and tr.outstanding_loan_balance_derived IS NOT NULL order by tr.transaction_date DESC LIMIT 1";
			return this.jdbcTemplate.query(sql, rm, new Object[] { loanId });
		} catch (final EmptyResultDataAccessException e) {
			return null;
		}
	}
	 
	 public LoanAccountData retrieveOneLoanForCurrentQuarter(final SearchParameters searchParameters,Long loanId, String startDate, String endDate) {
         final AppUser currentUser = this.context.authenticatedUser();
         final String hierarchy = currentUser.getOffice().getHierarchy();
         final String hierarchySearchString = hierarchy + "%";

         final LoanMapper rm = new LoanMapper();

         final StringBuilder sqlBuilder = new StringBuilder();
		 try {
	            sqlBuilder.append("select ");
	            sqlBuilder.append(rm.loanSchema());
	            sqlBuilder.append(" join m_office o on (o.id = c.office_id or o.id = g.office_id) ");
	            sqlBuilder.append(" left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");
	            sqlBuilder.append(" where l.id=? and ( o.hierarchy like ? or transferToOffice.hierarchy like ?) and l.disbursedon_date between '"+startDate+"' and '"+endDate+"'");

	            return this.jdbcTemplate.queryForObject(sqlBuilder.toString(), rm, new Object[] { loanId, hierarchySearchString,
	                    hierarchySearchString });
	        } catch (final EmptyResultDataAccessException e) {
	            //throw new LoanNotFoundException(loanId);
	            return this.jdbcTemplate.queryForObject(sqlBuilder.toString(), rm, new Object[] { loanId, hierarchySearchString,
                    hierarchySearchString });
	        }
	 }
	 
	public Collection<LoanTransactionData> retrieveLoanTransactionsMonthlyPayments(Long loanId, String startDate, String endDate) {
		try {
			this.context.authenticatedUser();

			final LoanTransactionsMapper rm = new LoanTransactionsMapper();

			// retrieve all loan transactions that are not invalid and have not
			// been 'contra'ed by another transaction
			// repayments at time of disbursement (e.g. charges)

			/***
			 * TODO Vishwas: Remove references to "Contra" from the codebase
			 ***/
			final String sql = "select " + rm.LoanPaymentsSchema()
			// + " where tr.loan_id = ? and tr.transaction_type_enum in (2) and
			// (tr.is_reversed=0 or tr.manually_adjusted_or_reversed = 1) order
			// by tr.transaction_date ASC,id ";
					+ " where tr.loan_id = ? and tr.transaction_type_enum in (2) and  (tr.is_reversed=0 or tr.manually_adjusted_or_reversed = 1) and tr.transaction_date between '"
					+ startDate + "' and '" + endDate + "'  order by tr.transaction_date ASC,id";
			return this.jdbcTemplate.query(sql, rm, new Object[] { loanId });
		} catch (final EmptyResultDataAccessException e) {
			return null;
		}
	}
	 
	 
	public Page<LoanAccountData> retrieveLoansForCurrentQuarter(final SearchParameters searchParameters, String startDate, String endDate) {

		final AppUser currentUser = this.context.authenticatedUser();
		final String hierarchy = currentUser.getOffice().getHierarchy();
		final String hierarchySearchString = hierarchy + "%";

		final StringBuilder sqlBuilder = new StringBuilder(200);
		sqlBuilder.append("select SQL_CALC_FOUND_ROWS ");
		sqlBuilder.append(this.loaanLoanMapper.loanSchema());

		// TODO - ideoholic
		// get all the loans which are active in the current quarter
		sqlBuilder.append(" join m_office o on o.id = c.office_id");
		sqlBuilder.append(" left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");
		sqlBuilder.append(" where ( o.hierarchy like ? or transferToOffice.hierarchy like ?) and l.disbursedon_date between '" + startDate + "' and '" + endDate + "'");

		int arrayPos = 2;
		List<Object> extraCriterias = new ArrayList<>();
		extraCriterias.add(hierarchySearchString);
		extraCriterias.add(hierarchySearchString);

		String sqlQueryCriteria = searchParameters.getSqlSearch();
		if (StringUtils.isNotBlank(sqlQueryCriteria)) {
			sqlQueryCriteria = sqlQueryCriteria.replaceAll("accountNo", "l.account_no");
			sqlBuilder.append(" and (").append(sqlQueryCriteria).append(")");
		}

		if (StringUtils.isNotBlank(searchParameters.getExternalId())) {
			sqlBuilder.append(" and l.external_id = ?");
			extraCriterias.add(searchParameters.getExternalId());
			arrayPos = arrayPos + 1;
		}

		if (StringUtils.isNotBlank(searchParameters.getAccountNo())) {
			sqlBuilder.append(" and l.account_no = ?");
			extraCriterias.add(searchParameters.getAccountNo());
			arrayPos = arrayPos + 1;
		}

		if (searchParameters.isOrderByRequested()) {
			sqlBuilder.append(" order by ").append(searchParameters.getOrderBy());

			if (searchParameters.isSortOrderProvided()) {
				sqlBuilder.append(' ').append(searchParameters.getSortOrder());
			}
		}

		if (searchParameters.isLimited()) {
			sqlBuilder.append(" limit ").append(searchParameters.getLimit());
			if (searchParameters.isOffset()) {
				sqlBuilder.append(" offset ").append(searchParameters.getOffset());
			}
		}

		final Object[] objectArray = extraCriterias.toArray();
		final Object[] finalObjectArray = Arrays.copyOf(objectArray, arrayPos);
		final String sqlCountRows = "SELECT FOUND_ROWS()";
		return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlCountRows, sqlBuilder.toString(), finalObjectArray, this.loaanLoanMapper);
	}

    @Override
    public Page<LoanAccountData> retrieveLoansToBeConsideredForTheCurrentQuarter() {
        // Get the dates
        QuarterDateRange quarter = QuarterDateRange.getCurrentQuarter();
        String strStartDate = quarter.getFormattedFromDateString();
        String strEndDate = quarter.getFormattedToDateString();

        final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null, null);
        return retrieveLoansToBeConsideredForTheQuarter(searchParameters, strStartDate, strEndDate);

    }
	 
	 
	private Page<LoanAccountData> retrieveLoansToBeConsideredForTheQuarter(final SearchParameters searchParameters, String startDate, String endDate) {
		
        final AppUser currentUser = this.context.authenticatedUser();
        final String hierarchy = currentUser.getOffice().getHierarchy();
        final String hierarchySearchString = hierarchy + "%";

        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select SQL_CALC_FOUND_ROWS ");
        sqlBuilder.append(this.loaanLoanMapper.loanSchema());

        // TODO - for time being this will data scope list of loans returned to
        // only loans that have a client associated.
        // to support scenario where loan has group_id only OR client_id will
        // probably require a UNION query
        // but that at present is an edge case
		sqlBuilder.append(" join m_office o on o.id = c.office_id");
		sqlBuilder.append(" left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");
		sqlBuilder.append(" where ( o.hierarchy like ? or transferToOffice.hierarchy like ?) and l.loan_status_id='300' or l.closedon_date between '" + startDate + "' and '"
				+ endDate + "' ");

        int arrayPos = 2;
        List<Object> extraCriterias = new ArrayList<>();
        extraCriterias.add(hierarchySearchString);
        extraCriterias.add(hierarchySearchString);

        String sqlQueryCriteria = searchParameters.getSqlSearch();
        if (StringUtils.isNotBlank(sqlQueryCriteria)) {
            sqlQueryCriteria = sqlQueryCriteria.replaceAll("accountNo", "l.account_no");
            sqlBuilder.append(" and (").append(sqlQueryCriteria).append(")");
        }

        if (StringUtils.isNotBlank(searchParameters.getExternalId())) {
            sqlBuilder.append(" and l.external_id = ?");
            extraCriterias.add(searchParameters.getExternalId());
            arrayPos = arrayPos + 1;
        }

        if (StringUtils.isNotBlank(searchParameters.getAccountNo())) {
            sqlBuilder.append(" and l.account_no = ?");
            extraCriterias.add(searchParameters.getAccountNo());
            arrayPos = arrayPos + 1;
        }

        if (searchParameters.isOrderByRequested()) {
            sqlBuilder.append(" order by ").append(searchParameters.getOrderBy());

            if (searchParameters.isSortOrderProvided()) {
                sqlBuilder.append(' ').append(searchParameters.getSortOrder());
            }
        }

        if (searchParameters.isLimited()) {
            sqlBuilder.append(" limit ").append(searchParameters.getLimit());
            if (searchParameters.isOffset()) {
                sqlBuilder.append(" offset ").append(searchParameters.getOffset());
            }
        }

        final Object[] objectArray = extraCriterias.toArray();
        final Object[] finalObjectArray = Arrays.copyOf(objectArray, arrayPos);
        final String sqlCountRows = "SELECT FOUND_ROWS()";
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlCountRows, sqlBuilder.toString(), finalObjectArray, this.loaanLoanMapper);
    }
 
	 private static final class LoanMapper implements RowMapper<LoanAccountData> {

	        public String loanSchema() {
	            return "l.id as id, l.account_no as accountNo, l.external_id as externalId, l.fund_id as fundId, f.name as fundName,"
	                    + " l.loan_type_enum as loanType, l.loanpurpose_cv_id as loanPurposeId, cv.code_value as loanPurposeName,"
	                    + " lp.id as loanProductId, lp.name as loanProductName, lp.description as loanProductDescription,"
	                    + " lp.is_linked_to_floating_interest_rates as isLoanProductLinkedToFloatingRate, "
	                    + " lp.allow_variabe_installments as isvariableInstallmentsAllowed, "
	                    + " lp.allow_multiple_disbursals as multiDisburseLoan,"
	                    + " lp.can_define_fixed_emi_amount as canDefineInstallmentAmount,"
	                    + " c.id as clientId, c.account_no as clientAccountNo, c.display_name as clientName, c.office_id as clientOfficeId,"
	                    + " g.id as groupId, g.account_no as groupAccountNo, g.display_name as groupName,"
	                    + " g.office_id as groupOfficeId, g.staff_id As groupStaffId , g.parent_id as groupParentId, (select mg.display_name from m_group mg where mg.id = g.parent_id) as centerName, "
	                    + " g.hierarchy As groupHierarchy , g.level_id as groupLevel, g.external_id As groupExternalId, "
	                    + " g.status_enum as statusEnum, g.activation_date as activationDate, "
	                    + " l.submittedon_date as submittedOnDate, sbu.username as submittedByUsername, sbu.firstname as submittedByFirstname, sbu.lastname as submittedByLastname,"
	                    + " l.rejectedon_date as rejectedOnDate, rbu.username as rejectedByUsername, rbu.firstname as rejectedByFirstname, rbu.lastname as rejectedByLastname,"
	                    + " l.withdrawnon_date as withdrawnOnDate, wbu.username as withdrawnByUsername, wbu.firstname as withdrawnByFirstname, wbu.lastname as withdrawnByLastname,"
	                    + " l.approvedon_date as approvedOnDate, abu.username as approvedByUsername, abu.firstname as approvedByFirstname, abu.lastname as approvedByLastname,"
	                    + " l.expected_disbursedon_date as expectedDisbursementDate, l.disbursedon_date as actualDisbursementDate, dbu.username as disbursedByUsername, dbu.firstname as disbursedByFirstname, dbu.lastname as disbursedByLastname,"
	                    + " l.closedon_date as closedOnDate, cbu.username as closedByUsername, cbu.firstname as closedByFirstname, cbu.lastname as closedByLastname, l.writtenoffon_date as writtenOffOnDate, "
	                    + " l.expected_firstrepaymenton_date as expectedFirstRepaymentOnDate, l.interest_calculated_from_date as interestChargedFromDate, l.expected_maturedon_date as expectedMaturityDate, "
	                    + " l.principal_amount_proposed as proposedPrincipal, l.principal_amount as principal, l.approved_principal as approvedPrincipal, l.arrearstolerance_amount as inArrearsTolerance, l.number_of_repayments as numberOfRepayments, l.repay_every as repaymentEvery,"
	                    + " l.grace_on_principal_periods as graceOnPrincipalPayment, l.recurring_moratorium_principal_periods as recurringMoratoriumOnPrincipalPeriods, l.grace_on_interest_periods as graceOnInterestPayment, l.grace_interest_free_periods as graceOnInterestCharged,l.grace_on_arrears_ageing as graceOnArrearsAgeing,"
	                    + " l.nominal_interest_rate_per_period as interestRatePerPeriod, l.annual_nominal_interest_rate as annualInterestRate, "
	                    + " l.repayment_period_frequency_enum as repaymentFrequencyType, l.interest_period_frequency_enum as interestRateFrequencyType, "
	                    + " l.term_frequency as termFrequency, l.term_period_frequency_enum as termPeriodFrequencyType, "
	                    + " l.amortization_method_enum as amortizationType, l.interest_method_enum as interestType, l.interest_calculated_in_period_enum as interestCalculationPeriodType,"
	                    + " l.allow_partial_period_interest_calcualtion as allowPartialPeriodInterestCalcualtion,"
	                    + " l.loan_status_id as lifeCycleStatusId, l.loan_transaction_strategy_id as transactionStrategyId, "
	                    + " lps.name as transactionStrategyName, "
	                    + " l.currency_code as currencyCode, l.currency_digits as currencyDigits, l.currency_multiplesof as inMultiplesOf, rc.`name` as currencyName, rc.display_symbol as currencyDisplaySymbol, rc.internationalized_name_code as currencyNameCode, "
	                    + " l.loan_officer_id as loanOfficerId, s.display_name as loanOfficerName, "
	                    + " l.principal_disbursed_derived as principalDisbursed,"
	                    + " l.principal_repaid_derived as principalPaid,"
	                    + " l.principal_writtenoff_derived as principalWrittenOff,"
	                    + " l.principal_outstanding_derived as principalOutstanding,"
	                    + " l.interest_charged_derived as interestCharged,"
	                    + " l.interest_repaid_derived as interestPaid,"
	                    + " l.interest_waived_derived as interestWaived,"
	                    + " l.interest_writtenoff_derived as interestWrittenOff,"
	                    + " l.interest_outstanding_derived as interestOutstanding,"
	                    + " l.fee_charges_charged_derived as feeChargesCharged,"
	                    + " l.total_charges_due_at_disbursement_derived as feeChargesDueAtDisbursementCharged,"
	                    + " l.fee_charges_repaid_derived as feeChargesPaid,"
	                    + " l.fee_charges_waived_derived as feeChargesWaived,"
	                    + " l.fee_charges_writtenoff_derived as feeChargesWrittenOff,"
	                    + " l.fee_charges_outstanding_derived as feeChargesOutstanding,"
	                    + " l.penalty_charges_charged_derived as penaltyChargesCharged,"
	                    + " l.penalty_charges_repaid_derived as penaltyChargesPaid,"
	                    + " l.penalty_charges_waived_derived as penaltyChargesWaived,"
	                    + " l.penalty_charges_writtenoff_derived as penaltyChargesWrittenOff,"
	                    + " l.penalty_charges_outstanding_derived as penaltyChargesOutstanding,"
	                    + " l.total_expected_repayment_derived as totalExpectedRepayment,"
	                    + " l.total_repayment_derived as totalRepayment,"
	                    + " l.total_expected_costofloan_derived as totalExpectedCostOfLoan,"
	                    + " l.total_costofloan_derived as totalCostOfLoan,"
	                    + " l.total_waived_derived as totalWaived,"
	                    + " l.total_writtenoff_derived as totalWrittenOff,"
	                    + " l.writeoff_reason_cv_id as writeoffReasonId,"
	                    + " codev.code_value as writeoffReason,"
	                    + " l.total_outstanding_derived as totalOutstanding,"
	                    + " l.total_overpaid_derived as totalOverpaid,"
	                    + " l.fixed_emi_amount as fixedEmiAmount,"
	                    + " l.max_outstanding_loan_balance as outstandingLoanBalance,"
	                    + " l.loan_sub_status_id as loanSubStatusId,"
	                    + " la.principal_overdue_derived as principalOverdue,"
	                    + " la.interest_overdue_derived as interestOverdue,"
	                    + " la.fee_charges_overdue_derived as feeChargesOverdue,"
	                    + " la.penalty_charges_overdue_derived as penaltyChargesOverdue,"
	                    + " la.total_overdue_derived as totalOverdue,"
	                    + " la.overdue_since_date_derived as overdueSinceDate,"
	                    + " l.sync_disbursement_with_meeting as syncDisbursementWithMeeting,"
	                    + " l.loan_counter as loanCounter, l.loan_product_counter as loanProductCounter,"
	                    + " l.is_npa as isNPA, l.days_in_month_enum as daysInMonth, l.days_in_year_enum as daysInYear, "
	                    + " l.interest_recalculation_enabled as isInterestRecalculationEnabled, "
	                    + " lir.id as lirId, lir.loan_id as loanId, lir.compound_type_enum as compoundType, lir.reschedule_strategy_enum as rescheduleStrategy, "
	                    + " lir.rest_frequency_type_enum as restFrequencyEnum, lir.rest_frequency_interval as restFrequencyInterval, "
	                    + " lir.rest_frequency_nth_day_enum as restFrequencyNthDayEnum, "
	                    + " lir.rest_frequency_weekday_enum as restFrequencyWeekDayEnum, "
	                    + " lir.rest_frequency_on_day as restFrequencyOnDay, "
	                    + " lir.compounding_frequency_type_enum as compoundingFrequencyEnum, lir.compounding_frequency_interval as compoundingInterval, "
	                    + " lir.compounding_frequency_nth_day_enum as compoundingFrequencyNthDayEnum, "
	                    + " lir.compounding_frequency_weekday_enum as compoundingFrequencyWeekDayEnum, "
	                    + " lir.compounding_frequency_on_day as compoundingFrequencyOnDay, "
	                    + " lir.is_compounding_to_be_posted_as_transaction as isCompoundingToBePostedAsTransaction, "
	                    + " lir.allow_compounding_on_eod as allowCompoundingOnEod, "
	                    + " l.is_floating_interest_rate as isFloatingInterestRate, "
	                    + " l.interest_rate_differential as interestRateDifferential, "
	                    + " l.create_standing_instruction_at_disbursement as createStandingInstructionAtDisbursement, "
	                    + " lpvi.minimum_gap as minimuminstallmentgap, lpvi.maximum_gap as maximuminstallmentgap, "
	                    + " lp.can_use_for_topup as canUseForTopup, "
	                    + " l.is_topup as isTopup, "
	                    + " topup.closure_loan_id as closureLoanId, "
	                    + " topuploan.account_no as closureLoanAccountNo, "
	                    + " topup.topup_amount as topupAmount "
	                    + " from m_loan l" //
	                    + " join m_product_loan lp on lp.id = l.product_id" //
	                    + " left join m_loan_recalculation_details lir on lir.loan_id = l.id "
	                    + " join m_currency rc on rc.`code` = l.currency_code" //
	                    + " left join m_client c on c.id = l.client_id" //
	                    + " left join m_group g on g.id = l.group_id" //
	                    + " left join m_loan_arrears_aging la on la.loan_id = l.id" //
	                    + " left join m_fund f on f.id = l.fund_id" //
	                    + " left join m_staff s on s.id = l.loan_officer_id" //
	                    + " left join m_appuser sbu on sbu.id = l.submittedon_userid"
	                    + " left join m_appuser rbu on rbu.id = l.rejectedon_userid"
	                    + " left join m_appuser wbu on wbu.id = l.withdrawnon_userid"
	                    + " left join m_appuser abu on abu.id = l.approvedon_userid"
	                    + " left join m_appuser dbu on dbu.id = l.disbursedon_userid"
	                    + " left join m_appuser cbu on cbu.id = l.closedon_userid"
	                    + " left join m_code_value cv on cv.id = l.loanpurpose_cv_id"
	                    + " left join m_code_value codev on codev.id = l.writeoff_reason_cv_id"
	                    + " left join ref_loan_transaction_processing_strategy lps on lps.id = l.loan_transaction_strategy_id"
	                    + " left join m_product_loan_variable_installment_config lpvi on lpvi.loan_product_id = l.product_id"
	                    + " left join m_loan_topup as topup on l.id = topup.loan_id"
	                    + " left join m_loan as topuploan on topuploan.id = topup.closure_loan_id";

	        }

	        @Override
	        public LoanAccountData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

	            final String currencyCode = rs.getString("currencyCode");
	            final String currencyName = rs.getString("currencyName");
	            final String currencyNameCode = rs.getString("currencyNameCode");
	            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
	            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
	            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
	            final CurrencyData currencyData = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
	                    currencyDisplaySymbol, currencyNameCode);

	            final Long id = rs.getLong("id");
	            final String accountNo = rs.getString("accountNo");
	            final String externalId = rs.getString("externalId");

	            final Long clientId = JdbcSupport.getLong(rs, "clientId");
	            final String clientAccountNo = rs.getString("clientAccountNo");
	            final Long clientOfficeId = JdbcSupport.getLong(rs, "clientOfficeId");
	            final String clientName = rs.getString("clientName");

	            final Long groupId = JdbcSupport.getLong(rs, "groupId");
	            final String groupName = rs.getString("groupName");
	            final String groupAccountNo = rs.getString("groupAccountNo");
	            final String groupExternalId = rs.getString("groupExternalId");
	            final Long groupOfficeId = JdbcSupport.getLong(rs, "groupOfficeId");
	            final Long groupStaffId = JdbcSupport.getLong(rs, "groupStaffId");
	            final Long groupParentId = JdbcSupport.getLong(rs, "groupParentId");
	            final String centerName = rs.getString("centerName");
	            final String groupHierarchy = rs.getString("groupHierarchy");
	            final String groupLevel = rs.getString("groupLevel");

	            final Integer loanTypeId = JdbcSupport.getInteger(rs, "loanType");
	            final EnumOptionData loanType = AccountEnumerations.loanType(loanTypeId);

	            final Long fundId = JdbcSupport.getLong(rs, "fundId");
	            final String fundName = rs.getString("fundName");

	            final Long loanOfficerId = JdbcSupport.getLong(rs, "loanOfficerId");
	            final String loanOfficerName = rs.getString("loanOfficerName");

	            final Long loanPurposeId = JdbcSupport.getLong(rs, "loanPurposeId");
	            final String loanPurposeName = rs.getString("loanPurposeName");

	            final Long loanProductId = JdbcSupport.getLong(rs, "loanProductId");
	            final String loanProductName = rs.getString("loanProductName");
	            final String loanProductDescription = rs.getString("loanProductDescription");
	            final boolean isLoanProductLinkedToFloatingRate = rs.getBoolean("isLoanProductLinkedToFloatingRate");
	            final Boolean multiDisburseLoan = rs.getBoolean("multiDisburseLoan");
	            final Boolean canDefineInstallmentAmount = rs.getBoolean("canDefineInstallmentAmount");
	            final BigDecimal outstandingLoanBalance = rs.getBigDecimal("outstandingLoanBalance");

	            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
	            final String submittedByUsername = rs.getString("submittedByUsername");
	            final String submittedByFirstname = rs.getString("submittedByFirstname");
	            final String submittedByLastname = rs.getString("submittedByLastname");

	            final LocalDate rejectedOnDate = JdbcSupport.getLocalDate(rs, "rejectedOnDate");
	            final String rejectedByUsername = rs.getString("rejectedByUsername");
	            final String rejectedByFirstname = rs.getString("rejectedByFirstname");
	            final String rejectedByLastname = rs.getString("rejectedByLastname");

	            final LocalDate withdrawnOnDate = JdbcSupport.getLocalDate(rs, "withdrawnOnDate");
	            final String withdrawnByUsername = rs.getString("withdrawnByUsername");
	            final String withdrawnByFirstname = rs.getString("withdrawnByFirstname");
	            final String withdrawnByLastname = rs.getString("withdrawnByLastname");

	            final LocalDate approvedOnDate = JdbcSupport.getLocalDate(rs, "approvedOnDate");
	            final String approvedByUsername = rs.getString("approvedByUsername");
	            final String approvedByFirstname = rs.getString("approvedByFirstname");
	            final String approvedByLastname = rs.getString("approvedByLastname");

	            final LocalDate expectedDisbursementDate = JdbcSupport.getLocalDate(rs, "expectedDisbursementDate");
	            final LocalDate actualDisbursementDate = JdbcSupport.getLocalDate(rs, "actualDisbursementDate");
	            final String disbursedByUsername = rs.getString("disbursedByUsername");
	            final String disbursedByFirstname = rs.getString("disbursedByFirstname");
	            final String disbursedByLastname = rs.getString("disbursedByLastname");

	            final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
	            final String closedByUsername = rs.getString("closedByUsername");
	            final String closedByFirstname = rs.getString("closedByFirstname");
	            final String closedByLastname = rs.getString("closedByLastname");

	            final LocalDate writtenOffOnDate = JdbcSupport.getLocalDate(rs, "writtenOffOnDate");
	            final Long writeoffReasonId = JdbcSupport.getLong(rs, "writeoffReasonId");
	            final String writeoffReason = rs.getString("writeoffReason");
	            final LocalDate expectedMaturityDate = JdbcSupport.getLocalDate(rs, "expectedMaturityDate");

	            final Boolean isvariableInstallmentsAllowed = rs.getBoolean("isvariableInstallmentsAllowed");
	            final Integer minimumGap = rs.getInt("minimuminstallmentgap");
	            final Integer maximumGap = rs.getInt("maximuminstallmentgap");

	            final LoanApplicationTimelineData timeline = new LoanApplicationTimelineData(submittedOnDate, submittedByUsername,
	                    submittedByFirstname, submittedByLastname, rejectedOnDate, rejectedByUsername, rejectedByFirstname, rejectedByLastname,
	                    withdrawnOnDate, withdrawnByUsername, withdrawnByFirstname, withdrawnByLastname, approvedOnDate, approvedByUsername,
	                    approvedByFirstname, approvedByLastname, expectedDisbursementDate, actualDisbursementDate, disbursedByUsername,
	                    disbursedByFirstname, disbursedByLastname, closedOnDate, closedByUsername, closedByFirstname, closedByLastname,
	                    expectedMaturityDate, writtenOffOnDate, closedByUsername, closedByFirstname, closedByLastname);

	            final BigDecimal principal = rs.getBigDecimal("principal");
	            final BigDecimal approvedPrincipal = rs.getBigDecimal("approvedPrincipal");
	            final BigDecimal proposedPrincipal = rs.getBigDecimal("proposedPrincipal");
	            final BigDecimal totalOverpaid = rs.getBigDecimal("totalOverpaid");
	            final BigDecimal inArrearsTolerance = rs.getBigDecimal("inArrearsTolerance");

	            final Integer numberOfRepayments = JdbcSupport.getInteger(rs, "numberOfRepayments");
	            final Integer repaymentEvery = JdbcSupport.getInteger(rs, "repaymentEvery");
	            final BigDecimal interestRatePerPeriod = rs.getBigDecimal("interestRatePerPeriod");
	            final BigDecimal annualInterestRate = rs.getBigDecimal("annualInterestRate");
	            final BigDecimal interestRateDifferential = rs.getBigDecimal("interestRateDifferential");
	            final boolean isFloatingInterestRate = rs.getBoolean("isFloatingInterestRate");

	            final Integer graceOnPrincipalPayment = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnPrincipalPayment");
	            final Integer recurringMoratoriumOnPrincipalPeriods = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "recurringMoratoriumOnPrincipalPeriods");
	            final Integer graceOnInterestPayment = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnInterestPayment");
	            final Integer graceOnInterestCharged = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnInterestCharged");
	            final Integer graceOnArrearsAgeing = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "graceOnArrearsAgeing");

	            final Integer termFrequency = JdbcSupport.getInteger(rs, "termFrequency");
	            final Integer termPeriodFrequencyTypeInt = JdbcSupport.getInteger(rs, "termPeriodFrequencyType");
	            final EnumOptionData termPeriodFrequencyType = LoanEnumerations.termFrequencyType(termPeriodFrequencyTypeInt);

	            final int repaymentFrequencyTypeInt = JdbcSupport.getInteger(rs, "repaymentFrequencyType");
	            final EnumOptionData repaymentFrequencyType = LoanEnumerations.repaymentFrequencyType(repaymentFrequencyTypeInt);

	            final int interestRateFrequencyTypeInt = JdbcSupport.getInteger(rs, "interestRateFrequencyType");
	            final EnumOptionData interestRateFrequencyType = LoanEnumerations.interestRateFrequencyType(interestRateFrequencyTypeInt);

	            final Long transactionStrategyId = JdbcSupport.getLong(rs, "transactionStrategyId");
	            final String transactionStrategyName = rs.getString("transactionStrategyName");

	            final int amortizationTypeInt = JdbcSupport.getInteger(rs, "amortizationType");
	            final int interestTypeInt = JdbcSupport.getInteger(rs, "interestType");
	            final int interestCalculationPeriodTypeInt = JdbcSupport.getInteger(rs, "interestCalculationPeriodType");

	            final EnumOptionData amortizationType = LoanEnumerations.amortizationType(amortizationTypeInt);
	            final EnumOptionData interestType = LoanEnumerations.interestType(interestTypeInt);
	            final EnumOptionData interestCalculationPeriodType = LoanEnumerations
	                    .interestCalculationPeriodType(interestCalculationPeriodTypeInt);
	            final Boolean allowPartialPeriodInterestCalcualtion = rs.getBoolean("allowPartialPeriodInterestCalcualtion");

	            final Integer lifeCycleStatusId = JdbcSupport.getInteger(rs, "lifeCycleStatusId");
	            final LoanStatusEnumData status = LoanEnumerations.status(lifeCycleStatusId);

	            final Integer loanSubStatusId = JdbcSupport.getInteger(rs, "loanSubStatusId");
	            EnumOptionData loanSubStatus = null;
	            if (loanSubStatusId != null) {
	                loanSubStatus = LoanSubStatus.loanSubStatus(loanSubStatusId);
	            }

	            // settings
	            final LocalDate expectedFirstRepaymentOnDate = JdbcSupport.getLocalDate(rs, "expectedFirstRepaymentOnDate");
	            final LocalDate interestChargedFromDate = JdbcSupport.getLocalDate(rs, "interestChargedFromDate");

	            final Boolean syncDisbursementWithMeeting = rs.getBoolean("syncDisbursementWithMeeting");

	            final BigDecimal feeChargesDueAtDisbursementCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs,
	                    "feeChargesDueAtDisbursementCharged");
	            LoanSummaryData loanSummary = null;
	            Boolean inArrears = false;
	            if (status.id().intValue() >= 300) {

	                // loan summary
	                final BigDecimal principalDisbursed = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalDisbursed");
	                final BigDecimal principalPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalPaid");
	                final BigDecimal principalWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalWrittenOff");
	                final BigDecimal principalOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalOutstanding");
	                final BigDecimal principalOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalOverdue");

	                final BigDecimal interestCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestCharged");
	                final BigDecimal interestPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestPaid");
	                final BigDecimal interestWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWaived");
	                final BigDecimal interestWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWrittenOff");
	                final BigDecimal interestOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestOutstanding");
	                final BigDecimal interestOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestOverdue");

	                final BigDecimal feeChargesCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesCharged");
	                final BigDecimal feeChargesPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesPaid");
	                final BigDecimal feeChargesWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesWaived");
	                final BigDecimal feeChargesWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesWrittenOff");
	                final BigDecimal feeChargesOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesOutstanding");
	                final BigDecimal feeChargesOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesOverdue");

	                final BigDecimal penaltyChargesCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesCharged");
	                final BigDecimal penaltyChargesPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesPaid");
	                final BigDecimal penaltyChargesWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesWaived");
	                final BigDecimal penaltyChargesWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesWrittenOff");
	                final BigDecimal penaltyChargesOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesOutstanding");
	                final BigDecimal penaltyChargesOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesOverdue");

	                final BigDecimal totalExpectedRepayment = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalExpectedRepayment");
	                final BigDecimal totalRepayment = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalRepayment");
	                final BigDecimal totalExpectedCostOfLoan = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalExpectedCostOfLoan");
	                final BigDecimal totalCostOfLoan = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalCostOfLoan");
	                final BigDecimal totalWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalWaived");
	                final BigDecimal totalWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalWrittenOff");
	                final BigDecimal totalOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalOutstanding");
	                final BigDecimal totalOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalOverdue");

	                final LocalDate overdueSinceDate = JdbcSupport.getLocalDate(rs, "overdueSinceDate");
	                if (overdueSinceDate != null) {
	                    inArrears = true;
	                }

	                loanSummary = new LoanSummaryData(currencyData, principalDisbursed, principalPaid, principalWrittenOff,
	                        principalOutstanding, principalOverdue, interestCharged, interestPaid, interestWaived, interestWrittenOff,
	                        interestOutstanding, interestOverdue, feeChargesCharged, feeChargesDueAtDisbursementCharged, feeChargesPaid,
	                        feeChargesWaived, feeChargesWrittenOff, feeChargesOutstanding, feeChargesOverdue, penaltyChargesCharged,
	                        penaltyChargesPaid, penaltyChargesWaived, penaltyChargesWrittenOff, penaltyChargesOutstanding,
	                        penaltyChargesOverdue, totalExpectedRepayment, totalRepayment, totalExpectedCostOfLoan, totalCostOfLoan,
	                        totalWaived, totalWrittenOff, totalOutstanding, totalOverdue, overdueSinceDate,writeoffReasonId, writeoffReason);
	            }

	            GroupGeneralData groupData = null;
	            if (groupId != null) {
	                final Integer groupStatusEnum = JdbcSupport.getInteger(rs, "statusEnum");
	                final EnumOptionData groupStatus = ClientEnumerations.status(groupStatusEnum);
	                final LocalDate activationDate = JdbcSupport.getLocalDate(rs, "activationDate");
	                groupData = GroupGeneralData.instance(groupId, groupAccountNo, groupName, groupExternalId, groupStatus, activationDate,
	                        groupOfficeId, null, groupParentId, centerName, groupStaffId, null, groupHierarchy, groupLevel, null);
	            }

	            final Integer loanCounter = JdbcSupport.getInteger(rs, "loanCounter");
	            final Integer loanProductCounter = JdbcSupport.getInteger(rs, "loanProductCounter");
	            final BigDecimal fixedEmiAmount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "fixedEmiAmount");
	            final Boolean isNPA = rs.getBoolean("isNPA");

	            final int daysInMonth = JdbcSupport.getInteger(rs, "daysInMonth");
	            final EnumOptionData daysInMonthType = CommonEnumerations.daysInMonthType(daysInMonth);
	            final int daysInYear = JdbcSupport.getInteger(rs, "daysInYear");
	            final EnumOptionData daysInYearType = CommonEnumerations.daysInYearType(daysInYear);
	            final boolean isInterestRecalculationEnabled = rs.getBoolean("isInterestRecalculationEnabled");
	            final Boolean createStandingInstructionAtDisbursement = rs.getBoolean("createStandingInstructionAtDisbursement");

	            LoanInterestRecalculationData interestRecalculationData = null;
	            if (isInterestRecalculationEnabled) {

	                final Long lprId = JdbcSupport.getLong(rs, "lirId");
	                final Long productId = JdbcSupport.getLong(rs, "loanId");
	                final int compoundTypeEnumValue = JdbcSupport.getInteger(rs, "compoundType");
	                final EnumOptionData interestRecalculationCompoundingType = LoanEnumerations
	                        .interestRecalculationCompoundingType(compoundTypeEnumValue);
	                final int rescheduleStrategyEnumValue = JdbcSupport.getInteger(rs, "rescheduleStrategy");
	                final EnumOptionData rescheduleStrategyType = LoanEnumerations.rescheduleStrategyType(rescheduleStrategyEnumValue);
	                final CalendarData calendarData = null;
	                final int restFrequencyEnumValue = JdbcSupport.getInteger(rs, "restFrequencyEnum");
	                final EnumOptionData restFrequencyType = LoanEnumerations.interestRecalculationFrequencyType(restFrequencyEnumValue);
	                final int restFrequencyInterval = JdbcSupport.getInteger(rs, "restFrequencyInterval");
	                final Integer restFrequencyNthDayEnumValue = JdbcSupport.getInteger(rs, "restFrequencyNthDayEnum");
	                EnumOptionData restFrequencyNthDayEnum = null;
	                if (restFrequencyNthDayEnumValue != null) {
	                    restFrequencyNthDayEnum = LoanEnumerations.interestRecalculationCompoundingNthDayType(restFrequencyNthDayEnumValue);
	                }
	                final Integer restFrequencyWeekDayEnumValue = JdbcSupport.getInteger(rs, "restFrequencyWeekDayEnum");
	                EnumOptionData restFrequencyWeekDayEnum = null;
	                if (restFrequencyWeekDayEnumValue != null) {
	                    restFrequencyWeekDayEnum = LoanEnumerations
	                            .interestRecalculationCompoundingDayOfWeekType(restFrequencyWeekDayEnumValue);
	                }
	                final Integer restFrequencyOnDay = JdbcSupport.getInteger(rs, "restFrequencyOnDay");
	                final CalendarData compoundingCalendarData = null;
	                final Integer compoundingFrequencyEnumValue = JdbcSupport.getInteger(rs, "compoundingFrequencyEnum");
	                EnumOptionData compoundingFrequencyType = null;
	                if (compoundingFrequencyEnumValue != null) {
	                    compoundingFrequencyType = LoanEnumerations.interestRecalculationFrequencyType(compoundingFrequencyEnumValue);
	                }
	                final Integer compoundingInterval = JdbcSupport.getInteger(rs, "compoundingInterval");
	                final Integer compoundingFrequencyNthDayEnumValue = JdbcSupport.getInteger(rs, "compoundingFrequencyNthDayEnum");
	                EnumOptionData compoundingFrequencyNthDayEnum = null;
	                if (compoundingFrequencyNthDayEnumValue != null) {
	                    compoundingFrequencyNthDayEnum = LoanEnumerations
	                            .interestRecalculationCompoundingNthDayType(compoundingFrequencyNthDayEnumValue);
	                }
	                final Integer compoundingFrequencyWeekDayEnumValue = JdbcSupport.getInteger(rs, "compoundingFrequencyWeekDayEnum");
	                EnumOptionData compoundingFrequencyWeekDayEnum = null;
	                if (compoundingFrequencyWeekDayEnumValue != null) {
	                    compoundingFrequencyWeekDayEnum = LoanEnumerations
	                            .interestRecalculationCompoundingDayOfWeekType(compoundingFrequencyWeekDayEnumValue);
	                }
	                final Integer compoundingFrequencyOnDay = JdbcSupport.getInteger(rs, "compoundingFrequencyOnDay");

	                final Boolean isCompoundingToBePostedAsTransaction = rs.getBoolean("isCompoundingToBePostedAsTransaction");
	                final Boolean allowCompoundingOnEod = rs.getBoolean("allowCompoundingOnEod");
	                interestRecalculationData = new LoanInterestRecalculationData(lprId, productId, interestRecalculationCompoundingType,
	                        rescheduleStrategyType, calendarData, restFrequencyType, restFrequencyInterval, restFrequencyNthDayEnum,
	                        restFrequencyWeekDayEnum, restFrequencyOnDay, compoundingCalendarData, compoundingFrequencyType,
	                        compoundingInterval, compoundingFrequencyNthDayEnum, compoundingFrequencyWeekDayEnum, compoundingFrequencyOnDay,
	                        isCompoundingToBePostedAsTransaction, allowCompoundingOnEod);
	            }

	            final boolean canUseForTopup = rs.getBoolean("canUseForTopup");
	            final boolean isTopup = rs.getBoolean("isTopup");
	            final Long closureLoanId = rs.getLong("closureLoanId");
	            final String closureLoanAccountNo = rs.getString("closureLoanAccountNo");
	            final BigDecimal topupAmount = rs.getBigDecimal("topupAmount");

	            return LoanAccountData.basicLoanDetails(id, accountNo, status, externalId, clientId, clientAccountNo, clientName,
	                    clientOfficeId, groupData, loanType, loanProductId, loanProductName, loanProductDescription,
	                    isLoanProductLinkedToFloatingRate, fundId, fundName, loanPurposeId, loanPurposeName, loanOfficerId, loanOfficerName,
	                    currencyData, proposedPrincipal, principal, approvedPrincipal, totalOverpaid, inArrearsTolerance, termFrequency,
	                    termPeriodFrequencyType, numberOfRepayments, repaymentEvery, repaymentFrequencyType, null, null, transactionStrategyId,
	                    transactionStrategyName, amortizationType, interestRatePerPeriod, interestRateFrequencyType, annualInterestRate,
	                    interestType, isFloatingInterestRate, interestRateDifferential, interestCalculationPeriodType,
	                    allowPartialPeriodInterestCalcualtion, expectedFirstRepaymentOnDate, graceOnPrincipalPayment,
	                    recurringMoratoriumOnPrincipalPeriods, graceOnInterestPayment, graceOnInterestCharged, interestChargedFromDate,
	                    timeline, loanSummary, feeChargesDueAtDisbursementCharged, syncDisbursementWithMeeting, loanCounter,
	                    loanProductCounter, multiDisburseLoan, canDefineInstallmentAmount, fixedEmiAmount, outstandingLoanBalance, inArrears,
	                    graceOnArrearsAgeing, isNPA, daysInMonthType, daysInYearType, isInterestRecalculationEnabled,
	                    interestRecalculationData, createStandingInstructionAtDisbursement, isvariableInstallmentsAllowed, minimumGap,
	                    maximumGap, loanSubStatus, canUseForTopup, isTopup, closureLoanId, closureLoanAccountNo, topupAmount);
	        }
	    }
}
