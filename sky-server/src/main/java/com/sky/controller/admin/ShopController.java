package com.sky.controller.admin;


import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {

    private static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置店铺营业状态
     * @param status 店铺状态
     *              0 - 休息中
     *              1 - 营业中
     * @return Result
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setShopStatus(@PathVariable Integer status) {
        log.info("设置店铺状态为: {}", status==1 ? "营业中" : "休息中");
        // 这里可以添加具体的业务逻辑，比如更新数据库或缓存
        if (status != 0 && status != 1) {
            return Result.error(MessageConstant.STATUS_UNDEFINED);
        }
        // 假设我们使用Redis来存储店铺状态
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }

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
