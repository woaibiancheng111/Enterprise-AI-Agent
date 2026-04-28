INSERT IGNORE INTO employees (employee_id, name, department, job_position, email, phone, join_date) VALUES
('E001', '张三', '技术部', '高级工程师', 'zhangsan@company.com', '13800138001', '2020-03-15'),
('E002', '李四', '人事部', 'HR专员', 'lisi@company.com', '13800138002', '2021-06-20'),
('E003', '王五', '财务部', '会计', 'wangwu@company.com', '13800138003', '2019-09-10'),
('E004', '赵六', '市场部', '市场经理', 'zhaoliu@company.com', '13800138004', '2018-02-28'),
('E005', '钱七', '技术部', '初级工程师', 'qianqi@company.com', '13800138005', '2023-07-01');

INSERT IGNORE INTO leave_balances (employee_id, annual_leave, sick_leave, marriage_leave, maternity_leave) VALUES
('E001', 15, 10, 5, 3),
('E002', 12, 8, 3, 2),
('E003', 18, 12, 6, 4),
('E004', 20, 15, 8, 5),
('E005', 5, 3, 1, 0);

INSERT IGNORE INTO employee_users (user_id, username, password_hash, employee_id, display_name, role, enabled, created_at) VALUES
('U001', 'zhangsan', SHA2('123456', 256), 'E001', '张三', 'EMPLOYEE', 1, NOW()),
('U002', 'lisi', SHA2('123456', 256), 'E002', '李四', 'EMPLOYEE', 1, NOW()),
('U003', 'hr_admin', SHA2('123456', 256), 'E002', 'HR 管理员', 'HR', 1, NOW()),
('U004', 'admin', SHA2('123456', 256), NULL, '系统管理员', 'ADMIN', 1, NOW());
