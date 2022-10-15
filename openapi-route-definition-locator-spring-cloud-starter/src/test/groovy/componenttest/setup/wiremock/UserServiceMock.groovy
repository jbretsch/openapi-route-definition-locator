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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching

@Singleton(strict = false)
class UserServiceMock extends BaseWireMock {

    UserServiceMock() {
        super(9091)
    }

    void mockOpenApiDefinition(String path = "/internal/openapi-definition") {
        client.register(get(urlPathEqualTo(path))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/yaml")
                    .withBodyFile("user-service/openapi.public.yaml")
            )
        )
    }

    void mockGetUsers() {
        client.register(get(urlEqualTo("/users"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody('[{"id": "user-id-1"}]')
            )
        )
    }

    void mockGetUser() {
        client.register(get(urlPathMatching("/users/.*?"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody('{"id": "user-id-1"}')
            )
        )
    }

}
