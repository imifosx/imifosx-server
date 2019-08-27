--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--


INSERT INTO `c_configuration` (`name`, `value`, `date_value`, `enabled`, `is_trap_door`, `description`) VALUES
	('Limit-Max-Deposit-With-Multiplier', 10, NULL, 0, 0, 'If enabled, this will add limits on how much can be deposited into a savings account, the value is the multiplier which is multiplied with the Avg-Deposit-In-Savings to determine the limiting value'),
	('Avg-Deposit-In-Savings', 0, NULL, 0, 0, 'The average deposit value in the savings account that is used in conjunction with Limit-Max-Deposit-With-Multiplier');
