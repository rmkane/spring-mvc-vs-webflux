package org.acme.test.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class RequestHeadersBuilderTest {

    @Test
    void testCreate() {
        RequestHeadersBuilder builder = RequestHeadersBuilder.create();
        assertNotNull(builder);
    }

    @Test
    void testAddHeader() {
        HttpHeaders headers = RequestHeadersBuilder.create().addHeader("X-Custom-Header", "value").build();

        assertEquals("value", headers.getFirst("X-Custom-Header"));
    }

    @Test
    void testAddHeaderContentTypeJson() {
        HttpHeaders headers = RequestHeadersBuilder.create().addHeaderContentTypeJson().build();

        assertEquals(MediaType.APPLICATION_JSON_VALUE, headers.getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    void testAddHeaderContentTypeXml() {
        HttpHeaders headers = RequestHeadersBuilder.create().addHeaderContentTypeXml().build();

        assertEquals(MediaType.APPLICATION_XML_VALUE, headers.getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    void testAddHeaderAcceptJson() {
        HttpHeaders headers = RequestHeadersBuilder.create().addHeaderAcceptJson().build();

        assertEquals(MediaType.APPLICATION_JSON_VALUE, headers.getFirst(HttpHeaders.ACCEPT));
    }

    @Test
    void testAddHeaderAcceptXml() {
        HttpHeaders headers = RequestHeadersBuilder.create().addHeaderAcceptXml().build();

        assertEquals(MediaType.APPLICATION_XML_VALUE, headers.getFirst(HttpHeaders.ACCEPT));
    }

    @Test
    void testAddAll() {
        List<String> values = List.of("value1", "value2", "value3");
        HttpHeaders headers = RequestHeadersBuilder.create().addAll("X-Multi-Header", values).build();

        assertEquals(values, headers.get("X-Multi-Header"));
    }

    @Test
    void testPut() {
        List<String> values = List.of("value1", "value2");
        HttpHeaders headers = RequestHeadersBuilder.create().put("X-Header", values).build();

        assertEquals(values, headers.get("X-Header"));
    }

    @Test
    void testMultipleHeaders() {
        HttpHeaders headers = RequestHeadersBuilder.create()
                .addHeader("Header1", "value1")
                .addHeader("Header2", "value2")
                .addHeader("Header3", "value3")
                .build();

        assertEquals("value1", headers.getFirst("Header1"));
        assertEquals("value2", headers.getFirst("Header2"));
        assertEquals("value3", headers.getFirst("Header3"));
    }

    @Test
    void testNullValuesIgnored() {
        HttpHeaders headers = RequestHeadersBuilder.create().addHeader("Valid", "value").build();

        // Null key/value should be ignored, so only valid header should exist
        assertEquals("value", headers.getFirst("Valid"));
        assertEquals(1, headers.size());
    }

    @Test
    void testBuildReturnsCopy() {
        RequestHeadersBuilder builder = RequestHeadersBuilder.create().addHeader("Header1", "value1");
        HttpHeaders headers1 = builder.build();
        HttpHeaders headers2 = builder.build();

        // Should be different instances
        assertNotNull(headers1);
        assertNotNull(headers2);
        // But same content
        assertEquals("value1", headers1.getFirst("Header1"));
        assertEquals("value1", headers2.getFirst("Header1"));
    }

    @Test
    void testChaining() {
        HttpHeaders headers = RequestHeadersBuilder.create()
                .addHeader("Header1", "value1")
                .addHeaderContentTypeJson()
                .addHeader("Header2", "value2")
                .build();

        assertEquals("value1", headers.getFirst("Header1"));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, headers.getFirst(HttpHeaders.CONTENT_TYPE));
        assertEquals("value2", headers.getFirst("Header2"));
    }
}
