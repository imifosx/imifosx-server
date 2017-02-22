package org.ideoholic.imifosx.portfolio.servicecharge.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.ideoholic.imifosx.infrastructure.core.api.ApiRequestParameterHelper;
import org.ideoholic.imifosx.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.ideoholic.imifosx.portfolio.charge.data.ChargeData;
import org.ideoholic.imifosx.portfolio.servicecharge.constants.ServiceChargeApiConstants;
import org.ideoholic.imifosx.portfolio.servicecharge.service.ServiceChargeJournalDetailsReadPlatformService;
import org.ideoholic.imifosx.portfolio.servicecharge.service.ServiceChargeLoanDetailsReadPlatformService;
import org.ideoholic.imifosx.portfolio.servicecharge.util.ServiceChargeOperationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path(ServiceChargeApiConstants.SERVICE_CHARGE_REST_CALL)
@Component
@Scope("singleton")
public class ServiceChargeApiResource {

	private final Set<String> CHARGES_DATA_PARAMETERS = new HashSet<>(Arrays.asList("id", "name", "amount", "currency", "active", "chargeAppliesTo", "chargeTimeType",
			"chargeCalculationType", "chargeCalculationTypeOptions", "chargeAppliesToOptions", "chargeTimeTypeOptions", "currencyOptions", "loanChargeCalculationTypeOptions",
			"loanChargeTimeTypeOptions", "incomeAccount", "clientChargeCalculationTypeOptions", "clientChargeTimeTypeOptions"));

	private final String resourceNameForPermissions = "SERVICECHARGE";

	private final DefaultToApiJsonSerializer<ChargeData> toApiJsonSerializer;
	private final ApiRequestParameterHelper apiRequestParameterHelper;
	private final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService;
	private final ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService;

	@Autowired
	public ServiceChargeApiResource(final DefaultToApiJsonSerializer<ChargeData> toApiJsonSerializer, final ApiRequestParameterHelper apiRequestParameterHelper,
			final ServiceChargeJournalDetailsReadPlatformService scJournalDetailsReadPlatformService,
			final ServiceChargeLoanDetailsReadPlatformService scLoanDetailsReadPlatformService) {
		this.toApiJsonSerializer = toApiJsonSerializer;
		this.apiRequestParameterHelper = apiRequestParameterHelper;
		this.scJournalDetailsReadPlatformService = scJournalDetailsReadPlatformService;
		this.scLoanDetailsReadPlatformService = scLoanDetailsReadPlatformService;
	}

	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveServiceCharge(@Context final UriInfo uriInfo) {
		return "NOT YET IMPLEMENTED";
	}

	@GET
	@Path("{loandId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveServiceChargeForGivenLoan(@PathParam("loandId") final Long loanId, @Context final UriInfo uriInfo) {
		return "NOT YET IMPLEMENTED";
	}

	@GET
	@Path("getJournalEntries")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String printJournalEntries(@Context final UriInfo uriInfo, @QueryParam("table") final boolean displayTable) {
		Map<String, List<String>> resultMap = scJournalDetailsReadPlatformService.readJournalEntriesForGivenQuarter();
		if (displayTable) {
			return ServiceChargeOperationUtils.convertMapToHTMLTable(resultMap);
		}
		return resultMap.toString();
	}

}