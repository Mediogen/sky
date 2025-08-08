package com.sky.controller.admin;


import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Slf4j
@Api(tags = "管理端-数据统计接口")
public class ReportController {

    @Autowired
    private ReportService reportService;
    /**
     * 营业额统计
     * @param begin 统计的开始日期
     * @param end 统计的结束日期
     * @return Result<TurnoverReportVO> 营业额统计结果
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation(value = "营业额统计", notes = "根据开始和结束日期统计营业额")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("营业额统计, 开始日期: {}, 结束日期: {}", begin, end);
        return Result.success(reportService.turnoverStatistics(begin, end));
    }

    /**
     * 用户统计
     *
     */
    @ApiOperation(value = "用户统计", notes = "获取用户统计信息")
    @GetMapping("/userStatistics")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("用户统计, 开始日期: {}, 结束日期: {}", begin, end);
        UserReportVO userReportVO = reportService.userStatistics(begin,end);
        return Result.success(userReportVO);
    }

    /**
     * 订单统计
     *
     */
    @ApiOperation(value = "订单统计")
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("订单统计, 开始日期: {}, 结束日期: {}", begin, end);
        OrderReportVO orderReportVO = reportService.ordersStatistics(begin, end);
        return Result.success(orderReportVO);
    }

    /**
     * 查询销量排名top10
     */
    @ApiOperation(value = "查询销量排名top10")
    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("查询销量排名top10接口,开始日期: {}, 结束日期: {}",begin, end);
        return Result.success(reportService.top10(begin, end));
    }
}
