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

package componenttest.setup.basetest

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
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = TestApiGatewayApplication)
abstract class BaseCompTest extends Specification {

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

    Map extractRoute(List routes, String httpMethod, String path) {
        return routes.find {it.predicate.contains("[${httpMethod}]") && it.predicate.contains("[${path}]") } as Map
    }

    List getRoutesFromActuatorEndpoint() {
        String routesJson = webTestClient.get().uri("http://localhost:${localServerPort}/actuator/gateway/routes")
                .exchange()
                .returnResult(String)
                .getResponseBody()
                .blockFirst()

        return jsonSlurper.parseText(routesJson) as List
    }

    void waitForRouteAddition(Closure<?> conditions) {
        new PollingConditions(timeout: maxWaitTimeForRouteAddition.getSeconds()).eventually(conditions)
    }

    void waitForRouteRemoval(Closure<?> conditions) {
        new PollingConditions(timeout: maxWaitTimeForRouteRemoval.getSeconds()).eventually(conditions)
    }

    void waitForRemovalOfAllRoutes() {
        waitForRouteRemoval {
            // One route remains because it comes from an OpenAPI definition read from the classpath.
            assert getRoutesFromActuatorEndpoint().size() == 1
        }
    }

}
