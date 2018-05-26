package org.apache.fineract.portfolio.servicecharge.util;

import java.util.Calendar;

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
}
