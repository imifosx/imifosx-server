package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ServiceChargeJournalDetailsReadPlatformService {
	BigDecimal HUNDRED = new BigDecimal(100);
	// Add Journal related Service Charge methods here
	Map<String, List<BigDecimal>> readJournalEntriesForGivenQuarter();

	Map<String, List<BigDecimal>> computeFinalCalculations(Map<String, List<BigDecimal>> journalEntriest);
}
