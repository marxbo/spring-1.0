package com.marxbo.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * 注入注解
 *
 * @author marxbo
 * @version 1.0
 * @date 2020/3/21 0:51
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

    boolean required() default true;

    String value() default "";

}
