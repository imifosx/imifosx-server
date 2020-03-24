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
package org.ideoholic.fineract.accountingtransfer.service;

public interface ClientToAccountingHeaderTransferService {

	/**
	 * Method to create a transfer entry transaction From an accounting head To a
	 * member OR vice-versa. The JSON format is fixed and the same json format needs
	 * to be followed else it will throw GeneralPlatformDomainRuleException.
	 * 
	 * @param jsonCommand String transfer JSON command data that is passed in the
	 *                    request for transfer
	 * @return String that give the result of the opertation that can be used to
	 *         construct responsse
	 */
	String createTransferEntry(String jsonCommand);
}
