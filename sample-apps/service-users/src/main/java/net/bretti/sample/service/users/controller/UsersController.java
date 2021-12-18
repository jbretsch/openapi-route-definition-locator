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

package net.bretti.sample.service.users.controller;

import net.bretti.sample.service.users.dto.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/users")
public class UsersController {
    @GetMapping
    @ResponseBody
    public List<User> getUsers() {
        User user1 = User.builder().id(UUID.randomUUID()).name("John Doe").build();
        User user2 = User.builder().id(UUID.randomUUID()).name("Jane Doe").build();
        return Arrays.asList(user1, user2);
    }

    @GetMapping(path = "/{userId}")
    @ResponseBody
    public User getUser(@PathVariable UUID userId) {
        return User.builder().id(userId).name("John Doe").build();
    }
}
