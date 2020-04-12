package com.marxbo.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * 映射路径注解
 *
 * @author marxbo
 * @version 1.0
 * @date 2020/3/21 0:55
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {

    String value() default "";

}
