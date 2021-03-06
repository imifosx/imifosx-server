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

-- drop share certificate number generating table in base-schema
DROP TABLE IF EXISTS `m_share_account_certificate`;

-- create the share certificate number generating table
CREATE TABLE m_share_account_certificate (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `share_account_id` bigint(20) NOT NULL,
  `share_transaction_id` bigint(20) NOT NULL,
  `cert_num` bigint(20) NOT NULL,
  `from_num` bigint(20) NOT NULL,
  `to_num` bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY cert_num (cert_num)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;