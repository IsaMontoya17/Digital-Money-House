package com.digitalmoneyhouse.accountservice.model;

public enum TransactionType {
    INCOME,         // Ingreso desde tarjeta
    TRANSFER_IN,    // Dinero recibido por transferencia
    TRANSFER_OUT    // Dinero enviado por transferencia
}