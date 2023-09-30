/*
 * Copyright (c) 2022 Jan Bretschneider <mail@jan-bretschneider.de>
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You can find the License in the `LICENSE` file at the top level of
 * this repository or may obtain a copy at
 *
 *   https://raw.githubusercontent.com/jbretsch/openapi-route-definition-locator/master/LICENSE
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package net.bretti.openapi.route.definition.locator.core.impl.utils;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class MapMerge {
    public static Optional<Map<String, Object>> deepMerge(Optional<Map<String, Object>> original, Optional<Map<String, Object>> patch) {
        if (!patch.isPresent()) {
            return original.map(it -> deepCopy(it, true));
        }

        if (!original.isPresent()) {
            return patch.map(it -> deepCopy(it, false));
        }

        return Optional.of(deepMerge(original.get(), patch.get()));
    }

    @SafeVarargs
    public static Optional<Map<String, Object>> deepMerge(Optional<Map<String, Object>> original, Optional<Map<String, Object>>... patches) {
        if (patches.length == 0) {
            return original.map(it -> deepCopy(it, true));
        }

        Optional<Map<String, Object>> result = original;
        for (Optional<Map<String, Object>> patch: patches) {
            result = deepMerge(result, patch);
        }
        return result;
    }

    /**
     * Deep merge Maps with semantics almost as defined in
     * <a href="https://datatracker.ietf.org/doc/html/rfc7386">https://datatracker.ietf.org/doc/html/rfc7386</a>.
     * There is one exception: Merging two lists is done by concatenating them.
     * Returns the result.
     */
    private static Map<String, Object> deepMerge(Map<String, Object> original, Map<String, Object> patch) {
        Map<String, Object> result = deepCopy(original, true);
        for (Map.Entry<String, Object> patchEntry : patch.entrySet()) {
            String key = patchEntry.getKey();
            Object originalValue = original.get(key);
            Object patchValue = patchEntry.getValue();
            if (patchValue == null) {
                result.remove(key);
                continue;
            }

            if (originalValue instanceof Map && patchValue instanceof Map) {
                result.put(key, deepMerge((Map<String, Object>)originalValue, (Map<String, Object>)patchValue));
                continue;
            }

            if (originalValue instanceof List && patchValue instanceof List) {
                result.put(key, union(deepCopy((List<Object>) originalValue, true), deepCopy((List<Object>) patchValue, false)));
                continue;
            }

            if (patchValue instanceof Map) {
                result.put(key, deepCopy((Map<String, Object>)patchValue, false));
                continue;
            }

            if (patchValue instanceof List) {
                result.put(key, deepCopy((List<Object>)patchValue, false));
                continue;
            }

            result.put(key, patchValue);
        }
        return result;
    }

    @SafeVarargs
    private static <T> List<T> union(List<T>... lists) {
        return Stream.of(lists).flatMap(List::stream).collect(Collectors.toList());
    }

    private static List<Object> deepCopy(List<Object> list, boolean keepNullValuesInMaps) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                result.add(deepCopy((Map<String, Object>) item, keepNullValuesInMaps));
                continue;
            }

            if (item instanceof List) {
                result.add(deepCopy((List<Object>) item, keepNullValuesInMaps));
                continue;
            }

            result.add(item);
        }
        return result;
    }

    private static Map<String, Object> deepCopy(Map<String, Object> map, boolean keepNullValuesInMaps) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null && !keepNullValuesInMaps) {
                continue;
            }

            if (value instanceof Map) {
                result.put(key, deepCopy((Map<String, Object>) value, keepNullValuesInMaps));
                continue;
            }

            if (value instanceof List) {
                result.put(key, deepCopy((List<Object>) value, keepNullValuesInMaps));
                continue;
            }

            result.put(key, value);
        }
        return result;
    }

}
