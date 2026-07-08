package com.digitalmoneyhouse.testing.smoke;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import java.util.Base64;
import java.util.Map;
import java.util.Random;

import static com.digitalmoneyhouse.testing.config.TestConfig.*;
import static com.digitalmoneyhouse.testing.utils.AuthHelper.getValidToken;
import static com.digitalmoneyhouse.testing.utils.AuthHelper.invalidateCache;
import static com.digitalmoneyhouse.testing.utils.DbHelper.seedStandardTransactionSet;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Digital Money House")
@Feature("Smoke Test Suite")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SmokeTestSuite {

    private static String testUserId;
    private static String testAccountId;
    private static String testKeycloakId;

    private static RequestSpecification requestSpec;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;

        registerTestUser();
        resolveTestIds();

        requestSpec = new RequestSpecBuilder()
                .addFilter(new AllureRestAssured())
                .build();
    }

    private static void registerTestUser() {
        Response loginResponse = given()
                .contentType("application/json")
                .body(Map.of("email", TEST_EMAIL, "password", TEST_PASSWORD))
                .when()
                .post("/auth/login");

        if (loginResponse.getStatusCode() == 200) {
            return;
        }

        given()
                .contentType("application/json")
                .body(Map.of(
                        "firstName", "Test",
                        "lastName", "User",
                        "dni", "11221122",
                        "email", TEST_EMAIL,
                        "phone", "3001234567",
                        "password", TEST_PASSWORD
                ))
                .when()
                .post("/users/register")
                .then()
                .statusCode(201);
    }

    private static void resolveTestIds() {
        String token = getValidToken();

        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        testKeycloakId = payload.split("\"sub\":\"")[1].split("\"")[0];

        Response accountResponse = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/accounts/user/" + testKeycloakId);

        testAccountId = accountResponse.jsonPath().getString("id");
        testUserId = getUserIdFromAccount(testAccountId);
    }

    private static String getUserIdFromAccount(String accountId) {
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT id FROM users WHERE keycloak_id = ?";
            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, testKeycloakId);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return String.valueOf(rs.getInt("id"));
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudo obtener el userId desde la base de datos", e);
        }
        throw new RuntimeException("Usuario de prueba no encontrado en la base de datos");
    }

    // ─── TESTS ───────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-001: Registro exitoso de usuario con datos válidos")
    void tcSm001_registroExitosoUsuario_devuelve201YDatos() {
        long timestamp = System.currentTimeMillis();
        String emailDinamico = "nuevo" + timestamp + "@test.com";
        String dniDinamico = String.valueOf(timestamp).substring(5);

        Map<String, String> userBody = Map.of(
                "firstName", "Test",
                "lastName", "User",
                "dni", dniDinamico,
                "email", emailDinamico,
                "phone", "3001234567",
                "password", TEST_PASSWORD
        );

        given(requestSpec)
                .contentType("application/json")
                .body(userBody)
                .when()
                .post("/users/register")
                .then()
                .statusCode(201)
                .body("$", not(hasKey("password")))
                .body("id", notNullValue())
                .body("cvu", matchesPattern("\\d{22}"))
                .body("alias", matchesPattern("[a-zA-Z]+\\.[a-zA-Z]+\\.[a-zA-Z]+"));
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-002: Login exitoso con credenciales válidas retorna token JWT")
    void tcSm002_loginExitoso_retornaTokenJWT() {
        given(requestSpec)
                .contentType("application/json")
                .body(Map.of("email", TEST_EMAIL, "password", TEST_PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", notNullValue());
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-003: Logout con token válido retorna status 200")
    void tcSm003_logoutConTokenValido_retorna200() {
        String respuesta = given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(200)
                .extract().asString();

        Assertions.assertEquals("Sesión cerrada exitosamente", respuesta);
        invalidateCache();
    }

    @Test
    @Order(4)
    @Disabled("BLOCKED: Endpoint modificado en Sprint 2. Reemplazado por TC-SM-010")
    @Description("TC-SM-004: BLOCKED - Reemplazado por TC-SM-010")
    void tcSm004_obsoleto() {}

    @Test
    @Order(5)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-005: Consulta de saldo disponible de cuenta (GET /accounts/{id})")
    void tcSm005_consultarSaldoDeCuenta_devuelve200ConBalanceValido() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("balance", instanceOf(Number.class))
                .body("balance", greaterThanOrEqualTo(0.0f));
    }

    @Test
    @Order(6)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-006: Listado de transacciones de cuenta (GET /accounts/{id}/transactions)")
    void tcSm006_listarTransaccionesOrdenadas_devuelve200YArreglo() {
        seedStandardTransactionSet(testAccountId);

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/transactions")
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
    @Order(7)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-007: Consulta de perfil de usuario (GET /users/{id})")
    void tcSm007_consultarPerfilDeUsuario_devuelve200() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/users/" + testUserId)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("cvu", matchesPattern("\\d{22}"))
                .body("alias", matchesPattern("[a-zA-Z]+\\.[a-zA-Z]+\\.[a-zA-Z]+"))
                .body("email", equalTo(TEST_EMAIL));
    }

    @Test
    @Order(8)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-008: Crear y asociar una tarjeta exitosamente (POST /accounts/{id}/cards)")
    void tcSm008_crearYAsociarTarjeta_devuelve201() {
        String tarjetaDinamica = "4111" + String.format("%012d",
                (long) (Math.random() * 1000000000000L));

        Map<String, Object> cardBody = Map.of(
                "cardNumber", tarjetaDinamica,
                "cardHolderName", "Test User",
                "expirationDate", "12/2028",
                "cardType", "DEBIT"
        );

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(cardBody)
                .when()
                .post("/accounts/" + testAccountId + "/cards")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("cardNumber", equalTo(tarjetaDinamica));
    }

    @Test
    @Order(9)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-009: Listado de tarjetas de una cuenta (GET /accounts/{id}/cards)")
    void tcSm009_listarTarjetasDeCuenta_devuelve200ConElementos() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/cards")
                .then()
                .statusCode(200)
                .body("$", isA(java.util.List.class))
                .body("size()", greaterThan(0));
    }

    @Test
    @Order(10)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-010: Consulta de cuenta por keycloak_id retorna datos correctos")
    void tcSm010_consultarCuentaPorKeycloakId_devuelveDatosCorrectos() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/user/" + testKeycloakId)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("cvu", matchesPattern("\\d{22}"))
                .body("alias", matchesPattern("[a-zA-Z]+\\.[a-zA-Z]+\\.[a-zA-Z]+"))
                .body("userId", equalTo(testKeycloakId));
    }

    @Test
    @Order(11)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-011: Actualización de perfil de usuario (PATCH /users/{id})")
    void tcSm011_actualizarPerfilUsuario_devuelve200ConDatosActualizados() {
        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("firstName", "NuevoNombre"))
                .when()
                .patch("/users/" + testUserId)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("firstName", equalTo("NuevoNombre"))
                .body("email", equalTo(TEST_EMAIL))
                .body("cvu", matchesPattern("\\d{22}"))
                .body("alias", matchesPattern("[a-zA-Z]+\\.[a-zA-Z]+\\.[a-zA-Z]+"));
    }

    @Test
    @Order(12)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-012: Actualización de alias de cuenta (PATCH /accounts/{id})")
    void tcSm012_actualizarAliasDeCuenta_devuelve200ConAliasActualizado() {
        String[] palabras = {"perro", "gato", "casa", "auto", "luna", "sol", "mar", "rio", "monte", "campo"};
        Random random = new Random();
        String nuevoAlias = palabras[random.nextInt(palabras.length)] + "." +
                palabras[random.nextInt(palabras.length)] + "." +
                palabras[random.nextInt(palabras.length)];

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of("alias", nuevoAlias))
                .when()
                .patch("/accounts/" + testAccountId)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("alias", equalTo(nuevoAlias))
                .body("cvu", notNullValue())
                .body("userId", equalTo(testKeycloakId));
    }

    @Test
    @Order(13)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-013: Consulta de actividad de cuenta (GET /accounts/{id}/activity)")
    void tcSm013_consultarActividadDeCuenta_devuelve200YArregloOrdenado() {
        seedStandardTransactionSet(testAccountId);

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/activity")
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
    @Order(14)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-014: Consulta de detalle de transacción (GET /accounts/{id}/activity/{transactionId})")
    void tcSm014_consultarDetalleDeTransaccion_devuelve200ConDatos() {
        seedStandardTransactionSet(testAccountId);

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
                .body("description", equalTo("Deposito de prueba"));
    }

    @Test
    @Order(15)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-015: Ingreso de dinero desde tarjeta (POST /accounts/{id}/transferences)")
    void tcSm015_ingresoDineroDeseTarjeta_devuelve201ConTransaccion() {
        Integer cardId = given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/accounts/" + testAccountId + "/cards")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getInt("[0].id");

        given(requestSpec)
                .header("Authorization", "Bearer " + getValidToken())
                .contentType("application/json")
                .body(Map.of(
                        "cardId", cardId,
                        "amount", 500.00,
                        "description", "Depósito de prueba smoke"
                ))
                .when()
                .post("/accounts/" + testAccountId + "/transferences")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("amount", equalTo(500.00f))
                .body("type", equalTo("INCOME"))
                .body("description", equalTo("Depósito de prueba smoke"));
    }
}