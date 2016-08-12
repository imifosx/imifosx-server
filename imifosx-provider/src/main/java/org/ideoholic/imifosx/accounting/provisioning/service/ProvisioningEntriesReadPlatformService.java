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
package org.ideoholic.imifosx.accounting.provisioning.service;

import java.util.Collection;
import java.util.Date;

import org.ideoholic.imifosx.accounting.provisioning.data.LoanProductProvisioningEntryData;
import org.ideoholic.imifosx.accounting.provisioning.data.ProvisioningEntryData;
import org.ideoholic.imifosx.infrastructure.core.service.Page;
import org.ideoholic.imifosx.infrastructure.core.service.SearchParameters;


public interface ProvisioningEntriesReadPlatformService {

    public Collection<LoanProductProvisioningEntryData> retrieveLoanProductsProvisioningData(Date date) ;
    
    public ProvisioningEntryData retrieveProvisioningEntryData(Long entryId) ;
    
    public Page<ProvisioningEntryData> retrieveAllProvisioningEntries(Integer offset, Integer limit) ;
    
    public ProvisioningEntryData retrieveProvisioningEntryData(String date) ;
    
    public ProvisioningEntryData retrieveProvisioningEntryDataByCriteriaId(Long criteriaId) ;
    
    public ProvisioningEntryData retrieveExistingProvisioningIdDateWithJournals() ;
    
    public Page<LoanProductProvisioningEntryData> retrieveProvisioningEntries(SearchParameters searchParams) ;
}
