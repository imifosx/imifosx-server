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
package org.ideoholic.fineract.sharestransfer.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "m_share_account_certificate")
public class ShareAccountCertificate extends AbstractPersistableCustom<Long> {

    
    @Column(name = "share_account_id")
    private Long shareAccountId;

    @Column(name = "share_transaction_id")
    private Long shareTransactionId;

    @Column(name = "cert_num")
    private Long certNum;

    @Column(name = "from_num") 
    private Long fromNum ;
    
    @Column(name = "to_num") 
    private Long toNum ;
    
    protected ShareAccountCertificate() {

    }

    private ShareAccountCertificate(final Long shareAccountId, final Long shareTransactionId,
    		final Long certNum, final Long fromNum, final Long toNum) {
        this.shareAccountId = shareAccountId;
        this.shareTransactionId = shareTransactionId;
        this.certNum = certNum;
        this.fromNum = fromNum ;
        this.toNum = toNum ;
    }

	public Long getShareAccountId() {
		return shareAccountId;
	}

	public void setShareAccountId(Long shareAccountId) {
		this.shareAccountId = shareAccountId;
	}

	public Long getShareTransactionId() {
		return shareTransactionId;
	}

	public void setShareTransactionId(Long shareTransactionId) {
		this.shareTransactionId = shareTransactionId;
	}

	public Long getCertNum() {
		return certNum;
	}

	public void setCertNum(Long certNum) {
		this.certNum = certNum;
	}

	public Long getFromNum() {
		return fromNum;
	}

	public void setFromNum(Long fromNum) {
		this.fromNum = fromNum;
	}

	public Long getToNum() {
		return toNum;
	}

	public void setToNum(Long toNum) {
		this.toNum = toNum;
	}
    
  
}
