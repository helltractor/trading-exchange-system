package com.helltractor.exchange;

import com.helltractor.exchange.db.DataBaseTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SchemaExporter {
    
    public static void main(String[] args) throws IOException {
        DataBaseTemplate dataBaseTemplate = new DataBaseTemplate(null);
        String ddl = """
                -- init exchange database
                
                DROP DATABASE IF EXISTS exchange;
                
                CREATE DATABASE exchange;
                
                USE exchange;
                
                """;
        ddl = ddl + dataBaseTemplate.exportDDL();
        System.out.println(ddl);
        Path path = Path.of(".").toAbsolutePath().getParent().resolve("build").resolve("sql")
                .resolve("schema.sql");
        Files.writeString(path, ddl);
        System.out.println("mysql -u root --password=password < " + path);
    }
    
}
