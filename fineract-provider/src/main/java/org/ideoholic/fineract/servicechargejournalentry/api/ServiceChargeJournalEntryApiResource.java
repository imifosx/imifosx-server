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
package org.ideoholic.fineract.servicechargejournalentry.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ideoholic.fineract.commands.ServiceChargeJournalEntryCommand;
import org.ideoholic.fineract.servicechargejournalentry.service.ServiceChargeJournalEntryCreationPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("servicechargejournalentries")
@Component
@Scope("singleton")
public class ServiceChargeJournalEntryApiResource {

	private final ServiceChargeJournalEntryCreationPlatformService scJeWriter;

	@Autowired
	public ServiceChargeJournalEntryApiResource(final ServiceChargeJournalEntryCreationPlatformService scJeWriter) {
		this.scJeWriter = scJeWriter;
	}

	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	@ApiOperation(value = "Create \"Balanced\" Journal Entries", notes = "Note: A Balanced (simple) Journal entry would have atleast one \"Debit\" and one \"Credit\" entry whose amounts are equal \n"
			+ "Compound Journal entries may have \"n\" debits and \"m\" credits where both \"m\" and \"n\" are greater than 0 and the net sum or all debits and credits are equal \n\n"
			+ "\n" + "Mandatory Fields\n" + "officeId, transactionDate\n\n"
			+ "\ncredits- glAccountId, amount, comments\n\n " + "\ndebits-  glAccountId, amount, comments\n\n " + "\n"
			+ "Optional Fields\n" + "paymentTypeId, accountNumber, checkNumber, routingCode, receiptNumber, bankNumber")
	@ApiImplicitParams({
			@ApiImplicitParam(paramType = "body", value = "body", dataType = "body", dataTypeClass = ServiceChargeJournalEntryCommand.class) })
	@ApiResponses({
			@ApiResponse(code = 200, message = "") })
	public String createGLJournalEntry(@ApiParam(hidden = true) final String jsonRequestBody) {
		System.out.println("jsonRequestBody:" + jsonRequestBody);
		String result = null;
		result = this.scJeWriter.createServiceChargeJournalEntry(jsonRequestBody);

		return result;

	}
}
