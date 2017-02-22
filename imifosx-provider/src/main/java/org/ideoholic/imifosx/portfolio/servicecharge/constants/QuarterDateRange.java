package org.ideoholic.imifosx.portfolio.servicecharge.constants;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.ideoholic.imifosx.accounting.journalentry.api.DateParam;

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
		int quarter = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR);
		QuarterDateRange q = Q1;
		switch (quarter) {
		case 1:
			q = Q1;
			break;
		case 2:
			q = Q2;
			break;
		case 3:
			q = Q3;
			break;
		case 4:
			q = Q4;
			break;
		}
		return q;
	}

	@Override
	public String toString() {
		return name().toString();
	}

}
