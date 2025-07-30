package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Aspect
@Slf4j
@Component
public class AutoFillAspect {


    // 切点：拦截 com.sky.mapper 包下所有方法，并且方法上有 @AutoFill 注解
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointcut() {}

    @Before("autoFillPointcut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("自动填充切面开始执行...");
        // 获取方法上的 @AutoFill 注解
        //1. 获取方法签名对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //2. 获取方法上的注解
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);

        //3. 获取注解的值--数据库操作类型
        OperationType operationType = autoFill.value();
        //4.获得方法参数--实体对象
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || args[0] == null) {
            return;
        }
        Object entity = args[0];
        //准备数据
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = BaseContext.getCurrentId(); // 假设当前用户ID为1，实际应用中应从上下文或安全框架获取
        //5. 根据操作类型进行自动填充
        switch (operationType) {
            case INSERT:
                log.info("执行插入操作，进行自动填充...");
                try {
                    // 设置创建时间和创建人
                    //获取实体对象的set与get方法
                    entity.getClass().getMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class).invoke(entity, now);
                    entity.getClass().getMethod(AutoFillConstant.SET_CREATE_USER, Long.class).invoke(entity, currentUserId);
                    // 设置修改时间和修改人
                    entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
                    entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class).invoke(entity, currentUserId);
                } catch (Exception e) {
                    log.error("自动填充失败: {}", e.getMessage());
                }
                break;
            case UPDATE:
                log.info("执行更新操作，进行自动填充...");
                try {
                    // 设置修改时间和修改人
                    entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
                    entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class).invoke(entity, currentUserId);
                } catch (Exception e) {
                    log.error("自动填充失败: {}", e.getMessage());
                }
                break;


        }
    }
}
