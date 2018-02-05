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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeLoanProductSummary;
import org.springframework.stereotype.Service;

/**
 * Factory pattern to get an object of the type of
 * ServiceChargeLoanProductSummary This class holds a list of all the objects
 * created. If there is a request for a duplicate object then the existing
 * object from the current map is returned else a new object is created and
 * added to the list
 *
 */
@Service
public class ServiceChargeLoanSummaryFactory {
	
	Map<Long, ServiceChargeLoanProductSummary> loanSummaryObjectMap;
	
	short type;
	/**
	 * Type param is used to decide on the implementing type of the class that needs to be returned
	 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.ServiceChargeLoanProductSummary
	 * @param type
	 */
	ServiceChargeLoanSummaryFactory(short type){
		this.type = type;
		loanSummaryObjectMap = new HashMap<Long, ServiceChargeLoanProductSummary>();
	}
	
	public ServiceChargeLoanProductSummary getLoanSummaryObject(LoanAccountData loanAccData) {
		return null;
	}

	private class LoanSummaryQuarterly implements ServiceChargeLoanProductSummary {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getPeriodicOutstanding()
		 */
		@Override
		public List<BigDecimal> getPeriodicOutstanding() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getPeriodicRepayments()
		 */
		@Override
		public List<BigDecimal> getPeriodicRepayments() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getTotalOutstanding()
		 */
		@Override
		public BigDecimal getTotalOutstanding() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.ideoholic.imifosx.portfolio.servicecharge.data.
		 * ServiceChargeLoanProductSummary#getTotalRepayments()
		 */
		@Override
		public BigDecimal getTotalRepayments() {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
