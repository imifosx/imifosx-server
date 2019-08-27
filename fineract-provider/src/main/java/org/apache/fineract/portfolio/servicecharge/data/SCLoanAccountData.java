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
