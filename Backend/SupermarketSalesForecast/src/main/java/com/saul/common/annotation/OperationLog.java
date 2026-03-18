package com.saul.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * <p>
 * 在需要记录操作日志的方法上添加此注解，AOP 切面会自动记录日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 操作类型
     * LOGIN - 登录
     * PASSWORD - 密码修改
     * PRODUCT - 商品操作
     * SALES - 销售操作
     * INVENTORY - 库存操作
     */
    String type();

    /**
     * 操作描述
     */
    String desc();
}