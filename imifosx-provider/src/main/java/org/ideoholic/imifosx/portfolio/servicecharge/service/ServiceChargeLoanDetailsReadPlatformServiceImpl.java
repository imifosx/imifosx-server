
package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.ideoholic.imifosx.infrastructure.core.domain.JdbcSupport;
import org.ideoholic.imifosx.infrastructure.core.service.DateUtils;
import org.ideoholic.imifosx.infrastructure.core.service.Page;
import org.ideoholic.imifosx.infrastructure.core.service.RoutingDataSource;
import org.ideoholic.imifosx.infrastructure.core.service.SearchParameters;
import org.ideoholic.imifosx.organisation.monetary.data.CurrencyData;
import org.ideoholic.imifosx.portfolio.account.data.AccountTransferData;
import org.ideoholic.imifosx.portfolio.loanaccount.data.LoanAccountData;
import org.ideoholic.imifosx.portfolio.loanaccount.data.LoanChargeData;
import org.ideoholic.imifosx.portfolio.loanaccount.data.LoanTransactionData;
import org.ideoholic.imifosx.portfolio.loanaccount.data.LoanTransactionEnumData;
import org.ideoholic.imifosx.portfolio.loanaccount.service.LoanChargeReadPlatformService;
import org.ideoholic.imifosx.portfolio.loanaccount.service.LoanReadPlatformService;
import org.ideoholic.imifosx.portfolio.loanproduct.service.LoanEnumerations;
import org.ideoholic.imifosx.portfolio.paymentdetail.data.PaymentDetailData;
import org.ideoholic.imifosx.portfolio.paymenttype.data.PaymentTypeData;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.QuarterDateRange;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeLoanDetailsReadPlatformServiceImpl implements ServiceChargeLoanDetailsReadPlatformService {

	private final static Logger logger = LoggerFactory.getLogger(ServiceChargeLoanDetailsReadPlatformServiceImpl.class);

	private final LoanReadPlatformService loanReadPlatformService;
	private final LoanChargeReadPlatformService loanChargeReadPlatformService;
	private final JdbcTemplate jdbcTemplate;
	
	@Autowired
	public ServiceChargeLoanDetailsReadPlatformServiceImpl(LoanReadPlatformService loanReadPlatformService, LoanChargeReadPlatformService loanChargeReadPlatformService,
			final RoutingDataSource dataSource) {
		this.loanReadPlatformService = loanReadPlatformService;
		this.loanChargeReadPlatformService = loanChargeReadPlatformService;
		this.jdbcTemplate = new JdbcTemplate(dataSource);;
	}

	public BigDecimal getTotalLoansForCurrentQuarter() {
		BigDecimal totalLoans = BigDecimal.ZERO;
		
		// Get the dates
		QuarterDateRange quarter = QuarterDateRange.getCurrentQuarter();
		String startDate = quarter.getFormattedFromDateString();
		String endDate = quarter.getFormattedToDateString();
		
		final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null, null);
		Page<LoanAccountData> loanAccountData = null;

		loanAccountData = loanReadPlatformService.retrieveLoansForCurrentQuarter(searchParameters,startDate,endDate);

		if (loanAccountData != null) {
			int totalNumberLoans = loanAccountData.getPageItems().size();
			totalLoans = new BigDecimal(totalNumberLoans);
		}

		return totalLoans;
	}

	public BigDecimal getAllLoansRepaymentData() throws Exception {
		logger.debug("entered into getAllLoansRepaymentData");

		BigDecimal totalRepayment = BigDecimal.ZERO;
		
		// create MathContext object with 2 precision
		MathContext mc = new MathContext(2);

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

		getLoansOutstandingAmount();
		
		for (int i = 0; i < loanAccountData.getPageItems().size(); i++) {
			logger.debug("Total number of accounts" + loanAccountData.getPageItems().size());
			logger.debug("Monthly Payments");
			try {
				System.out.println("The loan id is " + loanAccountData.getPageItems().get(i).getId());
				final Collection<LoanTransactionData> currentLoanRepayments = this.loanReadPlatformService
						.retrieveLoanTransactionsMonthlyPayments(loanAccountData.getPageItems().get(i).getId(), startDate, endDate);

				for (LoanTransactionData loanTransactionData : currentLoanRepayments) {
					logger.debug("Date = " + loanTransactionData.dateOf() + "  Repayment Amount = " + loanTransactionData.getAmount());

					// perform add operation on bg1 with augend bg2 and context mc
					totalRepayment = totalRepayment.add(loanTransactionData.getAmount(), mc);
				}
				
				// Get Loan Charge Name
				String loanCharges = getLoanChargeName(loanAccountData.getPageItems().get(i).getId());
		        	System.out.println("********** Loan Charge Name ************** "+loanCharges);	
		        	
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
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
	
	
	public BigDecimal getLoansOutstandingAmount() throws Exception {
		logger.debug("entered into getLoansOutstandingAmount");

		
		BigDecimal totalOutstandingAmount = BigDecimal.ZERO;
		// create MathContext object with 2 precision
		MathContext mc = new MathContext(2);

		// Get the dates
		QuarterDateRange quarter = QuarterDateRange.getCurrentQuarter();
		String startDate = quarter.getFormattedFromDateString();
		String endDate = quarter.getFormattedToDateString();

		final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null, null);
		Page<LoanAccountData> loanAccountDataForOutstandingAmount = null;
		try {
			loanAccountDataForOutstandingAmount = loanReadPlatformService.retrieveLoanDisbursementDetailsQuarterly(searchParameters,startDate,endDate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
		for (int i = 0; i < loanAccountDataForOutstandingAmount.getPageItems().size(); i++) {
			
			
			System.out.println("Total Outstanding Amount "+loanAccountDataForOutstandingAmount.getPageItems().get(i).getTotalOutstandingAmount());
			logger.debug("outstanding Amount");
			logger.debug("Account Loan id "+loanAccountDataForOutstandingAmount.getPageItems().get(i).getId());
			logger.debug("Outstanding Amount: "+loanAccountDataForOutstandingAmount.getPageItems().get(i).getTotalOutstandingAmount());
			totalOutstandingAmount = totalOutstandingAmount.add(loanAccountDataForOutstandingAmount.getPageItems().get(i).getTotalOutstandingAmount(), mc);
			
		}
		


		return totalOutstandingAmount;
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

		LoanAccountData loanAccountData = loanReadPlatformService.retrieveOneLoanForCurrentQuarter(searchParameters, loanId, startDate, endDate);
		if (loanAccountData != null) {
			return result = true;
		}
		return result;
	}

	private BigDecimal getTotalRepaymentsForGivenQuarter(Long loanId, QuarterDateRange range) {
		BigDecimal totalRepayment = BigDecimal.ZERO;
		// create MathContext object with 2 precision
		MathContext mc = new MathContext(2);

		// Get the dates
		String startDate = range.getFormattedFromDateString();
		String endDate = range.getFormattedToDateString();

		final Collection<LoanTransactionData> currentLoanRepayments = this.loanReadPlatformService.
				retrieveLoanTransactionsMonthlyPayments(loanId, startDate, endDate);

		for (LoanTransactionData loanTransactionData : currentLoanRepayments) {
			logger.debug("Date = " + loanTransactionData.dateOf() + "  Repayment Amount = " + loanTransactionData.getAmount());

			// perform add operation on bg1 with augend bg2 and context mc
			totalRepayment = totalRepayment.add(loanTransactionData.getAmount(), mc);
		}

		return totalRepayment;
	}

	private BigDecimal getTotalOutstandingAmountForGivenQuarter(Long loanId, QuarterDateRange range) {
		// TODO Musaib: given the loan-id, find the outstanding loan amount per-month in given quarter
		// this value is the amount-yet-to-be-paid on the 1st of every month, before any payment is made
		// or at the end of every month, after all payments are made
		// final value to be returned is the sum of all these values
		
		BigDecimal totalOutstandingAmount=BigDecimal.ZERO;
		BigDecimal totalRepayment = BigDecimal.ZERO;
		// create MathContext object with 2 precision
		MathContext mc = new MathContext(2);
		
		
		String startDate = range.getFormattedFromDateString();
		Date startDateDate = range.getFromDateForCurrentYear();
		Calendar cal = Calendar.getInstance();
		Date endDateDate = range.getFromDateForCurrentYear();
		
		cal.setTime(endDateDate);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		endDateDate = cal.getTime();
		String endDate = DateUtils.formatToSqlDate(endDateDate);
		
				
		for(int i=0;i<=2;i++){		
		
			final Collection<LoanTransactionData> currentLoanRepayments = retrieveLoanTransactionsOutstandingPayments(loanId, startDate, endDate);

			for (LoanTransactionData loanTransactionData : currentLoanRepayments) {
				logger.debug("Date = " + loanTransactionData.dateOf() + "  Repayment Amount = " + loanTransactionData.getOutstandingLoanBalance());

				// perform add operation on bg1 with augend bg2 and context mc
				totalOutstandingAmount = totalOutstandingAmount.add(loanTransactionData.getOutstandingLoanBalance(), mc);
			}
			cal.setTime(startDateDate);
			cal.add(Calendar.MONTH, 1);
			startDateDate = cal.getTime();
			startDate = DateUtils.formatToSqlDate(startDateDate);
			
			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			endDateDate = cal.getTime();
			endDate = DateUtils.formatToSqlDate(endDateDate);
			
			
		}
		System.out.println("Total outstanding amount "+totalOutstandingAmount);
		return totalOutstandingAmount;
	}
	
	@Override
	public Collection<LoanTransactionData> retrieveLoanTransactionsOutstandingPayments(
			Long loanId,String startDate, String endDate) {
	    try {
            

            final LoanTransactionsMapper rm = new LoanTransactionsMapper();

            // retrieve all loan transactions that are not invalid and have not
            // been 'contra'ed by another transaction
            // repayments at time of disbursement (e.g. charges)

            /***
             * TODO Vishwas: Remove references to "Contra" from the codebase
             ***/
            final String sql = "select "
                    + rm.LoanPaymentsSchema()
                   // + " where tr.loan_id = ? and tr.transaction_type_enum in (2) and  (tr.is_reversed=0 or tr.manually_adjusted_or_reversed = 1) order by tr.transaction_date ASC,id ";
                     + " where tr.loan_id = ? and tr.transaction_type_enum in (1,2) and  (tr.is_reversed=0 or tr.manually_adjusted_or_reversed = 1) and tr.transaction_date between '"+startDate+"' and '"+endDate+"'  order by tr.transaction_date ASC,id";
            return this.jdbcTemplate.query(sql, rm, new Object[] { loanId });
        } catch (final EmptyResultDataAccessException e) {
            return null;
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

}