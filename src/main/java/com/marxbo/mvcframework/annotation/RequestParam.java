package com.marxbo.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * 参数绑定注解
 *
 * @author marxbo
 * @version 1.0
 * @date 2020/3/21 0:55
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {

    String name() default "";

    String value() default "";

}
