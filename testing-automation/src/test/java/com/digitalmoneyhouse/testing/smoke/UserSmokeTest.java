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
@Feature("Users Service - Smoke")
public class UserSmokeTest {

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.filters(new AllureRestAssured());
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-001: Registro exitoso de usuario con datos válidos")
    void tcSm001_registroExitosoUsuario_devuelve201YDatos() {
        long timestamp = System.currentTimeMillis();
        String emailDinamico = "nuevo" + timestamp + "@test.com";

        String dniDinamico = String.valueOf(timestamp).substring(5);

        Map<String, String> userBody = Map.of(
                "firstName", "Test",
                "lastName", "User",
                "dni", dniDinamico,
                "email", emailDinamico,
                "phone", "3001234567",
                "password", TEST_PASSWORD
        );

        given()
                .contentType("application/json")
                .body(userBody)
                .when()
                .post("/users/register")
                .then()
                .statusCode(201)
                .body("$", not(hasKey("password")))
                .body("id", notNullValue())
                .body("cvu", matchesPattern("\\d{22}"))
                .body("alias", matchesPattern("[a-zA-Z]+\\.[a-zA-Z]+\\.[a-zA-Z]+"));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-007: Consulta de perfil de usuario (GET /users/{id})")
    void tcSm007_consultarPerfilDeUsuario_devuelve200() {
        given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/users/" + TEST_USER_ID)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("cvu", matchesPattern("\\d{22}"))
                .body("alias", matchesPattern("[a-zA-Z]+\\.[a-zA-Z]+\\.[a-zA-Z]+"))
                .body("email", equalTo(TEST_EMAIL));
    }
}