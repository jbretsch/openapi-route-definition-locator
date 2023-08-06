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
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_NAME_UPDATES;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_UPDATE_RESULT;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_UPDATE_RESULT_DETAILED;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_UPDATE_RESULT_DETAILED_FAILURE_PUBLICATION;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_UPDATE_RESULT_DETAILED_FAILURE_RETRIEVAL;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_UPDATE_RESULT_DETAILED_SUCCESS_WITHOUT_CHANGES;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_UPDATE_RESULT_DETAILED_SUCCESS_WITH_CHANGES;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_UPDATE_RESULT_FAILURE;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_UPDATE_RESULT_SUCCESS;
import static net.bretti.openapi.route.definition.locator.core.impl.OpenApiRouteDefinitionLocatorMetrics.METRIC_TAG_UPSTREAM_SERVICE;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@RequiredArgsConstructor
@Slf4j
public class OpenApiDefinitionRepository implements ApplicationListener<RefreshRoutesResultEvent> {
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
    private final ResourceLoader resourceLoader;
    private Throwable lastRouteDefinitionPublicationFailureCause;

    void getOpenApiDefinitions() {
        config.getServices().forEach(this::getAndUpdateOperationsSafely);
    }

    int getRegisteredOperationsCount(OpenApiRouteDefinitionLocatorProperties.Service service) {
        return Optional.ofNullable(operations.get(service)).orElse(Collections.emptyList()).size();
    }

    private void getAndUpdateOperationsSafely(OpenApiRouteDefinitionLocatorProperties.Service service) {
        try {
            getAndUpdateOperations(service);
        } catch (Exception e) {
            log.error("Unexpected error while retrieving and publishing REST operations for {}", service.getId(), e);
        }
    }

    private void getAndUpdateOperations(OpenApiRouteDefinitionLocatorProperties.Service service) {
        long start = System.nanoTime();
        List<OpenApiOperation> oldOpenApiOperations = operations.get(service);
        try {
            log.info("Getting list of operations for {}", service.getId());
            List<OpenApiOperation> newOpenApiOperations = getOperations(service);

            if (newOpenApiOperations.equals(oldOpenApiOperations)) {
                log.info("List of {} operations is unchanged for {}", oldOpenApiOperations.size(), service.getId());
                firstRetrievalFailures.remove(service);
                metricsRecordRetrievalResult(service, METRIC_TAG_UPDATE_RESULT_SUCCESS,
                        METRIC_TAG_UPDATE_RESULT_DETAILED_SUCCESS_WITHOUT_CHANGES, start);
                return;
            }

            log.info("Got new list of {} operations for {}", newOpenApiOperations.size(), service.getId());
            operations.put(service, newOpenApiOperations);
            publishNewOpenApiOperationsAndRollbackOnFailure(service, oldOpenApiOperations);

            // Only reached if no rollback was performed.
            firstRetrievalFailures.remove(service);
            metricsRecordRetrievalResult(service, METRIC_TAG_UPDATE_RESULT_SUCCESS,
                    METRIC_TAG_UPDATE_RESULT_DETAILED_SUCCESS_WITH_CHANGES, start);
        } catch (Exception e) {
            String updateResultFailureDetailed = e instanceof OpenApiRouteDefinitionPublishException
                    ? METRIC_TAG_UPDATE_RESULT_DETAILED_FAILURE_PUBLICATION
                    : METRIC_TAG_UPDATE_RESULT_DETAILED_FAILURE_RETRIEVAL;
            metricsRecordRetrievalResult(service, METRIC_TAG_UPDATE_RESULT_FAILURE, updateResultFailureDetailed, start);
            log.error("Error while retrieving and publishing REST operations for {}", service.getId(), e);
            Instant now = Instant.now();
            Instant firstRetrievalFailure = firstRetrievalFailures.computeIfAbsent(service, k -> now);

            if (CollectionUtils.isEmpty(oldOpenApiOperations)) {
                log.error("Retrieving and publishing operations for {} keeps failing since {}. Currently, no operations for this " +
                          "service are registered.", service.getId(), firstRetrievalFailure);
                return;
            }

            Duration removeAfterDuration = config.getUpdateScheduler().getRemoveRoutesOnUpdateFailuresAfter();
            Instant removeAfterInstant = firstRetrievalFailure.plus(removeAfterDuration);

            if (now.isAfter(removeAfterInstant)) {
                operations.remove(service);
                log.error("De-registering operations of {}. First retrieval/publishing failure was at {}. " +
                          "That is more than {} ago.", service.getId(), firstRetrievalFailure, removeAfterDuration);
                publishNewOpenApiOperations(service);
                return;
            }

            log.error("Keeping operations of {} despite retrieval/publishing failure. First failure was at {}. " +
                      "That is less than {} ago. If attempts keep failing, operations of that service will be " +
                      "de-registered after {}.", service.getId(), firstRetrievalFailure, removeAfterDuration,
                      removeAfterInstant);
        }
    }

