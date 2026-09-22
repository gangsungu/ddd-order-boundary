package com.roykhan.dddorderboundary;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

// 컨텍스트마다 자기 스키마에만 테이블을 두는지 실제 DB 에서 확인한다.
// 새 엔티티에 스키마를 빠뜨리면 기본 스키마(public)에 만들어져 소유권이 흐려지므로 여기서 걸린다
@SpringBootTest
@DisplayName("컨텍스트별 스키마 통합 테스트")
class SchemaPerContextTest {

    private static final String BASE_PACKAGE = DddOrderBoundaryApplication.class.getPackageName() + ".";

    // 컨텍스트 패키지 → 스키마. order 는 SQL 예약어라 테이블 이름과 같은 orders 를 쓴다
    private static final Map<String, String> SCHEMA_BY_CONTEXT = Map.of(
        "order", "orders",
        "product", "product"
    );

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("모든 엔티티의 테이블이 자기 컨텍스트의 스키마에 만들어진다")
    void 엔티티별_스키마() {
        List<Class<?>> entityTypes = entityManagerFactory.getMetamodel().getEntities().stream()
            .<Class<?>>map(EntityType::getJavaType)
            .toList();

        assertThat(entityTypes).isNotEmpty();
        for (Class<?> entityType : entityTypes) {
            String context = entityType.getName().substring(BASE_PACKAGE.length()).split("\\.")[0];
            String schema = SCHEMA_BY_CONTEXT.get(context);
            String table = entityType.getAnnotation(Table.class).name();

            assertThat(schema).as("%s 컨텍스트의 스키마가 정해지지 않았다", context).isNotNull();
            assertThat(countTables(schema, table))
                .as("%s 의 테이블 %s 가 %s 스키마에 없다", entityType.getSimpleName(), table, schema)
                .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("외래 키는 같은 스키마 안에서만 걸린다")
    void 스키마를_넘는_외래_키_없음() {
        List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList(
            "select constraint_schema, constraint_name, unique_constraint_schema"
                + " from information_schema.referential_constraints");

        // 주문 → 주문 항목, 재고 → 예약 연관이 있으므로 비어 있으면 조회 자체가 잘못된 것이다
        assertThat(foreignKeys).isNotEmpty();
        assertThat(foreignKeys).allSatisfy(foreignKey ->
            assertThat(foreignKey.get("unique_constraint_schema"))
                .as("외래 키 %s 가 %s 스키마 밖을 가리킨다",
                    foreignKey.get("constraint_name"), foreignKey.get("constraint_schema"))
                .isEqualTo(foreignKey.get("constraint_schema")));
    }

    private int countTables(String schema, String table) {
        return jdbcTemplate.queryForObject(
            "select count(*) from information_schema.tables where table_schema = ? and table_name = ?",
            Integer.class, schema, table);
    }
}
