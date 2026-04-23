package com.boardinggate.web.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 方法参数注解，用于直接注入当前登录用户。
 * <pre>
 *   {@code @GetMapping("/me")}
 *   public ApiResponse&lt;LoginUser&gt; me(@CurrentUser LoginUser user) { ... }
 * </pre>
 * 注入时机：由 {@link CurrentUserArgumentResolver} 从 {@link LoginUserContext} 读取。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
