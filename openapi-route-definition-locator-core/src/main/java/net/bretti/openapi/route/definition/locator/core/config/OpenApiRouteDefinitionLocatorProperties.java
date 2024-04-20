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

package net.bretti.openapi.route.definition.locator.core.config;

import lombok.Data;
import net.bretti.openapi.route.definition.locator.core.config.validation.OnlyUniqueServiceIds;
import net.bretti.openapi.route.definition.locator.core.config.validation.ValidBaseUri;
import net.bretti.openapi.route.definition.locator.core.config.validation.ValidOpenApiDefinitionUri;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ConfigurationProperties(prefix = "openapi-route-definition-locator")
@Validated
@Data
public class OpenApiRouteDefinitionLocatorProperties {

    private static final String DEFAULT_OPENAPI_DEFINITION_URI = "/internal/openapi-definition";

    /**
     * List of services for routes should be registered in the gateway based on their
     * OpenAPI definitions.
     */
    @Valid
    @OnlyUniqueServiceIds
    private List<Service> services = new ArrayList<>();

    /**
     * Settings that should be added to all {@link RouteDefinition}s created for the configured
     * {@link OpenApiRouteDefinitionLocatorProperties#services}.
     */
    @Valid
    private DefaultRouteSettings defaultRouteSettings = new DefaultRouteSettings();

    /**
     * Configures the scheduler which periodically retrieves the OpenAPI definitions from
     * the configured services.
     */
    @Valid
    private UpdateScheduler updateScheduler = new UpdateScheduler();

    /**
     * The URI of the OpenAPI definitions to be retrieved from the configured services.
     * This generally is a relative URI; relative to the base URI of each configured service.
     * The default is "/internal/openapi-definition".
     */
    @ValidOpenApiDefinitionUri
    private URI openapiDefinitionUri = URI.create(DEFAULT_OPENAPI_DEFINITION_URI);

    @Data
    public static class Service {

        /**
         * Identifier of the service.
         */
        @NotBlank
        private String id;

        /**
         * Base URI of the service.
         */
        @NotNull
        @ValidBaseUri
        private URI uri;

        /**
         * The URI of the OpenAPI definition to be retrieved from the service.
         * This generally is a relative URI; relative to the service's base URI.
         * But it can also be an absolute URI. As the OpenAPI definition is loaded
         * via Spring's <a href="https://docs.spring.io/spring-framework/docs/6.0.19/reference/html/core.html#resources-resourceloader">
         * ResourceLoader</a>, you can use schemas such as {@code http:}, {@code https:}, {@code file:} or
         * {@code classpath:}. The default is the value of the property
         * {@code openapi-route-definition-locator.openapi-definition-uri}.
         */
        @ValidOpenApiDefinitionUri
        private URI openapiDefinitionUri;

        /**
         * Settings that should be applied to all {@link RouteDefinition}s created for this service.
         */
        @Valid
        private DefaultRouteSettings defaultRouteSettings = new DefaultRouteSettings();
    }

    @Data
    public static class UpdateScheduler {

        /**
         * Fixed delay between runs to retrieve the services' OpenAPI definitions.
         * If no timeunit is given, milliseconds are used.
         */
        @NotNull
        private Duration fixedDelay = Duration.of(5, ChronoUnit.MINUTES);

        /**
         * When an error occurs while retrieving a service's OpenAPI definition, its registered routes/operations
         * are not immediately de-registered. They are only de-registered if there was no successful retrieval
         * for the amount of time configured here. If no timeunit is given, milliseconds are used.
         */
        @NotNull
        private Duration removeRoutesOnUpdateFailuresAfter = Duration.of(15, ChronoUnit.MINUTES);
    }

    /**
     * Settings that should be applied to all created {@link RouteDefinition}s. Contains a subset of the attributes of a
     * {@link RouteDefinition}.
     */
    @Data
    public static class DefaultRouteSettings {
        /**
         * The predicates that should be added to the created {@link RouteDefinition}s.
         */
        @Valid
        private List<PredicateDefinition> predicates = new ArrayList<>();

        /**
         * The filters that should be added to the created {@link RouteDefinition}s.
         */
        @Valid
        private List<FilterDefinition> filters = new ArrayList<>();

        /**
         * The metadata that should be added to the created {@link RouteDefinition}s.
         */
        private Map<String, Object> metadata = new HashMap<>();

        /**
         * The order that should be applied to the created {@link RouteDefinition}s.
         */
        private Optional<Integer> order = Optional.empty();
    }
}
