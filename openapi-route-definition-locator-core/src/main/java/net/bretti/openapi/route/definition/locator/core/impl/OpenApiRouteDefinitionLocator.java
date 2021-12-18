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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class OpenApiRouteDefinitionLocator implements RouteDefinitionLocator {

    private final OpenApiDefinitionRepository repository;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        List<RouteDefinition> routeDefinitions = new ArrayList<>();
        repository.getOperations().forEach((service, operations) -> operations.forEach(operation -> {
            RouteDefinition routeDefinition = new RouteDefinition();
            routeDefinition.setId(UUID.randomUUID().toString());
            routeDefinition.setUri(operation.getBaseUri());

            PredicateDefinition pathPredicate = new PredicateDefinition("Path=" + operation.getPath());
            PredicateDefinition methodPredicate = new PredicateDefinition("Method=" + operation.getHttpMethod());

            List<PredicateDefinition> predicates = new ArrayList<>();
            predicates.add(methodPredicate);
            predicates.add(pathPredicate);
            predicates.addAll(operation.getPredicates());
            routeDefinition.setPredicates(predicates);

            routeDefinition.setFilters(operation.getFilters());

            operation.getOrder().ifPresent(routeDefinition::setOrder);
            operation.getMetadata().ifPresent(routeDefinition::setMetadata);

            routeDefinitions.add(routeDefinition);
        }));

        return Flux.fromIterable(routeDefinitions);
    }
}
