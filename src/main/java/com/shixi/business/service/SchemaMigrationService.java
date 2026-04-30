package com.shixi.business.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaMigrationService implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn("leave_applications", "reviewer_id", "VARCHAR(64) NULL");
        ensureColumn("leave_applications", "review_comment", "VARCHAR(512) NULL");
        ensureColumn("leave_applications", "reviewed_at", "DATETIME NULL");
        ensureColumn("reimbursement_applications", "reviewer_id", "VARCHAR(64) NULL");
        ensureColumn("reimbursement_applications", "review_comment", "VARCHAR(512) NULL");
        ensureColumn("reimbursement_applications", "reviewed_at", "DATETIME NULL");
    }

    private void ensureColumn(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }
}
