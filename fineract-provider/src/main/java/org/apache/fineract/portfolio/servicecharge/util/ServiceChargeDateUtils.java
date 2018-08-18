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
			if (isReverse()) {
				return !current.before(end);
			}
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

			System.out.println("Diff:" + getDiffBetweenDates(beginDate, beginDate, 1));
		}
	}
}
