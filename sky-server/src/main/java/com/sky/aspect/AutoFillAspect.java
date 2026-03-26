package com.sky.aspect;

import com.sky.anno.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自动填充的切面类
 *
 */
@Aspect//表示当前类是一个切面类
@Component// 表示当前类是一个切面类
@Slf4j
public class AutoFillAspect {
    @Before("@annotation(com.sky.anno.AutoFill)")
    public void autoFill(JoinPoint joinPoint) {
        //1、获取目标方法上的注解，并拿到注解里的属性值
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();//获取方法签名
        Method method = methodSignature.getMethod();//获取方法
        AutoFill autoFill = method.getAnnotation(AutoFill.class);//获取注解
        OperationType value = autoFill.value();//获取操作类型
        //2、判断属性值如果是insert，则进行自动填充 ，如果是update，则进行更新时间填充   获取到目标方法的参数
        Object[] args = joinPoint.getArgs();//获取参数
        if(args == null || args.length == 0){
            return;
        }
        Object entity = args[0];//获取参数
        try {
            if(value == OperationType.INSERT){
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);//获取方法
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setCreateTime.invoke(entity, LocalDateTime.now());
                setUpdateTime.invoke(entity, LocalDateTime.now());
                setCreateUser.invoke(entity, BaseContext.getCurrentId());
                setUpdateUser.invoke(entity, BaseContext.getCurrentId());
            }
            else if(value == OperationType.UPDATE){
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                setUpdateTime.invoke(entity, LocalDateTime.now());
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setUpdateUser.invoke(entity, BaseContext.getCurrentId());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
