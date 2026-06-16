package com.digitalmoneyhouse.testing.config;

public class TestConfig {

    public static final String BASE_URL = "http://localhost:8080";

    public static final String TEST_EMAIL = "nuevo@test.com";
    public static final String TEST_PASSWORD = "Test1234!";

    public static final String TEST_ACCOUNT_ID = "1";
    public static final String TEST_USER_ID = "1";
    public static final String TEST_KEYCLOAK_ID = "b25c9b50-934c-4321-86c8-3782e2493550";

    public static final String DB_URL = "jdbc:mysql://localhost:3307/digital_money_house";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "root1234";

    private TestConfig() {
        // Clase utilitaria
    }
}