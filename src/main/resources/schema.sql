CREATE TABLE IF NOT EXISTS employees (
  employee_id VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  department VARCHAR(64) NOT NULL,
  job_position VARCHAR(128) NOT NULL,
  email VARCHAR(128) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  join_date DATE NOT NULL,
  PRIMARY KEY (employee_id),
  KEY idx_employees_name (name),
  KEY idx_employees_department (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS employee_users (
  user_id VARCHAR(64) NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash CHAR(64) NOT NULL,
  employee_id VARCHAR(32) NULL,
  display_name VARCHAR(64) NOT NULL,
  role VARCHAR(32) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_employee_users_username (username),
  KEY idx_employee_users_employee_id (employee_id),
  CONSTRAINT fk_employee_users_employee
    FOREIGN KEY (employee_id) REFERENCES employees (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS leave_balances (
  employee_id VARCHAR(32) NOT NULL,
  annual_leave INT NOT NULL DEFAULT 0,
  sick_leave INT NOT NULL DEFAULT 0,
  marriage_leave INT NOT NULL DEFAULT 0,
  maternity_leave INT NOT NULL DEFAULT 0,
  PRIMARY KEY (employee_id),
  CONSTRAINT fk_leave_balances_employee
    FOREIGN KEY (employee_id) REFERENCES employees (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS leave_applications (
  application_id VARCHAR(32) NOT NULL,
  employee_id VARCHAR(32) NOT NULL,
  leave_type VARCHAR(32) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  days INT NOT NULL,
  reason VARCHAR(512) NOT NULL,
  status VARCHAR(32) NOT NULL,
  apply_date DATE NOT NULL,
  PRIMARY KEY (application_id),
  KEY idx_leave_applications_employee (employee_id),
  KEY idx_leave_applications_apply_date (apply_date),
  CONSTRAINT fk_leave_applications_employee
    FOREIGN KEY (employee_id) REFERENCES employees (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reimbursement_applications (
  application_id VARCHAR(32) NOT NULL,
  employee_id VARCHAR(32) NOT NULL,
  type VARCHAR(32) NOT NULL,
  amount DECIMAL(12, 2) NOT NULL,
  description VARCHAR(512) NOT NULL,
  invoice_number VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL,
  apply_date DATE NOT NULL,
  PRIMARY KEY (application_id),
  KEY idx_reimbursement_applications_employee (employee_id),
  KEY idx_reimbursement_applications_apply_date (apply_date),
  CONSTRAINT fk_reimbursement_applications_employee
    FOREIGN KEY (employee_id) REFERENCES employees (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tool_call_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tool_name VARCHAR(128) NOT NULL,
  domain VARCHAR(64) NOT NULL,
  actor_id VARCHAR(64) NULL,
  target_employee_id VARCHAR(32) NULL,
  input_json TEXT NULL,
  result_summary TEXT NULL,
  success TINYINT(1) NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_tool_call_logs_tool_name (tool_name),
  KEY idx_tool_call_logs_actor_id (actor_id),
  KEY idx_tool_call_logs_target_employee (target_employee_id),
  KEY idx_tool_call_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
