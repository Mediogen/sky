package com.sky.service;

import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;

public interface WorkspaceService {
    BusinessDataVO getBusinessData();

    OrderOverViewVO overviewOrders();

    DishOverViewVO overviewDishes();

    SetmealOverViewVO overviewSetmeals();
}
