package com.digitalmoneyhouse.testing.smoke;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import static com.digitalmoneyhouse.testing.config.TestConfig.*;
import static com.digitalmoneyhouse.testing.utils.AuthHelper.getValidToken;
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

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-SM-006: Listado de transacciones de cuenta (GET /accounts/{id}/transactions)")
    void tcSm006_listarTransaccionesOrdenadas_devuelve200YArreglo() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM transactions WHERE account_id = ?")) {
                deleteStmt.setInt(1, Integer.parseInt(TEST_ACCOUNT_ID));
                deleteStmt.executeUpdate();
            }

            String sqlInsert = "INSERT INTO transactions (id, account_id, amount, type, created_at, description, origin_cvu, dest_cvu) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(sqlInsert)) {

                // Transacción 1
                insertStmt.setInt(1, 101);
                insertStmt.setInt(2, Integer.parseInt(TEST_ACCOUNT_ID));
                insertStmt.setDouble(3, 1000.00);
                insertStmt.setString(4, "INCOME");
                insertStmt.setTimestamp(5, java.sql.Timestamp.valueOf("2026-06-16 02:32:44"));
                insertStmt.setString(6, "Deposito de prueba");
                insertStmt.setNull(7, java.sql.Types.VARCHAR);
                insertStmt.setNull(8, java.sql.Types.VARCHAR);
                insertStmt.addBatch();

                // Transacción 2
                insertStmt.setInt(1, 102);
                insertStmt.setInt(2, Integer.parseInt(TEST_ACCOUNT_ID));
                insertStmt.setDouble(3, 300.00);
                insertStmt.setString(4, "TRANSFER_IN");
                insertStmt.setTimestamp(5, java.sql.Timestamp.valueOf("2026-06-12 09:00:00"));
                insertStmt.setString(6, "Transferencia recibida");
                insertStmt.setString(7, "1234567890123456789012");
                insertStmt.setString(8, "4907349814412647186490");
                insertStmt.addBatch();

                // Transacción 3
                insertStmt.setInt(1, 103);
                insertStmt.setInt(2, Integer.parseInt(TEST_ACCOUNT_ID));
                insertStmt.setDouble(3, 200.00);
                insertStmt.setString(4, "TRANSFER_OUT");
                insertStmt.setTimestamp(5, java.sql.Timestamp.valueOf("2026-06-11 14:00:00"));
                insertStmt.setString(6, "Transferencia enviada");
                insertStmt.setString(7, "4907349814412647186490");
                insertStmt.setString(8, "1234567890123456789012");
                insertStmt.addBatch();

                insertStmt.executeBatch();
            }
        } catch (java.sql.SQLException e) {
            org.junit.jupiter.api.Assertions.fail("FALLÓ SQL. Estado: " + e.getSQLState() + " | Código: " + e.getErrorCode() + " | Mensaje: " + e.getMessage());
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.fail("FALLÓ CONEXIÓN U OTRO ERROR: " + e.getMessage());
        }

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