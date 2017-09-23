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

package org.ideoholic.imifosx.portfolio.servicecharge.data;

import java.io.Serializable;
import java.math.BigDecimal;

import org.ideoholic.imifosx.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;

/**
 * Immutable data object for service charge data
 */
public class ServiceChargeData implements Comparable<ServiceChargeData>, Serializable {
	private static final long serialVersionUID = 3L;

	private final Long id;
	private final int scQuarter;
	private final int scYear;
	private final int scHeader;
	private final BigDecimal scAmount;

	public static ServiceChargeData template(final int quarter, final int year, final ServiceChargeReportTableHeaders header, final BigDecimal amount) {
		return new ServiceChargeData(0L, quarter, year, header, amount);
	}

	public static ServiceChargeData withTemplate(final Long id, final int quarter, final int year, final ServiceChargeReportTableHeaders header,
			final BigDecimal amount) {
		return new ServiceChargeData(id, quarter, year, header, amount);
	}

	public static ServiceChargeData instance(final Long id, final int scQuarter, final int scYear, final ServiceChargeReportTableHeaders header,
			final BigDecimal scAmount) {

		return new ServiceChargeData(id, scQuarter, scYear, header, scAmount);
	}

	public static ServiceChargeData lookup(final Long id) {
		final int scQuarter = 0;
		final int scYear = 0;
		final ServiceChargeReportTableHeaders scHeader = null;
		final BigDecimal scAmount = null;
		return new ServiceChargeData(id, scQuarter, scYear, scHeader, scAmount);
	}

	protected ServiceChargeData(final Long id, final int scQuarter, final int scYear, final ServiceChargeReportTableHeaders scHeader,
			final BigDecimal scAmount) {
		this.id = id;
		this.scQuarter = scQuarter;
		this.scYear = scYear;
		this.scHeader = scHeader.getValue();
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
	public final int getQuarter() {
		return scQuarter;
	}

	/**
	 * @return the Year of this Service Charge
	 */
	public final int getYear() {
		return scYear;
	}

	/**
	 * @return the header of this Service Charge component as string mapped to {@link ServiceChargeReportTableHeaders}
	 */
	public final String getHeader() {
		ServiceChargeReportTableHeaders header = ServiceChargeReportTableHeaders.fromInt(scHeader);
		if (header == null) {
			return "UNKNOWN";
		}
		return header.getCode();
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