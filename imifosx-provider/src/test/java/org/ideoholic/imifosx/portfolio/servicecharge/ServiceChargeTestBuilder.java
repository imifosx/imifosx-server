package org.ideoholic.imifosx.portfolio.servicecharge;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.ideoholic.imifosx.infrastructure.configuration.spring.TestsWithoutDatabaseAndNoJobsConfiguration;
import org.ideoholic.imifosx.portfolio.servicecharge.service.ServiceChargeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;


@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ActiveProfiles("basicauth")
@ContextConfiguration(classes = TestsWithoutDatabaseAndNoJobsConfiguration.class)
public class ServiceChargeTestBuilder {
	
	@Autowired
	ServiceChargeService serviceCharge;

	@Test
	public void test() {
		Assert.assertNotNull(serviceCharge);
		serviceCharge.getAllLoansData();
	}

}
