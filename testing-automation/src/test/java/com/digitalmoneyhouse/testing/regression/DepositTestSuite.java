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
 * Regression: ingreso de dinero desde tarjeta (POST /accounts/{id}/transferences).
 * Cubre TC-RG-059 a TC-RG-065.
 */
@Epic("Digital Money House")
@Feature("Regression - Depósito con tarjeta")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DepositTestSuite {

    private static String testAccountId;
    private static String testCardId;

    private static String otherAccountId;
    private static String otherCardId;

    private static RequestSpecification requestSpec;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;

        registerUser(TEST_EMAIL, "Test", "User", "11221122", "3001234567");
        testAccountId = resolveAccountId(keycloakIdFromToken(getValidToken()), getValidToken());
        testCardId = resolveOrCreateCard(testAccountId, getValidToken());

        String otherToken = registerAndLoginFreshUser("depositother");
        otherAccountId = resolveAccountId(keycloakIdFromToken(otherToken), getValidToken());
        otherCardId = resolveOrCreateCard(otherAccountId, otherToken);

        requestSpec = new RequestSpecBuilder()
                .addFilter(new AllureRestAssured())
                .build();
    }

    private static String resolveOrCreateCard(String accountId, String token) {
        var existing = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/accounts/" + accountId + "/cards");

        if (!existing.jsonPath().getList("$").isEmpty()) {
            return existing.jsonPath().getString("[0].id");
        }

        String cardNumber = "4222" + String.format("%012d", (long) (Math.random() * 1000000000000L));
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(Map.of(
                        "cardNumber", cardNumber, "cardHolderName", "Test User",
                        "expirationDate", "12/2028", "cardType", "DEBIT"
                ))
                .when()
                .post("/accounts/" + accountId + "/cards")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");
    }

    private static double getBalance(String accountId, String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/accounts/" + accountId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getDouble("balance");
    }

    @Test
    @Order(1)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-059: Ingreso exitoso con descripción personalizada")
    void tcRg059_ingresoConDescripcionPersonalizada_devuelve201() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("cardId", Long.parseLong(testCardId), "amount", 500.00, "description", "Depósito de prueba"))
                .when()
                .post("/accounts/" + testAccountId + "/transferences")
                .then()
                .statusCode(201)
                .body("type", equalTo("INCOME"))
                .body("description", equalTo("Depósito de prueba"));
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-060: Ingreso exitoso sin descripción genera descripción automática")
    void tcRg060_sinDescripcion_generaDescripcionAutomatica() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("cardId", Long.parseLong(testCardId), "amount", 200.00))
                .when()
                .post("/accounts/" + testAccountId + "/transferences")
                .then()
                .statusCode(201)
                .body("type", equalTo("INCOME"))
                .body("description", not(emptyOrNullString()));
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-061: Ingreso aumenta el balance de la cuenta")
    void tcRg061_ingresoAumentaBalance() {
        double balanceAntes = getBalance(testAccountId, getValidToken());

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("cardId", Long.parseLong(testCardId), "amount", 300.00))
                .when()
                .post("/accounts/" + testAccountId + "/transferences")
                .then()
                .statusCode(201);

        double balanceDespues = getBalance(testAccountId, getValidToken());

        Assertions.assertEquals(balanceAntes + 300.00, balanceDespues, 0.01);
    }

    @Test
    @Order(4)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-062: Ingreso con monto cero retorna 400")
    void tcRg062_montoCero_devuelve400() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("cardId", Long.parseLong(testCardId), "amount", 0))
                .when()
                .post("/accounts/" + testAccountId + "/transferences")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-063: Ingreso con tarjeta inexistente retorna 404")
    void tcRg063_tarjetaInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("cardId", 999999, "amount", 500.00))
                .when()
                .post("/accounts/" + testAccountId + "/transferences")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-064: Ingreso con tarjeta que no pertenece a la cuenta retorna 403")
    void tcRg064_tarjetaNoPerteneceALaCuenta_devuelve403() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("cardId", Long.parseLong(otherCardId), "amount", 500.00))
                .when()
                .post("/accounts/" + testAccountId + "/transferences")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(7)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-065: Ingreso sin token retorna 401")
    void tcRg065_sinToken_devuelve401() {
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of("cardId", Long.parseLong(testCardId), "amount", 500.00))
                .when()
                .post("/accounts/" + testAccountId + "/transferences")
                .then()
                .statusCode(401);
    }
}