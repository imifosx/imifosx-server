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
package org.ideoholic.fineract.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseUtils {

    private final static Logger logger = LoggerFactory.getLogger(DatabaseUtils.class);

    public static Long getNextIdForClient(JdbcTemplate jdbcTemplate) {
        return getCountStarOfTable("m_client", jdbcTemplate);
    }

    public static Long getNextIdForSavings(JdbcTemplate jdbcTemplate) {
        return getCountStarOfTable("m_savings_account", jdbcTemplate);
    }

    public static Long getNextIdForLoan(JdbcTemplate jdbcTemplate) {
        return getCountStarOfTable("m_loan", jdbcTemplate);
    }

    public static Long getNextIdForShare(JdbcTemplate jdbcTemplate) {
        return getCountStarOfTable("m_share_account", jdbcTemplate);
    }

    public static Long getNextIdForGroup(JdbcTemplate jdbcTemplate) {
        return getCountStarOfTable("m_group", jdbcTemplate);
    }

    private static Long getCountStarOfTable(String string, JdbcTemplate jdbcTemplate) {
        Number count = 0;
        String sql = "select count(*) from " + string;
        logger.debug("DatabaseUtils.getCountStarOfTable::Running query:" + sql);
        try {
            count = jdbcTemplate.queryForObject(sql, Number.class);
            logger.debug("DatabaseUtils.getCountStarOfTable:: Count value after running query:" + count);
        } catch (EmptyResultDataAccessException e) {}
        logger.debug("DatabaseUtils.getCountStarOfTable:: Count-Start value as result of running query:" + count);
        return count.longValue();
    }
}
