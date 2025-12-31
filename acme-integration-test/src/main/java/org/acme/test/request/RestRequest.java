package org.acme.test.request;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/** Immutable HTTP request for integration tests. */
public final class RestRequest implements HttpRequest {
    @NonNull
    private final URI uri;
    @NonNull
    private final HttpMethod method;
    @NonNull
    private final HttpHeaders headers;
    @Nullable
    private final byte[] body;
    @Nullable
    private final MultiValueMap<String, Object> multipartBody;
    @NonNull
    private final Map<String, Object> attributes;

    RestRequest(
            @NonNull URI uri,
            @NonNull HttpMethod method,
            @NonNull HttpHeaders headers,
            @Nullable byte[] body,
            @Nullable MultiValueMap<String, Object> multipartBody,
            @NonNull Map<String, Object> attributes) {
        this.uri = uri;
        this.method = method;
        this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
        this.body = body;
        this.multipartBody = multipartBody;
        this.attributes = new HashMap<>(attributes);
    }

    RestRequest(
            @NonNull URI uri,
            @NonNull HttpMethod method,
            @NonNull HttpHeaders headers,
            @Nullable byte[] body) {
        this(uri, method, headers, body, null, new HashMap<>());
    }

    RestRequest(
            @NonNull URI uri,
            @NonNull HttpMethod method,
            @NonNull HttpHeaders headers,
            @Nullable MultiValueMap<String, Object> multipartBody) {
        this(uri, method, headers, null, multipartBody, new HashMap<>());
    }

    @NonNull
    @Override
    public HttpMethod getMethod() {
        return method;
    }

    public String getMethodValue() {
        return method.name();
    }

    @NonNull
    @Override
    public URI getURI() {
        return uri;
    }

    @NonNull
    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @NonNull
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** Raw body bytes, or {@code null} if no body. */
    @Nullable
    public byte[] getBody() {
        return body;
    }

    public Optional<String> getBodyAsString(@NonNull Charset charset) {
        return Optional.ofNullable(body).map(b -> new String(b, charset));
    }

    /** Multipart form data body, or {@code null} if not a multipart request. */
    @Nullable
    public MultiValueMap<String, Object> getMultipartBody() {
        return multipartBody;
    }

    /** Returns {@code true} if this is a multipart form data request. */
    public boolean isMultipart() {
        return multipartBody != null && !multipartBody.isEmpty();
    }
}
