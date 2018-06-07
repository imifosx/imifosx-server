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
package org.apache.fineract.portfolio.servicecharge.service;

import java.math.BigDecimal;

public interface ServiceChargeCalculationPlatformService {
	/**
	 * Service charge calculation method for a given loan, with the loan being already saved and is currently active and functional
	 * 
	 * @param loanId
	 * @return Service Charge
	 */
	BigDecimal calculateServiceChargeForLoan(Long loanId);

	/**
	 * Service charge calculation method for a given loan, when the loan is yet being disbursed and hence only the principal amount is known. It
	 * assumes that the loan is disbursed, no repayments yet made and principal amount is the outstanding amount of the loan.
	 * 
	 * @param principal
	 * @param numberOfRepayments
	 * @return Service Charge - Computed value for the given principal amount
	 */
	BigDecimal calculateServiceChargeForPrincipal(BigDecimal principal, Integer numberOfRepayments);
}
