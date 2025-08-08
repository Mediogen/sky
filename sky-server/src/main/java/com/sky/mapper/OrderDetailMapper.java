package com.sky.mapper;


import com.sky.entity.OrderDetail;
import com.sky.vo.SalesTop10VO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface OrderDetailMapper {
    void insertBatch(List<OrderDetail> orderDetailList);


    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> getByOrderId(Long id);


    List<SalesTop10VO> salesTop10(LocalDate begin, LocalDate endPlusOne);
}
