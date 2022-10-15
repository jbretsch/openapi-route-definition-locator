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
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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
         * But it can also be an absolute URI. The default is
         * the value of the property `openapi-route-definition-locator.openapi-definition-uri`.
         */
        @ValidOpenApiDefinitionUri
        private URI openapiDefinitionUri;
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

}
