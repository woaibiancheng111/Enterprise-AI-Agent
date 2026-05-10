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
        ensureServiceTicketsTable();
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

    private void ensureServiceTicketsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS service_tickets (
                  ticket_id VARCHAR(64) NOT NULL,
                  employee_id VARCHAR(32) NULL,
                  employee_name VARCHAR(64) NOT NULL,
                  department VARCHAR(64) NOT NULL,
                  requirement_type VARCHAR(64) NOT NULL,
                  title VARCHAR(128) NOT NULL,
                  description TEXT NOT NULL,
                  priority VARCHAR(16) NOT NULL,
                  status VARCHAR(32) NOT NULL,
                  assignee_group VARCHAR(64) NOT NULL,
                  sla VARCHAR(64) NOT NULL,
                  required_fields TEXT NULL,
                  action_items TEXT NULL,
                  submitted_by VARCHAR(64) NOT NULL,
                  submitted_at DATETIME NOT NULL,
                  created_at DATETIME NOT NULL,
                  PRIMARY KEY (ticket_id),
                  KEY idx_service_tickets_employee (employee_id),
                  KEY idx_service_tickets_status (status),
                  KEY idx_service_tickets_assignee_group (assignee_group),
                  KEY idx_service_tickets_submitted_at (submitted_at),
                  CONSTRAINT fk_service_tickets_employee
                    FOREIGN KEY (employee_id) REFERENCES employees (employee_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }
}
