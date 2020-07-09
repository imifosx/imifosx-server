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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ShareAccountCertificateRepository extends JpaRepository<ShareAccountCertificate, Long>, JpaSpecificationExecutor<ShareAccountCertificate> {

	@Query("update ShareAccountCertificate shareAccountCertificate  set shareAccountCertificate.shareAccountId = :shareAccountIdTo"
			+ ", shareAccountId = (select id from ShareAccountTransaction shareAccountTransaction where shareAccountTransaction.shareAccount.id = :shareAccountIdTo ORDER BY id DESC LIMIT 1)  where shareAccountCertificate.shareAccountId = :shareAccountIdFrom")
	void update(@Param("shareAccountIdFrom") Long shareAccountIdFrom,@Param("shareAccountIdTo") Long shareAccountIdTo);

}
