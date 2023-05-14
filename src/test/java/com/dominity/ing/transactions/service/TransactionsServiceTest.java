package com.dominity.ing.transactions.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionsServiceTest {
    @Test
    void testExample1() throws IOException {
        // given
        var requestJson = "[{\"debitAccount\": \"32309111922661937852684864\",\"creditAccount\": \"06105023389842834748547303\",\"amount\": 10.9},{\"debitAccount\": \"31074318698137062235845814\",\"creditAccount\": \"66105036543749403346524547\",\"amount\": 200.9},{\"debitAccount\": \"66105036543749403346524547\",\"creditAccount\": \"32309111922661937852684864\",\"amount\": 50.1}]";
        var responseJson = "[{\"account\":\"06105023389842834748547303\",\"debitCount\":0,\"creditCount\":1,\"balance\":10.9},{\"account\":\"31074318698137062235845814\",\"debitCount\":1,\"creditCount\":0,\"balance\":-200.9},{\"account\":\"32309111922661937852684864\",\"debitCount\":1,\"creditCount\":1,\"balance\":39.2},{\"account\":\"66105036543749403346524547\",\"debitCount\":1,\"creditCount\":1,\"balance\":150.8}]";
        var outputStream = new ByteArrayOutputStream();
        // when
        new TransactionsService().prepareTotal(new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8)), outputStream);
        // then
        assertThat(outputStream.toByteArray()).isEqualTo(responseJson.getBytes(StandardCharsets.UTF_8));
    }
}