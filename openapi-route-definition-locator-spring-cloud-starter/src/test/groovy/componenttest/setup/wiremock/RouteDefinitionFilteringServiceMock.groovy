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

package componenttest.setup.wiremock


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

@Singleton(strict = false)
class RouteDefinitionFilteringServiceMock extends BaseWireMock {

    RouteDefinitionFilteringServiceMock() {
        super(9096)
    }

    def mockOpenApiDefinitionWithInvalidEnabledValues() {
        client.register(get(urlPathEqualTo("/internal/openapi-definition"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/yaml")
                        .withBody("""
openapi: 3.0.3
info:
  title: Route Definition Filtering API - Invalid Enabled Values
  version: 0.1.0
servers:
  - url: http://localhost:9096
paths:
  /test-invalid-enabled-string:
    get:
      summary: Test route with invalid enabled value (string)
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        enabled: "invalid-string"
  /test-invalid-enabled-number:
    get:
      summary: Test route with invalid enabled value (number)
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        enabled: 123
  /test-null-enabled:
    get:
      summary: Test route with null enabled value
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        enabled: null
  /test-disabled:
    get:
      summary: Test route with proper disabled value
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        enabled: false
""".trim())))
    }

    def mockOpenApiDefinitionWithInvalidGatewayNames() {
        client.register(get(urlPathEqualTo("/internal/openapi-definition"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/yaml")
                        .withBody("""
openapi: 3.0.3
info:
  title: Route Definition Filtering API - Invalid Gateway Names
  version: 0.1.0
servers:
  - url: http://localhost:9096
paths:
  /test-gateway-names-string:
    get:
      summary: Test route with gateway-names as string
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        gateway-names: "not-an-array"
  /test-gateway-names-number:
    get:
      summary: Test route with gateway-names as number
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        gateway-names: 123
  /test-gateway-names-mixed-array:
    get:
      summary: Test route with gateway-names as mixed array
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        gateway-names:
          - "test-gateway"
          - 123
          - null
          - "another-valid"
  /test-gateway-names-empty-array:
    get:
      summary: Test route with gateway-names as empty array
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        gateway-names: []
""".trim())))
    }

    def mockOpenApiDefinitionWithMixedFiltering() {
        client.register(get(urlPathEqualTo("/internal/openapi-definition"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/yaml")
                        .withBody("""
openapi: 3.0.3
info:
  title: Route Definition Filtering API - Mixed Filtering
  version: 0.1.0
servers:
  - url: http://localhost:9096
paths:
  /test-enabled-no-gateway:
    get:
      summary: Test route that is enabled with no gateway restrictions
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        enabled: true
  /test-disabled-matching-gateway:
    get:
      summary: Test route that is disabled but has matching gateway
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        enabled: false
        gateway-names:
          - "test-gateway"
  /test-enabled-wrong-gateway:
    get:
      summary: Test route that is enabled but has wrong gateway
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        enabled: true
        gateway-names:
          - "wrong-gateway"
  /test-enabled-matching-gateway:
    get:
      summary: Test route that is enabled with matching gateway
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        enabled: true
        gateway-names:
          - "test-gateway"
""".trim())))
    }

    def mockOpenApiDefinitionWithSettingsPrecedence() {
        client.register(get(urlPathEqualTo("/internal/openapi-definition"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/yaml")
                        .withBody("""
openapi: 3.0.3
info:
  title: Route Definition Filtering API - Settings Precedence
  version: 0.1.0
servers:
  - url: http://localhost:9096
x-gateway-route-settings:
  enabled: true
  gateway-names:
    - "global-gateway"
paths:
  /test-operation-overrides-global-enabled:
    get:
      summary: Operation-level enabled overrides global
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        enabled: false
        gateway-names:
          - "test-gateway"
  /test-operation-overrides-global-gateway:
    get:
      summary: Operation-level gateway-names override global
      responses:
        200:
          description: Success
      x-gateway-route-settings:
        gateway-names:
          - "test-gateway"
""".trim())))
    }

    def mockOpenApiDefinitionWithEnvironmentFiltering() {
        client.register(get(urlPathEqualTo("/internal/openapi-definition"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/yaml")
                        .withBody("""
openapi: 3.0.3
info:
  title: Route Definition Filtering API - Environment Filtering
  version: 0.1.0
servers:
  - url: http://localhost:9096
paths:
  /test-no-environment:
    get:
      summary: Test route with no x-environment (should be included)
      responses:
        200:
          description: Success
  /test-environment-dev:
    get:
      summary: Test route with x-environment dev (should be included)
      responses:
        200:
          description: Success
      x-environment: dev
  /test-environment-prod:
    get:
      summary: Test route with x-environment prod (should be filtered out)
      responses:
        200:
          description: Success
      x-environment: prod
""".trim())))
    }
}
