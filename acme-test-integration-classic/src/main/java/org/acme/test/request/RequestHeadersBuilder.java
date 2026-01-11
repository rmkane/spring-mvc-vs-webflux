package org.acme.test.request;

import java.util.Collection;
import java.util.List;

import org.apache.http.entity.ContentType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.acme.test.util.MapUtils;

/** Simple builder for HTTP headers backed by a {@link MultiValueMap}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestHeadersBuilder {

    /** Environment variable name for SSL client subject DN. */
    private static final String SSL_CLIENT_SUBJECT_DN_ENV = "SSL_CLIENT_SUBJECT_DN";
    /** Environment variable name for SSL client issuer DN. */
    private static final String SSL_CLIENT_ISSUER_DN_ENV = "SSL_CLIENT_ISSUER_DN";

    /** Header name for SSL client subject DN from X509 certificate. */
    private static final String SSL_CLIENT_SUBJECT_DN_HEADER = "ssl-client-subject-dn";
    /** Header name for SSL client issuer DN from X509 certificate. */
    private static final String SSL_CLIENT_ISSUER_DN_HEADER = "ssl-client-issuer-dn";

    @NonNull
    private final LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

    /**
     * Creates a new RequestHeadersBuilder instance.
     *
     * @return A new RequestHeadersBuilder
     */
    public static RequestHeadersBuilder create() {
        return new RequestHeadersBuilder();
    }

    /**
     * Adds a header to the request.
     *
     * @param name  The header name
     * @param value The header value
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addHeader(String name, String value) {
        MapUtils.addIf(headers, name, value);
        return this;
    }

    /**
     * Sets the Content-Type header.
     *
     * @param mediaType The media type string
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addHeaderContentType(String mediaType) {
        return addHeader(HttpHeaders.CONTENT_TYPE, mediaType);
    }

    /**
     * Sets the Content-Type header.
     *
     * @param mediaType The media type
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addHeaderContentType(MediaType mediaType) {
        return addHeaderContentType(mediaType.toString());
    }

    /**
     * Sets the Content-Type header.
     *
     * @param contentType The content type
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addHeaderContentType(ContentType contentType) {
        return addHeaderContentType(contentType.toString());
    }

    /**
     * Sets the Content-Type header to application/xml.
     *
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addHeaderContentTypeXml() {
        return addHeaderContentType(MediaType.APPLICATION_XML_VALUE);
    }

    /**
     * Sets the Content-Type header to application/json.
     *
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addHeaderContentTypeJson() {
        return addHeaderContentType(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * Sets the Accept header.
     *
     * @param mediaType The media type string
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addHeaderAccept(String mediaType) {
        return addHeader(HttpHeaders.ACCEPT, mediaType);
    }

    /**
     * Sets the Accept header.
     *
     * @param mediaType The media type
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addHeaderAccept(MediaType mediaType) {
        return addHeaderAccept(mediaType.toString());
    }

    /**
     * Sets the Accept header.
     *
     * @param contentType The content type
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addHeaderAccept(ContentType contentType) {
        return addHeaderAccept(contentType.toString());
    }

    /**
     * Sets the Accept header to application/xml.
     *
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addHeaderAcceptXml() {
        return addHeaderAccept(MediaType.APPLICATION_XML_VALUE);
    }

    /**
     * Sets the Accept header to application/json.
     *
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addHeaderAcceptJson() {
        return addHeaderAccept(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * Adds default SSL client headers (ssl-client-subject-dn and
     * ssl-client-issuer-dn) from environment variables. This is a convenience
     * method for integration tests.
     *
     * @return This builder for method chaining
     * @throws IllegalArgumentException if required environment variables are not
     *                                  set
     */
    public RequestHeadersBuilder withDefaultHeaders() {
        String subjectDn = getEnvRequired(SSL_CLIENT_SUBJECT_DN_ENV);
        String issuerDn = getEnvRequired(SSL_CLIENT_ISSUER_DN_ENV);
        return addHeader(SSL_CLIENT_SUBJECT_DN_HEADER, subjectDn)
                .addHeader(SSL_CLIENT_ISSUER_DN_HEADER, issuerDn);
    }

    /**
     * Adds multiple values to a header.
     *
     * @param name   The header name
     * @param values The header values
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder addAll(String name, Collection<String> values) {
        MapUtils.addAllIf(headers, name, values);
        return this;
    }

    /**
     * Sets a header with multiple values, replacing any existing values.
     *
     * @param name   The header name
     * @param values The header values
     * @return This builder for method chaining
     */
    public RequestHeadersBuilder put(String name, List<String> values) {
        MapUtils.putIf(headers, name, values);
        return this;
    }

    /**
     * Builds and returns a read-only copy of the headers.
     *
     * @return A read-only HttpHeaders instance
     */
    @NonNull
    public HttpHeaders build() {
        return HttpHeaders.readOnlyHttpHeaders(headers);
    }

    /**
     * Gets a required environment variable, throwing an exception if it's not set.
     *
     * @param variableName The name of the environment variable
     * @return The value of the environment variable
     * @throws IllegalArgumentException if the environment variable is not set or is
     *                                  empty
     */
    private String getEnvRequired(String variableName) {
        String value = System.getenv(variableName);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Environment variable '" + variableName + "' is not set");
        }
        return value;
    }
}
