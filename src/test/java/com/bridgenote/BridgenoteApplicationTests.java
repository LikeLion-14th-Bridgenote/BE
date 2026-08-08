package com.bridgenote;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 컨텍스트 로드 스모크 테스트.
 *
 * <p>초기 스캐폴드 한정: 아직 실제 DB(Supabase) 없이도 컨텍스트가 뜨도록,
 * Hibernate가 부팅 시 JDBC 메타데이터를 조회하지 않게 하고 dialect를 명시한다.
 * (HikariCP 커넥션 풀은 첫 사용 시점에 열리므로 부팅 중에는 DB에 접속하지 않는다.)
 * 실제 엔티티/리포지토리를 추가해 DB 연동 테스트가 필요해지면
 * 아래 properties를 걷어내고 테스트용 DB(또는 Testcontainers)로 전환할 것.
 */
@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
		"spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false"
})
class BridgenoteApplicationTests {

	@Test
	void contextLoads() {
	}

}
