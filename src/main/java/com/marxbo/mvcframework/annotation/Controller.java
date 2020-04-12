package com.marxbo.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * 控制层注解
 *
 * @author marxbo
 * @version 1.0
 * @date 2020/3/21 0:52
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {

    String value() default "";

}
