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

package net.bretti.openapi.route.definition.locator.autoconfigure

import net.bretti.openapi.route.definition.locator.core.impl.filter.EnabledFlagFilter
import net.bretti.openapi.route.definition.locator.core.impl.filter.GatewayNameFilter
import net.bretti.openapi.route.definition.locator.core.filter.OpenApiRouteDefinitionFilter
import org.assertj.core.api.Assertions
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration
import spock.lang.Specification

class FilterOrderingTest extends Specification {

    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()

    def "Filter ordering is correct - EnabledFlagFilter runs before GatewayNameFilter"() {
        expect:
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        WebFluxAutoConfiguration,
                ))
                .run({ context ->
                    List<OpenApiRouteDefinitionFilter> filters = context.getBeansOfType(OpenApiRouteDefinitionFilter).values().toList()

                    // Verify we have both filters
                    Assertions.assertThat(filters).hasSize(2)

                    // Verify ordering: EnabledFlagFilter (order 100) should come before GatewayNameFilter (order 200)
                    Assertions.assertThat(filters[0]).isInstanceOf(EnabledFlagFilter)
                    Assertions.assertThat(filters[1]).isInstanceOf(GatewayNameFilter)
                })
    }

    def "EnabledFlagFilter is not created when disabled"() {
        expect:
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        WebFluxAutoConfiguration,
                ))
                .withPropertyValues("openapi-route-definition-locator.internal.filters.enabled-flag-filter.enabled=false")
                .run({ context ->
                    List<OpenApiRouteDefinitionFilter> filters = context.getBeansOfType(OpenApiRouteDefinitionFilter).values().toList()

                    // Verify we have only the GatewayNameFilter
                    Assertions.assertThat(filters).hasSize(1)
                    Assertions.assertThat(filters[0]).isInstanceOf(GatewayNameFilter)

                    // Verify EnabledFlagFilter is not present
                    Assertions.assertThat(context.getBeansOfType(EnabledFlagFilter)).isEmpty()
                })
    }

    def "GatewayNameFilter is not created when disabled"() {
        expect:
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        WebFluxAutoConfiguration,
                ))
                .withPropertyValues("openapi-route-definition-locator.internal.filters.gateway-name-filter.enabled=false")
                .run({ context ->
                    List<OpenApiRouteDefinitionFilter> filters = context.getBeansOfType(OpenApiRouteDefinitionFilter).values().toList()

                    // Verify we have only the EnabledFlagFilter
                    Assertions.assertThat(filters).hasSize(1)
                    Assertions.assertThat(filters[0]).isInstanceOf(EnabledFlagFilter)

                    // Verify GatewayNameFilter is not present
                    Assertions.assertThat(context.getBeansOfType(GatewayNameFilter)).isEmpty()
                })
    }

    def "Both filters are not created when both are disabled"() {
        expect:
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        WebFluxAutoConfiguration,
                ))
                .withPropertyValues(
                        "openapi-route-definition-locator.internal.filters.enabled-flag-filter.enabled=false",
                        "openapi-route-definition-locator.internal.filters.gateway-name-filter.enabled=false"
                )
                .run({ context ->
                    List<OpenApiRouteDefinitionFilter> filters = context.getBeansOfType(OpenApiRouteDefinitionFilter).values().toList()

                    // Verify no filters are present
                    Assertions.assertThat(filters).isEmpty()

                    // Verify specific filter beans are not present
                    Assertions.assertThat(context.getBeansOfType(EnabledFlagFilter)).isEmpty()
                    Assertions.assertThat(context.getBeansOfType(GatewayNameFilter)).isEmpty()
                })
    }
}
