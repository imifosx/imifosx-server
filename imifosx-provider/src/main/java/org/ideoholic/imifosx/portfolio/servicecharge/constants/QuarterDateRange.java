package org.ideoholic.imifosx.portfolio.servicecharge.constants;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.ideoholic.imifosx.accounting.journalentry.api.DateParam;
import org.ideoholic.imifosx.infrastructure.core.service.DateUtils;

public enum QuarterDateRange {
	Q1("01 Jan ", "31 Mar "), Q2("01 Apr ", "30 Jun "), Q3("01 Jul ", "30 Sep "), Q4("30 Sep ", "31 Dec ");

	private final String fromDate;
	private final String toDate;
	// TODO: Explore using -
	// org.ideoholic.imifosx.organisation.teller.util.DateRange
	// private final DateRange dateRange;
	private final String dateFormatString = "dd MMMM yyyy";

	private QuarterDateRange(final String fromDate, final String toDate) {
		this.fromDate = fromDate;
		this.toDate = toDate;
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
		return new DateParam(fullDateString).getDate("Service Charge Journal Entries From Date", getDateFormatString(), locale);
	}

	public Date getToDate(int year) {
		String locale = Locale.getDefault().toString();
		String fullDateString = getToDateString(year);
		return new DateParam(fullDateString).getDate("Service Charge Journal Entries To Date", getDateFormatString(), locale);
	}

	public Date getFromDateForCurrentYear() {
		return getFromDate(Calendar.getInstance().get(Calendar.YEAR));
	}

	public Date getToDateForCurrentYear() {
		return getToDate(Calendar.getInstance().get(Calendar.YEAR));
	}

	// Find better way to do this
	public static QuarterDateRange getCurrentQuarter() {
		QuarterDateRange q = Q1;
		Calendar c = Calendar.getInstance(Locale.getDefault());
		int month = c.get(Calendar.MONTH);

		q = (month >= Calendar.JANUARY && month <= Calendar.MARCH) ? Q1
				: (month >= Calendar.APRIL && month <= Calendar.JUNE) ? Q2 : (month >= Calendar.JULY && month <= Calendar.SEPTEMBER) ? Q3 : Q4;
		return q;
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

}
