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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.apache.fineract.portfolio.servicecharge.data.ServiceChargeData;
import org.apache.fineract.portfolio.servicecharge.util.daterange.ServiceChargeDateRange;
import org.apache.fineract.portfolio.servicecharge.util.daterange.ServiceChargeDateRangeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeReadPlatformServiceImpl implements ServiceChargeReadPlatformService {

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public ServiceChargeReadPlatformServiceImpl(final RoutingDataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public Collection<ServiceChargeData> retrieveCharge(ServiceChargeDateRange dateRange, int year) {
		try {

			final ServiceChargeMapper scm = new ServiceChargeMapper();

			// retrieve service charges

			final String sql = "select " + scm.ServiceChargeSchema() + " where sc_quarter = ? and sc_year = ?";
			return this.jdbcTemplate.query(sql, scm, new Object[] { dateRange.getId(), year });
		} catch (final EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public Collection<ServiceChargeData> retrieveAllCharges() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceChargeData retrieveCharge(Long chargeId) {
		// TODO Auto-generated method stub
		return null;
	}

	private static final class ServiceChargeMapper implements RowMapper<ServiceChargeData> {

		public String ServiceChargeSchema() {

			return "sc_quarter, sc_year, sc_header, sc_amount from m_loan_service_charge";
		}

		@Override
		public ServiceChargeData mapRow(ResultSet rs, int rowNum) throws SQLException {

			final int quarter = rs.getInt("sc_quarter");
			final int year = rs.getInt("sc_year");
			final int header = rs.getInt("sc_header");
			final BigDecimal amount = rs.getBigDecimal("sc_amount");

			return ServiceChargeData.template(ServiceChargeDateRangeFactory.getServiceChargeDateRangeFromInt(quarter), year,
					ServiceChargeReportTableHeaders.fromInt(header), amount);
		}

	}

}
