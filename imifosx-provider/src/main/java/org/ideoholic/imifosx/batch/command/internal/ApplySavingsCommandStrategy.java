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
package org.ideoholic.imifosx.batch.command.internal;

import javax.ws.rs.core.UriInfo;

import org.ideoholic.imifosx.batch.command.CommandStrategy;
import org.ideoholic.imifosx.batch.domain.BatchRequest;
import org.ideoholic.imifosx.batch.domain.BatchResponse;
import org.ideoholic.imifosx.batch.exception.ErrorHandler;
import org.ideoholic.imifosx.batch.exception.ErrorInfo;
import org.ideoholic.imifosx.portfolio.savings.api.SavingsAccountsApiResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implements {@link org.ideoholic.imifosx.batch.command.CommandStrategy} and
 * applies a new savings on an existing client. It passes the contents of the
 * body from the BatchRequest to
 * {@link org.ideoholic.imifosx.portfolio.client.api.SavingsAccountsApiResource} and
 * gets back the response. This class will also catch any errors raised by
 * {@link org.ideoholic.imifosx.portfolio.client.api.SavingsAccountsApiResource} and
 * map those errors to appropriate status codes in BatchResponse.
 * 
 * @author Rishabh Shukla
 * 
 * @see org.ideoholic.imifosx.batch.command.CommandStrategy
 * @see org.ideoholic.imifosx.batch.domain.BatchRequest
 * @see org.ideoholic.imifosx.batch.domain.BatchResponse
 */
@Component
public class ApplySavingsCommandStrategy implements CommandStrategy {

    private final SavingsAccountsApiResource savingsAccountsApiResource;

    @Autowired
    public ApplySavingsCommandStrategy(final SavingsAccountsApiResource savingsAccountsApiResource) {
        this.savingsAccountsApiResource = savingsAccountsApiResource;
    }

    @Override
    public BatchResponse execute(BatchRequest request, @SuppressWarnings("unused") UriInfo uriInfo) {

        final BatchResponse response = new BatchResponse();
        final String responseBody;

        response.setRequestId(request.getRequestId());
        response.setHeaders(request.getHeaders());

        // Try-catch blocks to map exceptions to appropriate status codes
        try {

            // Calls 'submitApplication' function from
            // 'SavingsAccountsApiResource' to Apply Savings to an existing
            // client
            responseBody = savingsAccountsApiResource.submitApplication(request.getBody());

            response.setStatusCode(200);
            // Sets the body of the response after savings is successfully
            // applied
            response.setBody(responseBody);

        } catch (RuntimeException e) {

            // Gets an object of type ErrorInfo, containing information about
            // raised exception
            ErrorInfo ex = ErrorHandler.handler(e);

            response.setStatusCode(ex.getStatusCode());
            response.setBody(ex.getMessage());
        }

        return response;
    }
}