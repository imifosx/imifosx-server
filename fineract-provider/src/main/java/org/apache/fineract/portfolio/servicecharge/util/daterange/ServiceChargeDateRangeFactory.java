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
package org.apache.fineract.portfolio.servicecharge.util.daterange;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeApiConstants;
import org.apache.fineract.portfolio.servicecharge.exception.ServiceChargeException;
import org.apache.fineract.portfolio.servicecharge.exception.ServiceChargeException.SERVICE_CHARGE_EXCEPTION_REASON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceChargeDateRangeFactory implements ServiceChargeApiConstants {

    private final static Logger logger = LoggerFactory.getLogger(ServiceChargeDateRangeFactory.class);
    private static volatile ServiceChargeDateRange scDateRangeInstance = null;

    public static ServiceChargeDateRange getServiceChargeDateRangeFromInt(int rangeCode) {
        return null;
    }

    // Find better way to do this
    public static ServiceChargeDateRange getCurrentDateRange() {
        if (scDateRangeInstance == null) {
            synchronized (ServiceChargeDateRange.class) {
                // Double check needed
                if (scDateRangeInstance == null) {
                    scDateRangeInstance = getDateRange();
                }
            }
        }
        return scDateRangeInstance;
    }

    private static ServiceChargeDateRange getDateRange() {
        ServiceChargeDateRange q = null;
        final String tenantIdentifier = ThreadLocalContextUtil.getTenant().getTenantIdentifier();
        // Read the system param: servicecharge_calculation_method, trim any
        // spaces and move text to uppercase
        final String scCalcMethodSystemParam = System.getenv(SC_CALCULATION_METHOD + tenantIdentifier).trim().toUpperCase();

        switch (scCalcMethodSystemParam) {
            case MONTHLY:
                q = MonthYearHolder.getCurrentMonth();
            break;
            case QUARTERLY:
                q = MonthYearHolder.getCurrentQuarter();
            break;
            case YEARLY:
                q = MonthYearHolder.getCurrentYear();
            break;
            default:
                throw new ServiceChargeException(SERVICE_CHARGE_EXCEPTION_REASON.SC_INVALID_CALCULATION_PARAM, null);
        }
        return q;
    }

    public static void setQuarterAndYear(String quarter, int year) {
        MonthYearHolder.setMonthAndYear(quarter, year);
    }

    public static boolean checkIfGivenDateIsInCurrentQuarter(Date date) {
        ServiceChargeDateRange range = getCurrentDateRange();
        final Date fromDate = range.getFromDateForCurrentYear();
        final Date toDate = range.getToDateForCurrentYear();
        // if date >= fromDate && date <= toDate
        if ((date.equals(fromDate) || date.after(fromDate)) && (date.before(toDate) || date.equals(toDate))) { return true; }
        return false;
    }

    private static class MonthYearHolder {

        private static String month = StringUtils.EMPTY;
        private static int year = 0;

        static void setMonthAndYear(String monthParam, int yearParam) {
            month = monthParam;
            year = yearParam;
        }

        static ServiceChargeDateRange getCurrentMonth() {
            logger.debug("MonthYearHolder.getCurrentQuarter: month::" + month + " year::" + year);
            ServiceChargeDateRange q = null;
            if (month != null && !month.isEmpty()) {
                q = MonthlyServiceChargeDateRange.getCurrentMonth(month, year);
            }
            return q;
        }

        static ServiceChargeDateRange getCurrentQuarter() {
            logger.debug("MonthYearHolder.getCurrentQuarter: month::" + month + " year::" + year);
            ServiceChargeDateRange q = null;
            if (month != null && !month.isEmpty()) {
                q = QuarterlyServiceChargeDateRange.getCurrentQuarter(month, year);
            }
            return q;
        }

        static ServiceChargeDateRange getCurrentYear() {
            logger.debug("MonthYearHolder.getCurrentQuarter: month::" + month + " year::" + year);
            ServiceChargeDateRange q = null;
            if (month != null && !month.isEmpty()) {
                q = YearlyServiceChargeDateRange.getCurrentYear(month, year);
            }
            return q;
        }

    }
}
