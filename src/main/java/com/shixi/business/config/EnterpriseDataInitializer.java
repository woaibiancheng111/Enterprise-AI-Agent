package com.shixi.business.config;

import com.shixi.business.entity.EmployeeEntity;
import com.shixi.business.entity.LeaveBalanceEntity;
import com.shixi.business.repository.EmployeeRepository;
import com.shixi.business.repository.LeaveBalanceRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class EnterpriseDataInitializer {

    @Bean
    ApplicationRunner seedEnterpriseData(
            EmployeeRepository employeeRepository,
            LeaveBalanceRepository leaveBalanceRepository) {
        return args -> {
            if (employeeRepository.count() > 0) {
                return;
            }

            employeeRepository.saveAll(List.of(
                    new EmployeeEntity("E001", "张三", "技术部", "高级工程师", "zhangsan@company.com", "13800138001", LocalDate.of(2020, 3, 15)),
                    new EmployeeEntity("E002", "李四", "人事部", "HR专员", "lisi@company.com", "13800138002", LocalDate.of(2021, 6, 20)),
                    new EmployeeEntity("E003", "王五", "财务部", "会计", "wangwu@company.com", "13800138003", LocalDate.of(2019, 9, 10)),
                    new EmployeeEntity("E004", "赵六", "市场部", "市场经理", "zhaoliu@company.com", "13800138004", LocalDate.of(2018, 2, 28)),
                    new EmployeeEntity("E005", "钱七", "技术部", "初级工程师", "qianqi@company.com", "13800138005", LocalDate.of(2023, 7, 1))
            ));

            leaveBalanceRepository.saveAll(List.of(
                    new LeaveBalanceEntity("E001", 15, 10, 5, 3),
                    new LeaveBalanceEntity("E002", 12, 8, 3, 2),
                    new LeaveBalanceEntity("E003", 18, 12, 6, 4),
                    new LeaveBalanceEntity("E004", 20, 15, 8, 5),
                    new LeaveBalanceEntity("E005", 5, 3, 1, 0)
            ));
        };
    }
}
