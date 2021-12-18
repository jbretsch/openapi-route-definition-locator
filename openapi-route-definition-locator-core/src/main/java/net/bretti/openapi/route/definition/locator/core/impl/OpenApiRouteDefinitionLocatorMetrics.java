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

package net.bretti.openapi.route.definition.locator.core.impl;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import net.bretti.openapi.route.definition.locator.core.config.OpenApiRouteDefinitionLocatorProperties;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class OpenApiRouteDefinitionLocatorMetrics {
    static final String METRIC_NAME_RETRIEVALS = "openapi_route_definition_locator_openapi_definition_retrievals";
    private static final String METRIC_DESCRIPTION_RETRIEVALS = "Time and count of attempts to retrieve the OpenAPI definition of registered services.";
    private static final String METRIC_NAME_ROUTES = "openapi_route_definition_locator_routes_count";
    private static final String METRIC_DESCRIPTION_ROUTES = "Number of routes managed by the OpenAPI Route Definition Locator";
    static final String METRIC_TAG_UPSTREAM_SERVICE = "upstream_service";
    static final String METRIC_TAG_RETRIEVAL_RESULT = "retrieval_result";
    static final String METRIC_TAG_RETRIEVAL_RESULT_SUCCESS = "success";
    static final String METRIC_TAG_RETRIEVAL_RESULT_FAILURE = "failure";

    private final MeterRegistry meterRegistry;
    private final OpenApiRouteDefinitionLocatorProperties config;
    private final OpenApiDefinitionRepository openApiDefinitionRepository;

    @PostConstruct
    private void postConstruct() {
        List<String> retrievalResults = Arrays.asList(
                METRIC_TAG_RETRIEVAL_RESULT_SUCCESS,
                METRIC_TAG_RETRIEVAL_RESULT_FAILURE
        );

        config.getServices().forEach(service -> {
            retrievalResults.forEach(retrievalResult ->
                    Timer.builder(METRIC_NAME_RETRIEVALS)
                            .description(METRIC_DESCRIPTION_RETRIEVALS)
                            .tags(METRIC_TAG_UPSTREAM_SERVICE, service.getId(),
                                    METRIC_TAG_RETRIEVAL_RESULT, retrievalResult)
                            .publishPercentiles(0.5, 0.8, 0.95, 0.98)
                            .register(meterRegistry));

            Gauge.builder(METRIC_NAME_ROUTES, () -> openApiDefinitionRepository.getRegisteredOperationsCount(service))
                    .description(METRIC_DESCRIPTION_ROUTES)
                    .tag(METRIC_TAG_UPSTREAM_SERVICE, service.getId())
                    .strongReference(true)
                    .register(meterRegistry);
        });
    }

}
