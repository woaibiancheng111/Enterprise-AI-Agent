package com.shixi.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserTest {

    @Test
    void employeeCanOnlyAccessOwnEmployeeData() {
        CurrentUser user = new CurrentUser("U001", "zhangsan", "E001", "张三", "EMPLOYEE");

        assertTrue(user.canAccessEmployee("E001"));
        assertTrue(user.canAccessEmployee("e001"));
        assertFalse(user.canAccessEmployee("E002"));
    }

    @Test
    void hrAndAdminCanAccessAllEmployeeData() {
        CurrentUser hr = new CurrentUser("U003", "hr_admin", "E002", "HR 管理员", "HR");
        CurrentUser admin = new CurrentUser("U004", "admin", null, "系统管理员", "ADMIN");

        assertTrue(hr.canAccessEmployee("E001"));
        assertTrue(admin.canAccessEmployee("E005"));
    }
}
