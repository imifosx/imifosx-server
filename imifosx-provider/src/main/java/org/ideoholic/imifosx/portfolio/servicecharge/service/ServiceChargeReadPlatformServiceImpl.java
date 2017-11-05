package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.ideoholic.imifosx.infrastructure.core.service.RoutingDataSource;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.QuarterDateRange;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.ServiceChargeReportTableHeaders;
import org.ideoholic.imifosx.portfolio.servicecharge.data.ServiceChargeData;
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
	public Collection<ServiceChargeData> retrieveCharge(QuarterDateRange quarterDateRange, int year) {
		try {

			final ServiceChargeMapper scm = new ServiceChargeMapper();

			// retrieve service charges

			final String sql = "select " + scm.ServiceChargeSchema() + " where sc_quarter = ? and sc_year = ?";
			return this.jdbcTemplate.query(sql, scm, new Object[] { quarterDateRange.getId(), year });
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

			return ServiceChargeData.template(QuarterDateRange.fromInt(quarter), year, ServiceChargeReportTableHeaders.fromInt(header), amount);
		}

	}

}
