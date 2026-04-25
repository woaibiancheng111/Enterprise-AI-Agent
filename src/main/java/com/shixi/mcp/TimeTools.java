package com.shixi.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class TimeTools {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool(description = "获取当前日期和时间，返回格式为 yyyy-MM-dd HH:mm:ss")
    public String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    @Tool(description = "获取当前日期，返回格式为 yyyy-MM-dd")
    public String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    @Tool(description = "获取指定日期是星期几")
    public String getDayOfWeek(
            @ToolParam(description = "日期，格式为 yyyy-MM-dd", required = true) String date) {
        LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
        DayOfWeek dayOfWeek = localDate.getDayOfWeek();
        return switch (dayOfWeek) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }

    @Tool(description = "计算两个日期之间的天数（包括开始日期，不包括结束日期）")
    public long calculateDaysBetween(
            @ToolParam(description = "开始日期，格式为 yyyy-MM-dd", required = true) String startDate,
            @ToolParam(description = "结束日期，格式为 yyyy-MM-dd", required = true) String endDate) {
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
        return ChronoUnit.DAYS.between(start, end);
    }

    @Tool(description = "计算两个日期之间的工作日天数（排除周六和周日）")
    public int calculateWorkDaysBetween(
            @ToolParam(description = "开始日期，格式为 yyyy-MM-dd", required = true) String startDate,
            @ToolParam(description = "结束日期，格式为 yyyy-MM-dd", required = true) String endDate) {
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
        
        int workDays = 0;
        LocalDate current = start;
        
        while (!current.isAfter(end)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workDays++;
            }
            current = current.plusDays(1);
        }
        
        return workDays;
    }

    @Tool(description = "检查指定日期是否为工作日（周一至周五）")
    public boolean isWorkDay(
            @ToolParam(description = "日期，格式为 yyyy-MM-dd", required = true) String date) {
        LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
        DayOfWeek dayOfWeek = localDate.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    @Tool(description = "检查指定日期是否为周末（周六或周日）")
    public boolean isWeekend(
            @ToolParam(description = "日期，格式为 yyyy-MM-dd", required = true) String date) {
        return !isWorkDay(date);
    }

    @Tool(description = "获取指定日期之后N天的日期")
    public String addDays(
            @ToolParam(description = "开始日期，格式为 yyyy-MM-dd", required = true) String startDate,
            @ToolParam(description = "要添加的天数", required = true) int days) {
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        return start.plusDays(days).format(DATE_FORMATTER);
    }

    @Tool(description = "获取指定日期之前N天的日期")
    public String minusDays(
            @ToolParam(description = "开始日期，格式为 yyyy-MM-dd", required = true) String startDate,
            @ToolParam(description = "要减去的天数", required = true) int days) {
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        return start.minusDays(days).format(DATE_FORMATTER);
    }

    @Tool(description = "获取指定月份的所有工作日列表")
    public List<String> getWorkDaysOfMonth(
            @ToolParam(description = "年份", required = true) int year,
            @ToolParam(description = "月份（1-12）", required = true) int month) {
        List<String> workDays = new ArrayList<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        
        LocalDate current = firstDay;
        while (!current.isAfter(lastDay)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workDays.add(current.format(DATE_FORMATTER));
            }
            current = current.plusDays(1);
        }
        
        return workDays;
    }

    @Tool(description = "格式化日期，将日期字符串从一种格式转换为另一种格式")
    public String formatDate(
            @ToolParam(description = "日期字符串", required = true) String date,
            @ToolParam(description = "输入格式，如 yyyy/MM/dd、MM-dd-yyyy 等", required = true) String inputFormat,
            @ToolParam(description = "输出格式，如 yyyy-MM-dd、yyyy年MM月dd日 等", required = true) String outputFormat) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputFormat);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);
        LocalDate localDate = LocalDate.parse(date, inputFormatter);
        return localDate.format(outputFormatter);
    }
}
