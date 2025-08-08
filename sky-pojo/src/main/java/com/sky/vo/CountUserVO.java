package com.sky.vo;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class CountUserVO implements Serializable {
    private LocalDate date;

    private Long newUserCount; // 新增用户数量
}
