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

package net.bretti.openapi.route.definition.locator.core.impl.utils

import spock.lang.Specification

class MapMergeTest extends Specification {
    def "deepMerge merges two maps correctly"() {
        expect:
        MapMerge.deepMerge(Optional.of(original), Optional.of(patch)) == Optional.of(expectedResult)

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
}
