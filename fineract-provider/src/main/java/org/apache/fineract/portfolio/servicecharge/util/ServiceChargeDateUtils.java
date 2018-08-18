package org.apache.fineract.portfolio.servicecharge.util;

import java.time.LocalDate;
import java.time.Period;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import javafx.util.Pair;

public class ServiceChargeDateUtils {

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
		return new Pair<>(financiyalYearFrom, financiyalYearTo);
	}

	public static int getDiffBetweenDates(Date beginDate, Date endDate, int offset) {
		Period duration;
		// Get begin LocalDate value
		Calendar cal = Calendar.getInstance(Locale.getDefault());
		cal.setTime(beginDate);
		LocalDate beginDateLocal = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
				cal.get(Calendar.DAY_OF_MONTH));

		cal.setTime(endDate);
		LocalDate endDateLocal = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
				cal.get(Calendar.DAY_OF_MONTH));

		duration = Period.between(beginDateLocal, endDateLocal);
		int daysBetweenStartAndEndDate = duration.getDays() + offset;
		return daysBetweenStartAndEndDate;
	}

	public static Date getDateFromLocaleDate(org.joda.time.LocalDate transactionDate) {
		Calendar cal = Calendar.getInstance(Locale.getDefault());
		cal.set(Calendar.DATE, transactionDate.getDayOfMonth());
		cal.set(Calendar.MONTH, transactionDate.getMonthOfYear());
		cal.set(Calendar.YEAR, transactionDate.getYear());
		return cal.getTime();
	}

}
