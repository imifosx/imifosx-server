package org.ideoholic.imifosx.portfolio.servicecharge.service;

import javax.sql.DataSource;

import org.ideoholic.imifosx.infrastructure.core.service.RoutingDataSource;
import org.ideoholic.imifosx.portfolio.servicecharge.data.ServiceChargeData;
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
