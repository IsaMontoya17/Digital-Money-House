package com.digitalmoneyhouse.testing.utils;

import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.digitalmoneyhouse.testing.config.TestConfig.*;
import static io.restassured.RestAssured.given;

public class AuthHelper {

    private static final Logger logger = LoggerFactory.getLogger(AuthHelper.class);
    private static String cachedToken;

    public static String getValidToken() {
        if (cachedToken == null) {
            cachedToken = login(TEST_EMAIL, TEST_PASSWORD);
        }
        return cachedToken;
    }

    public static String login(String email, String password) {
        logger.info("Solicitando nuevo token de autenticación para: {}", email);

        Response response = given()
                .baseUri(BASE_URL)
                .contentType("application/json")
                .body("{ \"email\": \"" + email + "\", \"password\": \"" + password + "\" }")
                .when()
                .post("/auth/login");

        if (response.getStatusCode() != 200) {
            logger.error("Error en la autenticación. Status: {}", response.getStatusCode());
            throw new RuntimeException("No se pudo obtener el token de acceso.");
        }

        return response.jsonPath().getString("accessToken");
    }

    public static void invalidateCache() {
        cachedToken = null;
    }
}