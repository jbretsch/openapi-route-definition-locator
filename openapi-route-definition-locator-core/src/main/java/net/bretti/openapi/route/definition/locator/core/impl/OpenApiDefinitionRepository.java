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

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bretti.openapi.route.definition.locator.core.config.OpenApiRouteDefinitionLocatorProperties;
import net.bretti.openapi.route.definition.locator.core.impl.utils.MapMerge;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_NAME_RETRIEVALS;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_RETRIEVAL_RESULT;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_RETRIEVAL_RESULT_FAILURE;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_RETRIEVAL_RESULT_SUCCESS;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_UPSTREAM_SERVICE;

@RequiredArgsConstructor
@Slf4j
public class OpenApiDefinitionRepository {
    private static final String X_GATEWAY_ROUTE_SETTINGS = "x-gateway-route-settings";
    private static final String FILTERS = "filters";
    private static final String PREDICATES = "predicates";
    private static final String ORDER = "order";
    private static final String METADATA = "metadata";

    private final OpenApiRouteDefinitionLocatorProperties config;

    @Getter
    private final ConcurrentHashMap<OpenApiRouteDefinitionLocatorProperties.Service, List<OpenApiOperation>> operations;
    private final ConcurrentHashMap<OpenApiRouteDefinitionLocatorProperties.Service, Instant> firstRetrievalFailures;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Optional<OpenApiRouteDefinitionLocatorTimedMetrics> metrics;

    void getOpenApiDefinitions() {
        List<Boolean> gotUpdates = config.getServices().stream()
                .map(this::getAndUpdateOperations)
                .collect(Collectors.toList());

        boolean gotAnyUpdates = gotUpdates.contains(true);
        if (gotAnyUpdates) {
            applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
        }
    }

    int getRegisteredOperationsCount(OpenApiRouteDefinitionLocatorProperties.Service service) {
        return Optional.ofNullable(operations.get(service)).orElse(Collections.emptyList()).size();
    }

    private boolean getAndUpdateOperations(OpenApiRouteDefinitionLocatorProperties.Service service) {
        long start = System.nanoTime();
        try {
            log.info("Getting list of operations from {}", service.getId());
            List<OpenApiOperation> newOpenApiOperations = getOperations(service);
            firstRetrievalFailures.remove(service);
            List<OpenApiOperation> oldOpenApiOperations = operations.get(service);
            if (!newOpenApiOperations.equals(oldOpenApiOperations)) {
                log.info("Got new list of operations from {}", service.getId());
                operations.put(service, newOpenApiOperations);
                metricsRecordRetrievalResult(service, METRIC_TAG_RETRIEVAL_RESULT_SUCCESS, start);
                return true;
            }
            log.info("List of operations is unchanged for {}", service.getId());
            metricsRecordRetrievalResult(service, METRIC_TAG_RETRIEVAL_RESULT_SUCCESS, start);
            return false;
        } catch (Exception e) {
            metricsRecordRetrievalResult(service, METRIC_TAG_RETRIEVAL_RESULT_FAILURE, start);
            log.error("Error while retrieving REST operations from {}", service.getId(), e);
            Instant now = Instant.now();
            Instant firstRetrievalFailure = firstRetrievalFailures.computeIfAbsent(service, k -> now);

            List<OpenApiOperation> oldOpenApiOperations = operations.get(service);
            if (oldOpenApiOperations == null || oldOpenApiOperations.isEmpty()) {
                log.error("Retrieving operations from {} keeps failing since {}. Currently, no operations for this " +
                          "service are registered.", service.getId(), firstRetrievalFailure);
                return false;
            }

            Duration removeAfterDuration = config.getUpdateScheduler().getRemoveRoutesOnUpdateFailuresAfter();
            Instant removeAfterInstant = firstRetrievalFailure.plus(removeAfterDuration);

            if (now.isAfter(removeAfterInstant)) {
                operations.remove(service);
                log.error("De-registering operations of {}. First retrieval failure was at {}. " +
                          "That is more than {} ago.", service.getId(), firstRetrievalFailure, removeAfterDuration);
                return true;
            }

            log.error("Keeping operations of {} despite retrieval failure. First retrieval failure was at {}. " +
                      "That is less than {} ago. If retrieval keeps failing, operations of that service will be " +
                      "de-registered after {}.", service.getId(), firstRetrievalFailure, removeAfterDuration,
                      removeAfterInstant);
            return false;
        }
    }

