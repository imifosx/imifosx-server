/**
 * 
 */
package org.apache.fineract.portfolio.servicecharge.util.daterange;

import java.util.Date;

/**
 * This will be the interface via which the date ranges will be accessed.
 * Basically this will help decide the range over which the Service Charge will
 * be calculated. Using the interface the aim is to make rest of the code
 * agnostic to the actual range value.
 */
public interface ServiceChargeDateRange {

    // Month reference strings for SC applicable date range calculation
    String _JANUARY = "JAN";
    String _FEBRUARY = "FEB";
    String _MARCH = "MAR";
    String _APRIL = "APR";
    String _MAY = "MAY";
    String _JUNE = "JUN";
    String _JULY = "JUL";
    String _AUGUST = "AUG";
    String _SEPTEMBER = "SEP";
    String _OCTOBER = "OCT";
    String _NOVEMBER = "NOV";
    String _DECEMBER = "DEC";

    /**
     * The ID of the date range.<br/>
     * Make sure that this is unique across all the distinctive objects among
     * all the sub-classes. This is because the object is stored and retrieved
     * via this Id
     * 
     * @return Integer
     */
    public Integer getId();

    /**
     * This is the name of the entity object that defines as to what it is
     * representing.<br/>
     * Eg: For quarter it can be Q1, Q2, Q3, Q4
     * 
     * @return String
     */
    public String getName();

    /**
     * Gets the starting date of the give range for the current year. The
     * calculation is done using the current system date.<br/>
     * 
     * @return String
     */
    public String getFromDateStringForCurrentYear();

    /**
     * Gets the ending date of the give range for the current year. The
     * calculation is done using the current system date.<br/>
     * 
     * @return String
     */
    public String getToDateStringForCurrentYear();

    /**
     * Gets the starting date of the give range for the current year. The
     * calculation is done using the current system date. The date needs to be
     * formated in <i>"dd MMMM yyyy"</i> format.<br/>
     * 
     * @return String
     */
    public String getFormattedFromDateString();

    /**
     * Gets the ending date of the give range for the current year. The
     * calculation is done using the current system date. The date needs to be
     * formated in <i>"dd MMMM yyyy"</i> format.<br/>
     * 
     * @return String
     */
    public String getFormattedToDateString();

    /**
     * Gets the starting date of the give range for the current year in
     * <i>java.util.Date</i> format. The calculation is done using the current
     * system date.<br/>
     * 
     * @return String
     */
    public Date getFromDateForCurrentYear();

    /**
     * Gets the ending date of the give range for the current year in
     * <i>java.util.Date</i> format. The calculation is done using the current
     * system date.<br/>
     * 
     * @return String
     */
    public Date getToDateForCurrentYear();

    /**
     * This facilitates setting the year to the SC range. Setting a year is used
     * to decide the year on which the calculation parameters needs to be
     * applied
     * 
     * @param year
     */
    void setYear(int year);

    /**
     * Gets the Service charge calculation method. This has to be returned by
     * the implementing class. Returned value has to be defined in the enum
     */
    ServiceChargeCalculatoinMethod getChargeCalculationMethodEnum();
    
    int getDateRangeDurationMonths();
}
