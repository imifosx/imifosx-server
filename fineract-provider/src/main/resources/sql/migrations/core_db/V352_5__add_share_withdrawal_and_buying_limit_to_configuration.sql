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
	('Share-Withdrawal-Limit', 10, NULL, 1, 0, 'Members cannot withdraw more than this value (in percentage) of paid-up share capital in one financial year'),
	('Share-Buying-Limit', 20, NULL, 1, 0, 'One member cannot subscribe to more than this value(in percentage) of paid-up share capital');