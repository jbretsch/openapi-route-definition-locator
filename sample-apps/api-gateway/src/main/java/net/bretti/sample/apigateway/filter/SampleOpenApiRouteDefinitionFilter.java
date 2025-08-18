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

package net.bretti.sample.apigateway.filter;

import net.bretti.openapi.route.definition.locator.core.config.OpenApiRouteDefinitionLocatorProperties;
import net.bretti.openapi.route.definition.locator.core.filter.OpenApiRouteDefinitionFilter;
import net.bretti.openapi.route.definition.locator.core.impl.utils.MapMerge;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class SampleOpenApiRouteDefinitionFilter implements OpenApiRouteDefinitionFilter {

    @Override
    public boolean test(RouteDefinition routeDefinition,
                        OpenApiRouteDefinitionLocatorProperties.Service service,
                        Map<String, Object> openApiGlobalExtensions,
                        Map<String, Object> openApiOperationExtensions) {

        // Example: Only publish operations marked for the current environment.
        Map<String, Object> openApiExtensions = MapMerge.deepMerge(openApiGlobalExtensions, openApiOperationExtensions);
        Object apiOperationEnv = openApiExtensions.get("x-environment");
        if (apiOperationEnv instanceof String) {
            String currentEnv = System.getenv("DEPLOY_ENV");
            return Objects.equals(currentEnv, apiOperationEnv.toString());
        }

        // Publish API operation if it specifies no environment.
        return true;
    }
}
