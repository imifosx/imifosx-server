package org.ideoholic.imifosx.portfolio.servicecharge.service;

import org.ideoholic.imifosx.accounting.journalentry.service.JournalEntryReadPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeJournalDetailsReadPlatformServiceImpl
		implements ServiceChargeJournalDetailsReadPlatformService {

	private final static Logger logger = LoggerFactory
			.getLogger(ServiceChargeJournalDetailsReadPlatformServiceImpl.class);

	private final JournalEntryReadPlatformService journalEntryReadPlatformService;

	@Autowired
	public ServiceChargeJournalDetailsReadPlatformServiceImpl(
			JournalEntryReadPlatformService journalEntryReadPlatformService) {
		this.journalEntryReadPlatformService = journalEntryReadPlatformService;
	}
}
