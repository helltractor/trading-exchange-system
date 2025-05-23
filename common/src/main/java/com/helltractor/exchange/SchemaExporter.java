package com.helltractor.exchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.helltractor.exchange.db.DbTemplate;

/**
 * Export schema to file.
 */
public class SchemaExporter {

    public static void main(String[] args) throws IOException {
        DbTemplate dbTemplate = new DbTemplate(null);
        String ddl = """
                -- init exchange database
                
                DROP DATABASE IF EXISTS exchange;
                
                CREATE DATABASE exchange;
                
                USE exchange;
                
                """;
        ddl = ddl + dbTemplate.exportDDL();
        System.out.println(ddl);
        Path path = Path.of(".").toAbsolutePath().getParent().resolve("build").resolve("mysql")
                .resolve("schema.sql");
        Files.writeString(path, ddl);
        System.out.println("mysql -u root --password=password < " + path);
    }
}
