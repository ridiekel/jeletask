package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class V2__Change_payload_to_varchar extends BaseJavaMigration {
    private static final Logger log = LoggerFactory.getLogger(V2__Change_payload_to_varchar.class);

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE mqtt_message_trace " +
                "ADD COLUMN payload_temp VARCHAR(10000)");
        }

        int batchSize = 100;
        int offset = 0;
        int totalProcessed = 0;

        while (true) {
            String selectSql = "SELECT id, payload FROM mqtt_message_trace " +
                "WHERE payload_temp IS NULL LIMIT ?";

            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                selectStmt.setInt(1, batchSize);
                ResultSet rs = selectStmt.executeQuery();

                int batchCount = 0;
                PreparedStatement updateStmt = connection.prepareStatement(
                    "UPDATE mqtt_message_trace SET payload_temp = ? WHERE id = ?"
                );

                while (rs.next()) {
                    String id = rs.getString("id");
                    String payload = rs.getString("payload");

                    // Truncate indien nodig
                    if (payload != null && payload.length() > 10000) {
                        payload = payload.substring(0, 9997) + "...";
                    }

                    updateStmt.setString(1, payload);
                    updateStmt.setString(2, id);
                    updateStmt.addBatch();
                    batchCount++;
                }

                if (batchCount == 0) {
                    break;
                }

                updateStmt.executeBatch();
                connection.commit();
                totalProcessed += batchCount;

                log.info("Processed {} records...", totalProcessed);
            }
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE mqtt_message_trace DROP COLUMN payload");
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE mqtt_message_trace " +
                "ALTER COLUMN payload_temp RENAME TO payload");
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE mqtt_message_trace " +
                "ALTER COLUMN payload SET NOT NULL");
        }

        log.info("Migration completed. Total records: {}", totalProcessed);
    }
}
