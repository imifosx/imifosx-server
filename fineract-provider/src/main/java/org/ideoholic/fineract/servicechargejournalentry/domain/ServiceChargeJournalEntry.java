/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ideoholic.fineract.servicechargejournalentry.domain;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.DateTime;

/**
 * @author Ideoholic
 *
 */
@Entity
@Table(name = "acc_gl_servicecharge_journal_entry")
public class ServiceChargeJournalEntry extends AbstractAuditableCustom<AppUser, Long> {
	private static final long serialVersionUID = 764856345295446081L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "journal_entry_id", nullable = false)
	private JournalEntry journalEntry;

	@ManyToOne
	@JoinColumn(name = "office_id", nullable = false)
	private Office office;

	@Column(name = "entry_date", nullable = false)
	@Temporal(TemporalType.DATE)
	private Date transactionDate;

	@Column(name = "mobilization_percentage", scale = 2, precision = 3, nullable = false)
	private BigDecimal mobilizationPercent;

	@Column(name = "servicing_percentage", scale = 2, precision = 3, nullable = false)
	private BigDecimal servicingPercent;

	@Column(name = "investment_percentage", scale = 2, precision = 3, nullable = false)
	private BigDecimal investmentPercent;

	@Column(name = "overheads_percentage", scale = 2, precision = 3, nullable = false)
	private BigDecimal overheadsPercent;

	@Column(name = "mobilization_amount", scale = 6, precision = 19, nullable = false)
	private BigDecimal mobilizationAmount;

	@Column(name = "servicing_amount", scale = 6, precision = 19, nullable = false)
	private BigDecimal servicingAmount;

	@Column(name = "investment_amount", scale = 6, precision = 19, nullable = false)
	private BigDecimal investmentAmount;

	@Column(name = "overheads_amount", scale = 6, precision = 19, nullable = false)
	private BigDecimal overheadsAmount;

	@Column(name = "is_reversed", nullable = false)
	private boolean isReversed = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "gl_reversal_id")
	private JournalEntry reversalJournalEntry;

	public ServiceChargeJournalEntry(final JournalEntry journalEntry, final Office office, final Date transactionDate,
			final BigDecimal mobilizationPercent, final BigDecimal servicingPercent, final BigDecimal investmentPercent,
			final BigDecimal overheadsPercent, final BigDecimal mobilizationAmount, final BigDecimal servicingAmount,
			final BigDecimal investmentAmount, final BigDecimal overheadsAmount, final boolean isReversed,
			final JournalEntry reversalJournalEntry, final AppUser createdBy, final DateTime createdDate,
			final AppUser lastModifiedBy, final DateTime lastModifiedDate) {
		this.journalEntry = journalEntry;
		this.office = office;
		this.transactionDate = transactionDate;
		this.mobilizationPercent = mobilizationPercent;
		this.servicingPercent = servicingPercent;
		this.investmentPercent = investmentPercent;
		this.overheadsPercent = overheadsPercent;
		this.mobilizationAmount = mobilizationAmount;
		this.servicingAmount = servicingAmount;
		this.investmentAmount = investmentAmount;
		this.overheadsAmount = overheadsAmount;
		this.isReversed = isReversed;
		this.reversalJournalEntry = reversalJournalEntry;
		this.setCreatedBy(createdBy);
		this.setCreatedDate(createdDate);
		this.setLastModifiedBy(lastModifiedBy);
		this.setLastModifiedDate(lastModifiedDate);
	}

	public static ServiceChargeJournalEntry createNew(final JournalEntry journalEntry, final Office office,
			final Date transactionDate, final BigDecimal mobilizationPercent, final BigDecimal servicingPercent,
			final BigDecimal investmentPercent, final BigDecimal overheadsPercent, final BigDecimal mobilizationAmount,
			final BigDecimal servicingAmount, final BigDecimal investmentAmount, final BigDecimal overheadsAmount,
			final boolean isReversed, final JournalEntry reversalJournalEntry, final AppUser createdBy,
			final DateTime createdDate, final AppUser lastModifiedBy, final DateTime lastModifiedDate) {
		return new ServiceChargeJournalEntry(journalEntry, office, transactionDate, mobilizationPercent,
				servicingPercent, investmentPercent, overheadsPercent, mobilizationAmount, servicingAmount,
				investmentAmount, overheadsAmount, isReversed, reversalJournalEntry, createdBy, createdDate,
				lastModifiedBy, lastModifiedDate);
	}

	public static ServiceChargeJournalEntry createNew(final JournalEntry journalEntry, final Office office,
			final Date transactionDate, final BigDecimal mobilizationPercent, final BigDecimal servicingPercent,
			final BigDecimal investmentPercent, final BigDecimal overheadsPercent, final BigDecimal mobilizationAmount,
			final BigDecimal servicingAmount, final BigDecimal investmentAmount, final BigDecimal overheadsAmount,
			final AppUser createdBy, final DateTime createdDate, final AppUser lastModifiedBy,
			final DateTime lastModifiedDate) {
		return new ServiceChargeJournalEntry(journalEntry, office, transactionDate, mobilizationPercent,
				servicingPercent, investmentPercent, overheadsPercent, mobilizationAmount, servicingAmount,
				investmentAmount, overheadsAmount, false, null, createdBy, createdDate, lastModifiedBy,
				lastModifiedDate);
	}

	public static ServiceChargeJournalEntry createNew(final JournalEntry journalEntry, final Office office,
			final Date transactionDate, final BigDecimal mobilizationPercent, final BigDecimal servicingPercent,
			final BigDecimal investmentPercent, final BigDecimal overheadsPercent, final BigDecimal mobilizationAmount,
			final BigDecimal servicingAmount, final BigDecimal investmentAmount, final BigDecimal overheadsAmount,
			final AppUser createdBy, final DateTime createdDate) {
		return new ServiceChargeJournalEntry(journalEntry, office, transactionDate, mobilizationPercent,
				servicingPercent, investmentPercent, overheadsPercent, mobilizationAmount, servicingAmount,
				investmentAmount, overheadsAmount, false, null, createdBy, createdDate, createdBy, createdDate);
	}

	public static ServiceChargeJournalEntry createNew(final JournalEntry journalEntry, final Office office,
			final Date transactionDate, final BigDecimal mobilizationPercent, final BigDecimal servicingPercent,
			final BigDecimal investmentPercent, final BigDecimal overheadsPercent, final BigDecimal mobilizationAmount,
			final BigDecimal servicingAmount, final BigDecimal investmentAmount, final BigDecimal overheadsAmount,
			final AppUser createdBy) {
		final DateTime createdDate = DateUtils.getLocalDateTimeOfTenant().toDateTime();
		return new ServiceChargeJournalEntry(journalEntry, office, transactionDate, mobilizationPercent,
				servicingPercent, investmentPercent, overheadsPercent, mobilizationAmount, servicingAmount,
				investmentAmount, overheadsAmount, false, null, createdBy, createdDate, createdBy, createdDate);
	}

	public boolean isReversed() {
		return isReversed;
	}

	public void setReversed(boolean isReversed) {
		this.isReversed = isReversed;
	}

	public JournalEntry getReversalJournalEntry() {
		return reversalJournalEntry;
	}

	public void setReversalJournalEntry(JournalEntry reversalJournalEntry) {
		this.reversalJournalEntry = reversalJournalEntry;
	}

	public JournalEntry getJournalEntry() {
		return journalEntry;
	}

	public Office getOffice() {
		return office;
	}

	public Date getTransactionDate() {
		return transactionDate;
	}

	public BigDecimal getMobilizationPercent() {
		return mobilizationPercent;
	}

	public BigDecimal getServicingPercent() {
		return servicingPercent;
	}

	public BigDecimal getInvestmentPercent() {
		return investmentPercent;
	}

	public BigDecimal getOverheadsPercent() {
		return overheadsPercent;
	}

	public BigDecimal getMobilizationAmount() {
		return mobilizationAmount;
	}

	public BigDecimal getServicingAmount() {
		return servicingAmount;
	}

	public BigDecimal getInvestmentAmount() {
		return investmentAmount;
	}

	public BigDecimal getOverheadsAmount() {
		return overheadsAmount;
	}

}
