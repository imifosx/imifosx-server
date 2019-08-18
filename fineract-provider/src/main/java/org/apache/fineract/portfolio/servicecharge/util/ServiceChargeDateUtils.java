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
package org.apache.fineract.portfolio.servicecharge.util;

import java.time.LocalDate;
import java.time.Period;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceChargeDateUtils implements ServiceChargeApiConstants {

    private final static Logger logger = LoggerFactory.getLogger(ServiceChargeDateUtils.class);

    public static final String SQL_DATE_FORMAT = "yyyy-MM-dd";

    /**
     *
     * @return Pair of values containing start and end of financial year
     */
    public static Pair<String, String> getCurrentFinancialYearDatePair() {
        int CurrentYear = Calendar.getInstance().get(Calendar.YEAR);
        int CurrentMonth = (Calendar.getInstance().get(Calendar.MONTH) + 1);

        String financiyalYearFrom = "";
        String financiyalYearTo = "";
        // For both from date and to date the format is YYYY-MM-DD
        if (CurrentMonth < 4) {
            financiyalYearFrom = (CurrentYear - 1) + "-04-01";
            financiyalYearTo = (CurrentYear) + "-03-31";
        } else {
            financiyalYearFrom = (CurrentYear) + "-04-01";
            financiyalYearTo = (CurrentYear + 1) + "-03-31";
        }
        return Pair.of(financiyalYearFrom, financiyalYearTo);
    }

    public static int getDiffBetweenDates(Date beginDate, Date endDate, int offset) {
        Period duration;
        // Get begin LocalDate value
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTime(beginDate);
        LocalDate beginDateLocal = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

        cal.setTime(endDate);
        LocalDate endDateLocal = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

        duration = Period.between(beginDateLocal, endDateLocal);
        int daysBetweenStartAndEndDate = duration.getDays() + offset;
        return daysBetweenStartAndEndDate;
    }

    /**
     * Method to check if given two dates are the same. Date check is done for
     * same date, month and year. This skips the check on the time and only date
     * part of the given two dates are checked.
     * 
     * @param first
     * @param second
     * @return
     */
    public static boolean checkIfGivenDatesAreSame(Date first, Date second) {
        Calendar calFirst = Calendar.getInstance(Locale.getDefault());
        Calendar calSecond = Calendar.getInstance(Locale.getDefault());
        calFirst.setTime(first);
        calSecond.setTime(second);
        if (calFirst.get(Calendar.DATE) != calSecond.get(Calendar.DATE)) { return false; }
        if (calFirst.get(Calendar.MONTH) != calSecond.get(Calendar.MONTH)) { return false; }
        if (calFirst.get(Calendar.YEAR) != calSecond.get(Calendar.YEAR)) { return false; }
        return true;
    }

    public static Date getDateFromLocaleDate(org.joda.time.LocalDate transactionDate) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.set(Calendar.DATE, transactionDate.getDayOfMonth());
        cal.set(Calendar.MONTH, transactionDate.getMonthOfYear());
        cal.set(Calendar.YEAR, transactionDate.getYear());
        return cal.getTime();
    }

    /**
     * Method to generate string format of given date<br>
     * String will be of DD-MM-YYYY format where each character is a number
     * 
     * @param localDate
     *            - org.joda.time.LocalDate
     * @return String
     */
    public static String getDateStringFromDate(org.joda.time.LocalDate localDate) {
        StringBuffer sb = new StringBuffer();
        sb.append(localDate.getDayOfMonth());
        sb.append(HYPHEN);
        sb.append(localDate.getMonthOfYear());
        sb.append(HYPHEN);
        sb.append(localDate.getYear());
        return sb.toString();
    }

    /**
     * Method to generate string format of given date<br>
     * String will be of DD-MM-YYYY format where each character is a number
     * 
     * @param localDate
     *            - java.util.Date
     * @return String
     */
    public static String getDateStringFromDate(Date date) {
        StringBuffer sb = new StringBuffer();
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTime(date);
        sb.append(cal.get(Calendar.DATE));
        sb.append(HYPHEN);
        sb.append(cal.get(Calendar.MONTH) + 1);
        sb.append(HYPHEN);
        sb.append(cal.get(Calendar.YEAR));
        return sb.toString();
    }

    /**
     * Method to check if the given map contains a key with the date that is
     * send as a key. Make sure that the key of the map is of type Date else
     * there will be runtime cast exception
     * 
     * @param dateMap
     * @param key
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static boolean checkIfDateMapContainsGivenDateKey(Map dateMap, Date key) {
        Set<Date> mapKeySet = dateMap.keySet();
        for (Date setKey : mapKeySet) {
            if (checkIfGivenDatesAreSame(key, setKey)) { return true; }
        }
        return false;
    }

    public static Date formatSqlStringToDate(final String dateString) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(SQL_DATE_FORMAT);
            date = sdf.parse(dateString);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return date;
    }

    public static Date determineSCLoopEndDate(Date firstDayOfRange, Date lastDayOfRange, Date disbursmentDate) {
        // Remove time-stamp from the date object
        Date firstDayOfMonthShort = formatSqlStringToDate(DateUtils.formatToSqlDate(firstDayOfRange));
        Date disbursmentDateShort = formatSqlStringToDate(DateUtils.formatToSqlDate(disbursmentDate));
        logger.debug("DateUtils.determineSCLoopEndDate::firstDayOfMonth:" + firstDayOfRange + "disbursmentDate:" + disbursmentDate);
        // In case the loan is disbursed after first day of the range
        if (disbursmentDateShort.after(firstDayOfMonthShort)) { return disbursmentDate; }
        // Subtract one day from the first day to allow first day inclusion
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(firstDayOfRange);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTime();
    }

    public static class DateIterator implements Iterator<Date>, Iterable<Date> {

        private Calendar end = Calendar.getInstance();
        private Calendar current = Calendar.getInstance();
        private boolean reverse;

        public DateIterator(Date start, Date end, boolean reverse) {
            this.reverse = reverse;
            setDateValues(start, end);
        }

        private void setDateValues(Date start, Date end) {
            if (isReverse()) {
                this.current.setTime(start);
                this.current.add(Calendar.DATE, 1);
                this.end.setTime(end);
                this.end.add(Calendar.DATE, 1);
            } else {
                this.current.setTime(start);
                this.current.add(Calendar.DATE, -1);
                this.end.setTime(end);
                this.end.add(Calendar.DATE, -1);
            }

        }

        public boolean isReverse() {
            return reverse;
        }

        @Override
        public boolean hasNext() {
            if (isReverse()) { return !current.before(end); }
            return !current.after(end);
        }

        @Override
        public Date next() {
            if (isReverse()) {
                current.add(Calendar.DATE, -1);
            } else {
                current.add(Calendar.DATE, 1);
            }
            return current.getTime();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Cannot remove");
        }

        @Override
        public Iterator<Date> iterator() {
            return this;
        }

        public static void main(String[] args) {

            Calendar cal = Calendar.getInstance(Locale.getDefault());
            cal.set(2018, 2, 31);
            // cal.set(2018, 2, 1);
            Date beginDate = cal.getTime();

            cal.set(2018, 2, 1);
            // cal.set(2018, 2, 31);
            Date endDate = cal.getTime();

            Iterator<Date> i = new DateIterator(beginDate, endDate, true);
            int iCounter = 1;
            while (i.hasNext()) {
                Date date = i.next();
                System.out.print(iCounter + "->");
                System.out.println(date);
                iCounter++;
            }

            logger.debug("Diff:" + getDiffBetweenDates(beginDate, beginDate, 1));
        }
    }
}
