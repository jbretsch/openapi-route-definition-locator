/*
 * Copyright (c) 2023 Jan Bretschneider <mail@jan-bretschneider.de>
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

package net.bretti.openapi.route.definition.locator.core.impl.utils

import spock.lang.Specification

class MapMergeTest extends Specification {
    def "deepMerge merges two maps correctly"() {
        expect:
        MapMerge.deepMerge(Optional.ofNullable(original), Optional.ofNullable(patch)) == Optional.ofNullable(expectedResult)

        where:
        original         | patch                  | expectedResult
        [a: 'b']         | [a: 'c']               | [a: 'c']
        [a: 'b']         | [b: 'c']               | [a: 'b', b: 'c']
        [a: 'b']         | [a: null]              | [:]
        [a: 'b', b: 'c'] | [a: null]              | [b: 'c']
        [a: ['b']]       | [a: 'c']               | [a: 'c']
        [a: 'c']         | [a: ['b']]             | [a: ['b']]
        [a: [b: 'c']]    | [a: [b: 'd', c: null]] | [a: [b: 'd']]
        [a: [[b: 'c']]]  | [a: [1]]               | [a: [[b: 'c'], 1]] // Deviation from RFC7386.
        [a: ['a', 'b']]  | [a: ['c', 'd']]        | [a: ['a', 'b', 'c', 'd']] // Deviation from RFC7386.
        [a: [a: 'b']]    | [a: ['c']]             | [a: ['c']]
        [a: [a: 'foo']]  | [a: null]              | [:]
        [a: [a: 'foo']]  | [a: 'bar']             | [a: 'bar']
        [e: null]        | [a: 1]                 | [e: null, a: 1]
        [e: null]        | null                   | [e: null]
        [a: [[e: null]]] | [a: [[f: null]]]       | [a: [[e: null], [:]]]
        [:]              | [a: [[f: null]]]       | [a: [[:]]]
        null             | [e: null]              | [:]
        [a: [1, 2]]      | [a: [a: 'b', c: null]] | [a: [a: 'b']]
        [:]              | [a: [bb: [ccc: null]]] | [a: [bb: [:]]]
    }

    def "deepMerge merges three maps correctly"() {
        expect:
        MapMerge.deepMerge(Optional.of(original), Optional.of(patch1), Optional.of(patch2)) == Optional.of(expectedResult)

        where:
        original | patch1    | patch2   | expectedResult
        [a: 'b'] | [a: 'c']  | [a: 'd'] | [a: 'd']
        [a: 'b'] | [b: 'c']  | [c: 'd'] | [a: 'b', b: 'c', c: 'd']
        [a: 'b'] | [a: null] | [b: 'c'] | [b: 'c']
    }

    // It is important that deepMerge() returns a deep copy of the input maps because metadata maps can be arbitrarily
    // modified via a `OpenApiRouteDefinitionCustomizer` implementation and there should be no interference whatsoever
    // between the metadata maps of different API operations.
    def "deepMerge(original, patch) returns a deep copy"() {
        when:
        Map<String, Object> originalPatched = MapMerge.deepMerge(Optional.ofNullable(original), Optional.ofNullable(patch)).get()

        then:
        originalPatched == originalPatchedExpected

        when:
        originalPatchModifier.call(originalPatched)
        Map<String, Object> originalPatched2 = MapMerge.deepMerge(Optional.ofNullable(original), Optional.ofNullable(patch)).get()

        then:
        originalPatched2 == originalPatchedExpected

        where:
        original                       | patch                          | originalPatchedExpected                              | originalPatchModifier
        [:]                            | [key1: [key11: 'value11']]     | [key1: [key11: 'value11']]                           | { it.key1.key12 = 'value12' }
        [:]                            | [key1: ['value11']]            | [key1: ['value11']]                                  | { it.key1.add('value12') }
        [key1: [key11: 'value11']]     | null                           | [key1: [key11: 'value11']]                           | { it.key1.key12 = 'value12' }
        null                           | [key1: [key11: 'value11']]     | [key1: [key11: 'value11']]                           | { it.key1.key12 = 'value12' }
        [key1: [key11: 'value11']]     | [:]                            | [key1: [key11: 'value11']]                           | { it.key1.key12 = 'value12' }
        [key1: [[key101: 'value101']]] | [key1: [[key111: 'value111']]] | [key1: [[key101: 'value101'], [key111: 'value111']]] | { it.key1[0].key102 = 'value102' }
        [key1: [[key101: 'value101']]] | [key1: [[key111: 'value111']]] | [key1: [[key101: 'value101'], [key111: 'value111']]] | { it.key1[1].key112 = 'value112' }
    }

    def "deepMerge(original) returns a deep copy"() {
        when:
        Map<String, Object> originalPatched = MapMerge.deepMerge(Optional.ofNullable(original)).get()

        then:
        originalPatched == originalPatchedExpected

        when:
        originalPatchModifier.call(originalPatched)
        Map<String, Object> originalPatched2 = MapMerge.deepMerge(Optional.ofNullable(original)).get()

        then:
        originalPatched2 == originalPatchedExpected

        where:
        original                   | originalPatchedExpected    | originalPatchModifier
        [:]                        | [:]                        | { it.key1 = 'value1' }
        [e: null]                  | [e: null]                  | { it.key1 = 'value1' }
        [:]                        | [:]                        | { it.key1 = ['value1'] }
        [key1: [key11: 'value11']] | [key1: [key11: 'value11']] | { it.key1.key12 = 'value12' }
        [key1: ['value11']]        | [key1: ['value11']]        | { it.key1.add('value12') }
    }
}
