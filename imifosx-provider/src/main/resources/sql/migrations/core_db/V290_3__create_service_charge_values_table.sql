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
