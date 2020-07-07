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
package org.ideoholic.fineract.sharestransfer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.ideoholic.fineract.accountingtransfer.service.ClientToAccountingHeaderTransferService;
import org.ideoholic.fineract.commands.TransferEntryCommand;
import org.ideoholic.fineract.sharestransfer.service.SharesTransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("sharestransfer")
@Component
@Scope("singleton")
public class SharesTransferApiResource {

	private final SharesTransferService transferService;

	@Autowired
	public SharesTransferApiResource(final SharesTransferService transferService) {
		this.transferService = transferService;
	}

	/*@POST
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Share Transfer", httpMethod = "POST", notes = "Transfer share")
    //@ApiImplicitParams({@ApiImplicitParam(value = "body", required = true, paramType = "body", dataType = "body", format = "body", dataTypeClass = SharesTransferEntry.class)})
    @ApiResponses({@ApiResponse(code = 200, message = "OK")})
	public String shareTransferEntry(@PathParam("type") @ApiParam(value = "type") final String accountType, @PathParam("accountId") @ApiParam(value = "accountId") final Long accountId, @QueryParam("command") @ApiParam(value = "command") final String commandParam,
            @ApiParam(hidden = true) final String apiRequestBodyAsJson) {
		String result = null;
			System.out.println("First Step 'Type'"+apiRequestBodyAsJson);
			System.out.println("First Step 'accountId'"+apiRequestBodyAsJson);
			System.out.println("First Step 'command'"+apiRequestBodyAsJson);
		result = this.transferService.createSharesTransferEntry(accountId,apiRequestBodyAsJson);

		return result;

	}*/
	
	
	@POST
	@Path("/updatesharecertificate")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Share Transfer", httpMethod = "POST", notes = "Transfer share")
    //@ApiImplicitParams({@ApiImplicitParam(value = "body", required = true, paramType = "body", dataType = "body", format = "body", dataTypeClass = SharesTransferEntry.class)})
    @ApiResponses({@ApiResponse(code = 200, message = "OK")})
	public String sharesCertificate(@ApiParam(hidden = true) final String apiRequestBodyAsJson) {
		System.out.println("*********IDEOHOLIC ********* "+apiRequestBodyAsJson);
		String result = null;
		result = this.transferService.updateSharesCertificate(apiRequestBodyAsJson);
		return result;

	}
	
}
