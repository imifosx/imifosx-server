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
import java.util.List;

/**
 * Interface for class that will be used for holding data related to loan
 * product summary, including outstanding, repayments data over a period (daily,
 * monthly, quarterly)
 *
 */
public interface ServiceChargeLoanProductSummary {
	short DAILY = 1;
	short MONTHLY = 2;
	short QUARTERLY = 3;
	short YEARLY = 4;

	/**
	 * Calculate the outstanding for the period and return the outstanding over the
	 * period defined in the implementing class
	 * 
	 * @return List of BigDecimal
	 */
	List<BigDecimal> getPeriodicOutstanding();

	/**
	 * Calculate the repayments for the period and return the repayments over the
	 * period defined in the implementing class
	 * 
	 * @return total amount as BigDecimal
	 */
	List<BigDecimal> getPeriodicRepayments();

	BigDecimal getTotalOutstanding();

	/**
	 * Calculate the total repayments for the period <br/>
	 * Eg: if the set period is QUARTERLY then it will return the sum of repayments
	 * over each of the 3 months of the quarter
	 * 
	 * @return total amount as BigDecimal
	 */
	BigDecimal getTotalRepayments();

}
