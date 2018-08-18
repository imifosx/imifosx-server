package org.apache.fineract.portfolio.servicecharge.saving;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationData;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.savings.SavingsCompoundingInterestPeriodType;
import org.apache.fineract.portfolio.savings.SavingsInterestCalculationDaysInYearType;
import org.apache.fineract.portfolio.savings.SavingsInterestCalculationType;
import org.apache.fineract.portfolio.savings.SavingsPeriodFrequencyType;
import org.apache.fineract.portfolio.savings.SavingsPostingInterestPeriodType;
import org.apache.fineract.portfolio.savings.data.SavingsAccountApplicationTimelineData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountStatusEnumData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountSubStatusEnumData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountSummaryData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountSubStatusEnum;
import org.apache.fineract.portfolio.savings.service.SavingsEnumerations;
import org.apache.fineract.portfolio.servicecharge.exception.DepositAccountTransactionUpperLimitException;
import org.apache.fineract.portfolio.tax.data.TaxGroupData;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class SavingAccountsCalculationPlatformServiceImpl implements SavingAccountsCalculationPlatformService {

	private final static Logger logger = LoggerFactory.getLogger(SavingAccountsCalculationPlatformServiceImpl.class);
	private final SavingAccountMapper savingAccountMapper;
	private final JdbcTemplate jdbcTemplate;
	private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
	private final DefaultToApiJsonSerializer<GlobalConfigurationData> toApiJsonSerializer;
	private final GlobalConfigurationRepositoryWrapper globalConfigurationRepository;
	private final ConfigurationDomainService configurationDomainService;

	@Autowired
	public SavingAccountsCalculationPlatformServiceImpl(final RoutingDataSource dataSource,
			final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
			final DefaultToApiJsonSerializer<GlobalConfigurationData> toApiJsonSerializer,
			final GlobalConfigurationRepositoryWrapper globalConfigurationRepository,
			final ConfigurationDomainService configurationDomainService) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.savingAccountMapper = new SavingAccountMapper();
		this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
		this.toApiJsonSerializer = toApiJsonSerializer;
		this.globalConfigurationRepository = globalConfigurationRepository;
		this.configurationDomainService = configurationDomainService;
	}

	@Override
	public void validateDepositUpperLimit(BigDecimal accountBalance, Long savingsId) {

		BigDecimal totalAverageSavingDeposit = calculateAverageSavings();
		if (null == totalAverageSavingDeposit) {
			return;
		}

		if (accountBalance.compareTo(totalAverageSavingDeposit) == 1) {
			throw new DepositAccountTransactionUpperLimitException(savingsId);
		}

	}

	@Override
	public BigDecimal calculateAverageSavings() {
		// In case the max deposit limit setting is not enabled
		if (!configurationDomainService.isMaxDepositLimitEnabled()) {
			return null;
		}

		// Multiplier to be used with average deposit amount
		int multiplier = configurationDomainService.retrieveMaxDepositMultiplier();

		BigDecimal avgDepositInSavings = findAvgDepositAmount();

		// Set limit is the multiplier times the average deposit
		// TODO: Improve the getRoundingMode() as this will return Enum integer and not the number of decimal places
		return avgDepositInSavings.multiply(new BigDecimal(multiplier),
				new MathContext(configurationDomainService.getRoundingMode()));
	}

	/**
	 * Only if the setting of Average deposit is enabled will set the value to the
	 * current average deposit, else get the value that is set in the configuration and return the configuration value
	 * 
	 * @return BigDecimal: average deposit amount
	 */
	private BigDecimal findAvgDepositAmount() {
		BigDecimal depositAmount;
		// Get configuration id
		final String propertyName = "Avg-Deposit-In-Savings";
		GlobalConfigurationProperty avgDepositConfig = this.globalConfigurationRepository
				.findOneByNameWithNotFoundDetection(propertyName);
		//
		if (avgDepositConfig.isEnabled()) {
			depositAmount = calculateAvgSavingsConfigAmount();
			updateConfiguration(avgDepositConfig.getId(), depositAmount.longValue());
		} else {
			depositAmount = new BigDecimal(avgDepositConfig.getValue());
		}
		return depositAmount;
	}

	private BigDecimal calculateAvgSavingsConfigAmount() {
		Collection<SavingsAccountData> listSavingsAccount = retrieveAllSavingAccounts();
		BigDecimal totalSavingsBalance = BigDecimal.ZERO;
		final RoundingMode roundingMode = MoneyHelper.getRoundingMode();

		for (SavingsAccountData savingsAccountData : listSavingsAccount) {
			totalSavingsBalance = totalSavingsBalance.add(savingsAccountData.getSummary().getAvailableBalance());
		}

		BigDecimal averageSavingDeposit = totalSavingsBalance.divide(new BigDecimal(listSavingsAccount.size()),
				roundingMode);

		logger.info(
				"SavingAccountsCalculationPlatformServiceImpl:calculateAvgSavingsConfigAmount()::total Current Account Balance-"
						+ averageSavingDeposit);

		return averageSavingDeposit;
	}

	private void updateConfiguration(GlobalConfigurationProperty avgDepositConfig,
			BigDecimal avgDepositAmount) {
		// avgDepositConfig.setValue(avgDepositAmount.longValue());
		globalConfigurationRepository.saveAndFlush(avgDepositConfig);
	}

	private String updateConfiguration(long configId, Long avgSavingsDeposit) {
		String jsonCommand = "{\"enabled\":\"true\", \"value\":" + avgSavingsDeposit + "}";

		final CommandWrapper commandRequest = new CommandWrapperBuilder() //
				.updateGlobalConfiguration(configId) //
				.withJson(jsonCommand) //
				.build();

		final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

		return this.toApiJsonSerializer.serialize(result);
	}

	private Collection<SavingsAccountData> retrieveAllSavingAccounts() {

		final StringBuilder sqlBuilder = new StringBuilder("select " + this.savingAccountMapper.schema());
		sqlBuilder.append(" where sa.status_enum = 300 or sa.status_enum = 303 or  sa.status_enum = 304");
		return this.jdbcTemplate.query(sqlBuilder.toString(), this.savingAccountMapper);

	}

	private static final class SavingAccountMapper implements RowMapper<SavingsAccountData> {

		private final String schemaSql;

		public SavingAccountMapper() {
			final StringBuilder sqlBuilder = new StringBuilder(400);
			sqlBuilder.append("sa.id as id, sa.account_no as accountNo, sa.external_id as externalId, ");
			sqlBuilder.append("sa.deposit_type_enum as depositType, ");
			sqlBuilder.append("c.id as clientId, c.display_name as clientName, ");
			sqlBuilder.append("g.id as groupId, g.display_name as groupName, ");
			sqlBuilder.append("sp.id as productId, sp.name as productName, ");
			sqlBuilder.append("s.id fieldOfficerId, s.display_name as fieldOfficerName, ");
			sqlBuilder.append("sa.status_enum as statusEnum, ");
			sqlBuilder.append("sa.sub_status_enum as subStatusEnum, ");
			sqlBuilder.append("sa.submittedon_date as submittedOnDate,");
			sqlBuilder.append("sbu.username as submittedByUsername,");
			sqlBuilder.append("sbu.firstname as submittedByFirstname, sbu.lastname as submittedByLastname,");

			sqlBuilder.append("sa.rejectedon_date as rejectedOnDate,");
			sqlBuilder.append("rbu.username as rejectedByUsername,");
			sqlBuilder.append("rbu.firstname as rejectedByFirstname, rbu.lastname as rejectedByLastname,");

			sqlBuilder.append("sa.withdrawnon_date as withdrawnOnDate,");
			sqlBuilder.append("wbu.username as withdrawnByUsername,");
			sqlBuilder.append("wbu.firstname as withdrawnByFirstname, wbu.lastname as withdrawnByLastname,");

			sqlBuilder.append("sa.approvedon_date as approvedOnDate,");
			sqlBuilder.append("abu.username as approvedByUsername,");
			sqlBuilder.append("abu.firstname as approvedByFirstname, abu.lastname as approvedByLastname,");

			sqlBuilder.append("sa.activatedon_date as activatedOnDate,");
			sqlBuilder.append("avbu.username as activatedByUsername,");
			sqlBuilder.append("avbu.firstname as activatedByFirstname, avbu.lastname as activatedByLastname,");

			sqlBuilder.append("sa.closedon_date as closedOnDate,");
			sqlBuilder.append("cbu.username as closedByUsername,");
			sqlBuilder.append("cbu.firstname as closedByFirstname, cbu.lastname as closedByLastname,");

			sqlBuilder.append(
					"sa.currency_code as currencyCode, sa.currency_digits as currencyDigits, sa.currency_multiplesof as inMultiplesOf, ");
			sqlBuilder.append("curr.name as currencyName, curr.internationalized_name_code as currencyNameCode, ");
			sqlBuilder.append("curr.display_symbol as currencyDisplaySymbol, ");

			sqlBuilder.append("sa.nominal_annual_interest_rate as nominalAnnualInterestRate, ");
			sqlBuilder.append("sa.interest_compounding_period_enum as interestCompoundingPeriodType, ");
			sqlBuilder.append("sa.interest_posting_period_enum as interestPostingPeriodType, ");
			sqlBuilder.append("sa.interest_calculation_type_enum as interestCalculationType, ");
			sqlBuilder.append("sa.interest_calculation_days_in_year_type_enum as interestCalculationDaysInYearType, ");
			sqlBuilder.append("sa.min_required_opening_balance as minRequiredOpeningBalance, ");
			sqlBuilder.append("sa.lockin_period_frequency as lockinPeriodFrequency,");
			sqlBuilder.append("sa.lockin_period_frequency_enum as lockinPeriodFrequencyType, ");
			// sqlBuilder.append("sa.withdrawal_fee_amount as
			// withdrawalFeeAmount,");
			// sqlBuilder.append("sa.withdrawal_fee_type_enum as
			// withdrawalFeeTypeEnum, ");
			sqlBuilder.append("sa.withdrawal_fee_for_transfer as withdrawalFeeForTransfers, ");
			sqlBuilder.append("sa.allow_overdraft as allowOverdraft, ");
			sqlBuilder.append("sa.overdraft_limit as overdraftLimit, ");
			sqlBuilder.append("sa.nominal_annual_interest_rate_overdraft as nominalAnnualInterestRateOverdraft, ");
			sqlBuilder.append("sa.min_overdraft_for_interest_calculation as minOverdraftForInterestCalculation, ");
			// sqlBuilder.append("sa.annual_fee_amount as annualFeeAmount,");
			// sqlBuilder.append("sa.annual_fee_on_month as annualFeeOnMonth,
			// ");
			// sqlBuilder.append("sa.annual_fee_on_day as annualFeeOnDay, ");
			// sqlBuilder.append("sa.annual_fee_next_due_date as
			// annualFeeNextDueDate, ");
			sqlBuilder.append("sa.total_deposits_derived as totalDeposits, ");
			sqlBuilder.append("sa.total_withdrawals_derived as totalWithdrawals, ");
			sqlBuilder.append("sa.total_withdrawal_fees_derived as totalWithdrawalFees, ");
			sqlBuilder.append("sa.total_annual_fees_derived as totalAnnualFees, ");
			sqlBuilder.append("sa.total_interest_earned_derived as totalInterestEarned, ");
			sqlBuilder.append("sa.total_interest_posted_derived as totalInterestPosted, ");
			sqlBuilder.append("sa.total_overdraft_interest_derived as totalOverdraftInterestDerived, ");
			sqlBuilder.append("sa.account_balance_derived as accountBalance, ");
			sqlBuilder.append("sa.total_fees_charge_derived as totalFeeCharge, ");
			sqlBuilder.append("sa.total_penalty_charge_derived as totalPenaltyCharge, ");
			sqlBuilder.append("sa.min_balance_for_interest_calculation as minBalanceForInterestCalculation,");
			sqlBuilder.append("sa.min_required_balance as minRequiredBalance, ");
			sqlBuilder.append("sa.enforce_min_required_balance as enforceMinRequiredBalance, ");
			sqlBuilder.append("sa.on_hold_funds_derived as onHoldFunds, ");
			sqlBuilder.append("sa.withhold_tax as withHoldTax, ");
			sqlBuilder.append("sa.total_withhold_tax_derived as totalWithholdTax, ");
			sqlBuilder.append("sa.last_interest_calculation_date as lastInterestCalculationDate, ");
			sqlBuilder.append("sa.total_savings_amount_on_hold as onHoldAmount, ");
			sqlBuilder.append("tg.id as taxGroupId, tg.name as taxGroupName, ");
			sqlBuilder.append("(select IFNULL(max(sat.transaction_date),sa.activatedon_date) ");
			sqlBuilder.append("from m_savings_account_transaction as sat ");
			sqlBuilder.append("where sat.is_reversed = 0 ");
			sqlBuilder.append("and sat.transaction_type_enum in (1,2) ");
			sqlBuilder.append("and sat.savings_account_id = sa.id) as lastActiveTransactionDate, ");
			sqlBuilder.append("sp.is_dormancy_tracking_active as isDormancyTrackingActive, ");
			sqlBuilder.append("sp.days_to_inactive as daysToInactive, ");
			sqlBuilder.append("sp.days_to_dormancy as daysToDormancy, ");
			sqlBuilder.append("sp.days_to_escheat as daysToEscheat ");
			sqlBuilder.append("from m_savings_account sa ");
			sqlBuilder.append("join m_savings_product sp ON sa.product_id = sp.id ");
			sqlBuilder.append("join m_currency curr on curr.code = sa.currency_code ");
			sqlBuilder.append("left join m_client c ON c.id = sa.client_id ");
			sqlBuilder.append("left join m_group g ON g.id = sa.group_id ");
			sqlBuilder.append("left join m_staff s ON s.id = sa.field_officer_id ");
			sqlBuilder.append("left join m_appuser sbu on sbu.id = sa.submittedon_userid ");
			sqlBuilder.append("left join m_appuser rbu on rbu.id = sa.rejectedon_userid ");
			sqlBuilder.append("left join m_appuser wbu on wbu.id = sa.withdrawnon_userid ");
			sqlBuilder.append("left join m_appuser abu on abu.id = sa.approvedon_userid ");
			sqlBuilder.append("left join m_appuser avbu on avbu.id = sa.activatedon_userid ");
			sqlBuilder.append("left join m_appuser cbu on cbu.id = sa.closedon_userid ");
			sqlBuilder.append("left join m_tax_group tg on tg.id = sa.tax_group_id  ");

			this.schemaSql = sqlBuilder.toString();
		}

		public String schema() {
			return this.schemaSql;
		}

		@Override
		public SavingsAccountData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final Long id = rs.getLong("id");
			final String accountNo = rs.getString("accountNo");
			final String externalId = rs.getString("externalId");
			final Integer depositTypeId = rs.getInt("depositType");
			final EnumOptionData depositType = SavingsEnumerations.depositType(depositTypeId);

			final Long groupId = JdbcSupport.getLong(rs, "groupId");
			final String groupName = rs.getString("groupName");
			final Long clientId = JdbcSupport.getLong(rs, "clientId");
			final String clientName = rs.getString("clientName");

			final Long productId = rs.getLong("productId");
			final String productName = rs.getString("productName");

			final Long fieldOfficerId = rs.getLong("fieldOfficerId");
			final String fieldOfficerName = rs.getString("fieldOfficerName");

			final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
			final SavingsAccountStatusEnumData status = SavingsEnumerations.status(statusEnum);

			final Integer subStatusEnum = JdbcSupport.getInteger(rs, "subStatusEnum");
			final SavingsAccountSubStatusEnumData subStatus = SavingsEnumerations.subStatus(subStatusEnum);

			final LocalDate lastActiveTransactionDate = JdbcSupport.getLocalDate(rs, "lastActiveTransactionDate");
			final boolean isDormancyTrackingActive = rs.getBoolean("isDormancyTrackingActive");
			final Integer numDaysToInactive = JdbcSupport.getInteger(rs, "daysToInactive");
			final Integer numDaysToDormancy = JdbcSupport.getInteger(rs, "daysToDormancy");
			final Integer numDaysToEscheat = JdbcSupport.getInteger(rs, "daysToEscheat");
			Integer daysToInactive = null;
			Integer daysToDormancy = null;
			Integer daysToEscheat = null;

			LocalDate localTenantDate = DateUtils.getLocalDateOfTenant();
			if (isDormancyTrackingActive && statusEnum.equals(SavingsAccountStatusType.ACTIVE.getValue())) {
				if (subStatusEnum < SavingsAccountSubStatusEnum.ESCHEAT.getValue()) {
					daysToEscheat = Days
							.daysBetween(localTenantDate, lastActiveTransactionDate.plusDays(numDaysToEscheat))
							.getDays();
				}
				if (subStatusEnum < SavingsAccountSubStatusEnum.DORMANT.getValue()) {
					daysToDormancy = Days
							.daysBetween(localTenantDate, lastActiveTransactionDate.plusDays(numDaysToDormancy))
							.getDays();
				}
				if (subStatusEnum < SavingsAccountSubStatusEnum.INACTIVE.getValue()) {
					daysToInactive = Days
							.daysBetween(localTenantDate, lastActiveTransactionDate.plusDays(numDaysToInactive))
							.getDays();
				}
			}

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

			final LocalDate activatedOnDate = JdbcSupport.getLocalDate(rs, "activatedOnDate");
			final String activatedByUsername = rs.getString("activatedByUsername");
			final String activatedByFirstname = rs.getString("activatedByFirstname");
			final String activatedByLastname = rs.getString("activatedByLastname");

			final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
			final String closedByUsername = rs.getString("closedByUsername");
			final String closedByFirstname = rs.getString("closedByFirstname");
			final String closedByLastname = rs.getString("closedByLastname");

			final SavingsAccountApplicationTimelineData timeline = new SavingsAccountApplicationTimelineData(
					submittedOnDate, submittedByUsername, submittedByFirstname, submittedByLastname, rejectedOnDate,
					rejectedByUsername, rejectedByFirstname, rejectedByLastname, withdrawnOnDate, withdrawnByUsername,
					withdrawnByFirstname, withdrawnByLastname, approvedOnDate, approvedByUsername, approvedByFirstname,
					approvedByLastname, activatedOnDate, activatedByUsername, activatedByFirstname, activatedByLastname,
					closedOnDate, closedByUsername, closedByFirstname, closedByLastname);

			final String currencyCode = rs.getString("currencyCode");
			final String currencyName = rs.getString("currencyName");
			final String currencyNameCode = rs.getString("currencyNameCode");
			final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
			final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
			final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
			final CurrencyData currency = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
					currencyDisplaySymbol, currencyNameCode);

			final BigDecimal nominalAnnualInterestRate = rs.getBigDecimal("nominalAnnualInterestRate");

			final EnumOptionData interestCompoundingPeriodType = SavingsEnumerations
					.compoundingInterestPeriodType(SavingsCompoundingInterestPeriodType
							.fromInt(JdbcSupport.getInteger(rs, "interestCompoundingPeriodType")));

			final EnumOptionData interestPostingPeriodType = SavingsEnumerations.interestPostingPeriodType(
					SavingsPostingInterestPeriodType.fromInt(JdbcSupport.getInteger(rs, "interestPostingPeriodType")));

			final EnumOptionData interestCalculationType = SavingsEnumerations.interestCalculationType(
					SavingsInterestCalculationType.fromInt(JdbcSupport.getInteger(rs, "interestCalculationType")));

			final EnumOptionData interestCalculationDaysInYearType = SavingsEnumerations
					.interestCalculationDaysInYearType(SavingsInterestCalculationDaysInYearType
							.fromInt(JdbcSupport.getInteger(rs, "interestCalculationDaysInYearType")));

			final BigDecimal minRequiredOpeningBalance = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
					"minRequiredOpeningBalance");

			final Integer lockinPeriodFrequency = JdbcSupport.getInteger(rs, "lockinPeriodFrequency");
			EnumOptionData lockinPeriodFrequencyType = null;
			final Integer lockinPeriodFrequencyTypeValue = JdbcSupport.getInteger(rs, "lockinPeriodFrequencyType");
			if (lockinPeriodFrequencyTypeValue != null) {
				final SavingsPeriodFrequencyType lockinPeriodType = SavingsPeriodFrequencyType
						.fromInt(lockinPeriodFrequencyTypeValue);
				lockinPeriodFrequencyType = SavingsEnumerations.lockinPeriodFrequencyType(lockinPeriodType);
			}

			/*
			 * final BigDecimal withdrawalFeeAmount =
			 * rs.getBigDecimal("withdrawalFeeAmount");
			 * 
			 * EnumOptionData withdrawalFeeType = null; final Integer withdrawalFeeTypeValue
			 * = JdbcSupport.getInteger(rs, "withdrawalFeeTypeEnum"); if
			 * (withdrawalFeeTypeValue != null) { withdrawalFeeType =
			 * SavingsEnumerations.withdrawalFeeType(withdrawalFeeTypeValue); }
			 */

			final boolean withdrawalFeeForTransfers = rs.getBoolean("withdrawalFeeForTransfers");

			final boolean allowOverdraft = rs.getBoolean("allowOverdraft");
			final BigDecimal overdraftLimit = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "overdraftLimit");
			final BigDecimal nominalAnnualInterestRateOverdraft = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
					"nominalAnnualInterestRateOverdraft");
			final BigDecimal minOverdraftForInterestCalculation = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
					"minOverdraftForInterestCalculation");

			final BigDecimal minRequiredBalance = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
					"minRequiredBalance");
			final boolean enforceMinRequiredBalance = rs.getBoolean("enforceMinRequiredBalance");

			/*
			 * final BigDecimal annualFeeAmount =
			 * JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "annualFeeAmount");
			 * 
			 * MonthDay annualFeeOnMonthDay = null; final Integer annualFeeOnMonth =
			 * JdbcSupport.getInteger(rs, "annualFeeOnMonth"); final Integer annualFeeOnDay
			 * = JdbcSupport.getInteger(rs, "annualFeeOnDay"); if (annualFeeAmount != null
			 * && annualFeeOnDay != null) { annualFeeOnMonthDay = new
			 * MonthDay(annualFeeOnMonth, annualFeeOnDay); }
			 * 
			 * final LocalDate annualFeeNextDueDate = JdbcSupport.getLocalDate(rs,
			 * "annualFeeNextDueDate");
			 */
			final BigDecimal totalDeposits = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalDeposits");
			final BigDecimal totalWithdrawals = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalWithdrawals");
			final BigDecimal totalWithdrawalFees = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
					"totalWithdrawalFees");
			final BigDecimal totalAnnualFees = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalAnnualFees");

			final BigDecimal totalInterestEarned = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
					"totalInterestEarned");
			final BigDecimal totalInterestPosted = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs,
					"totalInterestPosted");
			final BigDecimal accountBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "accountBalance");
			final BigDecimal totalFeeCharge = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalFeeCharge");
			final BigDecimal totalPenaltyCharge = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
					"totalPenaltyCharge");
			final BigDecimal totalOverdraftInterestDerived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs,
					"totalOverdraftInterestDerived");
			final BigDecimal totalWithholdTax = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalWithholdTax");

			final BigDecimal minBalanceForInterestCalculation = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
					"minBalanceForInterestCalculation");
			final BigDecimal onHoldFunds = rs.getBigDecimal("onHoldFunds");

			final BigDecimal onHoldAmount = rs.getBigDecimal("onHoldAmount");

			BigDecimal availableBalance = accountBalance;
			if (availableBalance != null && onHoldFunds != null) {

				availableBalance = availableBalance.subtract(onHoldFunds);
			}

			if (availableBalance != null && onHoldAmount != null) {

				availableBalance = availableBalance.subtract(onHoldAmount);
			}

			BigDecimal interestNotPosted = BigDecimal.ZERO;
			LocalDate lastInterestCalculationDate = null;
			if (totalInterestEarned != null) {
				interestNotPosted = totalInterestEarned.subtract(totalInterestPosted)
						.add(totalOverdraftInterestDerived);
				lastInterestCalculationDate = JdbcSupport.getLocalDate(rs, "lastInterestCalculationDate");
			}

			final SavingsAccountSummaryData summary = new SavingsAccountSummaryData(currency, totalDeposits,
					totalWithdrawals, totalWithdrawalFees, totalAnnualFees, totalInterestEarned, totalInterestPosted,
					accountBalance, totalFeeCharge, totalPenaltyCharge, totalOverdraftInterestDerived, totalWithholdTax,
					interestNotPosted, lastInterestCalculationDate, availableBalance);

			final boolean withHoldTax = rs.getBoolean("withHoldTax");
			final Long taxGroupId = JdbcSupport.getLong(rs, "taxGroupId");
			final String taxGroupName = rs.getString("taxGroupName");
			TaxGroupData taxGroupData = null;
			if (taxGroupId != null) {
				taxGroupData = TaxGroupData.lookup(taxGroupId, taxGroupName);
			}

			return SavingsAccountData.instance(id, accountNo, depositType, externalId, groupId, groupName, clientId,
					clientName, productId, productName, fieldOfficerId, fieldOfficerName, status, subStatus, timeline,
					currency, nominalAnnualInterestRate, interestCompoundingPeriodType, interestPostingPeriodType,
					interestCalculationType, interestCalculationDaysInYearType, minRequiredOpeningBalance,
					lockinPeriodFrequency, lockinPeriodFrequencyType, withdrawalFeeForTransfers, summary,
					allowOverdraft, overdraftLimit, minRequiredBalance, enforceMinRequiredBalance,
					minBalanceForInterestCalculation, onHoldFunds, nominalAnnualInterestRateOverdraft,
					minOverdraftForInterestCalculation, withHoldTax, taxGroupData, lastActiveTransactionDate,
					isDormancyTrackingActive, daysToInactive, daysToDormancy, daysToEscheat, onHoldAmount);
		}

	}

}
