package com.digitalmoneyhouse.testing.regression;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import java.util.Base64;
import java.util.Map;

import static com.digitalmoneyhouse.testing.config.TestConfig.*;
import static com.digitalmoneyhouse.testing.utils.AuthHelper.getValidToken;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Digital Money House")
@Feature("Regression - Transferencias entre cuentas")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransferTestSuite {

    private static String testAccountId;
    private static String testKeycloakId;

    private static String destinationAccountId;
    private static String destinationCvu;
    private static String destinationAlias;
    private static String destinationToken;

    private static String testCardId;

    private static RequestSpecification requestSpec;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;

        registerTestUser(TEST_EMAIL, "Test", "User", "11221122", "3001234567");
        testKeycloakId = keycloakIdFromToken(getValidToken());

        Response accountResponse = given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/user/" + testKeycloakId);
        testAccountId = accountResponse.jsonPath().getString("id");

        resolveDestinationAccount();
        resolveTestCard();
        fundOriginAccount(50000.00);

        requestSpec = new RequestSpecBuilder()
                .addFilter(new AllureRestAssured())
                .build();
    }

    private static void registerTestUser(String email, String firstName, String lastName, String dni, String phone) {
        Response loginResponse = given()
                .contentType("application/json")
                .body(Map.of("email", email, "password", TEST_PASSWORD))
                .when()
                .post("/auth/login");

        if (loginResponse.getStatusCode() == 200) {
            return;
        }

        given()
                .contentType("application/json")
                .body(Map.of(
                        "firstName", firstName,
                        "lastName", lastName,
                        "dni", dni,
                        "email", email,
                        "phone", phone,
                        "password", TEST_PASSWORD
                ))
                .when()
                .post("/users/register")
                .then()
                .statusCode(201);
    }

    private static String keycloakIdFromToken(String token) {
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        return payload.split("\"sub\":\"")[1].split("\"")[0];
    }

    private static void resolveDestinationAccount() {
        long timestamp = System.currentTimeMillis();
        String destinationEmail = "destinoregression" + timestamp + "@test.com";
        String destinationDni = String.valueOf(timestamp).substring(4);

        registerTestUser(destinationEmail, "Destino", "Regression", destinationDni, "3009876543");

        Response loginResponse = given()
                .contentType("application/json")
                .body(Map.of("email", destinationEmail, "password", TEST_PASSWORD))
                .when()
                .post("/auth/login");

        destinationToken = loginResponse.jsonPath().getString("accessToken");
        String destinationKeycloakId = keycloakIdFromToken(destinationToken);

        Response accountResponse = given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/user/" + destinationKeycloakId);

        destinationAccountId = accountResponse.jsonPath().getString("id");
        destinationCvu = accountResponse.jsonPath().getString("cvu");
        destinationAlias = accountResponse.jsonPath().getString("alias");
    }

    private static void resolveTestCard() {
        Response cardsResponse = given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/cards");

        if (cardsResponse.jsonPath().getList("$").isEmpty()) {
            String tarjetaDinamica = "4111" + String.format("%012d",
                    (long) (Math.random() * 1000000000000L));

            testCardId = given()
                    .header("Authorization", "Bearer " + getValidToken())
                    .contentType("application/json")
                    .body(Map.of(
                            "cardNumber", tarjetaDinamica,
                            "cardHolderName", "Test User",
                            "expirationDate", "12/2028",
                            "cardType", "DEBIT"
                    ))
                    .when()
                    .post("/accounts/" + testAccountId + "/cards")
                    .then()
                    .statusCode(201)
                    .extract()
                    .jsonPath()
                    .getString("id");
        } else {
            testCardId = cardsResponse.jsonPath().getString("[0].id");
        }
    }

    private static void fundOriginAccount(double amount) {
        given()
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("cardId", Long.parseLong(testCardId), "amount", amount))
                .when()
                .post("/accounts/" + testAccountId + "/transferences")
                .then()
                .statusCode(201);
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

    // ─── GET /accounts/{id}/transfers ──────────────────────────────────────

    @Test
    @Order(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-066: Consulta de últimos destinatarios con destinatarios existentes retorna 200")
    void tcRg066_consultarDestinatarios_devuelve200() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(200)
                .body("$", isA(java.util.List.class));
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-067: Consulta de últimos destinatarios en cuenta sin transferencias retorna lista vacía")
    void tcRg067_sinTransferenciasPrevias_devuelveListaVacia() {
        given(requestSpec)
                .header("Authorization", "Bearer " + destinationToken)
                .when()
                .get("/accounts/" + destinationAccountId + "/transfers")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-068: Consulta de últimos destinatarios de cuenta ajena retorna 403")
    void tcRg068_cuentaAjena_devuelve403() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + destinationAccountId + "/transfers")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(4)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-069: Consulta de últimos destinatarios con id de cuenta inexistente retorna 404")
    void tcRg069_cuentaInexistente_devuelve404() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/999999/transfers")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(5)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-070: Consulta de últimos destinatarios sin token retorna 401")
    void tcRg070_sinToken_devuelve401() {
        given(requestSpec)
                .when()
                .get("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(6)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-071: Destinatario con múltiples transferencias aparece una sola vez (deduplicado)")
    void tcRg071_destinatarioRepetido_apareceDeduplicado() {
        for (int i = 0; i < 3; i++) {
            given(requestSpec)
                    .header("Authorization", "Bearer " + getValidToken())
                    .contentType("application/json")
                    .body(Map.of("destination", destinationAlias, "amount", 10.00 + i))
                    .when()
                    .post("/accounts/" + testAccountId + "/transfers")
                    .then()
                    .statusCode(201);
        }

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(200)
                .body("findAll { it.cvu == '" + destinationCvu + "' }.size()", equalTo(1));
    }

    @Test
    @Order(7)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-072: Lista de últimos destinatarios no supera el máximo de 5")
    void tcRg072_masDe5Destinatarios_devuelveMaximo5() {
        for (int i = 0; i < 6; i++) {
            long ts = System.currentTimeMillis() + i;
            String email = "dest" + ts + "@test.com";
            registerTestUser(email, "Dest", "Rg072", String.valueOf(ts).substring(4), "3000000000");

            Response login = given()
                    .contentType("application/json")
                    .body(Map.of("email", email, "password", TEST_PASSWORD))
                    .when()
                    .post("/auth/login");
            String kcId = keycloakIdFromToken(login.jsonPath().getString("accessToken"));

            String alias = given()
                    .header("Authorization", "Bearer " + getValidToken())
                    .when()
                    .get("/accounts/user/" + kcId)
                    .jsonPath()
                    .getString("alias");

            given(requestSpec)
                    .header("Authorization", "Bearer " + getValidToken())
                    .contentType("application/json")
                    .body(Map.of("destination", alias, "amount", 5.00))
                    .when()
                    .post("/accounts/" + testAccountId + "/transfers")
                    .then()
                    .statusCode(201);
        }

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(200)
                .body("size()", lessThanOrEqualTo(5));
    }

    // ─── POST /accounts/{id}/transfers ─────────────────────────────────────

    @Test
    @Order(8)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-073: Transferencia exitosa usando alias como destino retorna 201")
    void tcRg073_transferenciaConAlias_devuelve201() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("destination", destinationAlias, "amount", 100.00))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(201)
                .body("type", equalTo("TRANSFER_OUT"))
                .body("destCvu", equalTo(destinationCvu));
    }

    @Test
    @Order(9)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-074: Transferencia exitosa usando CVU como destino retorna 201")
    void tcRg074_transferenciaConCvu_devuelve201() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("destination", destinationCvu, "amount", 100.00))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(201)
                .body("type", equalTo("TRANSFER_OUT"))
                .body("destCvu", equalTo(destinationCvu));
    }

    @Test
    @Order(10)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-075: Transferencia con monto mayor al saldo disponible retorna 410")
    void tcRg075_fondosInsuficientes_devuelve410() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("destination", destinationAlias, "amount", 999999999.00))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(410);
    }

    @Test
    @Order(11)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-076: Transferencia a alias/CVU inexistente retorna 400")
    void tcRg076_destinoInexistente_devuelve400() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("destination", "no.existe.alias", "amount", 50.00))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(12)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-077: Transferencia a la propia cuenta (origen = destino) retorna 400")
    void tcRg077_transferenciaAsiMismo_devuelve400() {
        String ownAlias = given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/user/" + testKeycloakId)
                .jsonPath()
                .getString("alias");

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("destination", ownAlias, "amount", 50.00))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(13)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-078: Transferencia con monto negativo o en cero retorna 400")
    void tcRg078_montoInvalido_devuelve400() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("destination", destinationAlias, "amount", -10.00))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(14)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-079: Transferencia sin el campo destination retorna 400")
    void tcRg079_sinDestination_devuelve400() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("amount", 50.00))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(15)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-080: Transferencia sin el campo amount retorna 400")
    void tcRg080_sinAmount_devuelve400() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("destination", destinationAlias))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(16)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-081: Transferencia sobre cuenta ajena retorna 403")
    void tcRg081_cuentaAjena_devuelve403() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("destination", destinationAlias, "amount", 50.00))
                .when()
                .post("/accounts/" + destinationAccountId + "/transfers")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(17)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-082: Transferencia sin token retorna 401")
    void tcRg082_sinToken_devuelve401() {
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of("destination", destinationAlias, "amount", 50.00))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(18)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-083: Transferencia exitosa debita el balance de la cuenta origen")
    void tcRg083_debitaBalanceOrigen() {
        double balanceAntes = getBalance(testAccountId, getValidToken());

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("destination", destinationAlias, "amount", 77.00))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(201);

        double balanceDespues = getBalance(testAccountId, getValidToken());

        System.out.println("TC-RG-083 -> Balance origen antes: " + balanceAntes
                + " | después: " + balanceDespues
                + " | diferencia esperada: -77.00");

        Assertions.assertEquals(balanceAntes - 77.00, balanceDespues, 0.01);
    }

    @Test
    @Order(19)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-RG-084: Transferencia exitosa acredita el balance de la cuenta destino")
    void tcRg084_acreditaBalanceDestino() {
        double balanceAntes = getBalance(destinationAccountId, destinationToken);

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("destination", destinationAlias, "amount", 33.00))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(201);

        double balanceDespues = getBalance(destinationAccountId, destinationToken);

        System.out.println("TC-RG-084 -> Balance destino antes: " + balanceAntes
                + " | después: " + balanceDespues
                + " | diferencia esperada: +33.00");

        Assertions.assertEquals(balanceAntes + 33.00, balanceDespues, 0.01);
    }

    @Test
    @Order(20)
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-RG-085: Transferencia exitosa genera transacción TRANSFER_IN en la cuenta destino")
    void tcRg085_generaTransferInEnDestino() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("destination", destinationAlias, "amount", 21.00))
                .when()
                .post("/accounts/" + testAccountId + "/transfers")
                .then()
                .statusCode(201);

        given(requestSpec)
                .header("Authorization", "Bearer " + destinationToken)
                .when()
                .get("/accounts/" + destinationAccountId + "/activity")
                .then()
                .statusCode(200)
                .body("findAll { it.type == 'TRANSFER_IN' && it.amount == 21.00f }.size()", greaterThanOrEqualTo(1));
    }
}