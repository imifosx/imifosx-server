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
package org.ideoholic.imifosx.portfolio.account.data;

import static org.ideoholic.imifosx.portfolio.account.api.AccountTransfersApiConstants.ACCOUNT_TRANSFER_RESOURCE_NAME;
import static org.ideoholic.imifosx.portfolio.account.api.AccountTransfersApiConstants.REQUEST_DATA_PARAMETERS;
import static org.ideoholic.imifosx.portfolio.account.api.AccountTransfersApiConstants.transferAmountParamName;
import static org.ideoholic.imifosx.portfolio.account.api.AccountTransfersApiConstants.transferDateParamName;
import static org.ideoholic.imifosx.portfolio.account.api.AccountTransfersApiConstants.transferDescriptionParamName;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ideoholic.imifosx.infrastructure.core.api.JsonCommand;
import org.ideoholic.imifosx.infrastructure.core.data.ApiParameterError;
import org.ideoholic.imifosx.infrastructure.core.data.DataValidatorBuilder;
import org.ideoholic.imifosx.infrastructure.core.exception.InvalidJsonException;
import org.ideoholic.imifosx.infrastructure.core.exception.PlatformApiDataValidationException;
import org.ideoholic.imifosx.infrastructure.core.serialization.FromJsonHelper;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

@Component
public class AccountTransfersDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final AccountTransfersDetailDataValidator accountTransfersDetailDataValidator;

    @Autowired
    public AccountTransfersDataValidator(final FromJsonHelper fromApiJsonHelper,
            final AccountTransfersDetailDataValidator accountTransfersDetailDataValidator) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.accountTransfersDetailDataValidator = accountTransfersDetailDataValidator;
    }

    public void validate(final JsonCommand command) {

        final String json = command.json();

        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ACCOUNT_TRANSFER_RESOURCE_NAME);

        final JsonElement element = command.parsedJson();

        this.accountTransfersDetailDataValidator.validate(command, baseDataValidator);

        final LocalDate transactionDate = this.fromApiJsonHelper.extractLocalDateNamed(transferDateParamName, element);
        baseDataValidator.reset().parameter(transferDateParamName).value(transactionDate).notNull();

        final BigDecimal transactionAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(transferAmountParamName, element);
        baseDataValidator.reset().parameter(transferAmountParamName).value(transactionAmount).notNull().positiveAmount();

        final String transactionDescription = this.fromApiJsonHelper.extractStringNamed(transferDescriptionParamName, element);
        baseDataValidator.reset().parameter(transferDescriptionParamName).value(transactionDescription).notBlank()
                .notExceedingLengthOf(200);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
    }
}