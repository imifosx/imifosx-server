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
package org.ideoholic.fineract.servicecharge.service;

import javax.sql.DataSource;

import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.ideoholic.fineract.servicecharge.data.ServiceChargeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeWritePlatformServiceImp implements ServiceChargeWritePlatformService {

	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;

	@Autowired
	public ServiceChargeWritePlatformServiceImp(final RoutingDataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(this.dataSource);

	}

	@Override
	public ServiceChargeData createCharge(ServiceChargeData serviceChargeData) {

		String transactionSql = "INSERT INTO m_loan_service_charge  (sc_quarter,sc_year,sc_header,sc_amount) VALUES (?, ?, ?, ?)";
		int result = this.jdbcTemplate.update(transactionSql, serviceChargeData.getQuarter().getId(), serviceChargeData.getYear(),
				serviceChargeData.getHeader().getValue(), serviceChargeData.getAmount());

		if (0 < result) {
			return serviceChargeData;
		}

		return null;
	}

	@Override
	public ServiceChargeData updateCharge(ServiceChargeData serviceChargeData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceChargeData deleteCharge(ServiceChargeData serviceChargeData) {
		// TODO Auto-generated method stub
		return null;
	}

}
