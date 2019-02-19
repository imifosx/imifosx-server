/**
 * 
 */
package org.apache.fineract.portfolio.servicecharge.util.daterange;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.accounting.journalentry.api.DateParam;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeApiConstants;
import org.apache.fineract.portfolio.servicecharge.exception.ServiceChargeException;
import org.apache.fineract.portfolio.servicecharge.exception.ServiceChargeException.SERVICE_CHARGE_EXCEPTION_REASON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A date range object that will implement the ServiceChargeDateRange This class
 * will have all the quarterly
 *
 */
enum MonthlyServiceChargeDateRange implements ServiceChargeDateRange, ServiceChargeApiConstants {
	JAN(1, "01 Jan ", "31 Jan "), FEB28(2, "01 Feb ", "28 Feb "), FEB29(3, "01 Feb ", "29 Feb "),
	MAR(4, "01 Mar ", "31 Mar "), APR(5, "01 Apr ", "30 Apr "), MAY(6, "01 May ", "31 May "),
	JUN(7, "01 Jun ", "30 Jun "), JUL(8, "01 Jul ", "31 Jul "), AUG(9, "01 Aug ", "31 Aug "),
	SEP(10, "01 Sep ", "30 Sep "), OCT(11, "01 Oct ", "31 Oct "), NOV(12, "01 Nov ", "30 Nov "),
	DEC(13, "01 Dec ", "31 Dec ");

	private final static Logger logger = LoggerFactory.getLogger(MonthlyServiceChargeDateRange.class);
	private final Integer id;
	private final String fromDate;
	private final String toDate;
	private int year;

	private final String dateFormatString = "dd MMMM yyyy";

	/**
	 * 
	 */
	private MonthlyServiceChargeDateRange(final Integer id, final String fromDate, final String toDate) {
		this.id = id;
		this.fromDate = fromDate;
		this.toDate = toDate;
		Calendar c = Calendar.getInstance(Locale.getDefault());
		this.year = c.get(Calendar.YEAR);
	}

	public String getFromDateString(int year) {
		return fromDate + year;
	}

	public String getToDateString(int year) {
		return toDate + year;
	}

