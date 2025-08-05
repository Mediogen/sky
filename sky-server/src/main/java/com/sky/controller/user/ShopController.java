package com.sky.controller.user;


import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
@Api(tags = "C端-店铺相关接口")
public class ShopController {
    private static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取店铺营业状态
     * @return Result<Integer>
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getShopStatus() {
        // 从Redis中获取店铺状态
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        if (status == null) {
            // 如果没有设置过状态，默认返回休息中
            status = 0;
            log.info("店铺状态未设置，默认返回休息中");
        }
        log.info("获取到店铺状态: {}", status == 1 ? "营业中" : "休息中");
        return Result.success(status);
    }
}
