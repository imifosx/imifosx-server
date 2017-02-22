package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.util.List;
import java.util.Map;

public interface ServiceChargeJournalDetailsReadPlatformService {
	// Add Journal related Service Charge methods here
	Map<String, List<String>> readJournalEntriesForGivenQuarter();
}
