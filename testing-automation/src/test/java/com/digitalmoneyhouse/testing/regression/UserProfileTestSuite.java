package com.digitalmoneyhouse.testing.regression;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import java.util.Map;

import static com.digitalmoneyhouse.testing.config.TestConfig.*;
import static com.digitalmoneyhouse.testing.utils.AuthHelper.getValidToken;
import static com.digitalmoneyhouse.testing.utils.RegressionHelpers.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Regression: consulta y actualización de perfil de usuario (GET/PATCH
 * /users/{id}). Cubre TC-RG-020 a TC-RG-022 y TC-RG-034 a TC-RG-042.
 */
@Epic("Digital Money House")
@Feature("Regression - Perfil de usuario")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserProfileTestSuite {

    private static String testUserId;
    private static String testKeycloakId;

    private static String otherEmail;
    private static String otherUserId;
    private static String otherToken;

    private static RequestSpecification requestSpec;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;

        registerUser(TEST_EMAIL, "Test", "User", "11221122", "3001234567");
        testKeycloakId = keycloakIdFromToken(getValidToken());
        testUserId = resolveNumericUserId(testKeycloakId);

        long ts = System.currentTimeMillis();
        otherEmail = "otrouser" + ts + "@test.com";
        registerUser(otherEmail, "Otro", "Usuario", uniqueDni(ts), "3009990000");
        otherToken = loginAndGetToken(otherEmail, TEST_PASSWORD);
        otherUserId = resolveNumericUserId(keycloakIdFromToken(otherToken));

        requestSpec = new RequestSpecBuilder()
                .addFilter(new AllureRestAssured())
                .build();
    }

    @Test
    @Order(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-020: Perfil de usuario devuelve CVU de 22 dígitos y alias de 3 palabras")
    void tcRg020_perfilDevuelveCvuYAliasConFormatoCorrecto() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/users/" + testUserId)
                .then()
                .statusCode(200)
                .body("cvu", matchesPattern("\\d{22}"))
                .body("alias", matchesPattern("[a-zA-Z]+\\.[a-zA-Z]+\\.[a-zA-Z]+"));
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-021: Perfil de usuario con ID inexistente retorna 404")
    void tcRg021_idInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/users/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-022: Perfil de usuario sin token retorna 401")
    void tcRg022_sinToken_devuelve401() {
        given(requestSpec)
                .when()
                .get("/users/" + testUserId)
                .then()
                .statusCode(401);
    }

    @Test
    @Order(4)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-034: Actualización exitosa de nombre y apellido de usuario")
    void tcRg034_actualizarNombreYApellido_devuelve200() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("firstName", "Nuevo", "lastName", "Apellido"))
                .when()
                .patch("/users/" + testUserId)
                .then()
                .statusCode(200)
                .body("firstName", equalTo("Nuevo"))
                .body("lastName", equalTo("Apellido"));
    }

    @Test
    @Order(5)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-035: Actualización de contraseña sincroniza con Keycloak")
    void tcRg035_actualizarPassword_sincronizaConKeycloak() {
        String nuevaPassword = "NuevoPass123!";

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("password", nuevaPassword))
                .when()
                .patch("/users/" + testUserId)
                .then()
                .statusCode(200);

        given(requestSpec)
                .contentType("application/json")
                .body(Map.of("email", TEST_EMAIL, "password", nuevaPassword))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue());

        // Revertimos para no romper AuthHelper.getValidToken() en el resto de la suite.
        String tokenConPasswordNueva = loginAndGetToken(TEST_EMAIL, nuevaPassword);
        given()
                .header("Authorization", "Bearer " + tokenConPasswordNueva)
                .contentType("application/json")
                .body(Map.of("password", TEST_PASSWORD))
                .when()
                .patch("/users/" + testUserId)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(6)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-036: Actualización con email ya registrado en otro usuario retorna 400")
    void tcRg036_emailYaRegistradoEnOtroUsuario_devuelve400() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("email", otherEmail))
                .when()
                .patch("/users/" + testUserId)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(7)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-037: Actualización con DNI ya registrado en otro usuario retorna 400")
    void tcRg037_dniYaRegistradoEnOtroUsuario_devuelve400() {
        String otroDni = given()
                .header("Authorization", "Bearer " + otherToken)
                .when()
                .get("/users/" + otherUserId)
                .jsonPath()
                .getString("dni");

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("dni", otroDni))
                .when()
                .patch("/users/" + testUserId)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(8)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-038: Actualización de perfil de usuario inexistente retorna 404")
    void tcRg038_usuarioInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("firstName", "Test"))
                .when()
                .patch("/users/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(9)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-039: Actualización de perfil de otro usuario retorna 403")
    void tcRg039_perfilDeOtroUsuario_devuelve403() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("firstName", "Intruso"))
                .when()
                .patch("/users/" + otherUserId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(10)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-040: Actualización con contraseña menor a 8 caracteres retorna 400")
    void tcRg040_passwordMenorA8_devuelve400() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("password", "abc"))
                .when()
                .patch("/users/" + testUserId)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(11)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-041: Actualización con email de formato inválido retorna 400")
    void tcRg041_emailFormatoInvalido_devuelve400() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("email", "emailinvalido"))
                .when()
                .patch("/users/" + testUserId)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(12)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-042: Actualización de perfil sin token retorna 401")
    void tcRg042_sinToken_devuelve401() {
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of("firstName", "Test"))
                .when()
                .patch("/users/" + testUserId)
                .then()
                .statusCode(401);
    }
}