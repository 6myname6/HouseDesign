package com.housedesign.security;

import java.lang.annotation.*;

/**
 * 方法参数注解：标记需要注入「当前登录用户 ID」的参数。
 * 由 {@link CurrentUserIdArgumentResolver} 解析并赋值。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {
}
