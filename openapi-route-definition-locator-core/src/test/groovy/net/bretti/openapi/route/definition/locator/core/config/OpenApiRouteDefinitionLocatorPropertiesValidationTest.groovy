/*
 * Copyright (c) 2025 Jan Bretschneider <mail@jan-bretschneider.de>
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

package net.bretti.openapi.route.definition.locator.core.config

import spock.lang.Specification

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory

class OpenApiRouteDefinitionLocatorPropertiesValidationTest extends Specification {

    private Validator validator

    def setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
    }

    def "gateway name validation passes for null value"() {
        given:
        OpenApiRouteDefinitionLocatorProperties properties = new OpenApiRouteDefinitionLocatorProperties()
        properties.gatewayName = null

        when:
        Set<ConstraintViolation<OpenApiRouteDefinitionLocatorProperties>> violations = validator.validate(properties)

        then:
        violations.isEmpty()
    }

    def "gateway name validation passes for valid non-blank string"() {
        given:
        OpenApiRouteDefinitionLocatorProperties properties = new OpenApiRouteDefinitionLocatorProperties()
        properties.gatewayName = "valid-gateway-name"

        when:
        Set<ConstraintViolation<OpenApiRouteDefinitionLocatorProperties>> violations = validator.validate(properties)

        then:
        violations.isEmpty()
    }

    def "gateway name validation fails for blank string"() {
        given:
        OpenApiRouteDefinitionLocatorProperties properties = new OpenApiRouteDefinitionLocatorProperties()
        properties.gatewayName = ""

        when:
        Set<ConstraintViolation<OpenApiRouteDefinitionLocatorProperties>> violations = validator.validate(properties)

        then:
        violations.size() == 1
        violations.first().message == "null or not blank"
        violations.first().propertyPath.toString() == "gatewayName"
    }

    def "gateway name validation fails for whitespace-only string"() {
        given:
        OpenApiRouteDefinitionLocatorProperties properties = new OpenApiRouteDefinitionLocatorProperties()
        properties.gatewayName = "   "

        when:
        Set<ConstraintViolation<OpenApiRouteDefinitionLocatorProperties>> violations = validator.validate(properties)

        then:
        violations.size() == 1
        violations.first().message == "null or not blank"
        violations.first().propertyPath.toString() == "gatewayName"
    }

    def "gateway name validation passes for string with content and whitespace"() {
        given:
        OpenApiRouteDefinitionLocatorProperties properties = new OpenApiRouteDefinitionLocatorProperties()
        properties.gatewayName = "  valid-gateway  "

        when:
        Set<ConstraintViolation<OpenApiRouteDefinitionLocatorProperties>> violations = validator.validate(properties)

        then:
        violations.isEmpty()
    }
}
