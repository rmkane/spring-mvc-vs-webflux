package org.acme.security.core.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CollectionWritingUtils {

    public static <E> Map<String, List<String>> mapToStringLists(Map<String, List<E>> items, Function<E, String> fn) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }

        return items.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(fn)
                                .toList(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }
}
