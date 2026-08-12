package com.bridgenote.common.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Set;

/**
 * Supabase Auth가 발급한 JWT(access token)를 공개 JWKS로 검증한다.
 * 현재 프로젝트 서명키는 비대칭(ES256, ECC P-256) — 공개키를 JWKS에서 받아 서명을 확인한다.
 * (키 회전/캐싱은 Nimbus의 RemoteJWKSet이 처리)
 */
@Component
public class SupabaseJwtProvider {

	private final ConfigurableJWTProcessor<SecurityContext> processor;

	public SupabaseJwtProvider(@Value("${supabase.jwks-uri}") String jwksUri) {
		try {
			JWKSource<SecurityContext> jwkSource = JWKSourceBuilder
					.create(URI.create(jwksUri).toURL())
					.retrying(true)
					.build();

			JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
					Set.of(JWSAlgorithm.ES256, JWSAlgorithm.RS256), jwkSource);

			DefaultJWTProcessor<SecurityContext> p = new DefaultJWTProcessor<>();
			p.setJWSKeySelector(keySelector);
			this.processor = p;
		} catch (Exception e) {
			throw new IllegalStateException("JWKS 초기화 실패: " + jwksUri, e);
		}
	}

	/**
	 * 토큰을 검증하고 인증 사용자를 반환한다. 서명/만료 등 문제가 있으면 {@link InvalidTokenException}.
	 */
	public AuthUser verify(String token) {
		try {
			JWTClaimsSet claims = processor.process(token, null);
			Object email = claims.getClaim("email");
			return new AuthUser(claims.getSubject(), email == null ? null : email.toString());
		} catch (Exception e) {
			throw new InvalidTokenException("유효하지 않은 토큰입니다.");
		}
	}
}
