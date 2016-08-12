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
package org.ideoholic.imifosx.portfolio.savings.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.ideoholic.imifosx.accounting.common.AccountingDropdownReadPlatformService;
import org.ideoholic.imifosx.accounting.common.AccountingEnumerations;
import org.ideoholic.imifosx.accounting.common.AccountingRuleType;
import org.ideoholic.imifosx.accounting.glaccount.data.GLAccountData;
import org.ideoholic.imifosx.accounting.producttoaccountmapping.data.ChargeToGLAccountMapper;
import org.ideoholic.imifosx.accounting.producttoaccountmapping.data.PaymentTypeToGLAccountMapper;
import org.ideoholic.imifosx.accounting.producttoaccountmapping.service.ProductToGLAccountMappingReadPlatformService;
import org.ideoholic.imifosx.commands.domain.CommandWrapper;
import org.ideoholic.imifosx.commands.service.CommandWrapperBuilder;
import org.ideoholic.imifosx.commands.service.PortfolioCommandSourceWritePlatformService;
import org.ideoholic.imifosx.infrastructure.core.api.ApiRequestParameterHelper;
import org.ideoholic.imifosx.infrastructure.core.data.CommandProcessingResult;
import org.ideoholic.imifosx.infrastructure.core.data.EnumOptionData;
import org.ideoholic.imifosx.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.ideoholic.imifosx.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.ideoholic.imifosx.infrastructure.security.service.PlatformSecurityContext;
import org.ideoholic.imifosx.organisation.monetary.data.CurrencyData;
import org.ideoholic.imifosx.organisation.monetary.service.CurrencyReadPlatformService;
import org.ideoholic.imifosx.portfolio.charge.data.ChargeData;
import org.ideoholic.imifosx.portfolio.charge.service.ChargeReadPlatformService;
import org.ideoholic.imifosx.portfolio.common.service.DropdownReadPlatformService;
import org.ideoholic.imifosx.portfolio.interestratechart.data.InterestRateChartData;
import org.ideoholic.imifosx.portfolio.interestratechart.service.InterestRateChartReadPlatformService;
import org.ideoholic.imifosx.portfolio.paymenttype.data.PaymentTypeData;
import org.ideoholic.imifosx.portfolio.paymenttype.service.PaymentTypeReadPlatformService;
import org.ideoholic.imifosx.portfolio.savings.DepositAccountType;
import org.ideoholic.imifosx.portfolio.savings.DepositsApiConstants;
import org.ideoholic.imifosx.portfolio.savings.SavingsCompoundingInterestPeriodType;
import org.ideoholic.imifosx.portfolio.savings.SavingsInterestCalculationDaysInYearType;
import org.ideoholic.imifosx.portfolio.savings.SavingsInterestCalculationType;
import org.ideoholic.imifosx.portfolio.savings.SavingsPostingInterestPeriodType;
import org.ideoholic.imifosx.portfolio.savings.data.RecurringDepositProductData;
import org.ideoholic.imifosx.portfolio.savings.service.DepositProductReadPlatformService;
import org.ideoholic.imifosx.portfolio.savings.service.DepositsDropdownReadPlatformService;
import org.ideoholic.imifosx.portfolio.savings.service.SavingsDropdownReadPlatformService;
import org.ideoholic.imifosx.portfolio.savings.service.SavingsEnumerations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Path("/recurringdepositproducts")
@Component
@Scope("singleton")
public class RecurringDepositProductsApiResource {

    private final DepositProductReadPlatformService depositProductReadPlatformService;
    private final SavingsDropdownReadPlatformService savingsDropdownReadPlatformService;
    private final CurrencyReadPlatformService currencyReadPlatformService;
    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer<RecurringDepositProductData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final AccountingDropdownReadPlatformService accountingDropdownReadPlatformService;
    private final ProductToGLAccountMappingReadPlatformService accountMappingReadPlatformService;
    private final ChargeReadPlatformService chargeReadPlatformService;
    private final InterestRateChartReadPlatformService chartReadPlatformService;
    private final InterestRateChartReadPlatformService interestRateChartReadPlatformService;
    private final DepositsDropdownReadPlatformService depositsDropdownReadPlatformService;
    private final DropdownReadPlatformService dropdownReadPlatformService;
    private final PaymentTypeReadPlatformService paymentTypeReadPlatformService;

