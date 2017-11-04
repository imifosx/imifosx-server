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
package org.ideoholic.imifosx.portfolio.servicecharge.scheduledjobs;

import java.math.BigDecimal;
import java.util.Calendar;

import org.ideoholic.imifosx.infrastructure.jobs.annotation.CronTarget;
import org.ideoholic.imifosx.infrastructure.jobs.service.JobName;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.QuarterDateRange;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.ideoholic.imifosx.portfolio.servicecharge.data.ServiceChargeData;
import org.ideoholic.imifosx.portfolio.servicecharge.data.ServiceChargeFinalSheetData;
import org.ideoholic.imifosx.portfolio.servicecharge.service.ServiceChargeJournalDetailsReadPlatformService;
import org.ideoholic.imifosx.portfolio.servicecharge.service.ServiceChargeWritePlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service(value = "serviceChargeScheduledJobRunnerService")
public class ServiceChargeScheduledJobRunnerServiceImpl implements ServiceChargeScheduledJobRunnerService {

	private final static Logger logger = LoggerFactory.getLogger(ServiceChargeScheduledJobRunnerServiceImpl.class);

	private final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService;
	private final ServiceChargeWritePlatformService scWritePlatformService;

	@Autowired
	public ServiceChargeScheduledJobRunnerServiceImpl(final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService,
			ServiceChargeWritePlatformService scWritePlatformService) {
		this.scJournalDetailsReadPlatformService = scJournalDetailsReadPlatformService;
		this.scWritePlatformService = scWritePlatformService;
	}

	@Override
	@CronTarget(jobName = JobName.GENERATE_SERVICECHARGE)
	public void generateServiceCharge() {
		logger.info("ServiceChargeScheduledJobRunnerServiceImpl::generateServiceCharge: Inside Generate Service Charge");

		QuarterDateRange quarter = QuarterDateRange.getCurrentQuarter();
		int year = Calendar.getInstance().get(Calendar.YEAR);
		ServiceChargeFinalSheetData finalSheetData = scJournalDetailsReadPlatformService.generatefinalSheetData();

		// Saving : Loan Servicing Cost per Loan
		saveServiceCharge(quarter, year, ServiceChargeReportTableHeaders.LOAN_SERVICING_PER_LOAN, finalSheetData);

		// Saving : Equivalent Annualized Cost (%) - I
		saveServiceCharge(quarter, year, ServiceChargeReportTableHeaders.ANNUALIZED_COST_I, finalSheetData);

		// Saving : Repayments Cost per 100 Rupee of Repayment
		saveServiceCharge(quarter, year, ServiceChargeReportTableHeaders.REPAYMENT_PER_100, finalSheetData);
	}

	private void saveServiceCharge(QuarterDateRange quarter, int year, ServiceChargeReportTableHeaders header, ServiceChargeFinalSheetData dataSheet) {
		BigDecimal amount = dataSheet.getColumnValue(header, 0);

		logger.info("ServiceChargeScheduledJobRunnerServiceImpl::saveServiceCharge: Data Details->");
		logger.info("quarter from-" + quarter.getFromDateStringForCurrentYear() + "quarter to-" + quarter.getToDateStringForCurrentYear());
		logger.info("Header-" + header.getCode());
		logger.info("Amount-" + amount);

		ServiceChargeData serviceCharge = ServiceChargeData.template(quarter, year, header, amount);
		scWritePlatformService.createCharge(serviceCharge);
	}

}
