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
package org.ideoholic.imifosx.spm.service;

import org.ideoholic.imifosx.infrastructure.security.service.PlatformSecurityContext;
import org.ideoholic.imifosx.portfolio.client.domain.Client;
import org.ideoholic.imifosx.spm.domain.Scorecard;
import org.ideoholic.imifosx.spm.domain.Survey;
import org.ideoholic.imifosx.spm.repository.ScorecardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScorecardService {

    private final PlatformSecurityContext securityContext;
    private final ScorecardRepository scorecardRepository;

    @Autowired
    public ScorecardService(final PlatformSecurityContext securityContext,
                            final ScorecardRepository scorecardRepository) {
        super();
        this.securityContext = securityContext;
        this.scorecardRepository = scorecardRepository;
    }

    public List<Scorecard> createScorecard(final List<Scorecard> scorecards) {
        this.securityContext.authenticatedUser();

        return this.scorecardRepository.save(scorecards);
    }

    public List<Scorecard> findBySurvey(final Survey survey) {
        this.securityContext.authenticatedUser();

        return this.scorecardRepository.findBySurvey(survey);
    }

    public List<Scorecard> findBySurveyAndClient(final Survey survey, final Client client) {
        this.securityContext.authenticatedUser();

        return this.scorecardRepository.findBySurveyAndClient(survey, client);
    }
}
