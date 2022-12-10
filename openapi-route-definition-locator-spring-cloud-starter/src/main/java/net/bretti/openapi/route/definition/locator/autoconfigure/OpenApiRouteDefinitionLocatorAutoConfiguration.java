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

import net.bretti.openapi.route.definition.locator.core.config.OpenApiRouteDefinitionLocatorProperties;
import net.bretti.openapi.route.definition.locator.core.customizer.OpenApiRouteDefinitionCustomizer;
import net.bretti.openapi.route.definition.locator.core.impl.OpenApiDefinitionRepository;
import net.bretti.openapi.route.definition.locator.core.impl.OpenApiDefinitionUpdateScheduler;
import net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocator;
import net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorTimedMetrics;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(GatewayAutoConfiguration.class)
@ConditionalOnBean(GatewayAutoConfiguration.class)
@ConditionalOnProperty(value = "openapi-route-definition-locator.enabled", matchIfMissing = true)
@EnableConfigurationProperties
@EnableScheduling
public class OpenApiRouteDefinitionLocatorAutoConfiguration {
    @Bean
    public OpenApiDefinitionRepository openApiDefinitionRepository(
            OpenApiRouteDefinitionLocatorProperties config,
            ApplicationEventPublisher applicationEventPublisher,
            Optional<OpenApiRouteDefinitionLocatorTimedMetrics> metrics,
            ResourceLoader resourceLoader) {
        return new OpenApiDefinitionRepository(config, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(),
                applicationEventPublisher, metrics, resourceLoader);
    }

    @Bean
    public OpenApiRouteDefinitionLocatorProperties openApiRouteDefinitionLocatorProperties() {
        return new OpenApiRouteDefinitionLocatorProperties();
    }

    @Bean
    public OpenApiRouteDefinitionLocator openApiRouteDefinitionLocator(
            OpenApiDefinitionRepository openApiDefinitionRepository,
            List<OpenApiRouteDefinitionCustomizer> openApiRouteDefinitionCustomizers
    ) {
        return new OpenApiRouteDefinitionLocator(openApiDefinitionRepository, openApiRouteDefinitionCustomizers);
    }

    @Bean
    public OpenApiDefinitionUpdateScheduler openApiDefinitionUpdateScheduler(
            OpenApiDefinitionRepository openApiDefinitionRepository
    ) {
        return new OpenApiDefinitionUpdateScheduler(openApiDefinitionRepository);
    }
}
