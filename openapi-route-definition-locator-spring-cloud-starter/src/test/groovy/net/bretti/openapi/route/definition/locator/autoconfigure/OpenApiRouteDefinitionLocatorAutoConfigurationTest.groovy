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

package net.bretti.openapi.route.definition.locator.autoconfigure


import net.bretti.openapi.route.definition.locator.core.config.OpenApiRouteDefinitionLocatorProperties
import net.bretti.openapi.route.definition.locator.core.impl.OpenApiDefinitionRepository
import net.bretti.openapi.route.definition.locator.core.impl.OpenApiDefinitionUpdateScheduler
import net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocator
import org.assertj.core.api.Assertions
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration
import spock.lang.Specification

class OpenApiRouteDefinitionLocatorAutoConfigurationTest extends Specification {
    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()

    def "OpenAPI Route Definition Locator is active if GatewayAutoConfiguration is present"() {
        expect:
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        WebFluxAutoConfiguration,
                ))
                .run({ context ->
                    Assertions.assertThat(context).hasSingleBean(OpenApiDefinitionRepository)
                    Assertions.assertThat(context).hasSingleBean(OpenApiRouteDefinitionLocatorProperties)
                    Assertions.assertThat(context).hasSingleBean(OpenApiRouteDefinitionLocator)
                    Assertions.assertThat(context).hasSingleBean(OpenApiDefinitionUpdateScheduler)
                })
    }

    def "OpenAPI Route Definition Locator is inactive if GatewayAutoConfiguration is present but OpenApiRouteDefinitionLocator is disabled"() {
        expect:
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        WebFluxAutoConfiguration,
                ))
                .withPropertyValues("openapi-route-definition-locator.enabled=false")
                .run({ context ->
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiDefinitionRepository)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocatorProperties)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocator)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiDefinitionUpdateScheduler)
                })
    }

    def "OpenAPI Route Definition Locator is inactive if Spring Cloud Gateway is disabled"() {
        expect:
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        WebFluxAutoConfiguration,
                ))
                .withPropertyValues("spring.cloud.gateway.enabled=false")
                .run({ context ->
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiDefinitionRepository)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocatorProperties)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocator)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiDefinitionUpdateScheduler)
                })
    }

    def "OpenAPI Route Definition Locator is inactive if GatewayAutoConfiguration is absent"() {
        expect:
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                ))
                .withPropertyValues("openapi-route-definition-locator.enabled=false")
                .run({ context ->
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiDefinitionRepository)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocatorProperties)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocator)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiDefinitionUpdateScheduler)
                })
    }

}
