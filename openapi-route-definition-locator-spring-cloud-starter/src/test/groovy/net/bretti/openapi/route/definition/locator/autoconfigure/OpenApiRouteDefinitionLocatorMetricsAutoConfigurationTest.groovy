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


import net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics
import net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorTimedMetrics
import org.assertj.core.api.Assertions
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration
import org.springframework.cloud.gateway.config.GatewayMetricsAutoConfiguration
import spock.lang.Specification

class OpenApiRouteDefinitionLocatorMetricsAutoConfigurationTest extends Specification {
    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()

    def "OpenAPI Route Definition Locator metrics are active if GatewayMetricsAutoConfiguration is present"() {
        expect:
        contextRunner
                .withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorMetricsAutoConfiguration,
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        GatewayMetricsAutoConfiguration,
                        WebFluxAutoConfiguration,
                        MetricsAutoConfiguration,
                        CompositeMeterRegistryAutoConfiguration,
                ))
                .run({ context ->
                    Assertions.assertThat(context).hasSingleBean(OpenApiRouteDefinitionLocatorMetrics)
                    Assertions.assertThat(context).hasSingleBean(OpenApiRouteDefinitionLocatorTimedMetrics)
                })
    }

    def "OpenAPI Route Definition Locator metrics are inactive if they are explicitly disabled"() {
        expect:
        contextRunner
                .withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorMetricsAutoConfiguration,
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        GatewayMetricsAutoConfiguration,
                        WebFluxAutoConfiguration,
                        MetricsAutoConfiguration,
                        CompositeMeterRegistryAutoConfiguration,
                ))
                .withPropertyValues("openapi-route-definition-locator.metrics.enabled=false")
                .run({ context ->
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocatorMetrics)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocatorTimedMetrics)
                })
    }

    def "OpenAPI Route Definition Locator metrics are inactive if the OpenAPI Route Definition Locator is explicitly disabled"() {
        expect:
        contextRunner
                .withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorMetricsAutoConfiguration,
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        GatewayMetricsAutoConfiguration,
                        WebFluxAutoConfiguration,
                        MetricsAutoConfiguration,
                        CompositeMeterRegistryAutoConfiguration,
                ))
                .withPropertyValues("openapi-route-definition-locator.enabled=false")
                .run({ context ->
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocatorMetrics)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocatorTimedMetrics)
                })
    }

    def "OpenAPI Route Definition Locator metrics are inactive if Spring Cloud metrics are explicitly disabled"() {
        expect:
        contextRunner
                .withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorMetricsAutoConfiguration,
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        GatewayMetricsAutoConfiguration,
                        WebFluxAutoConfiguration,
                        MetricsAutoConfiguration,
                        CompositeMeterRegistryAutoConfiguration,
                ))
                .withPropertyValues("spring.cloud.gateway.metrics.enabled=false")
                .run({ context ->
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocatorMetrics)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocatorTimedMetrics)
                })
    }

    def "OpenAPI Route Definition Locator metrics are inactive if metrics are globally absent"() {
        expect:
        contextRunner
                .withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
                .withConfiguration(AutoConfigurations.of(
                        OpenApiRouteDefinitionLocatorMetricsAutoConfiguration,
                        OpenApiRouteDefinitionLocatorAutoConfiguration,
                        GatewayAutoConfiguration,
                        GatewayMetricsAutoConfiguration,
                        WebFluxAutoConfiguration,
                ))
                .run({ context ->
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocatorMetrics)
                    Assertions.assertThat(context).doesNotHaveBean(OpenApiRouteDefinitionLocatorTimedMetrics)
                })
    }

}
