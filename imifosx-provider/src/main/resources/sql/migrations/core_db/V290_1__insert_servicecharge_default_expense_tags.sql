INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`) VALUES
('11', 'Mobilization', 'Mobilization Expense Accounting Tag', '1', NULL, '1'),
('11', 'Servicing', 'Servicing Expense Accounting Tag', '2', NULL, '1'),
('11', 'Investment', 'Investment Expense Accounting Tag', '3', NULL, '1'),
('11', 'Overheads', 'Overheads Expense Accounting Tag', '4', NULL, '1'),
('11', 'Provisions', 'Provisions Expense Accounting Tag', '5', NULL, '1'),
('11', 'BF-Servicing', 'Loan Servicing Cost on Account BF Expense Accounting Tag', '6', NULL, '1');


INSERT INTO `m_portfolio_command_source` (`id`, `action_name`, `entity_name`, `office_id`, `group_id`, `client_id`, `loan_id`, `savings_account_id`, `api_get_url`, `resource_id`, `subresource_id`, `command_as_json`, `maker_id`, `made_on_date`, `checker_id`, `checked_on_date`, `processing_result_enum`, `product_id`, `transaction_id`) VALUES
 (1,'CREATE', 'CODEVALUE', NULL, NULL, NULL, NULL, NULL, '/codes/11/codevalues/template', 11, NULL, '{"isActive":true,"name":"Mobilization","description":"Mobilization Expense Accounting Tag","position":"1"}', 1, '2017-03-01 10:10:10', NULL, NULL, 1, NULL, NULL),
 (2,'CREATE', 'CODEVALUE', NULL, NULL, NULL, NULL, NULL, '/codes/11/codevalues/template', 11, NULL, '{"isActive":true,"name":"Servicing","description":"Servicing Expense Accounting Tag","position":"2"}', 1, '2017-03-01 10:10:10', NULL, NULL, 1, NULL, NULL),
 (3,'CREATE', 'CODEVALUE', NULL, NULL, NULL, NULL, NULL, '/codes/11/codevalues/template', 11, NULL, '{"isActive":true,"name":"Investment","description":"Investment Expense Accounting Tag","position":"3"}', 1, '2017-03-01 10:10:10', NULL, NULL, 1, NULL, NULL),
 (4,'CREATE', 'CODEVALUE', NULL, NULL, NULL, NULL, NULL, '/codes/11/codevalues/template', 11, NULL, '{"isActive":true,"name":"Overheads","description":"Overheads Expense Accounting Tag","position":"4"}', 1, '2017-03-01 10:10:10', NULL, NULL, 1, NULL, NULL),
 (5,'CREATE', 'CODEVALUE', NULL, NULL, NULL, NULL, NULL, '/codes/11/codevalues/template', 11, NULL, '{"isActive":true,"name":"Provisions","description":"Provisions Expense Accounting Tag","position":"5"}', 1, '2017-03-01 10:10:10', NULL, NULL, 1, NULL, NULL),
 (6,'CREATE', 'CODEVALUE', NULL, NULL, NULL, NULL, NULL, '/codes/11/codevalues/template', 11, NULL, '{"isActive":true,"name":"BF-Servicing","description":"Loan Servicing Cost on Account BF Expense Accounting Tag","position":"6"}', 1, '2017-03-01 10:10:10', NULL, NULL, 1, NULL, NULL);