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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bretti.openapi.route.definition.locator.core.config.OpenApiRouteDefinitionLocatorProperties;
import net.bretti.openapi.route.definition.locator.core.filter.OpenApiRouteDefinitionFilter;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.bretti.openapi.route.definition.locator.core.impl.utils.GatewayRouteSettingsUtil.getGatewayRouteSettings;

@RequiredArgsConstructor
@Slf4j
@Order(200)
public class GatewayNameFilter implements OpenApiRouteDefinitionFilter {

    private static final String GATEWAY_NAMES = "gateway-names";

    private final OpenApiRouteDefinitionLocatorProperties properties;

    @Override
    public boolean test(RouteDefinition routeDefinition,
                        OpenApiRouteDefinitionLocatorProperties.Service service,
                        Map<String, Object> openApiGlobalExtensions,
                        Map<String, Object> openApiOperationExtensions) {

        Optional<Map<String, Object>> gatewayRouteSettings = getGatewayRouteSettings(openApiGlobalExtensions, openApiOperationExtensions);

        // Check gateway-names filtering
        Optional<List<String>> gatewayNames = getGatewayNames(gatewayRouteSettings);
        String configuredGatewayName = properties.getGatewayName();

        if (StringUtils.hasText(configuredGatewayName)) {
            if (gatewayNames.isPresent()) {
                // gateway-names specified: only allow if configured gateway name is in the list
                boolean allowed = gatewayNames.get().contains(configuredGatewayName);
                if (!allowed) {
                    log.info("Route is filtered out because gateway-names of API operation does not contain name of " +
                                    "running gateway, gateway-name={}, API operation gateway-names={}, service={}, " +
                                    "route-predicates={}", configuredGatewayName, gatewayNames.get(), service.getId(),
                            routeDefinition.getPredicates());
                }
                return allowed;
            } else {
                // no gateway-names specified: allow (publish in all gateways)
                return true;
            }
        } else {
            // no gateway name configured: allow all routes
            return true;
        }
    }


    private Optional<List<String>> getGatewayNames(Optional<Map<String, Object>> gatewayRouteSettings) {
        if (!gatewayRouteSettings.isPresent()) {
            return Optional.empty();
        }

        Object gatewayNames = gatewayRouteSettings.get().get(GATEWAY_NAMES);
        if (gatewayNames instanceof List) {
            List<String> result = new java.util.ArrayList<>();
            for (Object item : (List<?>) gatewayNames) {
                if (item instanceof String) {
                    result.add((String) item);
                }
            }
            return result.isEmpty() ? Optional.empty() : Optional.of(result);
        }

        return Optional.empty();
    }
}
