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
package org.apache.fineract.portfolio.servicecharge.constants;

import java.math.BigDecimal;

public interface ServiceChargeApiConstants {

    String serviceChargeRESTName = "servicecharge";
    String SLASH = "/";
    String HYPHEN = "-";
    String SERVICE_CHARGE_REST_CALL = SLASH + serviceChargeRESTName;
    int ROUNDOFF_DIGITS_LIMIT = 15;
    BigDecimal ONE_THOUSAND_TWO_HUNDRED = new BigDecimal("1200");
    BigDecimal THREE_SIXTY_FIVE_HUNDRED = new BigDecimal("36500");
    BigDecimal HUNDRED = new BigDecimal("100");
    BigDecimal _365 = new BigDecimal("365");
    String SERVICE_CHARGE_NAME = "Service Charge";
    String dividendDistribution = "dividendDistribution";
    // TODO: think of a way to statically load this ID value instead assuming
    Long ASSUMED_SERVICE_CHARGE_ID = 1L;

    String SC_CALCULATION_METHOD = "servicecharge_calculation_method_";
    String MONTHLY = "MONTHLY";
    String QUARTERLY = "QUARTERLY";
    String YEARLY = "YEARLY";

    // Month reference strings for SC applicable quarter calculation
    String JANUARY = "JAN";
    String FEBRUARY = "FEB";
    String MARCH = "MAR";
    String APRIL = "APR";
    String MAY = "MAY";
    String JUNE = "JUN";
    String JULY = "JUL";
    String AUGUST = "AUG";
    String SEPTEMBER = "SEP";
    String OCTOBER = "OCT";
    String NOVEMBER = "NOV";
    String DECEMBER = "DEC";
}
