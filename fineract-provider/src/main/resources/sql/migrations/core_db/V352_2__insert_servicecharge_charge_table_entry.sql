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
INSERT INTO `m_organisation_currency` (`id`, `code`, `decimal_places`, `name`, `display_symbol`, `internationalized_name_code`)
SELECT (SELECT MAX(id) + 1 from `m_organisation_currency`),'INR',2,'Indian Rupee','₹','currency.INR'
FROM `m_organisation_currency`
WHERE NOT('INR' IN (SELECT code FROM `m_organisation_currency`));


INSERT INTO `m_charge`(id, name, currency_code, charge_applies_to_enum, charge_time_enum, charge_calculation_enum, charge_payment_mode_enum, amount, fee_on_day, fee_interval, fee_on_month, is_penalty, is_active, is_deleted, min_cap, max_cap, fee_frequency, income_or_liability_account_id)
VALUES (1,'Service Charge', 'INR', 1, 8, 1, 0, 1.000000, NULL, NULL, NULL, 0, 1, 0, NULL, NULL, NULL, NULL);

INSERT INTO `m_portfolio_command_source` (`id`, `action_name`, `entity_name`, `office_id`, `group_id`, `client_id`, `loan_id`, `savings_account_id`, `api_get_url`, `resource_id`, `subresource_id`, `command_as_json`, `maker_id`, `made_on_date`, `checker_id`, `checked_on_date`, `processing_result_enum`, `product_id`, `transaction_id`) 
SELECT next_id,'CREATE', 'CHARGE', NULL, NULL, NULL, NULL, NULL, '/charges/template', 1, NULL, '{"chargeAppliesTo":1,"name":"Service Charge","currencyCode":"INR","chargeTimeType":8,"chargeCalculationType":1,"chargePaymentMode":0,"amount":"1.000000","active":true,"locale":"en","monthDayFormat":"dd MMM"}', 1, '2017-03-01 10:10:10', NULL, NULL, 1, NULL, NULL
FROM (SELECT MAX(id) + 1 as next_id FROM `m_portfolio_command_source`) tbl;
