package com.digitalmoneyhouse.testing.regression;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.Random;

import static com.digitalmoneyhouse.testing.config.TestConfig.*;
import static com.digitalmoneyhouse.testing.utils.AuthHelper.getValidToken;
import static com.digitalmoneyhouse.testing.utils.RegressionHelpers.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Regression: actualización de alias de cuenta (PATCH /accounts/{id}).
 * Cubre TC-RG-043 a TC-RG-048.
 */
@Epic("Digital Money House")
@Feature("Regression - Actualización de cuenta")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountUpdateTestSuite {

    private static final String[] PALABRAS = {
            "perro", "gato", "casa", "auto", "luna", "sol", "mar", "rio", "monte", "campo"
    };

    private static String testAccountId;

    private static String otherAccountId;
    private static String otherAlias;
    private static String otherToken;

    private static RequestSpecification requestSpec;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;

        registerUser(TEST_EMAIL, "Test", "User", "11221122", "3001234567");
        testAccountId = resolveAccountId(keycloakIdFromToken(getValidToken()), getValidToken());

        otherToken = registerAndLoginFreshUser("accupdate");
        String otherKeycloakId = keycloakIdFromToken(otherToken);
        otherAccountId = resolveAccountId(otherKeycloakId, getValidToken());
        otherAlias = given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/user/" + otherKeycloakId)
                .jsonPath()
                .getString("alias");

        requestSpec = new RequestSpecBuilder()
                .addFilter(new AllureRestAssured())
                .build();
    }

    private static String randomAlias() {
        Random random = new Random();
        return PALABRAS[random.nextInt(PALABRAS.length)] + "." +
                PALABRAS[random.nextInt(PALABRAS.length)] + "." +
                PALABRAS[random.nextInt(PALABRAS.length)];
    }

    @Test
    @Order(1)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-043: Actualización exitosa de alias de cuenta")
    void tcRg043_actualizacionExitosaDeAlias_devuelve200() {
        String nuevoAlias = randomAlias();

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("alias", nuevoAlias))
                .when()
                .patch("/accounts/" + testAccountId)
                .then()
                .statusCode(200)
                .body("alias", equalTo(nuevoAlias));
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-044: Actualización con alias ya registrado en otra cuenta retorna 409")
    void tcRg044_aliasYaRegistradoEnOtraCuenta_devuelve409() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("alias", otherAlias))
                .when()
                .patch("/accounts/" + testAccountId)
                .then()
                .statusCode(409);
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-045: Actualización con alias de formato inválido retorna 400")
    void tcRg045_aliasFormatoInvalido_devuelve400() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("alias", "alias_invalido"))
                .when()
                .patch("/accounts/" + testAccountId)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-046: Actualización de cuenta inexistente retorna 404")
    void tcRg046_cuentaInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("alias", randomAlias()))
                .when()
                .patch("/accounts/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(5)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-047: Actualización de cuenta ajena retorna 403")
    void tcRg047_cuentaAjena_devuelve403() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("alias", randomAlias()))
                .when()
                .patch("/accounts/" + otherAccountId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(6)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-048: Actualización de alias sin token retorna 401")
    void tcRg048_sinToken_devuelve401() {
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of("alias", randomAlias()))
                .when()
                .patch("/accounts/" + testAccountId)
                .then()
                .statusCode(401);
    }
}