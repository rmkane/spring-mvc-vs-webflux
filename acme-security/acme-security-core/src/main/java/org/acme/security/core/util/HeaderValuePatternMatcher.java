package org.acme.security.core.util;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class HeaderValuePatternMatcher {

    private final String exactValue;
    private final Pattern compiledPattern;
    private final String originalValue;

    public static HeaderValuePatternMatcher compile(String ignoredValuePattern) {
        if (!ignoredValuePattern.contains("*")) {
            return new HeaderValuePatternMatcher(ignoredValuePattern, null, ignoredValuePattern);
        }

        String regex = Arrays.stream(ignoredValuePattern.split("\\*", -1))
                .map(Pattern::quote)
                .collect(Collectors.joining(".*"));

        return new HeaderValuePatternMatcher(null, Pattern.compile(regex), ignoredValuePattern);
    }

    public boolean matches(String headerValue) {
        if (compiledPattern == null) {
            return exactValue.equals(headerValue);
        }

        return compiledPattern.matcher(headerValue).matches();
    }
}
