package com.sky.vo;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SumAmount implements Serializable {

    private LocalDate date;

    private BigDecimal turnover;

}
