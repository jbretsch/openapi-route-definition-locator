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

package componenttest.setup.wiremock

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

@Singleton(strict = false)
class OpenapiDefinitionServedFromDifferentHostServiceMock2 extends BaseWireMock {

    OpenapiDefinitionServedFromDifferentHostServiceMock2() {
        super(9094)
    }

    void mockOpenApiDefinition() {
        client.register(get(urlPathEqualTo("/custom-path-to/openapi-definition"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/yaml")
                    .withBodyFile("openapi-definition-served-from-different-host-service/openapi.public.yaml")
            )
        )
    }

}
