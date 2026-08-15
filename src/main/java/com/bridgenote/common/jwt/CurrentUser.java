package com.bridgenote.common.jwt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 로그인 사용자({@link AuthUser})를 주입한다.
 * 인증되지 않았으면 401을 반환한다.
 *
 * <pre>{@code
 * @PostMapping("/api/meetings")
 * public ... create(@CurrentUser AuthUser user, ...) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
