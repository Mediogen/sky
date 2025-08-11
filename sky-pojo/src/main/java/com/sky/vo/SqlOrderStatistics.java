package com.sky.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class SqlOrderStatistics implements Serializable {
    // 订单状态
    private Integer status;

    // 该状态对应的订单数量
    private Integer count; // 或者使用 Long 类型以防数量过大

}
