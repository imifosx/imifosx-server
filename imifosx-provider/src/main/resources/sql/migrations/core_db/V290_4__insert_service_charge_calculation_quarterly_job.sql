-- Insert job into DB that will run once end of every quarter to calculate service charge

INSERT INTO `job` (`id`, `name`, `display_name`, `cron_expression`, `create_time`, `task_priority`, `group_name`, `previous_run_start_time`, `next_run_time`, `job_key`, `initializing_errorlog`, `is_active`, `currently_running`, `updates_allowed`, `scheduler_group`, `is_misfired`)
SELECT next_id, 'Generate Service Charge', 'Generate Service Charge', '0 55 23 L 3/3 ? *', '2017-01-01 10:10:10', 5, null, null, null, 'Generate Service ChargeJobDetail1 _ DEFAULT', null, 1, 0, 1, 0, 0
FROM (SELECT MAX(id) + 1 as next_id FROM `job`) jobtbl;

