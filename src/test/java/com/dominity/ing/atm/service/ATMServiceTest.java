package com.dominity.ing.atm.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ATMServiceTest {
    private final ATMService service = new ATMService();

    @Test
    void testExample1() throws IOException {
        // given
        var requestJson = "[{\"region\":4,\"requestType\":\"STANDARD\",\"atmId\":1},{\"region\":1,\"requestType\":\"STANDARD\",\"atmId\":1},{\"region\":2,\"requestType\":\"STANDARD\",\"atmId\":1},{\"region\":3,\"requestType\":\"PRIORITY\",\"atmId\":2},{\"region\":3,\"requestType\":\"STANDARD\",\"atmId\":1},{\"region\":2,\"requestType\":\"SIGNAL_LOW\",\"atmId\":1},{\"region\":5,\"requestType\":\"STANDARD\",\"atmId\":2},{\"region\":5,\"requestType\":\"FAILURE_RESTART\",\"atmId\":1}]";
        var responseJson = "[{\"region\":1,\"atmId\":1},{\"region\":2,\"atmId\":1},{\"region\":3,\"atmId\":2},{\"region\":3,\"atmId\":1},{\"region\":4,\"atmId\":1},{\"region\":5,\"atmId\":1},{\"region\":5,\"atmId\":2}]";
        var outputStream = new ByteArrayOutputStream();
        // when
        service.prepareRoute(new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8)), outputStream);
        // then
        assertThat(outputStream.toByteArray()).isEqualTo(responseJson.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testExample2() throws IOException {
        // given
        var requestJson = "[{\"region\":1,\"requestType\":\"STANDARD\",\"atmId\":2},{\"region\":1,\"requestType\":\"STANDARD\",\"atmId\":1},{\"region\":2,\"requestType\":\"PRIORITY\",\"atmId\":3},{\"region\":3,\"requestType\":\"STANDARD\",\"atmId\":4},{\"region\":4,\"requestType\":\"STANDARD\",\"atmId\":5},{\"region\":5,\"requestType\":\"PRIORITY\",\"atmId\":2},{\"region\":5,\"requestType\":\"STANDARD\",\"atmId\":1},{\"region\":3,\"requestType\":\"SIGNAL_LOW\",\"atmId\":2},{\"region\":2,\"requestType\":\"SIGNAL_LOW\",\"atmId\":1},{\"region\":3,\"requestType\":\"FAILURE_RESTART\",\"atmId\":1}]";
        var responseJson = "[{\"region\":1,\"atmId\":2},{\"region\":1,\"atmId\":1},{\"region\":2,\"atmId\":3},{\"region\":2,\"atmId\":1},{\"region\":3,\"atmId\":1},{\"region\":3,\"atmId\":2},{\"region\":3,\"atmId\":4},{\"region\":4,\"atmId\":5},{\"region\":5,\"atmId\":2},{\"region\":5,\"atmId\":1}]";
        var outputStream = new ByteArrayOutputStream();
        // when
        service.prepareRoute(new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8)), outputStream);
        // then
        assertThat(outputStream.toByteArray()).isEqualTo(responseJson.getBytes(StandardCharsets.UTF_8));
    }
}
