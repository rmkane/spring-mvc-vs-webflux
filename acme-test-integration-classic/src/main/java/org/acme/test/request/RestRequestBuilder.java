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

    public static RestRequestBuilder create(String baseUrl) {
        RestRequestBuilder b = new RestRequestBuilder();
        b.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        return b;
    }

    public static RestRequestBuilder create(String baseUrl, String endpoint) {
        return create(baseUrl).endpoint(endpoint);
    }

    public RestRequestBuilder method(HttpMethod method) {
        this.method = Objects.requireNonNull(method, "method");
        return this;
    }

    public RestRequestBuilder headers(MultiValueMap<String, String> headers) {
        if (headers != null) {
            this.headers.addAll(headers);
        }
        return this;
    }

    public RestRequestBuilder addHeader(String name, String value) {
        MapUtils.addIf(this.headers, name, value);
        return this;
    }

    public RestRequestBuilder endpoint(String endpoint) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        return this;
    }

    public RestRequestBuilder pathVar(String name, Object value) {
        MapUtils.putIf(this.pathVariables, name, value);
        return this;
    }

    public RestRequestBuilder pathVars(Map<String, ?> variables) {
        @SuppressWarnings("unchecked")
        Map<String, Object> castVariables = (Map<String, Object>) variables;
        MapUtils.putAllIf(this.pathVariables, castVariables);
        return this;
    }

    public RestRequestBuilder queryParam(String name, String value) {
        MapUtils.putIf(this.queryParams, name, value);
        return this;
    }

    public RestRequestBuilder queryParams(Map<String, String> params) {
        MapUtils.putAllIf(this.queryParams, params);
        return this;
    }

    /** Add a single attribute to the request. */
    public RestRequestBuilder attribute(String name, Object value) {
        MapUtils.putIf(this.attributes, name, value);
        return this;
    }

    /** Add multiple attributes to the request. */
    public RestRequestBuilder attributes(Map<String, ?> attrs) {
        @SuppressWarnings("unchecked")
        Map<String, Object> castAttrs = (Map<String, Object>) attrs;
        MapUtils.putAllIf(this.attributes, castAttrs);
        return this;
    }

    public RestRequestBuilder body(byte[] body) {
        this.body = body;
        return this;
    }

    public RestRequestBuilder body(String body) {
        this.body = body == null ? null : body.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    /* --------------------- Authentication helpers --------------------- */

    /** Adds a Bearer token to the Authorization header. */
    public RestRequestBuilder bearerToken(String token) {
        if (token != null) {
            this.headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return this;
    }

    /** Adds Basic authentication to the Authorization header. */
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

    /** Add a simple string part. */
    public RestRequestBuilder part(String name, String value) {
        MapUtils.addIf(ensureMultipart(), name, value);
        return this;
    }

    /** Add an advanced part (e.g., HttpEntity with custom part headers). */
    public RestRequestBuilder part(String name, Object value) {
        MapUtils.addIf(ensureMultipart(), name, value);
        return this;
    }

    /** File from bytes with explicit field name and filename. */
    public RestRequestBuilder file(String field, byte[] content, String filename) {
        return file(field, content, filename, null);
    }

    /** File from bytes with optional per-part content type. */
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
     * File from java.io.File with explicit field name; infers filename from File.
     */
    public RestRequestBuilder file(String field, File file) {
        return file(field, file, null);
    }

    /** File from java.io.File with optional per-part content type. */
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
