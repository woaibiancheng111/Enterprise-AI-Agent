package com.shixi.business.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.app.EnterpriseApp;
import com.shixi.business.entity.ServiceTicketEntity;
import com.shixi.business.mapper.ServiceTicketMapper;
import com.shixi.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServiceTicketService {

    private final ServiceTicketMapper serviceTicketMapper;
    private final ObjectMapper objectMapper;

    public ServiceTicketService(ServiceTicketMapper serviceTicketMapper, ObjectMapper objectMapper) {
        this.serviceTicketMapper = serviceTicketMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SubmitTicketResult submit(EnterpriseApp.EmployeeTicket ticket, CurrentUser user) {
        if (ticket == null) {
            throw new IllegalArgumentException("工单不能为空");
        }
        if (ticket.missingFields() != null && !ticket.missingFields().isEmpty()) {
            throw new IllegalArgumentException("请先补齐工单缺失信息");
        }
        if (!"READY_TO_SUBMIT".equals(ticket.status())) {
            throw new IllegalArgumentException("只有待提交状态的工单可以提交服务台");
        }
        if (serviceTicketMapper.selectById(ticket.ticketId()) != null) {
            throw new IllegalArgumentException("该工单已提交，请勿重复提交");
        }

        LocalDateTime now = LocalDateTime.now();
        ServiceTicketEntity entity = new ServiceTicketEntity(
                ticket.ticketId(),
                blankToNull(ticket.employeeId()),
                ticket.employeeName(),
                ticket.department(),
                ticket.requirementType(),
                ticket.title(),
                ticket.description(),
                ticket.priority(),
                "SUBMITTED",
                ticket.assigneeGroup(),
                ticket.sla(),
                toJson(ticket.requiredFields()),
                toJson(ticket.actionItems()),
                user.userId(),
                now,
                parseDateTime(ticket.createdAt(), now)
        );
        serviceTicketMapper.insert(entity);

        EnterpriseApp.EmployeeTicket submittedTicket = ticket.withStatus("SUBMITTED");
        return new SubmitTicketResult(
                true,
                "工单已提交至" + ticket.assigneeGroup(),
                submittedTicket,
                now.toString()
        );
    }

    private LocalDateTime parseDateTime(String value, LocalDateTime defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException ignored) {
            return defaultValue;
        }
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record SubmitTicketResult(
            boolean success,
            String message,
            EnterpriseApp.EmployeeTicket ticket,
            String submittedAt) {
    }
}
