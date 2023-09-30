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

package componenttest

import componenttest.setup.basetest.BaseCompTest
import componenttest.setup.wiremock.OpenapiDefinitionServedFromDifferentHostServiceMock1
import componenttest.setup.wiremock.OpenapiDefinitionServedFromDifferentHostServiceMock2
import componenttest.setup.wiremock.OrderServiceMock
import componenttest.setup.wiremock.UserServiceMock
import org.springframework.test.web.reactive.server.FluxExchangeResult

import java.time.Duration
import java.time.Instant

class OpenApiRouteDefinitionLocatorCompTest extends BaseCompTest {

    def "API Gateway routes requests according to OpenAPI definitions"() {
        given:
        waitForRemovalOfAllRoutes()

        and:
        UserServiceMock.instance.mockOpenApiDefinition()
        UserServiceMock.instance.mockGetUsers()
        UserServiceMock.instance.mockGetUser()

        and:
        OrderServiceMock.instance.mockOpenApiDefinition()
        OrderServiceMock.instance.mockGetOrders()
        OrderServiceMock.instance.mockGetOrder()
        OrderServiceMock.instance.mockPostOrder()

        and:
        OpenapiDefinitionServedFromDifferentHostServiceMock1.instance.mockGetThings()
        OpenapiDefinitionServedFromDifferentHostServiceMock2.instance.mockOpenApiDefinition()

        when:
        waitForRouteAddition {
            assert getRoutesFromActuatorEndpoint().size() == 7
        }

        and:
        List routes = getRoutesFromActuatorEndpoint()

        then:
        Map getUsersRoute = extractRoute(routes, "GET", "/users")
        getUsersRoute.predicate == "((Methods: [GET] && Paths: [/users], match trailing slash: true) && Header: Authorization regexp=null)"
        getUsersRoute.route_id != null
        getUsersRoute.filters == [
                "[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]",
                "[[AddResponseHeader X-Response-DefaultForAllServices = 'sample-value-all'], order = 1]",
                "[[AddResponseHeader X-Response-DefaultForOneService = 'sample-value-one'], order = 2]",
                "[[AddResponseHeader X-Auth-Type-Was = 'Application'], order = 3]",
        ]
        getUsersRoute.uri == "http://localhost:9091"
        getUsersRoute.order == 6
        getUsersRoute.metadata == [
                defaultForAllServices                    : 'OptionValueAll',
                defaultForOneService                     : 'OptionValueOne',
                AddedByXAuthTypeRouteDefinitionCustomizer: 'Application',
        ]
        getUsersRoute.size() == 6

        and:
        Map getUserRoute = extractRoute(routes, "GET", "/users/{userId}")
        getUserRoute.predicate ==
                "((((Methods: [GET] && Paths: [/users/{userId}], match trailing slash: true) && " +
                "After: 2022-01-20T17:42:47.789+01:00[Europe/Berlin]) && " +
                "Header: Required-Test-Header regexp=required-test-header-.*) && " +
                "Header: Authorization regexp=null)"
        getUserRoute.route_id != null
        getUserRoute.filters == [
                "[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]",
                "[[AddResponseHeader X-Response-DefaultForAllServices = 'sample-value-all'], order = 1]",
                "[[AddResponseHeader X-Response-DefaultForOneService = 'sample-value-one'], order = 2]",
                "[[AddResponseHeader X-Auth-Type-Was = 'Application User'], order = 3]",
        ]
        getUserRoute.uri == "http://localhost:9091"
        getUserRoute.order == 6
        getUserRoute.metadata == [
                defaultForAllServices                    : 'OptionValueAll',
                defaultForOneService                     : 'OptionValueOne',
                AddedByXAuthTypeRouteDefinitionCustomizer: 'Application User',
        ]
        getUserRoute.size() == 6

        and:
        Map getOrdersRoute = extractRoute(routes, "GET", "/users/{userId}/orders")
        getOrdersRoute.predicate == "(Methods: [GET] && Paths: [/users/{userId}/orders], match trailing slash: true)"
        getOrdersRoute.route_id != null
        getOrdersRoute.filters == [
                "[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]",
                "[[AddResponseHeader X-Response-DefaultForAllServices = 'sample-value-all'], order = 1]",
                "[[PrefixPath prefix = '/api'], order = 2]",
                "[[AddResponseHeader X-Response-FromOpenApiDefinition = 'sample-value'], order = 3]",
                "[[SetStatus status = '418'], order = 4]",
        ]
        getOrdersRoute.uri == "http://localhost:9092"
        getOrdersRoute.order == 1
        getOrdersRoute.metadata == [
                optionName           : "OptionValue",
                compositeObject      : [
                        name     : "value",
                        otherName: 2,
                ],
                aList                : ["foo", "bar", "quuz"],
                defaultForAllServices: 'OptionValueAll',
                iAmNumber            : 1,
        ]
        getOrdersRoute.size() == 6

        and:
        Map getOrderRoute = extractRoute(routes, "GET", "/users/{userId}/orders/{orderId}")
        getOrderRoute.predicate == "(Methods: [GET] && Paths: [/users/{userId}/orders/{orderId}], match trailing slash: true)"
        getOrderRoute.route_id != null
        getOrderRoute.filters == [
                "[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]",
                "[[AddResponseHeader X-Response-DefaultForAllServices = 'sample-value-all'], order = 1]",
                "[[PrefixPath prefix = '/api'], order = 2]",
                "[[AddResponseHeader X-Response-FromOpenApiDefinition = 'sample-value'], order = 3]",
                "[[SetStatus status = '418'], order = 4]",
        ]
        getOrderRoute.uri == "http://localhost:9092"
        getOrderRoute.order == 1
        getOrderRoute.metadata == [
                optionName           : "OptionValue",
                compositeObject      : [name: "value"],
                aList                : ["foo", "bar"],
                defaultForAllServices: 'OptionValueAll',
                iAmNumber            : 1,
        ]
        getOrderRoute.size() == 6

        and:
        Map postOrderRoute = extractRoute(routes, "POST", "/users/{userId}/orders")
        postOrderRoute.predicate == "(Methods: [POST] && Paths: [/users/{userId}/orders], match trailing slash: true)"
        postOrderRoute.route_id != null
        postOrderRoute.filters == [
                "[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]",
                "[[AddResponseHeader X-Response-DefaultForAllServices = 'sample-value-all'], order = 1]",
                "[[PrefixPath prefix = '/api'], order = 2]",
                "[[AddResponseHeader X-Response-FromOpenApiDefinition = 'sample-value'], order = 3]",
        ]
        postOrderRoute.uri == "http://localhost:9092"
        postOrderRoute.order == 1
        postOrderRoute.metadata == [
                optionName           : "OptionValue",
                compositeObject      : [name: "value"],
                aList                : ["foo", "bar"],
                defaultForAllServices: 'OptionValueAll',
                iAmNumber            : 1,
        ]
        postOrderRoute.size() == 6

        and:
        Map getThingsRoute = extractRoute(routes, "GET", "/things")
        getThingsRoute.predicate == "(Methods: [GET] && Paths: [/things], match trailing slash: true)"
        getThingsRoute.route_id != null
        getThingsRoute.filters == [
                "[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]",
                "[[AddResponseHeader X-Response-DefaultForAllServices = 'sample-value-all'], order = 1]",
        ]
        getThingsRoute.uri == "http://localhost:9093"
        getThingsRoute.order == 5
        getThingsRoute.metadata == [defaultForAllServices: 'OptionValueAll']
        getThingsRoute.size() == 6

        and:
        Map getOpenApiInClassPathEntitiesRoute = extractRoute(routes, "GET", "/entities-of-service-with-openapi-definition-in-classpath")
        getOpenApiInClassPathEntitiesRoute.predicate == "(Methods: [GET] && Paths: [/entities-of-service-with-openapi-definition-in-classpath], match trailing slash: true)"
        getOpenApiInClassPathEntitiesRoute.route_id != null
        getOpenApiInClassPathEntitiesRoute.filters == [
                "[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]",
                "[[AddResponseHeader X-Response-DefaultForAllServices = 'sample-value-all'], order = 1]",
        ]
        getOpenApiInClassPathEntitiesRoute.uri == "http://localhost:9095"
        getOpenApiInClassPathEntitiesRoute.order == 5
        getOpenApiInClassPathEntitiesRoute.metadata == [defaultForAllServices: 'OptionValueAll']
        getOpenApiInClassPathEntitiesRoute.size() == 6

        when:
        FluxExchangeResult<String> getUsersWithoutHeaderResponse = webTestClient
                .get().uri("http://localhost:${localServerPort}/users")
                .exchange().returnResult(String)

        then:
        getUsersWithoutHeaderResponse.getRawStatusCode() == 404
        String getUsersWithoutHeaderResponseBody = getUsersWithoutHeaderResponse.getResponseBody().blockFirst()
        Map getUsersWithoutHeaderResponseBodyJson = jsonSlurper.parseText(getUsersWithoutHeaderResponseBody) as Map
        getUsersWithoutHeaderResponseBodyJson.timestamp != null
        getUsersWithoutHeaderResponseBodyJson.path == "/users"
        getUsersWithoutHeaderResponseBodyJson.status == 404
        getUsersWithoutHeaderResponseBodyJson.error == "Not Found"
        getUsersWithoutHeaderResponseBodyJson.message == null
        getUsersWithoutHeaderResponseBodyJson.requestId != null

        when:
        FluxExchangeResult<String> getUsersResponse = webTestClient
                .get().uri("http://localhost:${localServerPort}/users")
                .header("Authorization", "Bearer someToken")
                .exchange().returnResult(String)

        then:
        getUsersResponse.getRawStatusCode() == 200
        getUsersResponse.getResponseBody().blockFirst() == '[{"id": "user-id-1"}]'


        when:
        FluxExchangeResult<String> getUserWithoutHeaderResponse = webTestClient
                .get().uri("http://localhost:${localServerPort}/users/${USER_ID}")
                .exchange().returnResult(String)

        then:
        getUserWithoutHeaderResponse.getRawStatusCode() == 404
        String getUserWithoutHeaderResponseBody = getUserWithoutHeaderResponse.getResponseBody().blockFirst()
        Map getUserWithoutHeaderResponseBodyJson = jsonSlurper.parseText(getUserWithoutHeaderResponseBody) as Map
        getUserWithoutHeaderResponseBodyJson.timestamp != null
        getUserWithoutHeaderResponseBodyJson.path == "/users/${USER_ID}"
        getUserWithoutHeaderResponseBodyJson.status == 404
        getUserWithoutHeaderResponseBodyJson.error == "Not Found"
        getUserWithoutHeaderResponseBodyJson.message == null
        getUserWithoutHeaderResponseBodyJson.requestId != null

        when:
        FluxExchangeResult<String> getUserResponse = webTestClient
                .get().uri("http://localhost:${localServerPort}/users/${USER_ID}")
                .header("Authorization", "Bearer someToken")
                .header("Required-Test-Header", "required-test-header-value")
                .exchange().returnResult(String)

        then:
        getUserResponse.getRawStatusCode() == 200
        getUserResponse.getResponseBody().blockFirst() == '{"id": "user-id-1"}'

        when:
        FluxExchangeResult<String> getOrdersResponse = webTestClient
                .get().uri("http://localhost:${localServerPort}/users/${USER_ID}/orders")
                .exchange().returnResult(String)

        then:
        getOrdersResponse.getRawStatusCode() == 418
        getOrdersResponse.getResponseBody().blockFirst() == '[{"id": "order-id-1"}]'

        when:
        FluxExchangeResult<String> getOrderResponse = webTestClient
                .get().uri("http://localhost:${localServerPort}/users/${USER_ID}/orders/${ORDER_ID}")
                .exchange().returnResult(String)

        then:
        getOrderResponse.getRawStatusCode() == 418
        getOrderResponse.getResponseBody().blockFirst() == '{"id": "order-id-1"}'

        when:
        FluxExchangeResult<String> postOrderResponse = webTestClient
                .post().uri("http://localhost:${localServerPort}/users/${USER_ID}/orders")
                .exchange().returnResult(String)

        then:
        postOrderResponse.getRawStatusCode() == 201
        postOrderResponse.getResponseBody().blockFirst() == '{"id": "order-id-1"}'

        when:
        FluxExchangeResult<String> getContextInBaseUriThingsResponse = webTestClient
                .get().uri("http://localhost:${localServerPort}/things")
                .exchange().returnResult(String)

        then:
        getContextInBaseUriThingsResponse.getRawStatusCode() == 200
        getContextInBaseUriThingsResponse.getResponseBody().blockFirst() == '[{"id": "thing-id-1"}]'
    }