	public String getDateFormatString() {
		return dateFormatString;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.fineract.portfolio.servicecharge.util.daterange.
	 * ServiceChargeDateRange#getId()
	 */
	@Override
	public Integer getId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.fineract.portfolio.servicecharge.util.daterange.
	 * ServiceChargeDateRange#getName()
	 */
	@Override
	public String getName() {
		return name();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.fineract.portfolio.servicecharge.util.daterange.
	 * ServiceChargeDateRange#getFromDateStringForCurrentYear()
	 */
	@Override
	public String getFromDateStringForCurrentYear() {
		return getFromDateString(Calendar.getInstance().get(Calendar.YEAR));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.fineract.portfolio.servicecharge.util.daterange.
	 * ServiceChargeDateRange#getToDateStringForCurrentYear()
	 */
	@Override
	public String getToDateStringForCurrentYear() {
		return getToDateString(Calendar.getInstance().get(Calendar.YEAR));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.fineract.portfolio.servicecharge.util.daterange.
	 * ServiceChargeDateRange#getFormattedFromDateString()
	 */
	@Override
	public String getFormattedFromDateString() {
		return DateUtils.formatToSqlDate(getFromDateForCurrentYear());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.fineract.portfolio.servicecharge.util.daterange.
	 * ServiceChargeDateRange#getFormattedToDateString()
	 */
	@Override
	public String getFormattedToDateString() {
		return DateUtils.formatToSqlDate(getToDateForCurrentYear());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.fineract.portfolio.servicecharge.util.daterange.
	 * ServiceChargeDateRange#getFromDateForCurrentYear()
	 */
	@Override
	public Date getFromDateForCurrentYear() {
		return getFromDate(getYear());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.fineract.portfolio.servicecharge.util.daterange.
	 * ServiceChargeDateRange#getToDateForCurrentYear()
	 */
	@Override
	public Date getToDateForCurrentYear() {
		return getToDate(getYear());
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

	public static ServiceChargeDateRange getCurrentMonth(String monthCode, int year) {
		ServiceChargeDateRange q = null;
		if (!StringUtils.isEmpty(monthCode)) {
			final String qStr = monthCode.toUpperCase();
			switch (qStr) {
			case ServiceChargeApiConstants.JANUARY:
				q = JAN;
				break;
			case ServiceChargeApiConstants.FEBRUARY:
				if (isCurrentYearLeapYear(year)) {
					q = FEB29;
				} else {
					q = FEB28;
				}
				break;
			case ServiceChargeApiConstants.MARCH:
				q = MAR;
				break;
			case ServiceChargeApiConstants.APRIL:
				q = APR;
				break;
			case ServiceChargeApiConstants.MAY:
				q = MAY;
				break;
			case ServiceChargeApiConstants.JUNE:
				q = JUN;
				break;
			case ServiceChargeApiConstants.JULY:
				q = JUL;
				break;
			case ServiceChargeApiConstants.AUGUST:
				q = AUG;
				break;
			case ServiceChargeApiConstants.SEPTEMBER:
				q = SEP;
				break;
			case ServiceChargeApiConstants.OCTOBER:
				q = OCT;
				break;
			case ServiceChargeApiConstants.NOVEMBER:
				q = NOV;
				break;
			case ServiceChargeApiConstants.DECEMBER:
				q = DEC;
				break;
			default:
				// Throw exception to say what was expected
				throw new ServiceChargeException(SERVICE_CHARGE_EXCEPTION_REASON.SC_INVALID_MONTH_CODE, null);
			}
		} else {
			Calendar c = Calendar.getInstance(Locale.getDefault());
			int month = c.get(Calendar.MONTH);

			switch (month) {
			case Calendar.JANUARY:
				q = JAN;
				break;
			case Calendar.FEBRUARY:
				if (isCurrentYearLeapYear(year)) {
					q = FEB29;
				} else {
					q = FEB28;
				}
				break;
			case Calendar.MARCH:
				q = MAR;
				break;
			case Calendar.APRIL:
				q = APR;
				break;
			case Calendar.MAY:
				q = MAY;
				break;
			case Calendar.JUNE:
				q = JUN;
				break;
			case Calendar.JULY:
				q = JUL;
				break;
			case Calendar.AUGUST:
				q = AUG;
				break;
			case Calendar.SEPTEMBER:
				q = SEP;
				break;
			case Calendar.OCTOBER:
				q = OCT;
				break;
			case Calendar.NOVEMBER:
				q = NOV;
				break;
			case Calendar.DECEMBER:
				q = DEC;
				break;
			default:
				// Throw exception to say what was expected
				throw new ServiceChargeException(SERVICE_CHARGE_EXCEPTION_REASON.SC_INVALID_MONTH_CODE, null);
			}
		}
		if (year != 0) {
			q.setYear(year);
		}
		logger.debug("MonthlyServiceChargeDateRange.getCurrentQuarter(): derived quarter::" + q);
		return q;
	}

	@Override
	public String toString() {
		return name().toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.fineract.portfolio.servicecharge.util.daterange.
	 * ServiceChargeDateRange#setYear()
	 */
	@Override
	public void setYear(int year) {
		this.year = year;
	}

	public int getYear() {
		return this.year;
	}

	private static boolean isCurrentYearLeapYear(int year) {
		boolean leap = false; // Default, no leap year

		if (year % 4 == 0) {
			// If a %4 year is divisible by 100 then it also has to be divisible by 400 to
			// qualify as leap year
			if (year % 100 == 0) {
				// year is divisible by 400, hence the year is a leap year
				if (year % 400 == 0) {
					leap = true;
				}
			} else {
				leap = true;
			}
		}
		return leap;
	}
}
