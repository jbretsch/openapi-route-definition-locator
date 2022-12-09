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

package net.bretti.openapi.route.definition.locator.core.customizer;

import net.bretti.openapi.route.definition.locator.core.config.OpenApiRouteDefinitionLocatorProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.Map;

@FunctionalInterface
public interface OpenApiRouteDefinitionCustomizer {
    void customize(RouteDefinition routeDefinition,
                   OpenApiRouteDefinitionLocatorProperties.Service service,
                   Map<String, Object> openApiGlobalExtensions,
                   Map<String, Object> openApiOperationExtensions);
}