    def "OpenAPI Route Definition are removed on retrieval errors only after grace period"() {
        given:
        waitForRemovalOfAllRoutes()

        and:
        UserServiceMock.instance.mockOpenApiDefinition()
        OrderServiceMock.instance.mockOpenApiDefinition()

        when:
        waitForRouteAddition {
            assert getRoutesFromActuatorEndpoint().size() == 6
        }

        and:
        List routes = getRoutesFromActuatorEndpoint()

        then:
        extractRoute(routes, "GET", "/users") != null
        extractRoute(routes, "GET", "/users/{userId}") != null
        extractRoute(routes, "GET", "/users/{userId}/orders") != null
        extractRoute(routes, "GET", "/users/{userId}/orders/{orderId}") != null
        extractRoute(routes, "POST", "/users/{userId}/orders") != null
        extractRoute(routes, "GET", "/entities-of-service-with-openapi-definition-in-classpath") != null

        when:
        OrderServiceMock.instance.resetAll()
        Instant orderServiceUnavailableStart = Instant.now()
        waitForRouteRemoval {
            assert getRoutesFromActuatorEndpoint().size() == 3
        }
        Instant orderServiceRoutesRemoved = Instant.now()
        routes = getRoutesFromActuatorEndpoint()

        then:
        Duration actualGracePeriod = Duration.between(orderServiceUnavailableStart, orderServiceRoutesRemoved)
        Duration configuredGracePeriod = locatorProperties.getUpdateScheduler().getRemoveRoutesOnUpdateFailuresAfter()
        actualGracePeriod > configuredGracePeriod
        actualGracePeriod < configuredGracePeriod.plus(maxWaitTimeForRouteAddition)

        and:
        extractRoute(routes, "GET", "/users") != null
        extractRoute(routes, "GET", "/users/{userId}") != null
        extractRoute(routes, "GET", "/users/{userId}/orders") == null
        extractRoute(routes, "GET", "/users/{userId}/orders/{orderId}") == null
        extractRoute(routes, "POST", "/users/{userId}/orders") == null
        extractRoute(routes, "GET", "/entities-of-service-with-openapi-definition-in-classpath") != null
    }

