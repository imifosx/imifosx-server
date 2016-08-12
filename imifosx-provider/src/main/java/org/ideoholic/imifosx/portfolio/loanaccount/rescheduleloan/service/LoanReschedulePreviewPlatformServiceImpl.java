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
package org.ideoholic.imifosx.portfolio.loanaccount.rescheduleloan.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import org.ideoholic.imifosx.infrastructure.configuration.domain.ConfigurationDomainService;
import org.ideoholic.imifosx.organisation.holiday.domain.Holiday;
import org.ideoholic.imifosx.organisation.holiday.domain.HolidayRepository;
import org.ideoholic.imifosx.organisation.holiday.domain.HolidayStatusType;
import org.ideoholic.imifosx.organisation.monetary.domain.ApplicationCurrency;
import org.ideoholic.imifosx.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.ideoholic.imifosx.organisation.monetary.domain.MonetaryCurrency;
import org.ideoholic.imifosx.organisation.monetary.domain.MoneyHelper;
import org.ideoholic.imifosx.organisation.workingdays.domain.WorkingDays;
import org.ideoholic.imifosx.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.ideoholic.imifosx.portfolio.calendar.domain.Calendar;
import org.ideoholic.imifosx.portfolio.calendar.domain.CalendarEntityType;
import org.ideoholic.imifosx.portfolio.calendar.domain.CalendarInstance;
import org.ideoholic.imifosx.portfolio.calendar.domain.CalendarInstanceRepository;
import org.ideoholic.imifosx.portfolio.floatingrates.data.FloatingRateDTO;
import org.ideoholic.imifosx.portfolio.floatingrates.data.FloatingRatePeriodData;
import org.ideoholic.imifosx.portfolio.floatingrates.exception.FloatingRateNotFoundException;
import org.ideoholic.imifosx.portfolio.floatingrates.service.FloatingRatesReadPlatformService;
import org.ideoholic.imifosx.portfolio.loanaccount.data.HolidayDetailDTO;
import org.ideoholic.imifosx.portfolio.loanaccount.domain.Loan;
import org.ideoholic.imifosx.portfolio.loanaccount.loanschedule.domain.LoanRepaymentScheduleHistory;
import org.ideoholic.imifosx.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryWritePlatformService;
import org.ideoholic.imifosx.portfolio.loanaccount.rescheduleloan.domain.DefaultLoanReschedulerFactory;
import org.ideoholic.imifosx.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleModel;
import org.ideoholic.imifosx.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.ideoholic.imifosx.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequestRepository;
import org.ideoholic.imifosx.portfolio.loanaccount.rescheduleloan.exception.LoanRescheduleRequestNotFoundException;
import org.ideoholic.imifosx.portfolio.loanproduct.domain.InterestMethod;
import org.ideoholic.imifosx.portfolio.loanproduct.domain.LoanProductMinimumRepaymentScheduleRelatedDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoanReschedulePreviewPlatformServiceImpl implements LoanReschedulePreviewPlatformService {

    private final LoanRescheduleRequestRepository loanRescheduleRequestRepository;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final HolidayRepository holidayRepository;
    private final WorkingDaysRepositoryWrapper workingDaysRepository;
    private final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final FloatingRatesReadPlatformService floatingRatesReadPlatformService;

    @Autowired
    public LoanReschedulePreviewPlatformServiceImpl(final LoanRescheduleRequestRepository loanRescheduleRequestRepository,
            final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository,
            final ConfigurationDomainService configurationDomainService, final HolidayRepository holidayRepository,
            final WorkingDaysRepositoryWrapper workingDaysRepository,
            final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService,
            final CalendarInstanceRepository calendarInstanceRepository,
            final FloatingRatesReadPlatformService floatingRatesReadPlatformService) {
        this.loanRescheduleRequestRepository = loanRescheduleRequestRepository;
        this.applicationCurrencyRepository = applicationCurrencyRepository;
        this.configurationDomainService = configurationDomainService;
        this.holidayRepository = holidayRepository;
        this.workingDaysRepository = workingDaysRepository;
        this.loanScheduleHistoryWritePlatformService = loanScheduleHistoryWritePlatformService;
        this.calendarInstanceRepository = calendarInstanceRepository;
        this.floatingRatesReadPlatformService = floatingRatesReadPlatformService;
    }

    @Override
    public LoanRescheduleModel previewLoanReschedule(Long requestId) {
        final LoanRescheduleRequest loanRescheduleRequest = this.loanRescheduleRequestRepository.findOne(requestId);

        if (loanRescheduleRequest == null) { throw new LoanRescheduleRequestNotFoundException(requestId); }

        Loan loan = loanRescheduleRequest.getLoan();

        final boolean isHolidayEnabled = this.configurationDomainService.isRescheduleRepaymentsOnHolidaysEnabled();
        final List<Holiday> holidays = this.holidayRepository.findByOfficeIdAndGreaterThanDate(loan.getOfficeId(), loan
                .getDisbursementDate().toDate(), HolidayStatusType.ACTIVE.getValue());
        final WorkingDays workingDays = this.workingDaysRepository.findOne();
        final LoanProductMinimumRepaymentScheduleRelatedDetail loanProductRelatedDetail = loan.getLoanRepaymentScheduleDetail();
        final MonetaryCurrency currency = loanProductRelatedDetail.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);

        final InterestMethod interestMethod = loan.getLoanRepaymentScheduleDetail().getInterestMethod();
        final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
        final MathContext mathContext = new MathContext(8, roundingMode);
        List<LoanRepaymentScheduleHistory> oldPeriods = this.loanScheduleHistoryWritePlatformService.createLoanScheduleArchive(
                loan.getRepaymentScheduleInstallments(), loan, loanRescheduleRequest);
        HolidayDetailDTO holidayDetailDTO = new HolidayDetailDTO(isHolidayEnabled, holidays, workingDays);
        CalendarInstance restCalendarInstance = null;
        CalendarInstance compoundingCalendarInstance = null;
        if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
            restCalendarInstance = calendarInstanceRepository.findCalendarInstaneByEntityId(loan.loanInterestRecalculationDetailId(),
                    CalendarEntityType.LOAN_RECALCULATION_REST_DETAIL.getValue());
            compoundingCalendarInstance = calendarInstanceRepository.findCalendarInstaneByEntityId(
                    loan.loanInterestRecalculationDetailId(), CalendarEntityType.LOAN_RECALCULATION_COMPOUNDING_DETAIL.getValue());
        }
        final CalendarInstance loanCalendarInstance = calendarInstanceRepository.findCalendarInstaneByEntityId(loan.getId(),
                CalendarEntityType.LOANS.getValue());
        Calendar loanCalendar = null;
        if (loanCalendarInstance != null) {
            loanCalendar = loanCalendarInstance.getCalendar();
        }
        final FloatingRateDTO floatingRateDTO = constructFloatingRateDTO(loan);
        LoanRescheduleModel loanRescheduleModel = new DefaultLoanReschedulerFactory().reschedule(mathContext, interestMethod,
                loanRescheduleRequest, applicationCurrency, holidayDetailDTO, restCalendarInstance, compoundingCalendarInstance,
                loanCalendar, floatingRateDTO);
        LoanRescheduleModel loanRescheduleModelWithOldPeriods = LoanRescheduleModel.createWithSchedulehistory(loanRescheduleModel,
                oldPeriods);
        return loanRescheduleModelWithOldPeriods;
    }

    private FloatingRateDTO constructFloatingRateDTO(final Loan loan) {
        FloatingRateDTO floatingRateDTO = null;
        if (loan.loanProduct().isLinkedToFloatingInterestRate()) {
            boolean isFloatingInterestRate = loan.getIsFloatingInterestRate();
            BigDecimal interestRateDiff = loan.getInterestRateDifferential();
            List<FloatingRatePeriodData> baseLendingRatePeriods = null;
            try{
            	baseLendingRatePeriods = this.floatingRatesReadPlatformService.retrieveBaseLendingRate()
            								.getRatePeriods();
            }catch(final FloatingRateNotFoundException ex){
            	// Do not do anything
            }
            floatingRateDTO = new FloatingRateDTO(isFloatingInterestRate, loan.getDisbursementDate(), interestRateDiff,
                    baseLendingRatePeriods);
        }
        return floatingRateDTO;
    }

}