    private void metricsRecordRetrievalResult(
            OpenApiRouteDefinitionLocatorProperties.Service service,
            String metricRetrievalResult,
            long startNanoTime
    ) {
        metrics.ifPresent(metrics1 -> {
            long endNanoTime = System.nanoTime();
            metrics1.recordTime(METRIC_NAME_RETRIEVALS, (endNanoTime- startNanoTime), TimeUnit.NANOSECONDS,
                    METRIC_TAG_RETRIEVAL_RESULT, metricRetrievalResult,
                    METRIC_TAG_UPSTREAM_SERVICE, service.getId());
        });
    }

    private static List<OpenApiOperation> getOperations(OpenApiRouteDefinitionLocatorProperties.Service service) {
        String yaml = getOpenApiDefinitionAsYamlString(service);
        OpenAPI openApi = parseOpenApiDefinition(yaml, service);
        return getOperations(service, openApi);
    }

    private static String getOpenApiDefinitionAsYamlString(OpenApiRouteDefinitionLocatorProperties.Service service) {
        WebClient webClient = WebClient.create(service.getUri().toString());
        return webClient.get().uri("/internal/openapi-definition").retrieve().bodyToMono(String.class).block();
    }

    private static OpenAPI parseOpenApiDefinition(String yaml, OpenApiRouteDefinitionLocatorProperties.Service service) {
        SwaggerParseResult result = new OpenAPIParser().readContents(yaml, null, null);
        String messages = StringUtils.defaultString(StringUtils.join(result.getMessages(), "; "));
        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI == null) {
            throw new IllegalArgumentException("Error while parsing OpenAPI definition: " + messages);
        }
        if (StringUtils.isNoneBlank(messages)) {
            log.warn("Warnings while parsing OpenAPI definition of {}: {}", service, messages);
        }
        return openAPI;
    }

    private static List<OpenApiOperation> getOperations(OpenApiRouteDefinitionLocatorProperties.Service service, OpenAPI openApi) {
        Optional<Map<String, Object>> globalGatewayRouteSettings = getGatewayRouteSettings(openApi.getExtensions());

        List<OpenApiOperation> result = new ArrayList<>();
        openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((httpMethod, openApiOperation) -> {
                    Optional<Map<String, Object>> operationGatewayRouteSettings = getGatewayRouteSettings(openApiOperation.getExtensions());
                    Optional<Map<String, Object>> gatewayRouteSettings = MapMerge.deepMerge(globalGatewayRouteSettings, operationGatewayRouteSettings);

                    List<FilterDefinition> filters = getFilters(gatewayRouteSettings);
                    List<PredicateDefinition> predicates = getPredicates(gatewayRouteSettings);
                    Optional<Map<String, Object>> metadata = getMetadata(gatewayRouteSettings);
                    Optional<Integer> order = getOrder(gatewayRouteSettings);

                    OpenApiOperation operation = OpenApiOperation.builder()
                            .baseUri(service.getUri())
                            .httpMethod(map(httpMethod))
                            .path(path)
                            .filters(filters)
                            .predicates(predicates)
                            .metadata(metadata)
                            .order(order)
                            .build();
                    result.add(operation);
                })
        );
        return result;
    }

    private static HttpMethod map(PathItem.HttpMethod method) {
        return HttpMethod.resolve(method.name());
    }

    private static Optional<Map<String, Object>> getGatewayRouteSettings(Map<String, Object> extensions) {
        if (extensions == null) {
            return Optional.empty();
        }

        Object gatewayRouteSettings = extensions.get(X_GATEWAY_ROUTE_SETTINGS);
        if (!(gatewayRouteSettings instanceof Map)) {
            return Optional.empty();
        }

        return Optional.of((Map<String, Object>)gatewayRouteSettings);
    }

    private static List<FilterDefinition> getFilters(Optional<Map<String, Object>> gatewayRouteSettings) {
        return getGatewayRouteSettingsAtKey(gatewayRouteSettings, FILTERS, OpenApiDefinitionRepository::toFilterDefinition);
    }

    private static Optional<FilterDefinition> toFilterDefinition(Object filterDefinitionObj) {
        if (filterDefinitionObj instanceof String) {
            return Optional.of(new FilterDefinition((String)filterDefinitionObj));
        }

        if (filterDefinitionObj instanceof Map) {
            Map<String, Object> m = (Map<String, Object>)filterDefinitionObj;
            String name = (String)m.get("name");
            Map<String, Object> argsOrig = (Map<String, Object>)m.getOrDefault("args", Collections.emptyMap());
            Map<String, String> args = argsOrig.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
            FilterDefinition filterDefinition = new FilterDefinition();
            filterDefinition.setName(name);
            filterDefinition.setArgs(args);
            return Optional.of(filterDefinition);
        }

        log.error("Error while parsing '{}'", filterDefinitionObj);
        return Optional.empty();
    }

    private static List<PredicateDefinition> getPredicates(Optional<Map<String, Object>> gatewayRouteSettings) {
        return getGatewayRouteSettingsAtKey(gatewayRouteSettings, PREDICATES, OpenApiDefinitionRepository::toPredicateDefinition);
    }

    private static <T> List<T> getGatewayRouteSettingsAtKey(
            Optional<Map<String, Object>> gatewayRouteSettings,
            String key,
            Function<Object, Optional<T>> mapper
    ) {
        if (!gatewayRouteSettings.isPresent()) {
            return Collections.emptyList();
        }

        Object gatewayRouteSettingsAtKey = gatewayRouteSettings.get().get(key);
        if (!(gatewayRouteSettingsAtKey instanceof List)) {
            return Collections.emptyList();
        }

        List<Object> gatewayRouteSettingsAtKeyAsList = (List<Object>)gatewayRouteSettingsAtKey;

        return gatewayRouteSettingsAtKeyAsList.stream()
                .map(mapper)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private static Optional<PredicateDefinition> toPredicateDefinition(Object predicateDefinitionObj) {
        if (predicateDefinitionObj instanceof String) {
            return Optional.of(new PredicateDefinition((String)predicateDefinitionObj));
        }

        if (predicateDefinitionObj instanceof Map) {
            Map<String, Object> m = (Map<String, Object>)predicateDefinitionObj;
            String name = (String)m.get("name");
            Map<String, Object> argsOrig = (Map<String, Object>)m.getOrDefault("args", Collections.emptyMap());
            Map<String, String> args = argsOrig.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
            PredicateDefinition predicateDefinition = new PredicateDefinition();
            predicateDefinition.setName(name);
            predicateDefinition.setArgs(args);
            return Optional.of(predicateDefinition);
        }

        log.error("Error while parsing '{}'", predicateDefinitionObj);
        return Optional.empty();
    }

    private static Optional<Integer> getOrder(Optional<Map<String, Object>> gatewayRouteSettings) {
        if (!gatewayRouteSettings.isPresent()) {
            return Optional.empty();
        }

        Object gatewayOrder = gatewayRouteSettings.get().get(ORDER);
        if (!(gatewayOrder instanceof Integer)) {
            return Optional.empty();
        }

        return Optional.of((Integer)gatewayOrder);
    }

    private static Optional<Map<String, Object>> getMetadata(Optional<Map<String, Object>> gatewayRouteSettings) {
        if (!gatewayRouteSettings.isPresent()) {
            return Optional.empty();
        }

        Object gatewayMetadata = gatewayRouteSettings.get().get(METADATA);
        if (!(gatewayMetadata instanceof Map)) {
            return Optional.empty();
        }

        return Optional.of((Map<String, Object>)gatewayMetadata);
    }

}
