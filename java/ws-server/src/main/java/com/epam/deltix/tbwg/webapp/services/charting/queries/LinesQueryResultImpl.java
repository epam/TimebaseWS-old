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
package com.epam.deltix.tbwg.webapp.services.charting.queries;

import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;

import java.util.ArrayList;
import java.util.List;

public class LinesQueryResultImpl implements LinesQueryResult {

    private final String name;
    private final List<LineResult> lines = new ArrayList<>();
    private final TimeInterval interval;

    public LinesQueryResultImpl(String name, TimeInterval interval) {
        this.name = name;
        this.interval = interval;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<LineResult> getLines() {
        return lines;
    }

    @Override
    public TimeInterval getInterval() {
        return interval;
    }

}
