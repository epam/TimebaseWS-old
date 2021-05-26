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
package com.epam.deltix.tbwg.webapp.controllers;

import com.epam.deltix.tbwg.webapp.model.grafana.DynamicList;
import com.epam.deltix.tbwg.webapp.model.grafana.GrafanaVersion;
import com.epam.deltix.tbwg.webapp.model.grafana.StreamSchema;
import com.epam.deltix.tbwg.webapp.model.grafana.TimeSeriesEntry;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.DataQueryRequest;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.SelectQuery;
import com.epam.deltix.tbwg.webapp.services.grafana.GrafanaService;
import com.epam.deltix.tbwg.webapp.services.grafana.base.GrafanaServiceNew;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.NoSuchStreamException;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.ValidationException;
import com.epam.deltix.computations.base.exc.RecordValidationException;
import com.epam.deltix.grafana.model.DataFrame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

@ConditionalOnProperty(name = "grafana.enabled", havingValue = "true", matchIfMissing = true)
@RestController
@RequestMapping("/grafana/v0")
@CrossOrigin
public class ReactGrafanaController {

    private final GrafanaService grafanaService;
    private final GrafanaServiceNew grafanaServiceNew;

    @Autowired
    public ReactGrafanaController(GrafanaService grafanaService, GrafanaServiceNew grafanaServiceNew) {
        this.grafanaService = grafanaService;
        this.grafanaServiceNew = grafanaServiceNew;
    }

    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public GrafanaVersion grafanaVersion() {
        return new GrafanaVersion();
    }

    @RequestMapping(value = "/aggregationTypes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<String> aggregationTypes() {
        return grafanaService.aggregations();
    }

    @RequestMapping(value = "/streams", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DynamicList> streams(@RequestParam(required = false, defaultValue = "") String template,
                                               @RequestParam(required = false, defaultValue = "0") int offset,
                                               @RequestParam(required = false, defaultValue = "30") int limit) {
        return ResponseEntity.ok(grafanaServiceNew.listStreams(template, offset, limit));
    }

    @RequestMapping(value = "/symbols", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DynamicList> symbols(@RequestParam String stream,
                                               @RequestParam(required = false, defaultValue = "") String template,
                                               @RequestParam(required = false, defaultValue = "0") int offset,
                                               @RequestParam(required = false, defaultValue = "30") int limit)
            throws NoSuchStreamException {
        return ResponseEntity.ok(grafanaServiceNew.listSymbols(stream, template, offset, limit));
    }

    @RequestMapping(value = "/schema", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamSchema> schema(@RequestParam String stream) throws NoSuchStreamException {
        return ResponseEntity.ok(grafanaService.schema(stream));
    }

    @RequestMapping(value = "/groupByViewOptions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> groupByOptions() {
        return grafanaServiceNew.groupByViewOptions();
    }

    @RequestMapping(value = "/queries/selectTS", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TimeSeriesEntry> selectTS(@Valid @RequestBody DataQueryRequest<SelectQuery> request) throws ValidationException, RecordValidationException {
        return grafanaServiceNew.timeSeries(request);
    }

    @RequestMapping(value = "/queries/selectDF", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DataFrame> selectDataFrame(@Valid @RequestBody DataQueryRequest<SelectQuery> request) throws ValidationException, RecordValidationException {
        return grafanaServiceNew.dataFrames(request);
    }

    @RequestMapping(value = "/queries/select", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Object> select(@Valid @RequestBody DataQueryRequest<SelectQuery> request) throws ValidationException, RecordValidationException {
        return grafanaServiceNew.select(request);
    }


}