    @Autowired
    public RecurringDepositProductsApiResource(final DepositProductReadPlatformService depositProductReadPlatformService,
            final SavingsDropdownReadPlatformService savingsDropdownReadPlatformService,
            final CurrencyReadPlatformService currencyReadPlatformService, final PlatformSecurityContext context,
            final DefaultToApiJsonSerializer<RecurringDepositProductData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            final AccountingDropdownReadPlatformService accountingDropdownReadPlatformService,
            final ProductToGLAccountMappingReadPlatformService accountMappingReadPlatformService,
            final ChargeReadPlatformService chargeReadPlatformService, final InterestRateChartReadPlatformService chartReadPlatformService,
            final InterestRateChartReadPlatformService interestRateChartReadPlatformService,
            final DepositsDropdownReadPlatformService depositsDropdownReadPlatformService,
            final DropdownReadPlatformService dropdownReadPlatformService,
            final PaymentTypeReadPlatformService paymentTypeReadPlatformService) {
        this.depositProductReadPlatformService = depositProductReadPlatformService;
        this.savingsDropdownReadPlatformService = savingsDropdownReadPlatformService;
        this.currencyReadPlatformService = currencyReadPlatformService;
        this.context = context;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.accountingDropdownReadPlatformService = accountingDropdownReadPlatformService;
        this.accountMappingReadPlatformService = accountMappingReadPlatformService;
        this.chargeReadPlatformService = chargeReadPlatformService;
        this.chartReadPlatformService = chartReadPlatformService;
        this.interestRateChartReadPlatformService = interestRateChartReadPlatformService;
        this.depositsDropdownReadPlatformService = depositsDropdownReadPlatformService;
        this.dropdownReadPlatformService = dropdownReadPlatformService;
        this.paymentTypeReadPlatformService = paymentTypeReadPlatformService;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String create(final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createRecurringDepositProduct().withJson(apiRequestBodyAsJson)
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String update(@PathParam("productId") final Long productId, final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateRecurringDepositProduct(productId)
                .withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);

    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(DepositsApiConstants.RECURRING_DEPOSIT_PRODUCT_RESOURCE_NAME);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Collection<RecurringDepositProductData> products = (Collection) this.depositProductReadPlatformService
                .retrieveAll(DepositAccountType.RECURRING_DEPOSIT);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, products,
                DepositsApiConstants.RECURRING_DEPOSIT_PRODUCT_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("productId") final Long productId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(DepositsApiConstants.RECURRING_DEPOSIT_PRODUCT_RESOURCE_NAME);

        RecurringDepositProductData recurringDepositProductData = (RecurringDepositProductData) this.depositProductReadPlatformService
                .retrieveOne(DepositAccountType.RECURRING_DEPOSIT, productId);

        final Collection<ChargeData> charges = this.chargeReadPlatformService.retrieveSavingsProductCharges(productId);
        recurringDepositProductData = RecurringDepositProductData.withCharges(recurringDepositProductData, charges);

        final Collection<InterestRateChartData> charts = this.chartReadPlatformService.retrieveAllWithSlabsWithTemplate(productId);
        recurringDepositProductData = RecurringDepositProductData.withInterestChart(recurringDepositProductData, charts);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        if (recurringDepositProductData.hasAccountingEnabled()) {
            final Map<String, Object> accountingMappings = this.accountMappingReadPlatformService
                    .fetchAccountMappingDetailsForSavingsProduct(productId, recurringDepositProductData.accountingRuleTypeId());
            final Collection<PaymentTypeToGLAccountMapper> paymentChannelToFundSourceMappings = this.accountMappingReadPlatformService
                    .fetchPaymentTypeToFundSourceMappingsForSavingsProduct(productId);
            Collection<ChargeToGLAccountMapper> feeToGLAccountMappings = this.accountMappingReadPlatformService
                    .fetchFeeToIncomeAccountMappingsForSavingsProduct(productId);
            Collection<ChargeToGLAccountMapper> penaltyToGLAccountMappings = this.accountMappingReadPlatformService
                    .fetchPenaltyToIncomeAccountMappingsForSavingsProduct(productId);
            recurringDepositProductData = RecurringDepositProductData.withAccountingDetails(recurringDepositProductData,
                    accountingMappings, paymentChannelToFundSourceMappings, feeToGLAccountMappings, penaltyToGLAccountMappings);
        }

        if (settings.isTemplate()) {
            recurringDepositProductData = handleTemplateRelatedData(recurringDepositProductData);
        }

        return this.toApiJsonSerializer.serialize(settings, recurringDepositProductData,
                DepositsApiConstants.RECURRING_DEPOSIT_PRODUCT_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveTemplate(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(DepositsApiConstants.RECURRING_DEPOSIT_PRODUCT_RESOURCE_NAME);

        final RecurringDepositProductData recurringDepositProduct = handleTemplateRelatedData(null);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, recurringDepositProduct,
                DepositsApiConstants.RECURRING_DEPOSIT_PRODUCT_RESPONSE_DATA_PARAMETERS);
    }

    private RecurringDepositProductData handleTemplateRelatedData(final RecurringDepositProductData savingsProduct) {

        final EnumOptionData interestCompoundingPeriodType = SavingsEnumerations
                .compoundingInterestPeriodType(SavingsCompoundingInterestPeriodType.DAILY);

        final EnumOptionData interestPostingPeriodType = SavingsEnumerations
                .interestPostingPeriodType(SavingsPostingInterestPeriodType.MONTHLY);

        final EnumOptionData interestCalculationType = SavingsEnumerations
                .interestCalculationType(SavingsInterestCalculationType.DAILY_BALANCE);

        final EnumOptionData interestCalculationDaysInYearType = SavingsEnumerations
                .interestCalculationDaysInYearType(SavingsInterestCalculationDaysInYearType.DAYS_365);

        final EnumOptionData accountingRule = AccountingEnumerations.accountingRuleType(AccountingRuleType.NONE);

        CurrencyData currency = CurrencyData.blank();
        final Collection<CurrencyData> currencyOptions = this.currencyReadPlatformService.retrieveAllowedCurrencies();
        if (currencyOptions.size() == 1) {
            currency = new ArrayList<>(currencyOptions).get(0);
        }

        final Collection<EnumOptionData> interestCompoundingPeriodTypeOptions = this.savingsDropdownReadPlatformService
                .retrieveCompoundingInterestPeriodTypeOptions();

        final Collection<EnumOptionData> interestPostingPeriodTypeOptions = this.savingsDropdownReadPlatformService
                .retrieveInterestPostingPeriodTypeOptions();

        final Collection<EnumOptionData> interestCalculationTypeOptions = this.savingsDropdownReadPlatformService
                .retrieveInterestCalculationTypeOptions();

        final Collection<EnumOptionData> interestCalculationDaysInYearTypeOptions = this.savingsDropdownReadPlatformService
                .retrieveInterestCalculationDaysInYearTypeOptions();

        final Collection<EnumOptionData> lockinPeriodFrequencyTypeOptions = this.savingsDropdownReadPlatformService
                .retrieveLockinPeriodFrequencyTypeOptions();

        final Collection<EnumOptionData> withdrawalFeeTypeOptions = this.savingsDropdownReadPlatformService
                .retrievewithdrawalFeeTypeOptions();

        final Collection<PaymentTypeData> paymentTypeOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        final Collection<EnumOptionData> accountingRuleOptions = this.accountingDropdownReadPlatformService
                .retrieveAccountingRuleTypeOptions();
        final Map<String, List<GLAccountData>> accountingMappingOptions = this.accountingDropdownReadPlatformService
                .retrieveAccountMappingOptionsForSavingsProducts();
        final Collection<EnumOptionData> preClosurePenalInterestOnTypeOptions = this.depositsDropdownReadPlatformService
                .retrievePreClosurePenalInterestOnTypeOptions();

        // charges
        final boolean feeChargesOnly = true;
        Collection<ChargeData> chargeOptions = this.chargeReadPlatformService.retrieveSavingsProductApplicableCharges(feeChargesOnly);
        chargeOptions = CollectionUtils.isEmpty(chargeOptions) ? null : chargeOptions;

        Collection<ChargeData> penaltyOptions = this.chargeReadPlatformService.retrieveSavingsApplicablePenalties();
        penaltyOptions = CollectionUtils.isEmpty(penaltyOptions) ? null : penaltyOptions;

        final Collection<EnumOptionData> periodFrequencyTypeOptions = this.dropdownReadPlatformService.retrievePeriodFrequencyTypeOptions();

        // interest rate chart template
        final InterestRateChartData chartTemplate = this.interestRateChartReadPlatformService.template();

        RecurringDepositProductData recurringDepositProductToReturn = null;
        if (savingsProduct != null) {
            recurringDepositProductToReturn = RecurringDepositProductData.withTemplate(savingsProduct, currencyOptions,
                    interestCompoundingPeriodTypeOptions, interestPostingPeriodTypeOptions, interestCalculationTypeOptions,
                    interestCalculationDaysInYearTypeOptions, lockinPeriodFrequencyTypeOptions, withdrawalFeeTypeOptions,
                    paymentTypeOptions, accountingRuleOptions, accountingMappingOptions, chargeOptions, penaltyOptions, chartTemplate,
                    preClosurePenalInterestOnTypeOptions, periodFrequencyTypeOptions);
        } else {
            recurringDepositProductToReturn = RecurringDepositProductData.template(currency, interestCompoundingPeriodType,
                    interestPostingPeriodType, interestCalculationType, interestCalculationDaysInYearType, accountingRule, currencyOptions,
                    interestCompoundingPeriodTypeOptions, interestPostingPeriodTypeOptions, interestCalculationTypeOptions,
                    interestCalculationDaysInYearTypeOptions, lockinPeriodFrequencyTypeOptions, withdrawalFeeTypeOptions,
                    paymentTypeOptions, accountingRuleOptions, accountingMappingOptions, chargeOptions, penaltyOptions, chartTemplate,
                    preClosurePenalInterestOnTypeOptions, periodFrequencyTypeOptions);
        }

        return recurringDepositProductToReturn;
    }

    @DELETE
    @Path("{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String delete(@PathParam("productId") final Long productId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteRecurringDepositProduct(productId).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);

    }
}