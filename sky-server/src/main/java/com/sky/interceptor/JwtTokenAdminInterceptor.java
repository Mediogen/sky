package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);

            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("当前员工id:{}", empId);

            // --- 新增逻辑：校验 Redis 白名单 ---
            String redisKey = "login_token:" + token;
            Boolean isTokenInWhitelist = redisTemplate.hasKey(redisKey);
            if (isTokenInWhitelist == null || !isTokenInWhitelist) {
                // 如果 Token 不在白名单中（可能已登出），拒绝访问
                log.warn("Token不在Redis白名单中，可能是已登出或伪造的Token: {}", token);
                response.setStatus(401);
                return false;
            }

            // --- 新增逻辑：校验账户状态 ---
            Employee employee = employeeService.getById(empId);
            if (employee == null || StatusConstant.DISABLE.equals(employee.getStatus())) {
                // 如果用户不存在或账户已被禁用
                log.warn("用户不存在或账户已被禁用，用户ID: {}", empId);

                // （可选但推荐）从 Redis 中删除这个无效的 Token
                redisTemplate.delete(redisKey);

                response.setStatus(401);
                return false;
            }

            //3、通过，放行
            BaseContext.setCurrentId(empId);
            return true;

        } catch (Exception ex) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        BaseContext.removeCurrentId();
        log.info("线程变量数据清理完毕");
    }



}
