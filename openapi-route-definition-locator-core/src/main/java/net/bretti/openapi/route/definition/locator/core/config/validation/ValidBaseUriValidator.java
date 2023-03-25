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

package net.bretti.openapi.route.definition.locator.core.config.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class ValidBaseUriValidator implements ConstraintValidator<ValidBaseUri, URI> {
    @Override
    public boolean isValid(URI uri, ConstraintValidatorContext context) {
        if (uri == null) {
            // Covered by @NotNull.
            return true;
        }

        if (!uri.isAbsolute()) {
            setConstraintViolation(context, "Must be an absolute URI.");
            return false;
        }

        if (isNotEmpty(uri.getPath()) && !"/".equals(uri.getPath())) {
            setConstraintViolation(context, "Path must be empty or '/'.");
            return false;
        }

        if (isNotEmpty(uri.getQuery())) {
            setConstraintViolation(context, "Must have no query parameters.");
            return false;
        }

        if (isNotEmpty(uri.getFragment())) {
            setConstraintViolation(context, "Must have no fragment part.");
            return false;
        }

        return true;
    }

    private static void setConstraintViolation(ConstraintValidatorContext context, String messageTemplate) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(messageTemplate)
                .addConstraintViolation();
    }
}
