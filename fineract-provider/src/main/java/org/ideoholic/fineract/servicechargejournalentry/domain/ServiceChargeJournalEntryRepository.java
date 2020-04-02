package org.ideoholic.fineract.servicechargejournalentry.domain;

import java.sql.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceChargeJournalEntryRepository
		extends JpaRepository<ServiceChargeJournalEntry, Long>, JpaSpecificationExecutor<ServiceChargeJournalEntry> {

	@Query("select serviceChargeJournalEntry from ServiceChargeJournalEntry serviceChargeJournalEntry where "
			+ "serviceChargeJournalEntry.transactionId= :transactionId and journalEntry.isReversed=false")
	List<ServiceChargeJournalEntry> findServiceChargeJournalEntryByTransactionId(
			@Param("transactionId") String transactionId);

	@Query("select serviceChargeJournalEntry from ServiceChargeJournalEntry serviceChargeJournalEntry where "
			+ "(transactionDate between startDate and endDate) and journalEntry.isReversed=false")
	List<ServiceChargeJournalEntry> findServiceChargeJournalEntrysBetweenTransactionDates(
			@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
