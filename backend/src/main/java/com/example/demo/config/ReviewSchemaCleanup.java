package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Removes legacy database columns that are no longer used.
 * This component runs on application startup to clean up the schema.
 */
@Component
public class ReviewSchemaCleanup implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ReviewSchemaCleanup.class);

    private final DataSource dataSource;

    public ReviewSchemaCleanup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting review schema cleanup...");
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Check for and drop reviewer_user_name column
            if (columnExists(metaData, "reviews", "reviewer_user_name")) {
                logger.info("Found legacy column 'reviewer_user_name', dropping it...");
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE reviews DROP COLUMN reviewer_user_name");
                    logger.info("Successfully dropped 'reviewer_user_name' column");
                } catch (Exception e) {
                    logger.warn("Failed to drop 'reviewer_user_name' column", e);
                }
            }
            
            // Check for and drop reviewer_name column
            if (columnExists(metaData, "reviews", "reviewer_name")) {
                logger.info("Found legacy column 'reviewer_name', dropping it...");
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE reviews DROP COLUMN reviewer_name");
                    logger.info("Successfully dropped 'reviewer_name' column");
                } catch (Exception e) {
                    logger.warn("Failed to drop 'reviewer_name' column", e);
                }
            }
            
            logger.info("Schema cleanup completed");
        } catch (Exception e) {
            logger.error("Error during schema cleanup", e);
        }
    }

    private boolean columnExists(DatabaseMetaData metaData, String tableName, String columnName) throws Exception {
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }
}
