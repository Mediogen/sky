package com.sky.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CountOrdersVO {
    private LocalDate date;

    private Long OrdersCount;
}
