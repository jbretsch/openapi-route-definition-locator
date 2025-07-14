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

package componenttest

import componenttest.setup.basetest.BaseCompTest
import componenttest.setup.wiremock.RouteDefinitionFilteringServiceMock
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("route-definition-filtering")
class RouteDefinitionFilteringCompTest extends BaseCompTest {

    def "Routes with invalid enabled values default to enabled"() {
        given:
        waitForRemovalOfAllRoutes()

        and:
        RouteDefinitionFilteringServiceMock.instance.mockOpenApiDefinitionWithInvalidEnabledValues()

        when:
        waitForRouteAddition {
            // Routes with invalid enabled values should default to enabled (true)
            // Only the route with explicitly "enabled: false" should be filtered out
            assert getRoutesFromActuatorEndpoint().size() == 3
        }

        and:
        List routes = getRoutesFromActuatorEndpoint()

        then: "Route with enabled: 'invalid-string' should be included (defaults to true)"
        extractRoute(routes, "GET", "/test-invalid-enabled-string") != null

        and: "Route with enabled: 123 should be included (defaults to true)"
        extractRoute(routes, "GET", "/test-invalid-enabled-number") != null

        and: "Route with enabled: null should be included (defaults to true)"
        extractRoute(routes, "GET", "/test-null-enabled") != null

        and: "Route with enabled: false should be filtered out"
        extractRoute(routes, "GET", "/test-disabled") == null
    }

    def "Routes with invalid gateway-names values are handled gracefully"() {
        given:
        waitForRemovalOfAllRoutes()

        and:
        RouteDefinitionFilteringServiceMock.instance.mockOpenApiDefinitionWithInvalidGatewayNames()

        when:
        waitForRouteAddition {
            // All routes should be included since invalid gateway-names are ignored
            assert getRoutesFromActuatorEndpoint().size() == 4
        }

        and:
        List routes = getRoutesFromActuatorEndpoint()

        then: "Route with gateway-names as string should be included (ignored)"
        extractRoute(routes, "GET", "/test-gateway-names-string") != null

        and: "Route with gateway-names as number should be included (ignored)"
        extractRoute(routes, "GET", "/test-gateway-names-number") != null

        and: "Route with gateway-names as mixed array should be included (only strings processed)"
        extractRoute(routes, "GET", "/test-gateway-names-mixed-array") != null

        and: "Route with gateway-names as empty array should be included"
        extractRoute(routes, "GET", "/test-gateway-names-empty-array") != null
    }

    def "Mixed filtering scenarios work correctly"() {
        given:
        waitForRemovalOfAllRoutes()

        and:
        RouteDefinitionFilteringServiceMock.instance.mockOpenApiDefinitionWithMixedFiltering()

        when:
        waitForRouteAddition {
            // Only routes that pass both enabled and gateway-name filters should be included
            assert getRoutesFromActuatorEndpoint().size() == 2
        }

        and:
        List routes = getRoutesFromActuatorEndpoint()

        then: "Route that is enabled and has no gateway-names should be included"
        extractRoute(routes, "GET", "/test-enabled-no-gateway") != null

        and: "Route that is disabled should be filtered out (even with matching gateway name)"
        extractRoute(routes, "GET", "/test-disabled-matching-gateway") == null

        and: "Route that is enabled but has non-matching gateway-names should be filtered out"
        extractRoute(routes, "GET", "/test-enabled-wrong-gateway") == null

        and: "Route that is enabled with matching gateway-names should be included"
        extractRoute(routes, "GET", "/test-enabled-matching-gateway") != null
    }

    def "Global vs operation-level settings precedence works correctly"() {
        given:
        waitForRemovalOfAllRoutes()

        and:
        RouteDefinitionFilteringServiceMock.instance.mockOpenApiDefinitionWithSettingsPrecedence()

        when:
        waitForRouteAddition {
            // Test operation-level settings override global settings
            assert getRoutesFromActuatorEndpoint().size() == 1
        }

        and:
        List routes = getRoutesFromActuatorEndpoint()

        then: "Operation-level enabled: false should override global enabled: true"
        extractRoute(routes, "GET", "/test-operation-overrides-global-enabled") == null

        and: "Operation-level gateway-names should override global gateway-names"
        extractRoute(routes, "GET", "/test-operation-overrides-global-gateway") != null
    }

    def "EnvironmentRouteDefinitionFilter correctly filters routes based on x-environment"() {
        given:
        waitForRemovalOfAllRoutes()

        and:
        RouteDefinitionFilteringServiceMock.instance.mockOpenApiDefinitionWithEnvironmentFiltering()

        when:
        waitForRouteAddition {
            // Only routes with no x-environment or x-environment: dev should be included
            assert getRoutesFromActuatorEndpoint().size() == 2
        }

        and:
        List routes = getRoutesFromActuatorEndpoint()

        then: "Route with no x-environment should be included (null case)"
        extractRoute(routes, "GET", "/test-no-environment") != null

        and: "Route with x-environment: dev should be included"
        extractRoute(routes, "GET", "/test-environment-dev") != null

        and: "Route with x-environment: prod should be filtered out"
        extractRoute(routes, "GET", "/test-environment-prod") == null
    }
}
