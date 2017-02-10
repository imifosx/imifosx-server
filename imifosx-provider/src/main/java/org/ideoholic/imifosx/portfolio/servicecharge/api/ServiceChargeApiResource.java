package org.ideoholic.imifosx.portfolio.servicecharge.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.ideoholic.imifosx.infrastructure.core.api.ApiRequestParameterHelper;
import org.ideoholic.imifosx.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.ideoholic.imifosx.portfolio.charge.data.ChargeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path(ServiceChargeApiConstants.serviceChargeRESTString)
@Component
@Scope("singleton")
public class ServiceChargeApiResource {

	private final Set<String> CHARGES_DATA_PARAMETERS = new HashSet<>(Arrays.asList("id", "name", "amount", "currency",
			"active", "chargeAppliesTo", "chargeTimeType", "chargeCalculationType", "chargeCalculationTypeOptions",
			"chargeAppliesToOptions", "chargeTimeTypeOptions", "currencyOptions", "loanChargeCalculationTypeOptions",
			"loanChargeTimeTypeOptions", "incomeAccount", "clientChargeCalculationTypeOptions",
			"clientChargeTimeTypeOptions"));

	private final String resourceNameForPermissions = "SERVICECHARGE";

	private final DefaultToApiJsonSerializer<ChargeData> toApiJsonSerializer;
	private final ApiRequestParameterHelper apiRequestParameterHelper;

	@Autowired
	public ServiceChargeApiResource(final DefaultToApiJsonSerializer<ChargeData> toApiJsonSerializer,
			final ApiRequestParameterHelper apiRequestParameterHelper) {
		this.toApiJsonSerializer = toApiJsonSerializer;
		this.apiRequestParameterHelper = apiRequestParameterHelper;
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
	public String retrieveServiceChargeForGivenLoan(@PathParam("loandId") final Long loanId,
			@Context final UriInfo uriInfo) {

		return "NOT YET IMPLEMENTED";
	}

}