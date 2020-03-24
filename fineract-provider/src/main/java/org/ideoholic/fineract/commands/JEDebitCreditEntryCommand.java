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
package org.ideoholic.fineract.commands;

import java.math.BigDecimal;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * JSON Object mapper Java class that maps to the request of SingleDebitOrCreditEntry
 * request JSON. This is needed to convert the JSON to object, perform required
 * manipulation before converting back to JSON. Hence got JSON can be directly
 * used to post SingleDebitOrCreditEntry
 * 
 * @author ideoholic
 * @see org.apache.fineract.accounting.journalentry.command.SingleDebitOrCreditEntryCommand
 */
public class JEDebitCreditEntryCommand {

	@JsonProperty("glAccountId")
	private Long glAccountId;
	@JsonProperty("amount")
	private BigDecimal amount;
	@JsonProperty("comments")
	private String comments;

	public Long getGlAccountId() {
		return this.glAccountId;
	}

	public BigDecimal getAmount() {
		return this.amount;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public void setGlAccountId(Long glAccountId) {
		this.glAccountId = glAccountId;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

}