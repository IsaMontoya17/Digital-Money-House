package com.digitalmoneyhouse.testing.utils;

import io.restassured.response.Response;

import java.util.Base64;
import java.util.Map;

import static com.digitalmoneyhouse.testing.config.TestConfig.TEST_PASSWORD;
import static io.restassured.RestAssured.given;

public class RegressionHelpers {

    private RegressionHelpers() {
    }

    /** Registra un usuario si no existe (login primero, igual que en SmokeTestSuite). */
    public static void registerUser(String email, String firstName, String lastName, String dni, String phone) {
        Response loginResponse = given()
                .contentType("application/json")
                .body(Map.of("email", email, "password", TEST_PASSWORD))
                .when()
                .post("/auth/login");

        if (loginResponse.getStatusCode() == 200) {
            return;
        }

        given()
                .contentType("application/json")
                .body(Map.of(
                        "firstName", firstName,
                        "lastName", lastName,
                        "dni", dni,
                        "email", email,
                        "phone", phone,
                        "password", TEST_PASSWORD
                ))
                .when()
                .post("/users/register")
                .then()
                .statusCode(201);
    }

    public static String loginAndGetToken(String email, String password) {
        return given()
                .contentType("application/json")
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/auth/login")
                .then()
                .extract()
                .jsonPath()
                .getString("accessToken");
    }

    public static String keycloakIdFromToken(String token) {
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        return payload.split("\"sub\":\"")[1].split("\"")[0];
    }

    public static String resolveAccountId(String keycloakId, String requesterToken) {
        return given()
                .header("Authorization", "Bearer " + requesterToken)
                .when()
                .get("/accounts/user/" + keycloakId)
                .jsonPath()
                .getString("id");
    }

    /**
     * Resuelve el id numérico de la tabla `users` (el que usan los endpoints
     * /users/{id}) a partir del keycloak_id, vía JDBC directo — mismo patrón
     * que SmokeTestSuite.getUserIdFromDb().
     */
    public static String resolveNumericUserId(String keycloakId) {
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                com.digitalmoneyhouse.testing.config.TestConfig.DB_URL,
                com.digitalmoneyhouse.testing.config.TestConfig.DB_USER,
                com.digitalmoneyhouse.testing.config.TestConfig.DB_PASSWORD)) {
            String sql = "SELECT id FROM users WHERE keycloak_id = ?";
            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, keycloakId);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return String.valueOf(rs.getInt("id"));
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudo obtener el userId desde la base de datos", e);
        }
        throw new RuntimeException("Usuario no encontrado en la base de datos para keycloak_id: " + keycloakId);
    }

    /** DNI numérico de 8 dígitos determinístico a partir de un timestamp. */
    public static String uniqueDni(long seed) {
        String digits = String.valueOf(seed);
        return digits.substring(Math.max(0, digits.length() - 8));
    }

    /** Registra un usuario nuevo con datos únicos y devuelve su token. */
    public static String registerAndLoginFreshUser(String prefix) {
        long ts = System.currentTimeMillis() + (long) (Math.random() * 1000);
        String email = prefix + ts + "@test.com";
        registerUser(email, "Fresh", prefix, uniqueDni(ts), "300" + String.valueOf(ts).substring(4, 10));
        return loginAndGetToken(email, TEST_PASSWORD);
    }
}