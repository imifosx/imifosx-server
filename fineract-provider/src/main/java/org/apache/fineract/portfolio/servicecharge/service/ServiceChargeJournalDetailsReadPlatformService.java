package org.apache.fineract.portfolio.servicecharge.service;

import java.math.BigDecimal;

import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeFinalSheetData;

public interface ServiceChargeJournalDetailsReadPlatformService {
	BigDecimal HUNDRED = new BigDecimal(100);
	
	// Add Journal related Service Charge methods here
	ServiceChargeFinalSheetData generatefinalSheetData();
}
