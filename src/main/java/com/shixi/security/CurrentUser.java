package com.shixi.security;

public record CurrentUser(
        String userId,
        String username,
        String employeeId,
        String displayName,
        String role
) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isHr() {
        return "HR".equals(role);
    }

    public boolean canAccessEmployee(String targetEmployeeId) {
        if (isAdmin() || isHr()) {
            return true;
        }
        return employeeId != null && employeeId.equalsIgnoreCase(targetEmployeeId);
    }
}
