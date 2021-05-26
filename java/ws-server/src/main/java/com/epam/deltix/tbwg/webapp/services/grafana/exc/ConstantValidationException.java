/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.tbwg.webapp.services.grafana.exc;

import com.epam.deltix.computations.data.base.ArgumentType;

public class ConstantValidationException extends ValidationException {

    private final ArgumentType type;
    private final Object value;
    private final Object min;
    private final Object max;

    public ConstantValidationException(ArgumentType type, String value, String min, String max) {
        this.type = type;
        this.value = value;
        this.min = min;
        this.max = max;
    }

    @Override
    public String getMessage() {
        return String.format("Value '%s' of type '%s' does not meet requirements min='%s', max='%s'.", value, type, min, max);
    }
}