    def "Error in OpenAPI definition of service A does not affect routes for service B"() {
        given:
        waitForRemovalOfAllRoutes()

        and: 'OpenAPI definition of one of two services contains an unknown Spring Cloud Gateway Filter'
        UserServiceMock.instance.mockOpenApiDefinition()
        OrderServiceMock.instance.mockOpenApiDefinitionContainingUnknownFilter()

        when: 'having waited for gateway to discover all services'
        sleep(maxWaitTimeForRouteAddition.toMillis())

        then: 'all services with valid OpenAPI definition have been registered'
        List routes = getRoutesFromActuatorEndpoint()
        getRoutesFromActuatorEndpoint().size() == 3

        extractRoute(routes, "GET", "/users") != null
        extractRoute(routes, "GET", "/users/{userId}") != null
        extractRoute(routes, "GET", "/users/{userId}/orders") == null
        extractRoute(routes, "GET", "/users/{userId}/orders/{orderId}") == null
        extractRoute(routes, "POST", "/users/{userId}/orders") == null
        extractRoute(routes, "GET", "/entities-of-service-with-openapi-definition-in-classpath") != null

        when: 'service with previously erroneous OpenAPI definition now has valid OpenAPI definition'
        OrderServiceMock.instance.resetAll()
        OrderServiceMock.instance.mockOpenApiDefinition()

        and: 'having waited for operations to have been published'
        waitForRouteAddition {
            assert getRoutesFromActuatorEndpoint().size() == 6
        }
        routes = getRoutesFromActuatorEndpoint()

        then: 'routes for service with now valid OpenAPI definition are registered'
        extractRoute(routes, "GET", "/users") != null
        extractRoute(routes, "GET", "/users/{userId}") != null
        extractRoute(routes, "GET", "/users/{userId}/orders") != null
        extractRoute(routes, "GET", "/users/{userId}/orders/{orderId}") != null
        extractRoute(routes, "POST", "/users/{userId}/orders") != null
        extractRoute(routes, "GET", "/entities-of-service-with-openapi-definition-in-classpath") != null

        when: 'service now has erroneous OpenAPI definition again'
        OrderServiceMock.instance.resetAll()
        OrderServiceMock.instance.mockOpenApiDefinitionContainingUnknownFilter()

        and: 'having waited for operations to have been removed'
        waitForRouteRemoval {
            assert getRoutesFromActuatorEndpoint().size() == 3
        }
        routes = getRoutesFromActuatorEndpoint()

        then: 'routes for service with erroneous OpenAPI definition have been removed'
        extractRoute(routes, "GET", "/users") != null
        extractRoute(routes, "GET", "/users/{userId}") != null
        extractRoute(routes, "GET", "/users/{userId}/orders") == null
        extractRoute(routes, "GET", "/users/{userId}/orders/{orderId}") == null
        extractRoute(routes, "POST", "/users/{userId}/orders") == null
        extractRoute(routes, "GET", "/entities-of-service-with-openapi-definition-in-classpath") != null

        when: 'service with previously erroneous OpenAPI definition now has valid OpenAPI definition'
        OrderServiceMock.instance.resetAll()
        OrderServiceMock.instance.mockOpenApiDefinition()

        and: 'having waited for operations to have been published'
        waitForRouteAddition {
            assert getRoutesFromActuatorEndpoint().size() == 6
        }
        routes = getRoutesFromActuatorEndpoint()

        then: 'routes for service with now valid OpenAPI definition are registered'
        extractRoute(routes, "GET", "/users") != null
        extractRoute(routes, "GET", "/users/{userId}") != null
        extractRoute(routes, "GET", "/users/{userId}/orders") != null
        extractRoute(routes, "GET", "/users/{userId}/orders/{orderId}") != null
        extractRoute(routes, "POST", "/users/{userId}/orders") != null
        extractRoute(routes, "GET", "/entities-of-service-with-openapi-definition-in-classpath") != null
    }

}
