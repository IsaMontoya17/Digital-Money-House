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
 * Regression: actividad de cuenta y detalle de transacción
 * (GET /accounts/{id}/activity, GET /accounts/{id}/activity/{transactionId}).
 * Cubre TC-RG-049 a TC-RG-058.
 *
 * Reutiliza DbHelper.seedStandardTransactionSet, que siembra 3
 * transacciones fijas (INCOME id=101 $1000, TRANSFER_IN $300,
 * TRANSFER_OUT $200), igual que SmokeTestSuite TC-SM-013/014.
 */
@Epic("Digital Money House")
@Feature("Regression - Actividad de cuenta")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ActivityTestSuite {

    private static String testAccountId;
    private static String freshAccountId; // cuenta sin transacciones
    private static String otherAccountId; // cuenta ajena, con su propia transacción sembrada
    private static String otherToken;
    private static String freshToken;

    private static RequestSpecification requestSpec;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;

        registerUser(TEST_EMAIL, "Test", "User", "11221122", "3001234567");
        testAccountId = resolveAccountId(keycloakIdFromToken(getValidToken()), getValidToken());
        seedStandardTransactionSet(testAccountId);

        freshToken = registerAndLoginFreshUser("freshactivity");
        freshAccountId = resolveAccountId(keycloakIdFromToken(freshToken), getValidToken());

        otherToken = registerAndLoginFreshUser("otheractivity");
        otherAccountId = resolveAccountId(keycloakIdFromToken(otherToken), getValidToken());

        requestSpec = new RequestSpecBuilder()
                .addFilter(new AllureRestAssured())
                .build();
    }

    @Test
    @Order(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-049: Actividad de cuenta sin transacciones devuelve array vacío")
    void tcRg049_sinTransacciones_devuelveArrayVacio() {
        given(requestSpec)
                .header("Authorization", "Bearer " + freshToken)   // <-- antes: getValidToken()
                .when()
                .get("/accounts/" + freshAccountId + "/activity")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-050: Actividad de cuenta ajena retorna 403")
    void tcRg050_cuentaAjena_devuelve403() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + otherAccountId + "/activity")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-051: Actividad de cuenta inexistente retorna 404")
    void tcRg051_cuentaInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/999999/activity")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-052: Actividad de cuenta sin token retorna 401")
    void tcRg052_sinToken_devuelve401() {
        given(requestSpec)
                .when()
                .get("/accounts/" + testAccountId + "/activity")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(5)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-053: Actividad ordenada de más reciente a más antigua")
    void tcRg053_actividadOrdenada_masRecienteAMasAntigua() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/activity")
                .then()
                .statusCode(200)
                .body("[0].type", equalTo("INCOME"))
                .body("[1].type", equalTo("TRANSFER_IN"))
                .body("[2].type", equalTo("TRANSFER_OUT"));
    }

    @Test
    @Order(6)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-054: Detalle de transacción exitoso")
    void tcRg054_detalleTransaccionExitoso_devuelve200() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/activity/101")
                .then()
                .statusCode(200)
                .body("id", equalTo(101))
                .body("amount", equalTo(1000.00f))
                .body("type", equalTo("INCOME"))
                .body("createdAt", notNullValue())
                .body("description", notNullValue());
    }

    @Test
    @Order(7)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-055: Detalle de transacción inexistente retorna 404")
    void tcRg055_transaccionInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/activity/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(8)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-056: Detalle de transacción que no pertenece a la cuenta retorna 404")
    void tcRg056_transaccionNoPerteneceALaCuenta_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + otherToken)
                .when()
                .get("/accounts/" + otherAccountId + "/activity/101")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(9)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-057: Detalle de transacción de cuenta ajena retorna 403")
    void tcRg057_cuentaAjena_devuelve403() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + otherAccountId + "/activity/101")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(10)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-058: Detalle de transacción sin token retorna 401")
    void tcRg058_sinToken_devuelve401() {
        given(requestSpec)
                .when()
                .get("/accounts/" + testAccountId + "/activity/101")
                .then()
                .statusCode(401);
    }
}