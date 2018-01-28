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

-- drop service charge table in base-schema
DROP TABLE IF EXISTS `m_loan_service_charge`;

-- create the service charge table
CREATE TABLE `m_loan_service_charge` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sc_quarter` tinyint(1) NOT NULL,
  `sc_year` smallint(4) NOT NULL,
  `sc_header` smallint(5) NOT NULL,
  `sc_amount` decimal(19,6) NOT NULL DEFAULT '0.000000',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
