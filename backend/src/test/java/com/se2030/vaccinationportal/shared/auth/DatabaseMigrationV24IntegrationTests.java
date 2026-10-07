package com.se2030.vaccinationportal.shared.auth;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseMigrationV24IntegrationTests {

    private DataSource createIsolatedDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:v24_migration_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    @Test
    @DisplayName("V24 migration applies cleanly on top of V4 and creates idx_refresh_token_sessions_family_revoked")
    void v24Migration_executesCleanlyAndCreatesIndex() throws Exception {
        DataSource dataSource = createIsolatedDataSource();
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V4__shared_authentication_foundation.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V24__add_refresh_token_sessions_family_revocation_index.sql"));

            DatabaseMetaData metaData = connection.getMetaData();
            List<String> indexes = new ArrayList<>();
            try (ResultSet rs = metaData.getIndexInfo(null, null, "REFRESH_TOKEN_SESSIONS", false, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if (indexName != null) {
                        indexes.add(indexName.toLowerCase());
                    }
                }
            }
            assertTrue(indexes.stream().anyMatch(idx -> idx.contains("idx_refresh_token_sessions_family_revoked")),
                    "Index idx_refresh_token_sessions_family_revoked must exist after V24 migration");
        }
    }
}
