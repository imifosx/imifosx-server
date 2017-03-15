package org.ideoholic.imifosx.portfolio.servicecharge.api;

import java.math.BigDecimal;
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
		BigDecimal totalRepayment = scLoanDetailsReadPlatformService.getAllLoansRepaymentData();
		System.out.println("************Total Repayment****************** "+totalRepayment);
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
		StringBuffer response = new StringBuffer();
		response.append(generateTableHeader(1));
		Map<String, List<BigDecimal>> journalResultMap = scJournalDetailsReadPlatformService.readJournalEntriesForGivenQuarter();
		if (displayTable) {
			response.append(ServiceChargeOperationUtils.convertMapToHTMLTable(journalResultMap, response));
		} else {
			response.append(journalResultMap.toString());
		}
		Map<String, List<BigDecimal>> calculationsMap = scJournalDetailsReadPlatformService.computeFinalCalculations(journalResultMap);
		response.append(generateTableHeader(2));
		if (displayTable) {
			response.append(ServiceChargeOperationUtils.convertMapToHTMLTable(calculationsMap, response));
		} else {
			response.append(calculationsMap.toString());
		}

		return response.toString();
	}

	private String generateTableHeader(int tableNumber) {
		StringBuffer sb = new StringBuffer();
		if (tableNumber == 1) {
			sb.append("<table table style=\"width:100%\" border=5pt>");
			sb.append("<tr>");
			sb.append("<td>");
			sb.append("Expenses Allocation Categaories");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Mobilisation");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Loan Servicing");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Investment");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Overheads");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Total");
			sb.append("</td>");
			sb.append("</tr>");
		} else if (tableNumber == 2) {
			sb.append("<table table style=\"width:100%\" border=5pt>");
			sb.append("<tr>");
			sb.append("<td>");
			sb.append("Particulars");
			sb.append("</td>");
			sb.append("<td>");
			sb.append("Value");
			sb.append("</td>");
			sb.append("</tr>");
		}
		return sb.toString();
	}

}