package com.digitalmoneyhouse.testing.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import static com.digitalmoneyhouse.testing.config.TestConfig.*;

public class DbHelper {

    private static final Logger logger = LoggerFactory.getLogger(DbHelper.class);

    private DbHelper() {
        // Clase utilitaria, no instanciable
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public record TestTransaction(
            int id,
            double amount,
            String type,
            String createdAt,
            String description,
            String originCvu,
            String destCvu
    ) {
        public static TestTransaction income(int id, double amount, String createdAt, String description) {
            return new TestTransaction(id, amount, "INCOME", createdAt, description, null, null);
        }

        public static TestTransaction transferIn(int id, double amount, String createdAt,
                                                 String description, String originCvu, String destCvu) {
            return new TestTransaction(id, amount, "TRANSFER_IN", createdAt, description, originCvu, destCvu);
        }

        public static TestTransaction transferOut(int id, double amount, String createdAt,
                                                  String description, String originCvu, String destCvu) {
            return new TestTransaction(id, amount, "TRANSFER_OUT", createdAt, description, originCvu, destCvu);
        }
    }

    public static void clearTransactions(String accountId) {
        String sql = "DELETE FROM transactions WHERE account_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Integer.parseInt(accountId));
            int deleted = stmt.executeUpdate();
            logger.info("Transacciones eliminadas para account_id={}: {}", accountId, deleted);

        } catch (SQLException e) {
            throw new RuntimeException("Error al limpiar transacciones de cuenta " + accountId, e);
        }
    }

    public static void seedTransactions(String accountId, TestTransaction... transactions) {
        String sql = "INSERT INTO transactions " +
                "(id, account_id, amount, type, created_at, description, origin_cvu, dest_cvu) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (TestTransaction t : transactions) {
                stmt.setInt(1, t.id());
                stmt.setInt(2, Integer.parseInt(accountId));
                stmt.setDouble(3, t.amount());
                stmt.setString(4, t.type());
                stmt.setTimestamp(5, Timestamp.valueOf(t.createdAt()));
                stmt.setString(6, t.description());
                if (t.originCvu() != null) {
                    stmt.setString(7, t.originCvu());
                } else {
                    stmt.setNull(7, java.sql.Types.VARCHAR);
                }
                if (t.destCvu() != null) {
                    stmt.setString(8, t.destCvu());
                } else {
                    stmt.setNull(8, java.sql.Types.VARCHAR);
                }
                stmt.addBatch();
            }

            stmt.executeBatch();
            logger.info("Insertadas {} transacciones de prueba para account_id={}",
                    transactions.length, accountId);

        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar transacciones de prueba en cuenta " + accountId, e);
        }
    }

    public static void seedStandardTransactionSet(String accountId) {
        clearTransactions(accountId);
        seedTransactions(accountId,
                TestTransaction.income(101, 1000.00, "2026-06-16 02:32:44", "Deposito de prueba"),
                TestTransaction.transferIn(102, 300.00, "2026-06-12 09:00:00",
                        "Transferencia recibida", "1234567890123456789012", "4907349814412647186490"),
                TestTransaction.transferOut(103, 200.00, "2026-06-11 14:00:00",
                        "Transferencia enviada", "4907349814412647186490", "1234567890123456789012")
        );
    }
}