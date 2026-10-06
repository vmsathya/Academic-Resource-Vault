package com.vault;

import com.vault.util.DBConnection;
import com.vault.web.WebServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_HOST = "0.0.0.0";

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("       [ACADEMIC RESOURCE VAULT - COLLEGE REPOSITORY SYSTEM]      ");
        System.out.println("==================================================================");

        // Load configuration
        Properties props = new Properties();
        int port = DEFAULT_PORT;
        String host = DEFAULT_HOST;

        File propFile = new File("resources/db.properties");
        if (!propFile.exists()) {
            propFile = new File("AcademicResourceVault/resources/db.properties");
        }

        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)) {
                props.load(in);
                if (props.containsKey("server.port")) {
                    port = Integer.parseInt(props.getProperty("server.port").trim());
                }
                if (props.containsKey("server.host")) {
                    host = props.getProperty("server.host").trim();
                }
            } catch (Exception e) {
                System.out.println("Notice: Using default host and port (" + host + ":" + port + ")");
            }
        }

        // Allow CLI override: java -cp ... com.vault.Main [port]
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        try {
            // 1. Initialize SQLite database & seed authentic curriculum data
            System.out.println("[1/2] Connecting to JDBC database and verifying schema...");
            DBConnection.initializeDatabase();

            // 2. Start Embedded Web Server
            System.out.println("[2/2] Launching Academic Resource Vault Web Server...");
            WebServer server = new WebServer(host, port);
            server.start();

            System.out.println("\n==> Academic Resource Vault is LIVE!");
            System.out.println("   Local Access:      http://localhost:" + port);
            System.out.println("   Network Access:    http://" + host + ":" + port);
            System.out.println("------------------------------------------------------------------");
            System.out.println("Pre-configured Credentials:");
            System.out.println("   * Staff / Admin:  admin@vault.edu    | Password: admin123");
            System.out.println("   * Faculty:        faculty@vault.edu  | Password: faculty123");
            System.out.println("   * Student:        student@vault.edu  | Password: student123");
            System.out.println("   * Student:        priya@vault.edu    | Password: student123");
            System.out.println("==================================================================\n");

            // Keep alive
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Academic Resource Vault...");
                server.stop();
            }));

        } catch (IOException e) {
            System.err.println("Fatal: Failed to start web server on port " + port + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
