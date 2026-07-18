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
 * Regression: alta, listado, detalle y baja de tarjetas.
 * Cubre TC-RG-023 a TC-RG-033.
 */
@Epic("Digital Money House")
@Feature("Regression - Tarjetas")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CardTestSuite {

    private static String testAccountId;
    private static String freshAccountId; // cuenta sin tarjetas, para TC-RG-027

    private static String createdCardId;
    private static String createdCardNumber;
    private static String freshToken;

    private static RequestSpecification requestSpec;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;

        registerUser(TEST_EMAIL, "Test", "User", "11221122", "3001234567");
        String testKeycloakId = keycloakIdFromToken(getValidToken());
        testAccountId = resolveAccountId(testKeycloakId, getValidToken());

        freshToken = registerAndLoginFreshUser("freshcards");
        freshAccountId = resolveAccountId(keycloakIdFromToken(freshToken), getValidToken());

        requestSpec = new RequestSpecBuilder()
                .addFilter(new AllureRestAssured())
                .build();
    }

    @Test
    @Order(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-023: Creación de tarjeta con datos inválidos retorna 400")
    void tcRg023_datosInvalidos_devuelve400() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of(
                        "cardNumber", "", "cardHolderName", "",
                        "expirationDate", "", "cardType", ""
                ))
                .when()
                .post("/accounts/" + testAccountId + "/cards")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-024: Creación de tarjeta con número de tarjeta inválido retorna 400")
    void tcRg024_numeroInvalido_devuelve400() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of(
                        "cardNumber", "1234", "cardHolderName", "Test",
                        "expirationDate", "12/2027", "cardType", "DEBIT"
                ))
                .when()
                .post("/accounts/" + testAccountId + "/cards")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-025: Creación de tarjeta con número ya registrado retorna 409")
    void tcRg025_numeroYaRegistrado_devuelve409() {
        createdCardNumber = "4111" + String.format("%012d", (long) (Math.random() * 1000000000000L));

        createdCardId = given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of(
                        "cardNumber", createdCardNumber, "cardHolderName", "Test User",
                        "expirationDate", "12/2027", "cardType", "DEBIT"
                ))
                .when()
                .post("/accounts/" + testAccountId + "/cards")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of(
                        "cardNumber", createdCardNumber, "cardHolderName", "Otro Titular",
                        "expirationDate", "12/2028", "cardType", "CREDIT"
                ))
                .when()
                .post("/accounts/" + testAccountId + "/cards")
                .then()
                .statusCode(409);
    }

    @Test
    @Order(4)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-026: Asociar tarjeta a cuenta inexistente retorna 404")
    void tcRg026_cuentaInexistente_devuelve404() {
        String cardNumber = "9999" + String.format("%012d", (long) (Math.random() * 1000000000000L));

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of(
                        "cardNumber", cardNumber, "cardHolderName", "Test User",
                        "expirationDate", "12/2027", "cardType", "DEBIT"
                ))
                .when()
                .post("/accounts/999999/cards")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(5)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-027: Listado de tarjetas de cuenta sin tarjetas devuelve array vacío")
    void tcRg027_sinTarjetas_devuelveArrayVacio() {
        given(requestSpec)
                .header("Authorization", "Bearer " + freshToken)   // <-- antes: getValidToken()
                .when()
                .get("/accounts/" + freshAccountId + "/cards")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @Order(6)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-028: Listado de tarjetas sin token retorna 401")
    void tcRg028_sinToken_devuelve401() {
        given(requestSpec)
                .when()
                .get("/accounts/" + testAccountId + "/cards")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(7)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-029: Detalle de tarjeta existente devuelve datos correctos")
    void tcRg029_detalleTarjetaExistente_devuelveDatosCorrectos() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/cards/" + createdCardId)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("cardNumber", equalTo(createdCardNumber))
                .body("cardHolderName", notNullValue())
                .body("cardType", notNullValue())
                .body("expirationDate", notNullValue());
    }

    @Test
    @Order(8)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-030: Detalle de tarjeta con ID inexistente retorna 404")
    void tcRg030_idInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/cards/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(9)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-031: Eliminación exitosa de tarjeta asociada a cuenta")
    void tcRg031_eliminacionExitosa_eliminaTarjeta() {
        String cardNumber = "5111" + String.format("%012d", (long) (Math.random() * 1000000000000L));

        String cardId = given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of(
                        "cardNumber", cardNumber, "cardHolderName", "Descartable",
                        "expirationDate", "12/2027", "cardType", "DEBIT"
                ))
                .when()
                .post("/accounts/" + testAccountId + "/cards")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .delete("/accounts/" + testAccountId + "/cards/" + cardId)
                .then()
                .statusCode(200);

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/cards/" + cardId)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(10)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-032: Eliminar tarjeta inexistente retorna 404")
    void tcRg032_eliminarInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .delete("/accounts/" + testAccountId + "/cards/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(11)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-033: Eliminar tarjeta sin token retorna 401")
    void tcRg033_sinToken_devuelve401() {
        given(requestSpec)
                .when()
                .delete("/accounts/" + testAccountId + "/cards/" + createdCardId)
                .then()
                .statusCode(401);
    }
}