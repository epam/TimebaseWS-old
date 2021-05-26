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

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Schema type description.
 */
public class TypeDef {

    public TypeDef() {
    }

    public TypeDef(String name, String title, FieldDef[] fields) {
        this.name = name != null ? name : "";
        this.title = title;
        this.fields = fields;
    }

    @JsonProperty
    public boolean      isEnum = false;

    @JsonProperty
    public String       name;

    @JsonProperty
    public String       title;

    /*
     *  list of fields
     */
    @JsonProperty
    public FieldDef[]   fields;

    /*
     *   Name of the parent TypeDef
     */
    @JsonProperty
    public String       parent;

    /*
     *   Set column visible if found
     */
    public void         setVisible(String fieldName) {
        Optional<FieldDef> first = Stream.of(fields).filter(x -> fieldName.equals(x.name)).findFirst();
        first.ifPresent(fieldDef -> fieldDef.hidden = false);
    }
}
