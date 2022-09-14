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

import componenttest.setup.app.TestApiGatewayApplication
import componenttest.setup.wiremock.OpenapiDefinitionServedFromDifferentHostServiceMock1
import componenttest.setup.wiremock.OpenapiDefinitionServedFromDifferentHostServiceMock2
import componenttest.setup.wiremock.OrderServiceMock
import componenttest.setup.wiremock.UserServiceMock
import groovy.json.JsonSlurper
import net.bretti.openapi.route.definition.locator.core.config.OpenApiRouteDefinitionLocatorProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.FluxExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.time.Instant

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = TestApiGatewayApplication)
class OpenApiRouteDefinitionLocatorCompTest extends Specification {

    static final String USER_ID = "user-id-1"
    static final String ORDER_ID = "order-id-1"

    JsonSlurper jsonSlurper = new JsonSlurper()

    @LocalServerPort
    int localServerPort

    @Autowired
    WebTestClient webTestClient

    @Autowired
    OpenApiRouteDefinitionLocatorProperties locatorProperties

    Duration maxWaitTimeForRouteAddition
    Duration maxWaitTimeForRouteRemoval

    def setup() {
        assert locatorProperties.getUpdateScheduler().getFixedDelay() == Duration.ofSeconds(1)
        assert locatorProperties.getUpdateScheduler().getRemoveRoutesOnUpdateFailuresAfter() == Duration.ofSeconds(5)
        maxWaitTimeForRouteAddition = locatorProperties.getUpdateScheduler().getFixedDelay().plusSeconds(1)
        maxWaitTimeForRouteRemoval = locatorProperties.getUpdateScheduler().getRemoveRoutesOnUpdateFailuresAfter() + maxWaitTimeForRouteAddition
        UserServiceMock.instance.resetAll()
        OrderServiceMock.instance.resetAll()
        OpenapiDefinitionServedFromDifferentHostServiceMock1.instance.resetAll()
        OpenapiDefinitionServedFromDifferentHostServiceMock2.instance.resetAll()
    }

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
            assert getRoutesFromActuatorEndpoint().size() == 6
        }

        and:
        List routes = getRoutesFromActuatorEndpoint()

        then:
        Map getUsersRoute = extractRoute(routes, "GET", "/users")
        getUsersRoute.predicate == "(Methods: [GET] && Paths: [/users], match trailing slash: true)"
        getUsersRoute.route_id != null
        getUsersRoute.filters == ["[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]"]
        getUsersRoute.uri == "http://localhost:9091"
        getUsersRoute.order == 0
        getUsersRoute.size() == 5

        and:
        Map getUserRoute = extractRoute(routes, "GET", "/users/{userId}")
        getUserRoute.predicate ==
                "(((Methods: [GET] && Paths: [/users/{userId}], match trailing slash: true) && " +
                "After: 2022-01-20T17:42:47.789+01:00[Europe/Berlin]) && " +
                "Header: Required-Test-Header regexp=required-test-header-.*)"
        getUserRoute.route_id != null
        getUserRoute.filters == ["[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]"]
        getUserRoute.uri == "http://localhost:9091"
        getUserRoute.order == 0
        getUserRoute.size() == 5

        and:
        Map getOrdersRoute = extractRoute(routes, "GET", "/users/{userId}/orders")
        getOrdersRoute.predicate == "(Methods: [GET] && Paths: [/users/{userId}/orders], match trailing slash: true)"
        getOrdersRoute.route_id != null
        getOrdersRoute.filters == [
                "[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]",
                "[[PrefixPath prefix = '/api'], order = 1]",
                "[[AddResponseHeader X-Response-FromOpenApiDefinition = 'sample-value'], order = 2]",
                "[[SetStatus status = '418'], order = 3]",
        ]
        getOrdersRoute.uri == "http://localhost:9092"
        getOrdersRoute.order == 1
        getOrdersRoute.metadata == [
                optionName     : "OptionValue",
                compositeObject: [
                        name     : "value",
                        otherName: 2,
                ],
                aList          : ["foo", "bar", "quuz"],
                iAmNumber      : 1,
        ]
        getOrdersRoute.size() == 6

        and:
        Map getOrderRoute = extractRoute(routes, "GET", "/users/{userId}/orders/{orderId}")
        getOrderRoute.predicate == "(Methods: [GET] && Paths: [/users/{userId}/orders/{orderId}], match trailing slash: true)"
        getOrderRoute.route_id != null
        getOrderRoute.filters == [
                "[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]",
                "[[PrefixPath prefix = '/api'], order = 1]",
                "[[AddResponseHeader X-Response-FromOpenApiDefinition = 'sample-value'], order = 2]",
                "[[SetStatus status = '418'], order = 3]",
        ]
        getOrderRoute.uri == "http://localhost:9092"
        getOrderRoute.order == 1
        getOrderRoute.metadata == [
                optionName     : "OptionValue",
                compositeObject: [name: "value"],
                aList          : ["foo", "bar"],
                iAmNumber      : 1,
        ]
        getOrderRoute.size() == 6

        and:
        Map postOrderRoute = extractRoute(routes, "POST", "/users/{userId}/orders")
        postOrderRoute.predicate == "(Methods: [POST] && Paths: [/users/{userId}/orders], match trailing slash: true)"
        postOrderRoute.route_id != null
        postOrderRoute.filters == [
                "[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]",
                "[[PrefixPath prefix = '/api'], order = 1]",
                "[[AddResponseHeader X-Response-FromOpenApiDefinition = 'sample-value'], order = 2]",
        ]
        postOrderRoute.uri == "http://localhost:9092"
        postOrderRoute.order == 1
        postOrderRoute.metadata == [
                optionName     : "OptionValue",
                compositeObject: [name: "value"],
                aList          : ["foo", "bar"],
                iAmNumber      : 1,
        ]
        postOrderRoute.size() == 6

        and:
        Map getThingsRoute = extractRoute(routes, "GET", "/things")
        getThingsRoute.predicate == "(Methods: [GET] && Paths: [/things], match trailing slash: true)"
        getThingsRoute.route_id != null
        getThingsRoute.filters == [
                "[[AddResponseHeader X-Response-FromGlobalConfig = 'global-sample-value'], order = 1]",
        ]
        getThingsRoute.uri == "http://localhost:9093"
        getThingsRoute.order == 0
        getThingsRoute.size() == 5

        when:
        FluxExchangeResult<String> getUsersResponse = webTestClient
                .get().uri("http://localhost:${localServerPort}/users")
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
        Map getUserWithoutHeaderResponseBodyJson = jsonSlurper.parseText(getUserWithoutHeaderResponseBody)
        getUserWithoutHeaderResponseBodyJson.timestamp != null
        getUserWithoutHeaderResponseBodyJson.path == "/users/${USER_ID}"
        getUserWithoutHeaderResponseBodyJson.status == 404
        getUserWithoutHeaderResponseBodyJson.error == "Not Found"
        getUserWithoutHeaderResponseBodyJson.message == null
        getUserWithoutHeaderResponseBodyJson.requestId != null

        when:
        FluxExchangeResult<String> getUserResponse = webTestClient
                .get().uri("http://localhost:${localServerPort}/users/${USER_ID}")
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
            assert getRoutesFromActuatorEndpoint().size() == 5
        }

        and:
        List routes = getRoutesFromActuatorEndpoint()

        then:
        extractRoute(routes, "GET", "/users") != null
        extractRoute(routes, "GET", "/users/{userId}") != null
        extractRoute(routes, "GET", "/users/{userId}/orders") != null
        extractRoute(routes, "GET", "/users/{userId}/orders/{orderId}") != null
        extractRoute(routes, "POST", "/users/{userId}/orders") != null

        when:
        OrderServiceMock.instance.resetAll()
        Instant orderServiceUnavailableStart = Instant.now()
        waitForRouteRemoval {
            assert getRoutesFromActuatorEndpoint().size() == 2
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
    }

    private Map extractRoute(List routes, String httpMethod, String path) {
        return routes.find {it.predicate.contains("[${httpMethod}]") && it.predicate.contains("[${path}]") } as Map
    }

    private List getRoutesFromActuatorEndpoint() {
        String routesJson = webTestClient.get().uri("http://localhost:${localServerPort}/actuator/gateway/routes")
                .exchange()
                .returnResult(String)
                .getResponseBody()
                .blockFirst()

        return jsonSlurper.parseText(routesJson) as List
    }

    private void waitForRouteAddition(Closure<?> conditions) {
        new PollingConditions(timeout: maxWaitTimeForRouteAddition.getSeconds()).eventually(conditions)
    }

    private void waitForRouteRemoval(Closure<?> conditions) {
        new PollingConditions(timeout: maxWaitTimeForRouteRemoval.getSeconds()).eventually(conditions)
    }

    private void waitForRemovalOfAllRoutes() {
        waitForRouteRemoval {
            assert getRoutesFromActuatorEndpoint().size() == 0
        }
    }

}
