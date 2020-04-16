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

-- create the service charge journal entry recording table
CREATE TABLE IF NOT EXISTS acc_gl_servicecharge_journal_entry (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `journal_entry_id` bigint(20) NOT NULL,
  `office_id` bigint(20) NOT NULL,
  `createdby_id` bigint(20) NOT NULL,
  `entry_date` date NOT NULL,
  `created_date` datetime NOT NULL,
  `lastmodifiedby_id` bigint(20) NOT NULL,
  `lastmodified_date` datetime NOT NULL,
  `mobilization_percentage` decimal(5,2) NOT NULL,
  `servicing_percentage` decimal(5,2) NOT NULL DEFAULT '0.00',
  `investment_percentage` decimal(5,2) NOT NULL DEFAULT '0.00',
  `overheads_percentage` decimal(5,2) NOT NULL DEFAULT '0.00',
  `mobilization_amount` decimal(19,6) NOT NULL DEFAULT '0.000000',
  `servicing_amount` decimal(19,6) NOT NULL DEFAULT '0.000000',
  `investment_amount` decimal(19,6) NOT NULL DEFAULT '0.000000',
  `overheads_amount` decimal(19,6) NOT NULL DEFAULT '0.000000',
  `is_reversed` tinyint(1) NOT NULL DEFAULT '0',
  `gl_reversal_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY `FK_acc_gl_servicecharge_journal_entry_m_office` (`office_id`),
  KEY `FK_acc_gl_servicecharge_journal_entry_acc_gl_journal_entry` (`journal_entry_id`),
  KEY `FK_acc_gl_servicecharge_journal_entry_acc_gl_journal_entry_rev` (`gl_reversal_id`),
  CONSTRAINT `FK_acc_gl_servicecharge_journal_entry_acc_gl_journal_entry_rev` FOREIGN KEY (`gl_reversal_id`) REFERENCES `acc_gl_journal_entry` (`id`),
  CONSTRAINT `FK_acc_gl_servicecharge_journal_entry_acc_gl_journal_entry` FOREIGN KEY (`journal_entry_id`) REFERENCES `acc_gl_journal_entry` (`id`),
  CONSTRAINT `FK_acc_gl_servicecharge_journal_entry_m_appuser_created` FOREIGN KEY (`createdby_id`) REFERENCES `m_appuser` (`id`),
  CONSTRAINT `FK_acc_gl_servicecharge_journal_entry_m_appuser_modified` FOREIGN KEY (`lastmodifiedby_id`) REFERENCES `m_appuser` (`id`),
  CONSTRAINT `FK_acc_gl_servicecharge_journal_entry_m_office` FOREIGN KEY (`office_id`) REFERENCES `m_office` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

