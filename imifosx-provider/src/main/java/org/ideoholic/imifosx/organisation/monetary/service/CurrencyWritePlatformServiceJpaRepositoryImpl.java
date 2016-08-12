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
package org.ideoholic.imifosx.organisation.monetary.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ideoholic.imifosx.infrastructure.core.api.JsonCommand;
import org.ideoholic.imifosx.infrastructure.core.data.CommandProcessingResult;
import org.ideoholic.imifosx.infrastructure.core.data.CommandProcessingResultBuilder;
import org.ideoholic.imifosx.infrastructure.security.service.PlatformSecurityContext;
import org.ideoholic.imifosx.organisation.monetary.domain.ApplicationCurrency;
import org.ideoholic.imifosx.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.ideoholic.imifosx.organisation.monetary.exception.CurrencyInUseException;
import org.ideoholic.imifosx.organisation.monetary.serialization.CurrencyCommandFromApiJsonDeserializer;
import org.ideoholic.imifosx.organisation.office.domain.OrganisationCurrency;
import org.ideoholic.imifosx.organisation.office.domain.OrganisationCurrencyRepository;
import org.ideoholic.imifosx.portfolio.charge.service.ChargeReadPlatformService;
import org.ideoholic.imifosx.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.ideoholic.imifosx.portfolio.savings.service.SavingsProductReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrencyWritePlatformServiceJpaRepositoryImpl implements CurrencyWritePlatformService {

    private final PlatformSecurityContext context;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final OrganisationCurrencyRepository organisationCurrencyRepository;
    private final CurrencyCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final LoanProductReadPlatformService loanProductService;
    private final SavingsProductReadPlatformService savingsProductService;
    private final ChargeReadPlatformService chargeService;

    @Autowired
    public CurrencyWritePlatformServiceJpaRepositoryImpl(final PlatformSecurityContext context,
            final CurrencyCommandFromApiJsonDeserializer fromApiJsonDeserializer,
            final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository,
            final OrganisationCurrencyRepository organisationCurrencyRepository, final LoanProductReadPlatformService loanProductService,
            final SavingsProductReadPlatformService savingsProductService, final ChargeReadPlatformService chargeService) {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.applicationCurrencyRepository = applicationCurrencyRepository;
        this.organisationCurrencyRepository = organisationCurrencyRepository;
        this.loanProductService = loanProductService;
        this.savingsProductService = savingsProductService;
        this.chargeService = chargeService;
    }

    @Transactional
    @Override
    public CommandProcessingResult updateAllowedCurrencies(final JsonCommand command) {

        this.context.authenticatedUser();

        this.fromApiJsonDeserializer.validateForUpdate(command.json());

        final String[] currencies = command.arrayValueOfParameterNamed("currencies");

        final Map<String, Object> changes = new LinkedHashMap<>();
        final List<String> allowedCurrencyCodes = new ArrayList<>();
        final Set<OrganisationCurrency> allowedCurrencies = new HashSet<>();
        for (final String currencyCode : currencies) {

            final ApplicationCurrency currency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currencyCode);

            final OrganisationCurrency allowedCurrency = currency.toOrganisationCurrency();

            allowedCurrencyCodes.add(currencyCode);
            allowedCurrencies.add(allowedCurrency);
        }

        for (OrganisationCurrency priorCurrency : this.organisationCurrencyRepository.findAll()) {
            if (!allowedCurrencyCodes.contains(priorCurrency.getCode())) {
                // Check if it's safe to remove this currency.
                if (!loanProductService.retrieveAllLoanProductsForCurrency(priorCurrency.getCode()).isEmpty()
                        || !savingsProductService.retrieveAllForCurrency(priorCurrency.getCode()).isEmpty()
                        || !chargeService.retrieveAllChargesForCurrency(priorCurrency.getCode()).isEmpty()) { throw new CurrencyInUseException(
                        priorCurrency.getCode()); }
            }
        }

        changes.put("currencies", allowedCurrencyCodes.toArray(new String[allowedCurrencyCodes.size()]));

        this.organisationCurrencyRepository.deleteAll();
        this.organisationCurrencyRepository.save(allowedCurrencies);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .with(changes) //
                .build();
    }
}