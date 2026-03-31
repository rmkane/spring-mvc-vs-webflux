package org.acme.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class MapUtilsTest {

    /* --------------------- Map operations --------------------- */

    @Test
    void testPutIfWithValidKeyAndValue() {
        Map<String, String> map = new HashMap<>();
        MapUtils.putIf(map, "key", "value");

        assertEquals("value", map.get("key"));
        assertEquals(1, map.size());
    }

    @Test
    void testPutIfWithNullKey() {
        Map<String, String> map = new HashMap<>();
        MapUtils.putIf(map, null, "value");

        assertTrue(map.isEmpty());
    }

    @Test
    void testPutIfWithNullValue() {
        Map<String, String> map = new HashMap<>();
        MapUtils.putIf(map, "key", null);

        assertFalse(map.containsKey("key"));
    }

    @Test
    void testPutIfWithBothNull() {
        Map<String, String> map = new HashMap<>();
        MapUtils.putIf(map, null, null);

        assertTrue(map.isEmpty());
    }

    @Test
    void testPutAllIfWithValidMap() {
        Map<String, String> target = new HashMap<>();
        Map<String, String> source = new HashMap<>();
        source.put("key1", "value1");
        source.put("key2", "value2");

        MapUtils.putAllIf(target, source);

        assertEquals("value1", target.get("key1"));
        assertEquals("value2", target.get("key2"));
        assertEquals(2, target.size());
    }

    @Test
    void testPutAllIfWithNullSource() {
        Map<String, String> target = new HashMap<>();
        target.put("existing", "value");

        MapUtils.putAllIf(target, null);

        assertEquals(1, target.size());
        assertEquals("value", target.get("existing"));
    }

    @Test
    void testPutAllIfWithNullValuesInSource() {
        Map<String, String> target = new HashMap<>();
        Map<String, String> source = new HashMap<>();
        source.put("key1", "value1");
        source.put("key2", null);
        source.put(null, "value3");

        MapUtils.putAllIf(target, source);

        assertEquals("value1", target.get("key1"));
        assertFalse(target.containsKey("key2"));
        assertFalse(target.containsKey(null));
        assertEquals(1, target.size());
    }

    /* --------------------- MultiValueMap operations --------------------- */

    @Test
    void testAddIfWithValidKeyAndValue() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        MapUtils.addIf(map, "key", "value");

        assertEquals("value", map.getFirst("key"));
        assertEquals(1, map.size());
    }

    @Test
    void testAddIfWithNullKey() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        MapUtils.addIf(map, null, "value");

        assertTrue(map.isEmpty());
    }

    @Test
    void testAddIfWithNullValue() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        MapUtils.addIf(map, "key", null);

        assertFalse(map.containsKey("key"));
    }

    @Test
    void testAddIfMultipleValues() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        MapUtils.addIf(map, "key", "value1");
        MapUtils.addIf(map, "key", "value2");

        List<String> values = map.get("key");
        assertEquals(2, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
    }

    @Test
    void testAddAllIfWithValidCollection() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        List<String> values = List.of("value1", "value2", "value3");

        MapUtils.addAllIf(map, "key", values);

        assertEquals(3, map.get("key").size());
        assertTrue(map.get("key").containsAll(values));
    }

    @Test
    void testAddAllIfWithNullKey() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        List<String> values = List.of("value1", "value2");

        MapUtils.addAllIf(map, null, values);

        assertTrue(map.isEmpty());
    }

    @Test
    void testAddAllIfWithNullCollection() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("existing", "value");

        MapUtils.addAllIf(map, "key", null);

        assertEquals(1, map.size());
        assertEquals("value", map.getFirst("existing"));
    }

    @Test
    void testAddAllIfWithNullValuesInCollection() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        List<String> values = new ArrayList<>();
        values.add("value1");
        values.add(null);
        values.add("value2");

        MapUtils.addAllIf(map, "key", values);

        List<String> result = map.get("key");
        assertEquals(2, result.size());
        assertTrue(result.contains("value1"));
        assertTrue(result.contains("value2"));
        assertFalse(result.contains(null));
    }

    @Test
    void testPutIfMultiValueMapWithValidKeyAndValues() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        List<String> values = List.of("value1", "value2");

        MapUtils.putIf(map, "key", values);

        assertEquals(values, map.get("key"));
    }

    @Test
    void testPutIfMultiValueMapWithNullKey() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        List<String> values = List.of("value1", "value2");

        MapUtils.putIf(map, null, values);

        assertTrue(map.isEmpty());
    }

    @Test
    void testPutIfMultiValueMapWithNullValues() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("existing", "value");

        MapUtils.putIf(map, "key", null);

        assertFalse(map.containsKey("key"));
        assertEquals(1, map.size());
    }

    @Test
    void testPutIfMultiValueMapReplacesExisting() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("key", "oldValue");

        List<String> newValues = List.of("newValue1", "newValue2");
        MapUtils.putIf(map, "key", newValues);

        assertEquals(newValues, map.get("key"));
        assertEquals(2, map.get("key").size());
    }

    @Test
    void testPutIfMultiValueMapWithImmutableList() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        List<String> values = List.of("value1", "value2");

        MapUtils.putIf(map, "key", values);

        // The list should be a copy (immutable)
        List<String> result = map.get("key");
        assertEquals(values, result);
    }
}
