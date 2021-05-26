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
package com.epam.deltix.tbwg.webapp.model.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Schema field definition.
 */
public class FieldDef {

    public FieldDef() {
    }

    public FieldDef(String name, String title, String type, boolean nullable) {
        this.name = name;
        this.type = type;
        this.title = title;
        this.nullable = nullable;
    }

    /**
     * Default visibility state.
     */
    @JsonProperty("hide")
    public boolean hidden = false;

    /**
     * Field Name.
     */
    @JsonProperty
    public String name;

    /**
     * Field Title.
     */
    @JsonProperty
    public String title;

    /**
     * Field Data Type.
     */
    @JsonProperty
    public String type;

    /**
     * Indicates that field is nullable.
     */
    @JsonProperty
    public boolean nullable;
}
