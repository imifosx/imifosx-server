package org.ideoholic.imifosx.portfolio.servicecharge.constants;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ideoholic.imifosx.accounting.journalentry.api.DateParam;
import org.ideoholic.imifosx.infrastructure.core.service.DateUtils;

public enum QuarterDateRange {
	Q1(1, "01 Jan ", "31 Mar "), Q2(2, "01 Apr ", "30 Jun "), Q3(3, "01 Jul ", "30 Sep "), Q4(4, "30 Sep ", "31 Dec ");

	private final Integer id;
	private final String fromDate;
	private final String toDate;
	// TODO: Explore using -
	// org.ideoholic.imifosx.organisation.teller.util.DateRange
	// private final DateRange dateRange;
	private final String dateFormatString = "dd MMMM yyyy";

	private QuarterDateRange(final Integer id, final String fromDate, final String toDate) {
		this.id = id;
		this.fromDate = fromDate;
		this.toDate = toDate;
	}

	public Integer getId() {
		return id;
	}

	public String getDateFormatString() {
		return dateFormatString;
	}

	public String getFromDateString(int year) {
		return fromDate + year;
	}

	public String getToDateString(int year) {
		return toDate + year;
	}

	public String getFromDateStringForCurrentYear() {
		return getFromDateString(Calendar.getInstance().get(Calendar.YEAR));
	}

	public String getToDateStringForCurrentYear() {
		return getToDateString(Calendar.getInstance().get(Calendar.YEAR));
	}

	public Date getFromDate(int year) {
		String locale = Locale.getDefault().toString();
		String fullDateString = getFromDateString(year);
		return new DateParam(fullDateString).getDate("Service Data Entries From Date", getDateFormatString(), locale);
	}

	public Date getToDate(int year) {
		String locale = Locale.getDefault().toString();
		String fullDateString = getToDateString(year);
		return new DateParam(fullDateString).getDate("Service Data Entries To Date", getDateFormatString(), locale);
	}

	public Date getFromDateForCurrentYear() {
		return getFromDate(QuarterYearHolder.getYear());
	}

	public Date getToDateForCurrentYear() {
		return getToDate(QuarterYearHolder.getYear());
	}

	// Find better way to do this
	public static QuarterDateRange getCurrentQuarter() {
		QuarterDateRange q = QuarterYearHolder.getCurrentQuarter();
		return q;
	}

	public static QuarterDateRange getPreviousQuarter() {
		QuarterDateRange q = getCurrentQuarter();
		switch (q) {
		case Q1:
			return Q4;
		case Q2:
			return Q1;
		case Q3:
			return Q2;
		case Q4:
			return Q3;
		default:
			return null;
		}
	}

	public String getFormattedFromDateString() {
		return DateUtils.formatToSqlDate(getFromDateForCurrentYear());
	}

	public String getFormattedToDateString() {
		return DateUtils.formatToSqlDate(getToDateForCurrentYear());
	}

	@Override
	public String toString() {
		return name().toString();
	}

	private static final Map<Integer, QuarterDateRange> intToEnumMap = new HashMap<>();
	static {
		for (final QuarterDateRange type : QuarterDateRange.values()) {
			intToEnumMap.put(type.getId(), type);
		}
	}

	public static QuarterDateRange fromInt(final int i) {
		final QuarterDateRange type = intToEnumMap.get(Integer.valueOf(i));
		return type;
	}

	public static void setQuarterAndYear(String quarter, int year) {
		QuarterYearHolder.setQuarterAndYear(quarter, year);
	}

	private static class QuarterYearHolder {
		private static String quarter = "";
		private static int year = 0;

		static void setQuarterAndYear(String quarterParam, int yearParam) {
			quarter = quarterParam;
			year = yearParam;
		}

		static int getYear() {
			if (year == 0) {
				return Calendar.getInstance().get(Calendar.YEAR);
			}
			return year;
		}

		static QuarterDateRange getCurrentQuarter() {
			QuarterDateRange q = null;
			if (!StringUtils.isEmpty(quarter)) {
				final String qStr = quarter.toUpperCase();
				switch (qStr) {
				case "Q1":
					q = Q1;
					break;
				case "Q2":
					q = Q2;
					break;
				case "Q3":
					q = Q3;
					break;
				case "Q4":
					q = Q4;
					break;
				default:
					q = null;
					break;
				}
			} else {
				Calendar c = Calendar.getInstance(Locale.getDefault());
				int month = c.get(Calendar.MONTH);

				q = (month >= Calendar.JANUARY && month <= Calendar.MARCH) ? Q1 : (month >= Calendar.APRIL && month <= Calendar.JUNE) ? Q2
						: (month >= Calendar.JULY && month <= Calendar.SEPTEMBER) ? Q3 : Q4;
			}
			return q;
		}
	}

}
