package com.digitalmoneyhouse.testing.regression;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static com.digitalmoneyhouse.testing.config.TestConfig.*;
import static com.digitalmoneyhouse.testing.utils.AuthHelper.getValidToken;
import static com.digitalmoneyhouse.testing.utils.DbHelper.seedStandardTransactionSet;
import static com.digitalmoneyhouse.testing.utils.RegressionHelpers.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Regression: consulta de cuenta (por userId y por id) y su listado de
 * transacciones. Cubre TC-RG-011 a TC-RG-019.
 *
 * TC-RG-012 y TC-RG-015 (token expirado) quedan @Disabled: requieren
 * esperar 300s a que expire el token, lo cual no es viable en un run de
 * regression normal. Ejecutar manualmente, o ajustar el
 * accessTokenLifespan del realm de Keycloak en el entorno de test a un
 * valor bajo (ej. 5s) para poder automatizarlos.
 */
@Epic("Digital Money House")
@Feature("Regression - Consulta de cuentas y transacciones")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountQueryTestSuite {

    private static String testAccountId;
    private static String testKeycloakId;
    private static String freshAccountId; // cuenta sin transacciones
    private static String freshToken;

    private static RequestSpecification requestSpec;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;

        registerUser(TEST_EMAIL, "Test", "User", "11221122", "3001234567");
        testKeycloakId = keycloakIdFromToken(getValidToken());
        testAccountId = resolveAccountId(testKeycloakId, getValidToken());

        freshToken = registerAndLoginFreshUser("freshquery");
        String freshKeycloakId = keycloakIdFromToken(freshToken);
        freshAccountId = resolveAccountId(freshKeycloakId, getValidToken());

        requestSpec = new RequestSpecBuilder()
                .addFilter(new AllureRestAssured())
                .build();
    }

    @Test
    @Order(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-011: Consulta de cuenta con userId inexistente retorna 404")
    void tcRg011_userIdInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/user/id-inexistente-9999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(2)
    @Disabled("Requiere esperar 300s a que expire el token. Ejecutar manualmente o ajustar accessTokenLifespan en Keycloak de test.")
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-012: Acceso con token expirado retorna 401")
    void tcRg012_tokenExpirado_devuelve401() {
        // Ver nota en el Javadoc de la clase.
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-013: Consulta de cuenta con ID inexistente retorna 404")
    void tcRg013_idInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-014: Consulta de cuenta sin token retorna 401")
    void tcRg014_sinToken_devuelve401() {
        given(requestSpec)
                .when()
                .get("/accounts/" + testAccountId)
                .then()
                .statusCode(401);
    }

    @Test
    @Order(5)
    @Disabled("Requiere esperar 300s a que expire el token. Ejecutar manualmente o ajustar accessTokenLifespan en Keycloak de test.")
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-015: Consulta de cuenta con token expirado retorna 401")
    void tcRg015_tokenExpirado_devuelve401() {
        // Ver nota en el Javadoc de la clase.
    }

    @Test
    @Order(6)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-016: Transacciones de cuenta sin movimientos devuelve array vacío")
    void tcRg016_sinMovimientos_devuelveArrayVacio() {
        given(requestSpec)
                .header("Authorization", "Bearer " + freshToken)   // <-- antes: getValidToken()
                .when()
                .get("/accounts/" + freshAccountId + "/transactions")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @Order(7)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-017: Transacciones ordenadas de más reciente a más antigua")
    void tcRg017_transaccionesOrdenadas_masRecienteAMasAntigua() {
        seedStandardTransactionSet(testAccountId);

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/transactions")
                .then()
                .statusCode(200)
                .body("[0].type", equalTo("INCOME"))
                .body("[1].type", equalTo("TRANSFER_IN"))
                .body("[2].type", equalTo("TRANSFER_OUT"));
    }

    @Test
    @Order(8)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-018: Transacciones de cuenta inexistente retorna 404")
    void tcRg018_cuentaInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/999999/transactions")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(9)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-019: Transacciones de cuenta sin token retorna 401")
    void tcRg019_sinToken_devuelve401() {
        given(requestSpec)
                .when()
                .get("/accounts/" + testAccountId + "/transactions")
                .then()
                .statusCode(401);
    }
}