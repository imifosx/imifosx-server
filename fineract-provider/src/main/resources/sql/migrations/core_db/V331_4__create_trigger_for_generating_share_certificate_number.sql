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

-- drop share certificate number generating trigger in base-schema
DELIMITER $$
DROP TRIGGER IF EXISTS share_number_generator $$


-- create the share certificate number generating trigger
CREATE TRIGGER share_number_generator
AFTER INSERT
ON m_share_account_transactions
FOR EACH ROW
BEGIN
	DECLARE certificate_num bigint(20);
	DECLARE f_num bigint(20) DEFAULT 0;
	DECLARE t_num bigint(20);
	#DECLARE msg varchar(255);
	
	IF NEW.type_enum = 500 THEN
		SET certificate_num = NEW.id;
		#SET msg = concat('certificate_num: ', certificate_num);
		#INSERT INTO messages(`message`) VALUES(msg);
		SELECT MAX(m_share_account_certificate.to_num) INTO f_num FROM m_share_account_certificate;
		#SET msg = concat('f_num: ', f_num);
		#INSERT INTO messages(`message`) VALUES(msg);
		IF (ISNULL(f_num)) THEN
			SET f_num = 1;
			#SET msg = concat('f_num has been set to 1: ', f_num);
			#INSERT INTO messages(`message`) VALUES(msg);
		ELSE
			SET f_num = f_num + 1;
			#SET msg = concat('f_num has been incremented by 1: ', f_num);
			#INSERT INTO messages(`message`) VALUES(msg);
		END IF;
		SET t_num = (f_num + NEW.total_shares) - 1;
		#SET msg = concat('t_num: ', t_num);
		#INSERT INTO messages(`message`) VALUES(msg);
		INSERT INTO m_share_account_certificate(`share_account_id`, `share_transaction_id`, `cert_num`, `from_num`, `to_num`)
		VALUES(NEW.account_id, NEW.id, certificate_num, f_num, t_num);
	END IF;
END$$
DELIMITER ;
