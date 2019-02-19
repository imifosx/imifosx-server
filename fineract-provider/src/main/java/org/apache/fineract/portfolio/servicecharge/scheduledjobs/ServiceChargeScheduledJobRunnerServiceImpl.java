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
package org.apache.fineract.portfolio.servicecharge.scheduledjobs;

import java.math.BigDecimal;
import java.util.Calendar;

import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeData;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeFinalSheetData;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeInstallmentCalculatorService;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeJournalDetailsReadPlatformService;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeReadPlatformService;
import org.apache.fineract.portfolio.servicecharge.service.ServiceChargeWritePlatformService;
import org.apache.fineract.portfolio.servicecharge.util.ServiceChargeOperationUtils;
import org.apache.fineract.portfolio.servicecharge.util.daterange.ServiceChargeDateRange;
import org.apache.fineract.portfolio.servicecharge.util.daterange.ServiceChargeDateRangeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service(value = "serviceChargeScheduledJobRunnerService")
public class ServiceChargeScheduledJobRunnerServiceImpl implements ServiceChargeScheduledJobRunnerService {

    private final static Logger logger = LoggerFactory.getLogger(ServiceChargeScheduledJobRunnerServiceImpl.class);

    @Autowired
    private ApplicationContext appContext;

    private final ServiceChargeWritePlatformService scWritePlatformService;
    private final ServiceChargeReadPlatformService scChargeReadPlatformService;
    private final ServiceChargeInstallmentCalculatorService serviceChargeInstallmentCalculator;
    private final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService;

    @Autowired
    public ServiceChargeScheduledJobRunnerServiceImpl(
            final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService,
            ServiceChargeWritePlatformService scWritePlatformService, ServiceChargeReadPlatformService scChargeReadPlatformService,
            ServiceChargeInstallmentCalculatorService serviceChargeInstallmentCalculator) {
        this.serviceChargeInstallmentCalculator = serviceChargeInstallmentCalculator;
        this.scJournalDetailsReadPlatformService = scJournalDetailsReadPlatformService;
        this.scChargeReadPlatformService = scChargeReadPlatformService;
        this.scWritePlatformService = scWritePlatformService;
    }

    @Override
    @CronTarget(jobName = JobName.GENERATE_SERVICECHARGE)
    public void generateServiceCharge() {
        logger.info("ServiceChargeScheduledJobRunnerServiceImpl::generateServiceCharge: Inside Generate Service Charge");

        ServiceChargeDateRange quarter = ServiceChargeDateRangeFactory.getCurrentDateRange();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        ServiceChargeDateRangeFactory.setQuarterAndYear(quarter.getName(), year);

        ServiceChargeData serviceCharge = ServiceChargeOperationUtils.getServiceChargeForCurrentQuarter(scChargeReadPlatformService);
        if (serviceCharge != null) {
            // The calculation has already been done for this quarter and so
            // skip it
            return;
        }
        ServiceChargeFinalSheetData finalSheetData = (ServiceChargeFinalSheetData) appContext.getBean("serviceChargeFinalSheetData");
        scJournalDetailsReadPlatformService.generatefinalSheetData(finalSheetData);

        // Saving : Loan Servicing Cost per Loan
        saveServiceCharge(quarter, year, ServiceChargeReportTableHeaders.LOAN_SERVICING_PER_LOAN, finalSheetData);

        // Saving : Equivalent Annualized Cost (%) - I
        saveServiceCharge(quarter, year, ServiceChargeReportTableHeaders.ANNUALIZED_COST_I, finalSheetData);

        // Saving : Repayments Cost per 100 Rupee of Repayment
        saveServiceCharge(quarter, year, ServiceChargeReportTableHeaders.REPAYMENT_PER_100, finalSheetData);

        // Now recalculate the service charge for all the relevant loans
        serviceChargeInstallmentCalculator.recalculateServiceChargeForAllLoans();
    }

    private void saveServiceCharge(ServiceChargeDateRange quarter, int year, ServiceChargeReportTableHeaders header,
            ServiceChargeFinalSheetData dataSheet) {
        BigDecimal amount = dataSheet.getColumnValue(header, 0);

        logger.info("ServiceChargeScheduledJobRunnerServiceImpl::saveServiceCharge: Data Details->");
        logger.info("quarter from-" + quarter.getFromDateStringForCurrentYear() + "quarter to-" + quarter.getToDateStringForCurrentYear());
        logger.info("Header-" + header.getCode());
        logger.info("Amount-" + amount);

        ServiceChargeData serviceCharge = ServiceChargeData.template(quarter, year, header, amount);
        scWritePlatformService.createCharge(serviceCharge);
    }

}