    private void metricsRecordRetrievalResult(
            OpenApiRouteDefinitionLocatorProperties.Service service,
            String metricUpdateResult,
            String metricUpdateResultDetailed,
            long startNanoTime
    ) {
        metrics.ifPresent(metrics1 -> {
            long endNanoTime = System.nanoTime();
            metrics1.recordTime(METRIC_NAME_UPDATES, (endNanoTime- startNanoTime), TimeUnit.NANOSECONDS,
                    METRIC_TAG_UPDATE_RESULT, metricUpdateResult,
                    METRIC_TAG_UPDATE_RESULT_DETAILED, metricUpdateResultDetailed,
                    METRIC_TAG_UPSTREAM_SERVICE, service.getId());
        });
    }

    private List<OpenApiOperation> getOperations(OpenApiRouteDefinitionLocatorProperties.Service service) {
        String yaml = getOpenApiDefinitionAsYamlString(service);
        OpenAPI openApi = parseOpenApiDefinition(yaml, service);
        return getOperations(service, openApi);
    }

    private String getOpenApiDefinitionAsYamlString(OpenApiRouteDefinitionLocatorProperties.Service service) {
        URI openApiDefinitionUri = firstNonNull(service.getOpenapiDefinitionUri(), config.getOpenapiDefinitionUri());
        URI fullOpenApiDefinitionUri = service.getUri().resolve(openApiDefinitionUri);

        log.info("Retrieving OpenAPI definition for {} from '{}'", service.getId(), fullOpenApiDefinitionUri);
        Resource resource = resourceLoader.getResource(fullOpenApiDefinitionUri.toString());
        try (InputStream is = resource.getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error loading '%s'", fullOpenApiDefinitionUri), e);
        }
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
                            .openApiExtension(firstNonNull(openApi.getExtensions(), Collections.emptyMap()))
                            .openApiOperationExtension(firstNonNull(openApiOperation.getExtensions(), Collections.emptyMap()))
                            .build();

                    result.add(operation);
                })
        );
        return result;
    }

    private static HttpMethod map(PathItem.HttpMethod method) {
        return HttpMethod.valueOf(method.name());
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

    private void publishNewOpenApiOperationsAndRollbackOnFailure(
            OpenApiRouteDefinitionLocatorProperties.Service service,
            List<OpenApiOperation> oldOpenApiOperations
    ) {
        try {
            publishNewOpenApiOperations(service);
        } catch (Exception e) {
            if (oldOpenApiOperations == null) {
                operations.remove(service);
            } else {
                operations.put(service, oldOpenApiOperations);
            }
            publishNewOpenApiOperations(service);
            throw e;
        }
    }

    private void publishNewOpenApiOperations(OpenApiRouteDefinitionLocatorProperties.Service service) {
        lastRouteDefinitionPublicationFailureCause = null;
        applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
        if (lastRouteDefinitionPublicationFailureCause != null) {
            throw new OpenApiRouteDefinitionPublishException(String.format("Error while publishing route" +
                    " definitions for %s", service.getId()), lastRouteDefinitionPublicationFailureCause);
        }
    }

    @Override
    public void onApplicationEvent(@NonNull RefreshRoutesResultEvent event) {
        if (event.isSuccess()) {
            return;
        }

        boolean isErrorCausedByThisClass = Arrays.stream(event.getThrowable().getStackTrace())
                .anyMatch(t -> t.getClassName().equals(this.getClass().getCanonicalName()));

        if (isErrorCausedByThisClass) {
            lastRouteDefinitionPublicationFailureCause = event.getThrowable();
        }
    }
}
