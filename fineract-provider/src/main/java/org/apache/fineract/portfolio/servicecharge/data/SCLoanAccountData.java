package org.apache.fineract.portfolio.servicecharge.data;

import java.math.BigDecimal;

import javax.persistence.Transient;

import org.joda.time.LocalDate;

public class SCLoanAccountData {

    // loan id
    private final Long id;

    // loan
    private final Long loanProductId;
    private final BigDecimal principalOutstanding;
    private final BigDecimal approvedPrincipal;

    // client
    private final Long clientId;

    @Transient
    private final BigDecimal feeChargesAtDisbursementCharged;

    // date
    private final LocalDate expectedDisbursementDate;
    private final LocalDate actualDisbursementDate;

    public SCLoanAccountData(final Long id, final Long loanProductId, final BigDecimal principalOutstanding,
            final BigDecimal approvedPrincipal, final Long clientId, final BigDecimal feeChargesAtDisbursementCharged,
            final LocalDate expectedDisbursementDate, final LocalDate actualDisbursementDate) {
        this.id = id;
        this.loanProductId = loanProductId;
        this.principalOutstanding = principalOutstanding;
        this.approvedPrincipal = approvedPrincipal;
        this.clientId = clientId;
        this.feeChargesAtDisbursementCharged = feeChargesAtDisbursementCharged;
        this.expectedDisbursementDate = expectedDisbursementDate;
        this.actualDisbursementDate = actualDisbursementDate;
    }

    public Long getClientId() {
        return this.clientId;
    }

    public Long getId() {
        return this.id;
    }

    public Long getLoanProductId() {
        return this.loanProductId;
    }

    public BigDecimal getApprovedPrincipal() {
        return this.approvedPrincipal;
    }

    public BigDecimal getPrincipalOutstanding() {
        return principalOutstanding;
    }

    public static SCLoanAccountData SCLoanDetails(final Long id, final Long loanProductId, final BigDecimal principalOutstanding,
            final BigDecimal approvedPrincipal, final Long clientId, final BigDecimal feeChargesAtDisbursementCharged,
            final LocalDate expectedDisbursementDate, final LocalDate actualDisbursementDate) {
        // TODO Auto-generated method stub
        return new SCLoanAccountData(id, loanProductId, principalOutstanding, approvedPrincipal, clientId, feeChargesAtDisbursementCharged,
                expectedDisbursementDate, actualDisbursementDate);
    }

    public LocalDate getDisbursementDate() {
        LocalDate disbursementDate = this.expectedDisbursementDate;
        if (this.actualDisbursementDate != null) {
            disbursementDate = this.actualDisbursementDate;
        }
        return disbursementDate;
    }
}
