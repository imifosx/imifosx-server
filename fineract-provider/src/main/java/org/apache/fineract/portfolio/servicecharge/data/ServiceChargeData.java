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

import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.apache.fineract.portfolio.servicecharge.util.daterange.ServiceChargeDateRange;

/**
 * Immutable data object for service charge data
 */
public class ServiceChargeData implements Comparable<ServiceChargeData>, Serializable {
	private static final long serialVersionUID = 3L;

	private final Long id;
	private final ServiceChargeDateRange scQuarter;
	private final int scYear;
	private final ServiceChargeReportTableHeaders scHeader;
	private final BigDecimal scAmount;

	public static ServiceChargeData template(final ServiceChargeDateRange quarter, final int year,
			final ServiceChargeReportTableHeaders header, final BigDecimal amount) {
		return new ServiceChargeData(0L, quarter, year, header, amount);
	}

	public static ServiceChargeData withTemplate(final Long id, final ServiceChargeDateRange quarter, final int year,
			final ServiceChargeReportTableHeaders header, final BigDecimal amount) {
		return new ServiceChargeData(id, quarter, year, header, amount);
	}

	public static ServiceChargeData instance(final Long id, final ServiceChargeDateRange scQuarter, final int scYear,
			final ServiceChargeReportTableHeaders header, final BigDecimal scAmount) {

		return new ServiceChargeData(id, scQuarter, scYear, header, scAmount);
	}

	public static ServiceChargeData lookup(final Long id) {
		final ServiceChargeDateRange scQuarter = null;
		final int scYear = 0;
		final ServiceChargeReportTableHeaders scHeader = null;
		final BigDecimal scAmount = null;
		return new ServiceChargeData(id, scQuarter, scYear, scHeader, scAmount);
	}

	protected ServiceChargeData(final Long id, final ServiceChargeDateRange scQuarter, final int scYear,
			final ServiceChargeReportTableHeaders scHeader, final BigDecimal scAmount) {
		this.id = id;
		this.scQuarter = scQuarter;
		this.scYear = scYear;
		this.scHeader = scHeader;
		this.scAmount = scAmount;
	}

	/**
	 * @return the id
	 */
	public final Long getId() {
		return id;
	}

	/**
	 * @return the Quarter of this Service Charge
	 */
	public final ServiceChargeDateRange getQuarter() {
		return scQuarter;
	}

	/**
	 * @return the Year of this Service Charge
	 */
	public final int getYear() {
		return scYear;
	}

	/**
	 * @return the header of this Service Charge component as string mapped to
	 *         {@link ServiceChargeReportTableHeaders}
	 */
	public final ServiceChargeReportTableHeaders getHeader() {
		if (scHeader == null) {
			return ServiceChargeReportTableHeaders.INVALID;
		}
		return scHeader;
	}

	/**
	 * @return the Amount of this Service Charge component
	 */
	public final BigDecimal getAmount() {
		return scAmount;
	}

	@Override
	public boolean equals(final Object obj) {
		final ServiceChargeData chargeData = (ServiceChargeData) obj;
		return this.id.equals(chargeData.id);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public int compareTo(final ServiceChargeData obj) {
		if (obj == null) {
			return -1;
		}
		return obj.id.compareTo(this.id);
	}

}