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

package net.bretti.sample.service.orders.controller;

import net.bretti.sample.service.orders.dto.Order;
import net.bretti.sample.service.orders.dto.OrderItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/users/{userId}/orders")
public class OrdersController {
    @GetMapping
    @ResponseBody
    public List<Order> get() {
        Order order1 = Order.builder()
                .id(UUID.randomUUID())
                .items(Arrays.asList(
                        OrderItem.builder().article("Bread").amount(2).build(),
                        OrderItem.builder().article("Butter").amount(1).build()
                ))
                .build();
        Order order2 = Order.builder()
                .id(UUID.randomUUID())
                .items(Arrays.asList(
                        OrderItem.builder().article("Potatoes").amount(1).build(),
                        OrderItem.builder().article("Sour Creme").amount(2).build()
                ))
                .build();
        return Arrays.asList(order1, order2);
    }

    @GetMapping(path = "/{orderId}")
    public Order getOrder(@PathVariable UUID orderId) {
        return Order.builder()
                .id(orderId)
                .items(Arrays.asList(
                        OrderItem.builder().article("Bread").amount(2).build(),
                        OrderItem.builder().article("Butter").amount(1).build()
                ))
                .build();
    }

}
