package org.acme.test.request;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.acme.test.util.MapUtils;

/** Builder for {@link RestRequest} instances. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RestRequestBuilder {
    @NonNull
    private String baseUrl;
    @Nullable
    private String endpoint;
    @NonNull
    private HttpMethod method = Objects.requireNonNull(HttpMethod.GET);
    @NonNull
    private final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    @NonNull
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    @NonNull
    private final Map<String, Object> pathVariables = new LinkedHashMap<>();
    @NonNull
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    @Nullable
    private byte[] body;

    // Multipart state: presence => multipart
    @Nullable
    private MultiValueMap<String, Object> multipartParts;

    /**
     * Creates a new RestRequestBuilder with the specified base URL.
     *
     * @param baseUrl The base URL for the request
     * @return A new RestRequestBuilder
     */
    public static RestRequestBuilder create(String baseUrl) {
        RestRequestBuilder b = new RestRequestBuilder();
        b.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        return b;
    }

    /**
     * Creates a new RestRequestBuilder with the specified base URL and endpoint.
     *
     * @param baseUrl  The base URL for the request
     * @param endpoint The API endpoint
     * @return A new RestRequestBuilder
     */
    public static RestRequestBuilder create(String baseUrl, String endpoint) {
        return create(baseUrl).endpoint(endpoint);
    }

    /**
     * Sets the HTTP method for the request.
     *
     * @param method The HTTP method
     * @return This builder for method chaining
     */
    public RestRequestBuilder method(HttpMethod method) {
        this.method = Objects.requireNonNull(method, "method");
        return this;
    }

    /**
     * Sets the headers for the request.
     *
     * @param headers The headers to set
     * @return This builder for method chaining
     */
    public RestRequestBuilder headers(MultiValueMap<String, String> headers) {
        if (headers != null) {
            this.headers.addAll(headers);
        }
        return this;
    }

    /**
     * Adds a header to the request.
     *
     * @param name  The header name
     * @param value The header value
     * @return This builder for method chaining
     */
    public RestRequestBuilder addHeader(String name, String value) {
        MapUtils.addIf(this.headers, name, value);
        return this;
    }

    /**
     * Sets the Content-Type header.
     *
     * @param mediaType The media type
     * @return This builder for method chaining
     */
    public RestRequestBuilder contentType(MediaType mediaType) {
        if (mediaType != null) {
            this.headers.set(HttpHeaders.CONTENT_TYPE, mediaType.toString());
        }
        return this;
    }

    /**
     * Sets the Content-Type header to application/json.
     *
     * @return This builder for method chaining
     */
    public RestRequestBuilder contentTypeJson() {
        return contentType(MediaType.APPLICATION_JSON);
    }

    /**
     * Sets the Content-Type header to application/xml.
     *
     * @return This builder for method chaining
     */
    public RestRequestBuilder contentTypeXml() {
        return contentType(MediaType.APPLICATION_XML);
    }

    /**
     * Sets the Accept header.
     *
     * @param mediaType The media type
     * @return This builder for method chaining
     */
    public RestRequestBuilder accept(MediaType mediaType) {
        if (mediaType != null) {
            this.headers.set(HttpHeaders.ACCEPT, mediaType.toString());
        }
        return this;
    }

    /**
     * Sets the Accept header to application/json.
     *
     * @return This builder for method chaining
     */
    public RestRequestBuilder acceptJson() {
        return accept(MediaType.APPLICATION_JSON);
    }

    /**
     * Sets the Accept header to application/xml.
     *
     * @return This builder for method chaining
     */
    public RestRequestBuilder acceptXml() {
        return accept(MediaType.APPLICATION_XML);
    }

    /**
     * Sets the endpoint path for the request.
     *
     * @param endpoint The API endpoint
     * @return This builder for method chaining
     */
    public RestRequestBuilder endpoint(String endpoint) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        return this;
    }

    /**
     * Adds a path variable for URI template expansion.
     *
     * @param name  The path variable name
     * @param value The path variable value
     * @return This builder for method chaining
     */
    public RestRequestBuilder pathVar(String name, Object value) {
        MapUtils.putIf(this.pathVariables, name, value);
        return this;
    }

    /**
     * Adds multiple path variables for URI template expansion.
     *
     * @param variables The path variables map
     * @return This builder for method chaining
     */
    public RestRequestBuilder pathVars(Map<String, ?> variables) {
        @SuppressWarnings("unchecked")
        Map<String, Object> castVariables = (Map<String, Object>) variables;
        MapUtils.putAllIf(this.pathVariables, castVariables);
        return this;
    }

    /**
     * Adds a query parameter to the request.
     *
     * @param name  The query parameter name
     * @param value The query parameter value
     * @return This builder for method chaining
     */
    public RestRequestBuilder queryParam(String name, String value) {
        MapUtils.putIf(this.queryParams, name, value);
        return this;
    }

    /**
     * Adds multiple query parameters to the request.
     *
     * @param params The query parameters map
     * @return This builder for method chaining
     */
    public RestRequestBuilder queryParams(Map<String, String> params) {
        MapUtils.putAllIf(this.queryParams, params);
        return this;
    }

    /**
     * Adds a single attribute to the request.
     *
     * @param name  The attribute name
     * @param value The attribute value
     * @return This builder for method chaining
     */
    public RestRequestBuilder attribute(String name, Object value) {
        MapUtils.putIf(this.attributes, name, value);
        return this;
    }

    /**
     * Adds multiple attributes to the request.
     *
     * @param attrs The attributes map
     * @return This builder for method chaining
     */
    public RestRequestBuilder attributes(Map<String, ?> attrs) {
        @SuppressWarnings("unchecked")
        Map<String, Object> castAttrs = (Map<String, Object>) attrs;
        MapUtils.putAllIf(this.attributes, castAttrs);
        return this;
    }

    /**
     * Sets the request body as bytes.
     *
     * @param body The request body as bytes
     * @return This builder for method chaining
     */
    public RestRequestBuilder body(byte[] body) {
        this.body = body;
        return this;
    }

    /**
     * Sets the request body as a string (encoded as UTF-8 bytes).
     *
     * @param body The request body as a string
     * @return This builder for method chaining
     */
    public RestRequestBuilder body(String body) {
        this.body = body == null ? null : body.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    /* --------------------- Authentication helpers --------------------- */

    /**
     * Adds a Bearer token to the Authorization header.
     *
     * @param token The bearer token
     * @return This builder for method chaining
     */
    public RestRequestBuilder bearerToken(String token) {
        if (token != null) {
            this.headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return this;
    }

    /**
     * Adds Basic authentication to the Authorization header.
     *
     * @param username The username
     * @param password The password
     * @return This builder for method chaining
     */
    public RestRequestBuilder basicAuth(String username, String password) {
        if (username != null && password != null) {
            String credentials = username + ":" + password;
            String encoded = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            this.headers.add(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }
        return this;
    }

    /* --------------------- Multipart helpers --------------------- */

    private MultiValueMap<String, Object> ensureMultipart() {
        if (this.multipartParts == null) {
            this.multipartParts = new LinkedMultiValueMap<>();
        }
        return this.multipartParts;
    }

    /**
     * Adds a simple string part to a multipart request.
     *
     * @param name  The part name
     * @param value The part value
     * @return This builder for method chaining
     */
    public RestRequestBuilder part(String name, String value) {
        MapUtils.addIf(ensureMultipart(), name, value);
        return this;
    }

    /**
     * Adds an advanced part (e.g., HttpEntity with custom part headers) to a
     * multipart request.
     *
     * @param name  The part name
     * @param value The part value
     * @return This builder for method chaining
     */
    public RestRequestBuilder part(String name, Object value) {
        MapUtils.addIf(ensureMultipart(), name, value);
        return this;
    }

    /**
     * Adds a file part from bytes with explicit field name and filename.
     *
     * @param field    The form field name
     * @param content  The file content as bytes
     * @param filename The filename
     * @return This builder for method chaining
     */
    public RestRequestBuilder file(String field, byte[] content, String filename) {
        return file(field, content, filename, null);
    }

    /**
     * Adds a file part from bytes with optional per-part content type.
     *
     * @param field       The form field name
     * @param content     The file content as bytes
     * @param filename    The filename
     * @param contentType The content type for this part (optional)
     * @return This builder for method chaining
     */
    public RestRequestBuilder file(
            String field, byte[] content, String filename, @Nullable MediaType contentType) {
        if (field == null || content == null)
            return this;

        // Resource with filename support
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename != null ? filename : "file";
            }
        };

        // Per-part headers
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentDisposition(
                createFormDataContentDisposition(field, resource.getFilename()));
        if (contentType != null) {
            partHeaders.setContentType(contentType);
        }

        ensureMultipart().add(field, new HttpEntity<>(resource, partHeaders));
        return this;
    }

    /**
     * Adds a file part from java.io.File with explicit field name; infers filename
     * from File.
     *
     * @param field The form field name
     * @param file  The file to upload
     * @return This builder for method chaining
     */
    public RestRequestBuilder file(String field, File file) {
        return file(field, file, null);
    }

    /**
     * Adds a file part from java.io.File with optional per-part content type.
     *
     * @param field       The form field name
     * @param file        The file to upload
     * @param contentType The content type for this part (optional)
     * @return This builder for method chaining
     */
    public RestRequestBuilder file(String field, File file, @Nullable MediaType contentType) {
        if (field == null || file == null)
            return this;

        FileSystemResource resource = new FileSystemResource(file);

        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentDisposition(
                createFormDataContentDisposition(field, resource.getFilename()));
        if (contentType != null) {
            partHeaders.setContentType(contentType);
        }

        ensureMultipart().add(field, new HttpEntity<>(resource, partHeaders));
        return this;
    }

    @NonNull
    private ContentDisposition createFormDataContentDisposition(String field, String filename) {
        return ContentDisposition.formData()
                .name(field)
                .filename(filename)
                .build();
    }

    /* --------------------- Build --------------------- */

    /**
     * Builds and returns the RestRequest.
     *
     * @return The built RestRequest
     */
    public RestRequest build() {
        String baseUrlValue = Objects.requireNonNull(baseUrl, "baseUrl must be set via create()");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrlValue);
        String endpointValue = endpoint;
        if (endpointValue != null && !endpointValue.isEmpty()) {
            uriBuilder.path(endpointValue);
        }
        queryParams.forEach(uriBuilder::queryParam);

        URI uri = pathVariables.isEmpty()
                ? uriBuilder.build().encode().toUri()
                : uriBuilder.buildAndExpand(pathVariables).encode().toUri();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.addAll(this.headers);

        if (multipartParts != null && !multipartParts.isEmpty()) {
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            return new RestRequest(uri, method, httpHeaders, null, multipartParts, attributes);
        }

        return new RestRequest(uri, method, httpHeaders, body, null, attributes);
    }
}
