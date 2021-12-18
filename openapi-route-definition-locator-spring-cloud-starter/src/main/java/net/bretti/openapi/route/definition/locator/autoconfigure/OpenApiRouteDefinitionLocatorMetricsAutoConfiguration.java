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

package net.bretti.openapi.route.definition.locator.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import net.bretti.openapi.route.definition.locator.core.config.OpenApiRouteDefinitionLocatorProperties;
import net.bretti.openapi.route.definition.locator.core.impl.OpenApiDefinitionRepository;
import net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics;
import net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorTimedMetrics;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.config.GatewayMetricsAutoConfiguration;
import org.springframework.cloud.gateway.route.RouteDefinitionMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({OpenApiRouteDefinitionLocatorAutoConfiguration.class, GatewayMetricsAutoConfiguration.class})
@ConditionalOnProperty(name = "openapi-route-definition-locator.metrics.enabled", matchIfMissing = true)
@ConditionalOnBean({ OpenApiDefinitionRepository.class, OpenApiRouteDefinitionLocatorProperties.class,
        RouteDefinitionMetrics.class, MeterRegistry.class })
public class OpenApiRouteDefinitionLocatorMetricsAutoConfiguration {

    @Bean
    public OpenApiRouteDefinitionLocatorMetrics openApiRouteDefinitionLocatorMetrics(
            MeterRegistry meterRegistry,
            OpenApiRouteDefinitionLocatorProperties config,
            OpenApiDefinitionRepository openApiDefinitionRepository) {
        return new OpenApiRouteDefinitionLocatorMetrics(meterRegistry, config, openApiDefinitionRepository);
    }

    @Bean
    public OpenApiRouteDefinitionLocatorTimedMetrics openApiRouteDefinitionLocatorTimedMetrics(
            MeterRegistry meterRegistry) {
        return new OpenApiRouteDefinitionLocatorTimedMetrics(meterRegistry);
    }

}
