package com.digitalmoneyhouse.testing.regression;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import java.util.Map;

import static com.digitalmoneyhouse.testing.config.TestConfig.*;
import static com.digitalmoneyhouse.testing.utils.RegressionHelpers.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Regression: registro de usuarios, login y accesos no autenticados.
 * Cubre TC-RG-001 a TC-RG-010.
 */
@Epic("Digital Money House")
@Feature("Regression - Registro y autenticación")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthRegistrationTestSuite {

    private static RequestSpecification requestSpec;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        // Aseguramos que el usuario de prueba global exista (email y DNI
        // conocidos, reutilizados como "ya existentes" en TC-RG-001/002).
        registerUser(TEST_EMAIL, "Test", "User", "11221122", "3001234567");

        requestSpec = new RequestSpecBuilder()
                .addFilter(new AllureRestAssured())
                .build();
    }

    @Test
    @Order(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-001: Registro con email ya existente retorna 400")
    void tcRg001_emailYaExistente_devuelve400() {
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of(
                        "firstName", "Test", "lastName", "User", "dni", "33333333",
                        "email", TEST_EMAIL, "password", "Test1234!", "phone", "3003333333"
                ))
                .when()
                .post("/users/register")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-002: Registro con DNI ya existente retorna 400")
    void tcRg002_dniYaExistente_devuelve400() {
        long ts = System.currentTimeMillis();
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of(
                        "firstName", "Test", "lastName", "User", "dni", "11221122",
                        "email", "nuevo" + ts + "@test.com", "password", "Test1234!", "phone", "3004444444"
                ))
                .when()
                .post("/users/register")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-003: Registro con body vacío retorna 400 con mensajes por campo")
    void tcRg003_bodyVacio_devuelve400ConMensajesPorCampo() {
        given(requestSpec)
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/users/register")
                .then()
                .statusCode(400)
                .body("$", hasKey("firstName"))
                .body("$", hasKey("lastName"))
                .body("$", hasKey("email"))
                .body("$", hasKey("password"))
                .body("$", hasKey("dni"))
                .body("$", hasKey("phone"));
    }

    @Test
    @Order(4)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-004: Registro con email con formato inválido retorna 400")
    void tcRg004_emailFormatoInvalido_devuelve400() {
        long ts = System.currentTimeMillis();
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of(
                        "firstName", "Test", "lastName", "User", "dni", uniqueDni(ts),
                        "email", "emailinvalido", "password", "Test1234!", "phone", "3005555555"
                ))
                .when()
                .post("/users/register")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-005: Registro con contraseña menor a 6 caracteres retorna 400")
    void tcRg005_passwordMenorA6_devuelve400() {
        long ts = System.currentTimeMillis();
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of(
                        "firstName", "Test", "lastName", "User", "dni", uniqueDni(ts),
                        "email", "test" + ts + "@test.com", "password", "Test", "phone", "3006666666"
                ))
                .when()
                .post("/users/register")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-006: Login con email inexistente retorna 404")
    void tcRg006_loginEmailInexistente_devuelve404() {
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of("email", "noexiste" + System.currentTimeMillis() + "@test.com", "password", "Test1234!"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(7)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-007: Login con contraseña incorrecta retorna 400")
    void tcRg007_loginPasswordIncorrecta_devuelve400() {
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of("email", TEST_EMAIL, "password", "wrongpass"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(8)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-008: Acceso a endpoint protegido sin token retorna 401")
    void tcRg008_endpointProtegidoSinToken_devuelve401() {
        String keycloakId = keycloakIdFromToken(loginAndGetToken(TEST_EMAIL, TEST_PASSWORD));

        given(requestSpec)
                .when()
                .get("/accounts/user/" + keycloakId)
                .then()
                .statusCode(401);
    }

    @Test
    @Order(9)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-009: CVU generado en el registro tiene exactamente 22 dígitos")
    void tcRg009_cvuGenerado_tiene22Digitos() {
        long ts = System.currentTimeMillis();
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of(
                        "firstName", "CVU", "lastName", "Test", "dni", uniqueDni(ts),
                        "email", "cvu" + ts + "@test.com", "password", "Test1234!", "phone", "3007777777"
                ))
                .when()
                .post("/users/register")
                .then()
                .statusCode(201)
                .body("cvu", matchesPattern("\\d{22}"));
    }

    @Test
    @Order(10)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-010: Alias generado en el registro tiene 3 palabras separadas por punto")
    void tcRg010_aliasGenerado_tiene3PalabrasSeparadasPorPunto() {
        long ts = System.currentTimeMillis();
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of(
                        "firstName", "Alias", "lastName", "Test", "dni", uniqueDni(ts),
                        "email", "alias" + ts + "@test.com", "password", "Test1234!", "phone", "3008888888"
                ))
                .when()
                .post("/users/register")
                .then()
                .statusCode(201)
                .body("alias", matchesPattern("[a-zA-Z]+\\.[a-zA-Z]+\\.[a-zA-Z]+"));
    }
}