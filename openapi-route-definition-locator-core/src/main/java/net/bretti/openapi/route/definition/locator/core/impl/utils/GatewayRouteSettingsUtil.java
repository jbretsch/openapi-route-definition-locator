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

package net.bretti.openapi.route.definition.locator.core.impl.utils;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Optional;

@UtilityClass
public final class GatewayRouteSettingsUtil {

    public static final String X_GATEWAY_ROUTE_SETTINGS = "x-gateway-route-settings";

    public static Optional<Map<String, Object>> getGatewayRouteSettings(Map<String, Object> globalExtensions, Map<String, Object> operationExtensions) {
        Optional<Map<String, Object>> globalGatewayRouteSettings = getGatewayRouteSettings(globalExtensions);
        Optional<Map<String, Object>> operationGatewayRouteSettings = getGatewayRouteSettings(operationExtensions);

        return MapMerge.deepMerge(globalGatewayRouteSettings, operationGatewayRouteSettings);
    }

    public static Optional<Map<String, Object>> getGatewayRouteSettings(Map<String, Object> extensions) {
        if (extensions == null) {
            return Optional.empty();
        }

        Object gatewayRouteSettings = extensions.get(X_GATEWAY_ROUTE_SETTINGS);
        if (!(gatewayRouteSettings instanceof Map)) {
            return Optional.empty();
        }

        return Optional.of((Map<String, Object>) gatewayRouteSettings);
    }
}