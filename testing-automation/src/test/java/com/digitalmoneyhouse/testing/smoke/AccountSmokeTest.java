package com.digitalmoneyhouse.testing.smoke;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.digitalmoneyhouse.testing.config.TestConfig.*;
import static com.digitalmoneyhouse.testing.utils.AuthHelper.getValidToken;
import static com.digitalmoneyhouse.testing.utils.DbHelper.seedStandardTransactionSet;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Digital Money House")
@Feature("Account Service - Smoke")
public class AccountSmokeTest {

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.filters(new AllureRestAssured());
    }

    @Test
    @Disabled("BLOCKED: Endpoint modificado en Sprint 2. Reemplazado por TC-SM-010")
    @Description("TC-SM-004: Consulta de cuenta con token válido retorna datos correctos")
    void tcSm004_consultaCuentaPorUserId_Obsoleto() {
        // Marcado como deshabilitado
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-005: Consulta de saldo disponible de cuenta (GET /accounts/{id})")
    void tcSm005_consultarSaldoDeCuenta_devuelve200ConBalanceValido() {
        given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + TEST_ACCOUNT_ID)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("balance", instanceOf(Number.class))
                .body("balance", greaterThanOrEqualTo(0.0f));
    }

    @BeforeEach
    void seedTransactionData() {
        seedStandardTransactionSet(TEST_ACCOUNT_ID);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-006: Listado de transacciones de cuenta (GET /accounts/{id}/transactions)")
    void tcSm006_listarTransaccionesOrdenadas_devuelve200YArreglo() {
        given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + TEST_ACCOUNT_ID + "/transactions")
                .then()
                .statusCode(200)
                .body("$", isA(java.util.List.class))
                .body("size()", equalTo(3))

                .body("[0].amount", equalTo(1000.00f))
                .body("[0].type", equalTo("INCOME"))

                .body("[1].amount", equalTo(300.00f))
                .body("[1].type", equalTo("TRANSFER_IN"))

                .body("[2].amount", equalTo(200.00f))
                .body("[2].type", equalTo("TRANSFER_OUT"));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-010: Consulta de cuenta por keycloak_id retorna datos correctos")
    void tcSm010_consultarCuentaPorKeycloakId_devuelveDatosCorrectos() {
        given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/user/" + TEST_KEYCLOAK_ID)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("cvu", matchesPattern("\\d{22}"))
                .body("alias", matchesPattern("[a-zA-Z]+\\.[a-zA-Z]+\\.[a-zA-Z]+"))
                .body("userId", equalTo(TEST_KEYCLOAK_ID));
    }
}