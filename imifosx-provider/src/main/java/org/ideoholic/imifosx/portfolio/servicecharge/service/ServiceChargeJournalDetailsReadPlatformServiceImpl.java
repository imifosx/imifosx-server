package org.ideoholic.imifosx.portfolio.servicecharge.service;

import org.ideoholic.imifosx.accounting.glaccount.service.GLAccountReadPlatformService;
import org.ideoholic.imifosx.accounting.journalentry.service.JournalEntryReadPlatformService;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.GLExpenseTagsForServiceCharge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeJournalDetailsReadPlatformServiceImpl implements
		ServiceChargeJournalDetailsReadPlatformService {

	private final static Logger logger = LoggerFactory
			.getLogger(ServiceChargeJournalDetailsReadPlatformServiceImpl.class);

	private final JournalEntryReadPlatformService journalEntryReadPlatformService;
	private final GLAccountReadPlatformService glAccountReadPlatformService;

	@Autowired
	public ServiceChargeJournalDetailsReadPlatformServiceImpl(
			JournalEntryReadPlatformService journalEntryReadPlatformService,
			GLAccountReadPlatformService glAccountReadPlatformService) {
		// Initialize the class level final autowired variables
		this.journalEntryReadPlatformService = journalEntryReadPlatformService;
		this.glAccountReadPlatformService = glAccountReadPlatformService;
	}
	
	public void readJournalEntriesForGivenQuarter(){
		glAccountReadPlatformService.retrieveAccountsByTagId(1L, GLExpenseTagsForServiceCharge.MOBILIZATION.getValue());
	}
}
