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
package org.apache.fineract.portfolio.servicecharge.api;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.servicecharge.constants.QuarterDateRange;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeApiConstants;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeFinalSheetData;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeCalculationPlatformService;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeInstallmentCalculatorService;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeJournalDetailsReadPlatformService;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeLoanDetailsReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path(ServiceChargeApiConstants.SERVICE_CHARGE_REST_CALL)
@Component
@Scope("singleton")
public class ServiceChargeApiResource {

    private final Set<String> CHARGES_DATA_PARAMETERS = new HashSet<>(Arrays.asList("id", "name", "amount", "currency", "active",
            "chargeAppliesTo", "chargeTimeType", "chargeCalculationType", "chargeCalculationTypeOptions", "chargeAppliesToOptions",
            "chargeTimeTypeOptions", "currencyOptions", "loanChargeCalculationTypeOptions", "loanChargeTimeTypeOptions", "incomeAccount",
            "clientChargeCalculationTypeOptions", "clientChargeTimeTypeOptions"));

    private final ServiceChargeInstallmentCalculatorService scCalculator;
    private final ServiceChargeCalculationPlatformService serviceChargeCalculator;
    private final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService;
    @Autowired
    private ApplicationContext appContext;

    @Autowired
    public ServiceChargeApiResource(final ServiceChargeInstallmentCalculatorService scCalculator,
            final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService,
            final ServiceChargeCalculationPlatformService serviceChargeCalculator) {
        this.scJournalDetailsReadPlatformService = scJournalDetailsReadPlatformService;
        this.serviceChargeCalculator = serviceChargeCalculator;
        this.scCalculator=scCalculator;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveServiceCharge(@Context final UriInfo uriInfo) {
        ServiceChargeFinalSheetData finalSheetData = (ServiceChargeFinalSheetData) appContext.getBean("serviceChargeFinalSheetData");
        scJournalDetailsReadPlatformService.generatefinalSheetData(finalSheetData);

        BigDecimal disbursmentCost = finalSheetData.getColumnValue(ServiceChargeReportTableHeaders.LOAN_SERVICING_PER_LOAN, 0);
        BigDecimal mobilizationCost = finalSheetData.getColumnValue(ServiceChargeReportTableHeaders.ANNUALIZED_COST_I, 0);
        BigDecimal repaymentCost = finalSheetData.getColumnValue(ServiceChargeReportTableHeaders.REPAYMENT_PER_100, 0);

        StringBuffer result = new StringBuffer();
        result.append("{");
        result.append("Disbursement:").append(disbursmentCost.toPlainString());
        result.append(",");
        result.append("Mobilisation:").append(mobilizationCost.toPlainString());
        result.append(",");
        result.append("Repayment:").append(repaymentCost.toPlainString());
        result.append("}");
        return result.toString();
    }

    @GET
    @Path("{loandId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveServiceChargeForGivenLoan(@PathParam("loandId") final Long loanId, @Context final UriInfo uriInfo) {
        BigDecimal serviceCharge = serviceChargeCalculator.calculateServiceChargeForLoan(loanId);

        return serviceCharge.toPlainString();
    }

    @GET
    @Path("getJournalEntries")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String printJournalEntries(@Context final UriInfo uriInfo, @QueryParam("table") final boolean displayTable) {
        String result = null;
        ServiceChargeFinalSheetData finalSheetData = (ServiceChargeFinalSheetData) appContext.getBean("serviceChargeFinalSheetData");
        scJournalDetailsReadPlatformService.generatefinalSheetData(finalSheetData);
        if (!displayTable) {
            result = finalSheetData.getResultsDataMap().toString();
        } else {
            result = finalSheetData.generateResultAsHTMLTable(false).toString();
        }
        return result;
    }

    @GET
    @Path("getQuarterJournalEntries")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String printJournalEntries(@Context final UriInfo uriInfo, @QueryParam("quarter") final String quarter,
            @QueryParam("year") final int year, @QueryParam("table") final boolean displayTable) {
        String result = null;
        ServiceChargeFinalSheetData finalSheetData = (ServiceChargeFinalSheetData) appContext.getBean("serviceChargeFinalSheetData");
        QuarterDateRange.setQuarterAndYear(quarter, year);
        scJournalDetailsReadPlatformService.generatefinalSheetData(finalSheetData);
        if (!displayTable) {
            result = finalSheetData.getResultsDataMap().toString();
        } else {
            result = finalSheetData.generateResultAsHTMLTable(false).toString();
        }
        return result;
    }
}