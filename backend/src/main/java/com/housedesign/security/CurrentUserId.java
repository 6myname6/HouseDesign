package com.housedesign.security;

import java.lang.annotation.*;

/**
 * 注入当前登录用户 id 的方法参数注解。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {
}
