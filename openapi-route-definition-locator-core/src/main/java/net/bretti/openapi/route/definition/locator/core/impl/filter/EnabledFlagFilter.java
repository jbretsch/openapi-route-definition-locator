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

package net.bretti.openapi.route.definition.locator.core.impl.filter;

import lombok.extern.slf4j.Slf4j;
import net.bretti.openapi.route.definition.locator.core.config.OpenApiRouteDefinitionLocatorProperties;
import net.bretti.openapi.route.definition.locator.core.filter.OpenApiRouteDefinitionFilter;
import net.bretti.openapi.route.definition.locator.core.impl.utils.GatewayRouteSettingsUtil;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.core.annotation.Order;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Order(100)
public class EnabledFlagFilter implements OpenApiRouteDefinitionFilter {

    private static final String ENABLED = "enabled";

    @Override
    public boolean test(RouteDefinition routeDefinition,
                        OpenApiRouteDefinitionLocatorProperties.Service service,
                        Map<String, Object> openApiGlobalExtensions,
                        Map<String, Object> openApiOperationExtensions) {

        Optional<Map<String, Object>> gatewayRouteSettings = GatewayRouteSettingsUtil.getGatewayRouteSettings(openApiGlobalExtensions, openApiOperationExtensions);

        boolean enabled = getEnabled(gatewayRouteSettings);
        if (!enabled) {
            log.info("Route is filtered out because of 'enabled: false' on API operation, service={}, route-predicates={}",
                    service.getId(), routeDefinition.getPredicates());
            return false;
        }

        return true;
    }

    private boolean getEnabled(Optional<Map<String, Object>> gatewayRouteSettings) {
        if (!gatewayRouteSettings.isPresent()) {
            return true; // Default: enabled
        }

        Object enabled = gatewayRouteSettings.get().get(ENABLED);
        if (enabled instanceof Boolean) {
            return (Boolean) enabled;
        }

        return true; // Default: enabled
    }
}
