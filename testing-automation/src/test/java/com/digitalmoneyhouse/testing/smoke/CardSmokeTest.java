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
@Feature("Cards - Smoke")
public class CardSmokeTest {

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.filters(new AllureRestAssured());
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-008: Crear y asociar una tarjeta exitosamente (POST /accounts/{id}/cards)")
    void tcSm008_crearYAsociarTarjeta_devuelve201() {
        long timestamp = System.currentTimeMillis();
        String tarjetaDinamica = "4111" + String.format("%012d", (long) (Math.random() * 1000000000000L));

        Map<String, Object> cardBody = Map.of(
                "cardNumber", tarjetaDinamica,
                "cardHolderName", "Test User",
                "expirationDate", "12/2028",
                "cardType", "DEBIT",
                "cvv", 123
        );

        given()
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(cardBody)
                .when()
                .post("/accounts/" + TEST_ACCOUNT_ID + "/cards")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("cardNumber", equalTo(tarjetaDinamica));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-009: Listado de tarjetas de una cuenta (GET /accounts/{id}/cards)")
    void tcSm009_listarTarjetasDeCuenta_devuelve200ConElementos() {
        given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + TEST_ACCOUNT_ID + "/cards")
                .then()
                .statusCode(200)
                .body("$", isA(java.util.List.class))
                .body("size()", greaterThan(0));
    }
}