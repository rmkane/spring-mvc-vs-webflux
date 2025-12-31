package org.acme.test.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.util.MultiValueMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility methods for working with Maps and MultiValueMaps. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MapUtils {

    /* --------------------- Map operations --------------------- */

    /**
     * Puts a key-value pair into the map only if both key and value are non-null.
     *
     * @param map   The map to put the entry into
     * @param key   The key to put
     * @param value The value to put
     * @param <K>   The type of the map keys
     * @param <V>   The type of the map values
     * @return The map (for method chaining)
     */
    public static <K, V> Map<K, V> putIf(Map<K, V> map, K key, V value) {
        if (key != null && value != null) {
            map.put(key, value);
        }
        return map;
    }

    /**
     * Puts all entries from the source map into the target map, but only entries
     * where both key and value are non-null.
     *
     * @param target The map to put entries into
     * @param source The map to copy entries from
     * @param <K>    The type of the map keys
     * @param <V>    The type of the map values
     * @return The target map (for method chaining)
     */
    public static <K, V> Map<K, V> putAllIf(Map<K, V> target, Map<K, V> source) {
        if (source != null) {
            source.forEach((k, v) -> putIf(target, k, v));
        }
        return target;
    }

    /* --------------------- MultiValueMap operations --------------------- */

    /**
     * Adds a key-value pair to the MultiValueMap only if both key and value are
     * non-null.
     *
     * @param map   The MultiValueMap to add the entry to
     * @param key   The key to add
     * @param value The value to add
     * @param <K>   The type of the map keys
     * @param <V>   The type of the map values
     * @return The map (for method chaining)
     */
    public static <K, V> MultiValueMap<K, V> addIf(MultiValueMap<K, V> map, K key, V value) {
        if (key != null && value != null) {
            map.add(key, value);
        }
        return map;
    }

    /**
     * Adds all values from a collection to the MultiValueMap for the given key,
     * only if both key and values collection are non-null. Individual null values
     * in the collection are skipped.
     *
     * @param map    The MultiValueMap to add entries to
     * @param key    The key to add values for
     * @param values The collection of values to add
     * @param <K>    The type of the map keys
     * @param <V>    The type of the map values
     * @return The map (for method chaining)
     */
    public static <K, V> MultiValueMap<K, V> addAllIf(
            MultiValueMap<K, V> map, K key, Collection<V> values) {
        if (key != null && values != null) {
            for (V value : values) {
                addIf(map, key, value);
            }
        }
        return map;
    }

    /**
     * Puts a key-value list pair into the MultiValueMap only if both key and values
     * are non-null. Replaces any existing values for the key.
     *
     * @param map    The MultiValueMap to put the entry into
     * @param key    The key to put
     * @param values The list of values to put
     * @param <K>    The type of the map keys
     * @param <V>    The type of the map values
     * @return The map (for method chaining)
     */
    public static <K, V> MultiValueMap<K, V> putIf(MultiValueMap<K, V> map, K key, List<V> values) {
        if (key != null && values != null) {
            map.put(key, List.copyOf(values));
        }
        return map;
    }
}
