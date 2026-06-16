package com.digitalmoneyhouse.testing.smoke;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static com.digitalmoneyhouse.testing.config.TestConfig.*;
import static com.digitalmoneyhouse.testing.utils.AuthHelper.getValidToken;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Digital Money House")
@Feature("Auth Service - Smoke")
public class AuthSmokeTest {

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.filters(new AllureRestAssured());
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-002: Login exitoso con credenciales válidas retorna token JWT")
    void tcSm002_loginExitoso_retornaTokenJWT() {
        Map<String, String> credentials = Map.of(
                "email", TEST_EMAIL,
                "password", TEST_PASSWORD
        );

        given()
                .contentType("application/json")
                .body(credentials)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", notNullValue());
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-003: Logout con token válido retorna status 200")
    void tcSm003_logoutConTokenValido_retorna200() {
        String respuestaText = given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(200)
                .extract().asString();

        org.junit.jupiter.api.Assertions.assertEquals("Sesión cerrada exitosamente", respuestaText);
        com.digitalmoneyhouse.testing.utils.AuthHelper.invalidateCache();
    }
}