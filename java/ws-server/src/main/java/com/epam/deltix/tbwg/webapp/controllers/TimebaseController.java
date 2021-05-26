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

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.gflog.LogLevel;
import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.DataFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.StaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.stream.MessageWriter2;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Token;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.qsrv.util.json.DataEncoding;
import com.epam.deltix.qsrv.util.json.JSONRawMessageParser;
import com.epam.deltix.qsrv.util.json.JSONRawMessagePrinter;
import com.epam.deltix.qsrv.util.json.PrintType;
import com.epam.deltix.tbwg.webapp.model.*;
import com.epam.deltix.tbwg.webapp.model.filter.FilterFactory;
import com.epam.deltix.tbwg.webapp.model.input.*;
import com.epam.deltix.tbwg.webapp.model.schema.*;
import com.epam.deltix.tbwg.webapp.services.TimebaseServiceImpl;
import com.epam.deltix.tbwg.webapp.utils.ColumnsManager;
import com.epam.deltix.tbwg.webapp.utils.qql.SelectBuilder;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.time.Interval;
import com.epam.deltix.util.time.Periodicity;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.deltix.tbwg.webapp.utils.TBWGUtils.*;
import static java.lang.String.format;

/**
 * Default controller for REST API
 */
@RestController
@RequestMapping("/api/v0")
@CrossOrigin
public class TimebaseController {

    static final int MAX_NUMBER_OF_RECORDS_PER_REST_RESULTSET;
    static final int MAX_EXPORT_PROCS;

    static {
        int maxNumberOfRecords;
        int maxExportProcs;
        try {
            maxNumberOfRecords = Integer.parseInt(System.getProperty("deltix.tbwg.webapp.services.maxRecordSetSize", "10000"));
            maxExportProcs = Integer.parseInt(System.getProperty("deltix.tbwg.webapp.services.maxExportProcs", "5"));
        } catch (NumberFormatException ex) {
            maxNumberOfRecords = 10000;
            maxExportProcs = 5;
        }
        MAX_NUMBER_OF_RECORDS_PER_REST_RESULTSET = maxNumberOfRecords;
        MAX_EXPORT_PROCS = maxExportProcs;
    }

    private static final Log LOGGER = LogFactory.getLog(TimebaseController.class);

    private final AtomicLong exportProcesses = new AtomicLong();
    private final Object exportSync = new Object();

    @Autowired
    private TimebaseServiceImpl service;

    @RequestMapping(value = "/", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public VersionDef version(Principal user) {
        VersionDef def = new VersionDef();
//        def.authentication = authSettings.isEnabled();

        return def;
    }

    /**
     * <p>Returns data from the specified streams, according to the specified options. The messages
     * are returned from the cursor strictly ordered by time. Within the same
     * exact timestamp, the order of messages is undefined and may vary from
     * call to call, i.e. it is non-deterministic.</p>
     *
     * <p>Note that the arguments of this method only determine the initial
     * configuration of the cursor.</p>
     *
     * @param select selection options
     * @param user   user
     * @return List of rows
     */
    @RequestMapping(value = "/select", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> select(@Valid @RequestBody(required = false) SelectRequest select, Principal user, OutputStream outputStream) {
//        ResponseEntity<StreamingResponseBody> entity = checkAuthentication(authSettings);
//        if (entity != null)
//            return entity;

        if (select == null)
            select = new SelectRequest();

        if (select.streams == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        ArrayList<DXTickStream> streams = new ArrayList<>();
        for (String streamId : select.streams) {
            DXTickStream stream = service.getStream(streamId);
            if (stream != null)
                streams.add(stream);
        }

        if (streams.isEmpty())
            return ResponseEntity.notFound().build();

        HashSet<IdentityKey> instruments = null;

        if (select.symbols != null) {
            instruments = new HashSet<>();

            for (DXTickStream stream : streams)
                Collections.addAll(instruments, match(stream, select.symbols));
        }

        SelectionOptions options = new SelectionOptions();
        options.channelQOS = ChannelQualityOfService.MIN_INIT_TIME;
        options.reversed = select.reverse;
        options.raw = true;

        final long startIndex = select.offset < 0 ? 0 : select.offset;
        final long endIndex = startIndex + select.rows - 1; // inclusive

        DXTickStream[] tickStreams = streams.toArray(new DXTickStream[streams.size()]);

        long startTime = select.getStartTime(TimebaseServiceImpl.getEndTime(tickStreams));

        TickCursor messageSource = service.getConnection().select(
                startTime,
                options,
                select.types,
                collectCharSequence(collect(instruments)),
                tickStreams);

        LOGGER.log(LogLevel.INFO, "SELECT * FROM " + Arrays.toString(select.streams) + " WHERE MESSAGE_INDEX IN [" + startIndex + ", " + endIndex + "] " +
                "AND TYPES = [" + Arrays.toString(select.types) + "] AND ENTITIES = [" + Arrays.toString(collect(instruments)) + "] " +
                "AND timestamp [" + GMT.formatDateTimeMillis(startTime) + ":" + GMT.formatDateTimeMillis(select.getEndTime()) + "]");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(new MessageSource2ResponseStream(messageSource, select.getEndTime(), startIndex, endIndex));
    }

    /**
     * <p>Returns data from the specified streams, according to the specified options. The messages
     * are returned from the cursor strictly ordered by time. Within the same
     * exact timestamp, the order of messages is undefined and may vary from
     * call to call, i.e. it is non-deterministic.</p>
     *
     * <p>Note that the arguments of this method only determine the initial
     * configuration of the cursor.</p>
     *
     * @param streams Specified streams to be subscribed
     * @param symbols Specified instruments (symbols) to be subscribed. If undefined, then all instruments will be subscribed.
     * @param types   Specified message types to be subscribed. If undefined, then all types will be subscribed.
     * @param depth   Specified time depth to look back in case when 'start time' is undefined.
     * @param from    Query start time
     * @param to      Query end time
     * @param offset  Start row offset. (By default = 0)
     * @param rows    Number of returning rows. (By default = 1000)
     * @param reverse Result direction of messages according to timestamp
     * @param user    user
     * @return List of rows
     */
    @RequestMapping(value = "/select", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> select(
            @RequestParam String[] streams,
            @RequestParam(required = false) String[] symbols,
            @RequestParam(required = false) String[] types,
            @RequestParam(required = false) String depth,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long offset,
            @RequestParam(required = false) Integer rows,
            @RequestParam(required = false) boolean reverse,
            Principal user, OutputStream outputStream) {
        SelectRequest request = new SelectRequest();
        request.streams = streams;
        request.symbols = symbols;
        request.types = types;
        request.from = from;
        request.to = to;
        if (rows != null)
            request.rows = rows;
        request.offset = offset != null ? offset : 0;
        request.reverse = reverse;
        request.depth = depth;

        return select(request, user, outputStream);
    }

    /**
     * <p>Returns data from this specified stream, according to the specified options. The messages
     * are returned from the cursor strictly ordered by time. Within the same
     * exact timestamp, the order of messages is undefined and may vary from
     * call to call, i.e. it is non-deterministic.</p>
     *
     * <p>Note that the arguments of this method only determine the initial
     * configuration of the cursor.</p>
     *
     * @param streamId stream key
     * @param select   selection options
     * @param user     user
     * @return List of rows
     */
    @RequestMapping(value = "/{streamId}/select", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> select(@PathVariable String streamId, @Valid @RequestBody(required = false) StreamRequest select, Principal user, OutputStream outputStream) {
        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            //noinspection unchecked
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        if (select == null)
            select = new StreamRequest();

        IdentityKey[] ids = match(stream, select.symbols);

        SelectionOptions options = getSelectionOption(select);

        final long startIndex = select.offset < 0 ? 0 : select.offset;
        final long endIndex = startIndex + select.rows - 1; // inclusive

        long startTime = select.getStartTime(TimebaseServiceImpl.getEndTime(stream));

        LOGGER.log(LogLevel.INFO, "SELECT * FROM " + streamId + " WHERE MESSAGE_INDEX IN [" + startIndex + ", " + endIndex + "] " +
                "AND TYPES = [" + Arrays.toString(select.types) + "] AND ENTITIES = [" + Arrays.toString(ids) + "] " +
                "AND timestamp [" + GMT.formatDateTimeMillis(startTime) + ":" + GMT.formatDateTimeMillis(select.getEndTime()) + "]");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(new MessageSource2ResponseStream(
                        stream.select(startTime, options, select.types, ids), select.getEndTime(), startIndex, endIndex)
                );
    }

    /**
     * <p>Returns data from this specified stream, according to the specified options. The messages
     * are returned from the cursor strictly ordered by time. Within the same
     * exact timestamp, the order of messages is undefined and may vary from
     * call to call, i.e. it is non-deterministic.</p>
     *
     * <p>Note that the arguments of this method only determine the initial
     * configuration of the cursor.</p>
     *
     * @param streamId Specified stream to be subscribed
     * @param symbols  Specified instruments(symbols) to be subscribed. If undefined, then all instruments will be subscribed.
     * @param types    Specified message types to be subscribed. If undefined, then all types will be subscribed.
     * @param depth    Specified time depth to look back in case when 'start time' is undefined.
     * @param from     Query start time
     * @param to       Query end time
     * @param offset   Start row offset.
     * @param rows     Number of returning rows.
     * @param reverse  Result direction of messages according to timestamp
     * @param user     user
     * @return List of rows
     * @pathExample /GDAX/select?from=2018-06-28T00:51:05.297Z&amp;to=2018-06-28T23:59:59.999Z
     * @pathExample /GDAX/select?from=2018-06-28T00:00:00.000Z&amp;to=2018-06-28T23:59:59.999Z&amp;offset=100000
     * @pathExample /GDAX/select?from=2018-06-28T00:00:00.000Z&amp;symbols=BTCEUR,ETHEUR
     * @pathExample /GDAX/select?from=2018-06-28T00:00:00.000Z&amp;types=deltix.timebase.api.messages.TradeMessage
     * @pathExample /GDAX/select?offset=10000&amp;rows=5000
     * @pathExample /GDAX/select?depth=3H
     * @responseExample application/json {"symbol":"BCHEUR","timestamp":"2018-06-28T00:51:05.297Z","currencyCode":999,"entries":[{"type":"L2EntryUpdate","exchangeId":"GDAX","price":624.809999999,"size":0.02305503,"action":"DELETE","level":3,"side":"ASK"},{"type":"L2EntryNew","exchangeId":"GDAX","price":626.7,"size":0.6850108,"level":19,"side":"ASK"}],"packageType":"INCREMENTAL_UPDATE"}
     */
    @RequestMapping(value = "/{streamId}/select", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> select(
            @PathVariable String streamId,
            @RequestParam(required = false) String[] symbols,
            @RequestParam(required = false) String[] types,
            @RequestParam(required = false) String depth,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long offset,
            @RequestParam(required = false) Integer rows,
            @RequestParam(required = false) boolean reverse,
            Principal user, OutputStream outputStream) {
        if (StringUtils.isEmpty(streamId))
            //noinspection unchecked
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        return select(new String[]{streamId}, symbols, types, depth, from, to, offset, rows, reverse, user, outputStream);
    }

    @RequestMapping(value = "/export", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> export(@Valid @RequestBody(required = false) ExportStreamsRequest select, Principal user, OutputStream outputStream) {
        if (select == null)
            select = new ExportStreamsRequest();

        if (select.streams == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        ArrayList<DXTickStream> streams = new ArrayList<>();
        for (String streamId : select.streams) {
            DXTickStream stream = service.getStream(streamId);
            if (stream != null)
                streams.add(stream);
        }

        if (streams.isEmpty())
            //noinspection unchecked
            return ResponseEntity.notFound().build();

        HashSet<IdentityKey> instruments = null;

        if (select.symbols != null) {
            instruments = new HashSet<>();

            for (DXTickStream stream : streams)
                Collections.addAll(instruments, match(stream, select.symbols));
        }

        SelectionOptions options = new SelectionOptions();
        options.channelQOS = ChannelQualityOfService.MIN_INIT_TIME;
        options.reversed = select.reverse;
        options.raw = true;

        final long startIndex = select.offset < 0 ? 0 : select.offset;
        final long endIndex = select.rows < 0 ? -1 : startIndex + select.rows - 1; // inclusive

        DXTickStream[] tickStreams = streams.toArray(new DXTickStream[streams.size()]);

        long startTime = select.getStartTime(TimebaseServiceImpl.getEndTime(tickStreams));

        TickCursor messageSource = service.getConnection().select(
                startTime,
                options,
                select.types,
                collectCharSequence(collect(instruments)),
                tickStreams);

        Interval periodicity = streams.size() == 1 ? streams.get(0).getPeriodicity().getInterval() : null;

        RecordClassDescriptor[] descriptors = TickDBShell.collectTypes(streams.toArray(new TickStream[streams.size()]));

        return getExportResponse(messageSource, startTime, select.getEndTime(), startIndex, endIndex, periodicity,
                select.types, collect(instruments), descriptors, select.streams);
    }

    @RequestMapping(value = "/export", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam String[] streams,
            @RequestParam(required = false) String[] symbols,
            @RequestParam(required = false) String[] types,
            @RequestParam(required = false) String depth,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false, defaultValue = "0") long offset,
            @RequestParam(required = false, defaultValue = "-1") int rows,
            @RequestParam(required = false) boolean reverse,
            Principal user, OutputStream outputStream) {
        ExportStreamsRequest request = new ExportStreamsRequest();
        request.streams = streams;
        request.symbols = symbols;
        request.types = types;
        request.from = from;
        request.to = to;
        request.rows = rows;
        request.offset = offset;
        request.reverse = reverse;

        return export(request, user, outputStream);
    }

    @RequestMapping(value = "/{streamId}/export", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> export(@PathVariable String streamId, @Valid @RequestBody(required = false) ExportRequest select, Principal user) {
        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            return ResponseEntity.notFound().build();

        if (select == null)
            select = new ExportRequest();

        IdentityKey[] ids = match(stream, select.symbols);

        SelectionOptions options = getSelectionOption(select);

        final long startIndex = select.offset < 0 ? 0 : select.offset;
        final long endIndex = select.rows < 0 ? -1 : startIndex + select.rows - 1; // inclusive

        long startTime = select.getStartTime(TimebaseServiceImpl.getEndTime(stream));

        Interval periodicity = stream.getPeriodicity().getInterval();

        TickCursor cursor = stream.select(startTime, options, select.types, ids);

        return getExportResponse(cursor, startTime, select.getEndTime(), startIndex, endIndex, periodicity, select.types, ids, stream.getTypes(), stream.getKey());
    }

    @RequestMapping(value = "/{streamId}/export", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> export(
            @PathVariable String streamId,
            @RequestParam(required = false) String[] symbols,
            @RequestParam(required = false) String[] types,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false, defaultValue = "0") long offset,
            @RequestParam(required = false, defaultValue = "-1") int rows,
            @RequestParam(required = false) boolean reverse,
            Principal user, OutputStream outputStream) {
        if (StringUtils.isEmpty(streamId))
            return ResponseEntity.notFound().build();

        ExportRequest request = new ExportRequest();

        request.from = from;
        request.offset = offset;
        request.to = to;
        request.rows = rows;
        request.reverse = reverse;
        request.types = types;
        request.symbols = symbols;

        return export(streamId, request, user);
    }

    /**
     * Stream DDL description.
     *
     * @param streamId stream key
     * @return json object that contains DDL description. See {@link DescribeResponse}.
     */
    @RequestMapping(value = "/{streamId}/describe", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> describeStream(@PathVariable String streamId, Principal user) {
        if (StringUtils.isEmpty(streamId))
            return ResponseEntity.notFound().build();

        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(DescribeResponse.create(stream));
    }

    private ResponseEntity<StreamingResponseBody> getExportResponse(
            TickCursor cursor,
            long startTime, long endTime,
            long startIndex, long endIndex,
            Interval periodicity,
            String[] types, IdentityKey[] ids, RecordClassDescriptor[] rcds, String... streams) {
        synchronized (exportSync) {
            long processes = exportProcesses.get();
            if (processes >= MAX_EXPORT_PROCS) {
                LOGGER.error().append("Cannot process EXPORT requests, cause ")
                        .append(processes)
                        .append(" EXPORT processes are currently running.")
                        .commit();
                return ResponseEntity.notFound().build();
            }
            exportProcesses.incrementAndGet();

            LOGGER.log(LogLevel.INFO, "EXPORT * FROM " + Arrays.toString(streams) + " WHERE MESSAGE_INDEX IN [" + startIndex + ", " + endIndex + "] " +
                    "AND TYPES = [" + Arrays.toString(types) + "] AND ENTITIES = [" + Arrays.toString(ids) + "] " +
                    "AND timestamp [" + GMT.formatDateTimeMillis(startTime) + ":" + GMT.formatDateTimeMillis(endTime) + "]");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment;filename=" + (streams.length == 1 ? streams[0] : "streams") + ".qsmsg.gz")
                    .body(new MessageSource2QMSGFile(cursor, endTime, startIndex, endIndex, periodicity, rcds));
        }
    }

    /**
     * <p>Purge selected stream, e.g. delete data earlier that given time</p>
     *
     * @param streamId stream key
     * @param request  Time measured in milliseconds that passed since January 1, 1970 UTC.
     * @param user     user
     * @return Any errors occurred while parsing and writing data
     */
    @RequestMapping(value = "/{streamId}/purge", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> purge(@PathVariable String streamId, @RequestBody(required = true) SimpleRequest request, Principal user, OutputStream outputStream) {
        ResponseEntity<StreamingResponseBody> entity = checkWritable("Purge stream [" + streamId + "] Failed");
        if (entity != null)
            return entity;

        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            return ResponseEntity.notFound().build();

        LOGGER.log(LogLevel.INFO, "PURGE [" + streamId + "] to " + request.timestamp);

        stream.purge(request.timestamp);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * <p>Deletes selected stream</p>
     *
     * @param streamId stream key
     * @param user     user
     * @return Any errors occurred while parsing and writing data
     */
    @RequestMapping(value = "/{streamId}/delete", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> delete(@PathVariable String streamId,
                                    Principal user, OutputStream outputStream) {
        ResponseEntity<StreamingResponseBody> entity = checkWritable("Delete stream [" + streamId + "] Failed");
        if (entity != null)
            return entity;

        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            return ResponseEntity.notFound().build();

        stream.delete();

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * <p>Rename selected stream.</p>
     *
     * @param streamId stream key
     * @param newStreamId new stream key
     */
    @RequestMapping(value = "/{streamId}/rename", method = RequestMethod.POST, headers = "Content-Type=multipart/form-data",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> renameStream(@PathVariable String streamId, @RequestParam String newStreamId, Principal user) {
        ResponseEntity<StreamingResponseBody> entity = checkWritable("Rename stream [" + streamId + "] Failed");
        if (entity != null)
            return entity;

        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            return ResponseEntity.notFound().build();

        stream.rename(newStreamId);
        stream.setName(newStreamId);

        return ResponseEntity.ok().build();
    }

    /**
     * <p>Rename selected symbols in stream.</p>
     *
     * @param streamId stream key
     * @param symbol symbol key
     * @param newSymbol new symbol key
     * @return status 404 if stream or symbol not found.
     */
    @RequestMapping(value = "/{streamId}/{symbol}/rename", method = RequestMethod.POST, headers = "Content-Type=multipart/form-data",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> renameSymbol(@PathVariable String streamId, @PathVariable String symbol, @RequestParam String newSymbol, Principal user) {
        ResponseEntity<StreamingResponseBody> entity = checkWritable("Rename stream [" + streamId + "] Failed");
        if (entity != null)
            return entity;

        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            return ResponseEntity.notFound().build();

        IdentityKey[] identities = Arrays.stream(stream.listEntities())
                .filter(i -> i.getSymbol().toString().equals(symbol))
                .toArray(IdentityKey[]::new);

        if (identities.length == 0) {
            return ResponseEntity.notFound().build();
        }

        stream.renameInstruments(
            identities,
            Arrays.stream(identities)
                .map(id -> new ConstantIdentityKey(newSymbol))
                .toArray(IdentityKey[]::new)
        );

        return ResponseEntity.ok().build();
    }

    /**
     * <p>Truncates selected stream, e.g. delete data older that given time</p>
     *
     * @param streamId stream key
     * @param request  Time measured in milliseconds that passed since January 1, 1970 UTC and List if Symbols.
     * @param user     user
     * @return Any errors occurred while parsing and writing data
     */
    @RequestMapping(value = "/{streamId}/truncate", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> truncate(@PathVariable String streamId,
                                      @RequestBody(required = true) SimpleRequest request,
                                      Principal user, OutputStream outputStream) {
        ResponseEntity<StreamingResponseBody> entity = checkWritable("Truncate stream [" + streamId + "] Failed");
        if (entity != null)
            return entity;

        DXTickStream stream = service.getStream(streamId);

        //noinspection unchecked
        if (stream == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        long timestamp = request.timestamp;
        LOGGER.log(LogLevel.INFO, "TRUNCATE [" + streamId + "] to " + request.timestamp);

        if (request.symbols != null && request.symbols.length > 0) {
            HashSet<IdentityKey> instruments = new HashSet<>();
            Collections.addAll(instruments, match(stream, request.symbols));

            stream.truncate(timestamp, instruments.toArray(new IdentityKey[0]));
        } else {
            stream.truncate(timestamp);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * <p>Writes messages into the stream. Messages should ordered by 'timestamp' or without 'timestamp' field.</p>
     * if timestamp field is not specified, current server time will be used.
     *
     * @param streamId stream key
     * @param messages messages in JSON format
     * @param user     user
     * @return Any errors occurred while parsing and writing data
     *
     */
    @RequestMapping(value = "/{streamId}/write", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> write(@PathVariable String streamId, @RequestBody String messages, Principal user, OutputStream outputStream) {

        ResponseEntity<StreamingResponseBody> entity = checkWritable("Write failed.");
        if (entity != null)
            return entity;

        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        JsonArray array = (JsonArray) new JsonParser().parse(messages);
        JSONRawMessageParser parser = new JSONRawMessageParser(stream.getTypes(), "$type");

        int count = 0;
        ErrorWriter listener = new ErrorWriter();
        try (TickLoader loader = stream.createLoader(new LoadingOptions(true))) {

            loader.addEventListener(listener);
            for (int i = 0; i < array.size(); i++) {
                JsonElement msg = array.get(i);

                try {
                    RawMessage raw = parser.parse((JsonObject) msg);
                    loader.send(raw);
                    count++;
                } catch (Exception e) {
                    listener.onError(new LoadingError("Message is invalid:" + msg.toString().replace("\"", "'"), e));
                }
            }

            LOGGER.log(LogLevel.INFO, "WRITE [" + streamId + "] " + count + " messages.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(listener);
    }

    ResponseEntity<StreamingResponseBody> checkWritable(String error) {
        if (service.isReadonly()) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(outputStream -> {
                try (OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
                    writer.write("{\"message\": \"Timebase connection is read-only\", \"error\"=\"" + error + "\"} ");
                }
            });
        }

        return null;
    }

    /**
     * <p>Returns data from this specified stream, according to the specified options. The messages
     * are returned from the cursor strictly ordered by time. Within the same
     * exact timestamp, the order of messages is undefined and may vary from
     * call to call, i.e. it is non-deterministic.</p>
     *
     * <p>Note that the arguments of this method only determine the initial
     * configuration of the cursor.</p>
     *
     * @param streamId stream key
     * @param symbolId symbol key
     * @param select   selection options
     * @param user     user
     * @return List of rows
     */
    @RequestMapping(value = "/{streamId}/{symbolId}/select", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> select(@PathVariable String streamId, @PathVariable String symbolId,
                                                        @Valid @RequestBody(required = false) InstrumentRequest select,
                                                        Principal user, OutputStream outputStream) {
        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            //noinspection unchecked
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        if (select == null)
            select = new InstrumentRequest();

        IdentityKey[] ids = match(stream, symbolId);

        SelectionOptions options = getSelectionOption(select);

        final long startIndex = select.offset < 0 ? 0 : select.offset;
        final long endIndex = startIndex + select.rows - 1; // inclusive

        long startTime = select.getStartTime(TimebaseServiceImpl.getEndTime(stream));

        LOGGER.log(LogLevel.INFO, "SELECT * FROM " + streamId + " WHERE MESSAGE_INDEX IN [" + startIndex + ", " + endIndex + "] " +
                "AND TYPES = [" + Arrays.toString(select.types) + "] AND ENTITIES = [" + Arrays.toString(ids) + "] " +
                "AND timestamp [" + GMT.formatDateTimeMillis(startTime) + ":" + GMT.formatDateTimeMillis(select.getEndTime()) + "]");


        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(new MessageSource2ResponseStream(
                        stream.select(startTime, options, select.types, ids), select.getEndTime(), startIndex, endIndex)
                );
    }

    /**
     * <p>Returns data from this specified stream, according to the selection options. The messages
     * are returned from the cursor strictly ordered by time. Within the same
     * exact timestamp, the order of messages is undefined and may vary from
     * call to call, i.e. it is non-deterministic.</p>
     *
     * <p>Note that the arguments of this method only determine the initial
     * configuration of the cursor.</p>
     *
     * @param streamId stream key
     * @param symbolId Specified symbol to be subscribed.
     * @param types    Specified message types to be subscribed. If undefined, then all types will be subscribed.
     * @param depth    Specified time depth to look back in case when 'start time' is undefined.
     * @param from     Query start time.
     * @param to       Query end time.
     * @param offset   Start row offset.
     * @param rows     Number of returning rows.
     * @param reverse  Result direction of messages according to timestamp
     * @param user     user
     * @return List of rows
     */
    @RequestMapping(value = "/{streamId}/{symbolId}/select", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> select(
            @PathVariable String streamId,
            @PathVariable String symbolId,
            @RequestParam(required = false) String[] types,
            @RequestParam(required = false) String depth,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long offset,
            @RequestParam(required = false) Integer rows,
            @RequestParam(required = false) boolean reverse,
            Principal user, OutputStream outputStream) {
        if (StringUtils.isEmpty(streamId))
            //noinspection unchecked
            return ResponseEntity.notFound().build();

        if (StringUtils.isEmpty(symbolId))
            //noinspection unchecked
            return ResponseEntity.notFound().build();

        return select(new String[]{streamId}, new String[]{symbolId}, types, depth, from, to, offset, rows, reverse, user, outputStream);
    }

    private SelectionOptions getSelectionOption(BaseRequest r) {
        SelectionOptions options = new SelectionOptions();
        options.channelQOS = ChannelQualityOfService.MIN_INIT_TIME;
        options.reversed = r.reverse;
        options.raw = true;

        return options;
    }

    private SelectionOptions getSelectionOption(ExportRequest r) {
        SelectionOptions options = new SelectionOptions();
        options.channelQOS = ChannelQualityOfService.MIN_INIT_TIME;
        options.reversed = r.reverse;
        options.raw = true;

        return options;
    }

    /**
     * <p>Returns data types for the specified stream</p>
     *
     * @param streamId stream key
     * @param user     user
     * @return List of rows
     */
    @RequestMapping(value = "/{streamId}/schema", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SchemaDef> schema(@PathVariable String streamId,
                                            @RequestParam(required = false, defaultValue = "false") boolean tree,
                                            Principal user, OutputStream outputStream) {

        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            //noinspection unchecked
            return null;

        RecordClassSet metaData = stream.getStreamOptions().getMetaData();

        RecordClassDescriptor[] top = metaData.getContentClasses();
        ClassDescriptor[] classes = metaData.getClasses();

        SchemaDef schema = new SchemaDef();

        schema.types = new TypeDef[top.length];
        for (int i = 0; i < schema.types.length; i++) {
            schema.types[i] = toTypeDef(top[i], !tree);
            ColumnsManager.applyDefaults(schema.types[i]);
        }

        schema.all = new TypeDef[classes.length];
        for (int i = 0; i < classes.length; i++) {
            schema.all[i] = toTypeDef(classes[i], !tree);
            ColumnsManager.applyDefaults(schema.all[i]);
        }

        return ResponseEntity.ok().body(schema);
    }

    public TypeDef toTypeDef(ClassDescriptor descriptor, boolean flat) {

        List<FieldDef> fields = new ArrayList<FieldDef>();
        String parent = null;

        if (descriptor instanceof RecordClassDescriptor) {
            RecordClassDescriptor rcd = (RecordClassDescriptor)descriptor;

            if (flat) {
                RecordLayout layout = new RecordLayout(rcd);
                toSimple(layout.getNonStaticFields(), fields);
                toSimple(layout.getStaticFields(), fields);
            } else {
                toSimple(rcd.getFields(), fields);
                parent = rcd.getParent() != null ? rcd.getParent().getName() : null;
            }

        } else if (descriptor instanceof EnumClassDescriptor) {
            EnumValue[] values = ((EnumClassDescriptor) descriptor).getValues();

            for (EnumValue v : values)
                fields.add(new FieldDef(v.symbol, v.symbol, "VARCHAR", false));
        }

        TypeDef type = new TypeDef(descriptor.getName(), descriptor.getTitle(), fields.toArray(new FieldDef[fields.size()]));
        type.parent = parent;
        type.isEnum = descriptor instanceof EnumClassDescriptor;

        return type;
    }

    @RequestMapping(value = "/currencies", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CurrencyDef[]> currencies(Principal user, OutputStream outputStream) {

        LOGGER.log(LogLevel.INFO, "GET CURRENCIES() ");

        DXTickStream stream = service.getCurrenciesStream();

        ArrayList<CurrencyDef> currencies = new ArrayList<CurrencyDef>();

        //todo: use ISO currency codes
//        if (stream == null) {
//            Collection<CurrencyMessage> list = service.provider.getCurrencyInfo();
//            for (CurrencyMessage msg : list)
//                currencies.add(new CurrencyDef(msg.getAlphabeticCode().toString(), msg.getNumericCode()));
//        } else {
//            try (TickCursor cursor = service.getConnection().select(
//                    Long.MIN_VALUE, new SelectionOptions(),
//                    new String[]{deltix.timebase.api.messages.currency.CurrencyMessage.CLASS_NAME},
//                    stream)) {
//                while (cursor.next()) {
//                    deltix.timebase.api.messages.currency.CurrencyMessage currencyMessage =
//                            (deltix.timebase.api.messages.currency.CurrencyMessage) cursor.getMessage();
//                    currencies.add(new CurrencyDef(currencyMessage.getSign().toString(), currencyMessage.getCode()));
//                }
//            }
//        }

        return ResponseEntity.ok(currencies.toArray(new CurrencyDef[currencies.size()]));
    }

    @RequestMapping(value = "/settings", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AppSettingDef> settings(Principal user, OutputStream outputStream) {

        LOGGER.log(LogLevel.INFO, "GET App Settings");

        return ResponseEntity.ok(new AppSettingDef());
    }

    @RequestMapping(value = "/describe", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SchemaDef> describe(@Valid @RequestBody(required = false) QueryRequest select, Principal user, OutputStream outputStream) {

        if (select == null || StringUtils.isEmpty(select.query))
            //noinspection unchecked
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        SelectionOptions options = getSelectionOption(select);

        LOGGER.log(LogLevel.INFO, "DESCRIBE QUERY (" + select.query + ")");

        DXTickDB connection = service.getConnection();

        if (connection instanceof TickDBClient) {
            ArrayList<Token> tokens = new ArrayList<>();
            ((TickDBClient) connection).compileQuery(select.query, tokens);
        }

        RecordClassSet metaData = new RecordClassSet();

        if (select.query.toLowerCase().trim().startsWith("select")) {
            InstrumentMessageSource source = connection.executeQuery(
                    select.query, options, null, null, select.getStartTime(Long.MIN_VALUE));

            // hack for now
            String stream = null;

            String[] words = select.query.split(" ");
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                if ("from".equalsIgnoreCase(word)) {
                    stream = words[i + 1];
                    if (stream.startsWith("\""))
                        stream = stream.replace("\"", "");
                    break;
                }
            }

            DXTickStream tickStream = this.service.getStream(stream);

            if (select.query.contains("*") && tickStream != null) {
                metaData = tickStream.getStreamOptions().getMetaData();
            } else {
                if (source.next()) {
                    RawMessage message = (RawMessage) source.getMessage();
                    metaData.addContentClasses(message.type);
                }
            }

        } else {
            // drop or create stream statements
            metaData.addContentClasses(Messages.ERROR_MESSAGE_DESCRIPTOR);
        }

        RecordClassDescriptor[] top = metaData.getContentClasses();
        ClassDescriptor[] classes = metaData.getClasses();

        SchemaDef schema = new SchemaDef();

        schema.types = new TypeDef[top.length];
        for (int i = 0; i < schema.types.length; i++) {

            RecordClassDescriptor rcd = top[i];
            List<FieldDef> fields = new ArrayList<FieldDef>();

            RecordLayout layout = new RecordLayout(rcd);
            toSimple(layout.getNonStaticFields(), fields);
            toSimple(layout.getStaticFields(), fields);

            schema.types[i] = new TypeDef(rcd.getName(), rcd.getTitle(), fields.toArray(new FieldDef[fields.size()]));
            ColumnsManager.applyDefaults(schema.types[i]);
        }

        schema.all = new TypeDef[classes.length];
        for (int i = 0; i < classes.length; i++) {

            List<FieldDef> fields = new ArrayList<FieldDef>();

            ClassDescriptor descriptor = classes[i];

            if (descriptor instanceof RecordClassDescriptor) {
                RecordLayout layout = new RecordLayout((RecordClassDescriptor) descriptor);
                toSimple(layout.getNonStaticFields(), fields);
                toSimple(layout.getStaticFields(), fields);
            } else if (descriptor instanceof EnumClassDescriptor) {
                EnumValue[] values = ((EnumClassDescriptor) descriptor).getValues();

                for (EnumValue v : values)
                    fields.add(new FieldDef(v.symbol, v.symbol, "VARCHAR", false));
            }

            schema.all[i] = new TypeDef(descriptor.getName(), descriptor.getTitle(), fields.toArray(new FieldDef[fields.size()]));
            schema.all[i].isEnum = descriptor instanceof EnumClassDescriptor;

            ColumnsManager.applyDefaults(schema.all[i]);
        }

        return ResponseEntity.ok().body(schema);
    }

    @RequestMapping(value = "/{streamId}/options", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<OptionsDef> options(@PathVariable String streamId, Principal user, OutputStream outputStream) {
        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            //noinspection unchecked
            return null;

        StreamOptions options = stream.getStreamOptions();

        OptionsDef def = new OptionsDef();
        def.name = options.name;
        def.key = stream.getKey();
        def.description = options.description;
        def.highAvailability = options.highAvailability;
        def.distributionFactor = options.distributionFactor;
        def.owner = options.owner;

        Periodicity p = options.periodicity;
        if (p != null)
            def.periodicity = new PeriodicityDef(p.getInterval() != null ? p.getInterval().toHumanString() : null, p.getType());

        def.scope = options.scope;
        def.bufferOptions = options.bufferOptions;

        long[] range = stream.getTimeRange();
        def.range = new TimeRangeDef(range);

        return ResponseEntity.ok().body(def);
    }

    void toSimple(DataFieldInfo[] list, List<FieldDef> fields) {
        if (list != null) {
            for (DataFieldInfo info : list) {
                if (info instanceof StaticFieldInfo)
                    fields.add(new StaticFieldDef(info.getName(), info.getTitle(), getTypeName(info.getType()), info.getType().isNullable(), ((StaticFieldInfo) info).getString()));
                else
                    fields.add(new FieldDef(info.getName(), info.getTitle(), getTypeName(info.getType()), info.getType().isNullable()));
            }
        }
    }

    void toSimple(DataField[] list, List<FieldDef> fields) {
        if (list != null) {
            for (DataField info : list) {
                if (info instanceof StaticDataField)
                    fields.add(new StaticFieldDef(info.getName(), info.getTitle(), getTypeName(info.getType()), info.getType().isNullable(), ((StaticDataField) info).getStaticValue()));
                else
                    fields.add(new FieldDef(info.getName(), info.getTitle(), getTypeName(info.getType()), info.getType().isNullable()));
            }
        }
    }

    private static String getTypeName(DataType type) {
        if (type instanceof ClassDataType) {
            RecordClassDescriptor[] descriptors = ((ClassDataType) type).getDescriptors();
            return type.getBaseName() + "[" + Stream.of(descriptors).map(NamedDescriptor::getName).collect(Collectors.joining(",")) + "]";
        } else if (type instanceof ArrayDataType) {
            DataType dataType = ((ArrayDataType) type).getElementDataType();
            return type.getBaseName() + "[" + getTypeName(dataType) + "]";
        }

        if (type.getEncoding() != null)
            return String.format("%s (%s)", type.getBaseName(), type.getEncoding());

        return type.getBaseName();
    }

    /**
     * <p>Returns time range of the specified stream and specified symbols</p>
     *
     * @param streamId stream key
     * @param symbols  symbols list (Optional)
     * @param user     user
     * @return List of rows
     */
    @RequestMapping(value = "/{streamId}/range", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<TimeRangeDef> range(@PathVariable String streamId, @RequestParam(value = "symbols", required = false) String[] symbols, Principal user) {
        DXTickStream stream = service.getStream(streamId);

        if (stream == null)
            //noinspection unchecked
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        IdentityKey[] ids = match(stream, symbols);
        long[] range = ids != null && ids.length > 0 ? stream.getTimeRange(ids) : stream.getTimeRange();

        return ResponseEntity.ok().body(new TimeRangeDef(range));
    }

    /**
     * Return a list of instruments, for which this stream has any data.
     *
     * @param streamId stream key
     * @param filter   string parameter
     * @param user     active user
     * @return Instruments list. If filter is empty returns all instruments, otherwise returns all instruments,
     * if <code>streamId</code> starts with <code>filter</code>, or instruments, that start with <code>filter</code> if not.
     */
    @RequestMapping(value = "/{streamId}/symbols", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> symbols(@PathVariable String streamId,
                                     @RequestParam(required = false, defaultValue = "") String filter,
                                     Principal user) {

        DXTickStream stream = service.getStream(streamId);
        if (stream == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        Stream<CharSequence> symbols = Arrays.stream(stream.listEntities()).map(IdentityKey::getSymbol);

        if (filter != null && !filter.isEmpty())
            symbols = symbols.filter(s -> s.toString().toLowerCase().contains(filter.toLowerCase()));

        return new ResponseEntity<>(symbols.collect(Collectors.toList()), HttpStatus.OK);
    }

    /**
     * Return a list of available streams
     *
     * @param filter start of stream key
     * @param user   active user
     * @return Streams list. If filter is not null or empty, returns only streams, for that
     * <code>key.startsWith(filter)</code> is true or stream contains symbol, for that <code>symbol.startsWith(filter)</code> is true.
     */
    @RequestMapping(value = "/streams", method = RequestMethod.GET)
    public ResponseEntity<StreamDef[]> streams(@RequestParam(required = false, defaultValue = "") String filter, Principal user) {
        DXTickStream[] streams = service.listStreams(filter);

//        List<DXTickStream> list = Arrays.stream(streams)
//                //.filter((stream)->stream.getScope() == StreamScope.DURABLE) // Hide 'transient' streams
//                .filter((s) -> !s.getKey().contains("#")) // Hide 'system' streams
//                .collect(Collectors.toList());

        StreamDef[] result = new StreamDef[streams.length];

        for (int i = 0, listSize = streams.length; i < listSize; i++) {
            DXTickStream stream = streams[i];
            result[i] = new StreamDef(stream.getKey(), stream.getName(), stream.listEntities().length);
        }

        return ResponseEntity.ok().body(result);
    }

    /**
     * Returns data from specified QQL getQuery. See timebase QQL documentation for more information. For example: "SELECT * FROM level1Stream WHERE (this is not deltix.qsrv.hf.pub.BestBidOfferMessage) or (isNational=10)"
     *
     * @param select selection options
     * @param user   active user
     * @return streams list
     */
    @RequestMapping(value = "/query", method = {RequestMethod.POST})
    public ResponseEntity<StreamingResponseBody> query(@Valid @RequestBody(required = false) QueryRequest select, Principal user, OutputStream outputStream) {

        if (select == null || StringUtils.isEmpty(select.query))
            //noinspection unchecked
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        if (service.isReadonly()) {
            if (select.query.toLowerCase().contains("drop") || select.query.toLowerCase().contains("create"))
                //noinspection unchecked
                return new ResponseEntity(HttpStatus.FORBIDDEN);
        }

        SelectionOptions options = getSelectionOption(select);

        final long startIndex = select.offset < 0 ? 0 : select.offset;
        final long endIndex = startIndex + select.rows - 1; // inclusive
        LOGGER.log(LogLevel.INFO, "QUERY (" + select.query + ") WHERE MESSAGE_INDEX in [" + startIndex + ", " + endIndex + "]");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(new MessageSource2ResponseStream(
                        service.getConnection().executeQuery(
                                select.query, options, null, null, select.getStartTime(Long.MIN_VALUE)), select.getEndTime(), startIndex, endIndex));
    }

    /**
     * Returns filtered messages from specified stream.
     *
     * @param streamId stream key
     * @param filter   filter request
     * @return messages, that accepted  by filters
     */
    @RequestMapping(value = "/{streamId}/filter", method = {RequestMethod.POST}, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> filter(@PathVariable String streamId,
                                                        @Valid @RequestBody FilterRequest filter, Principal user,
                                                        OutputStream outputStream) {
        DXTickStream stream = service.getStream(streamId);
        if (stream == null)
            return ResponseEntity.notFound().build();

        SelectionOptions options = getSelectionOption(filter);

        final long startIndex = filter.offset < 0 ? 0 : filter.offset;
        final long endIndex = startIndex + filter.rows - 1; // inclusive

        long startTime = filter.getStartTime(Long.MIN_VALUE);
        long endTime = filter.getEndTime();

        final SelectBuilder selectBuilder = SelectBuilder.builder(stream);
        for (Map.Entry<String, List<RawFilter>> entry : filter.filters.entrySet()) {
            for (RawFilter rawFilter : entry.getValue()) {
                try {
                    FilterFactory.createFilter(entry.getKey(), rawFilter).appendTo(selectBuilder);
                } catch (SelectBuilder.NoSuchFieldException | SelectBuilder.WrongTypeException exc) {
                    return ResponseEntity.status(400)
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .body(createMessage(exc.toString()));
                }
            }
        }
        String query = selectBuilder.toString();

        LOGGER.log(LogLevel.INFO, "QUERY (" + query + ") WHERE MESSAGE_INDEX in [" + startIndex + ", " + endIndex + "]");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(new MessageSource2ResponseStream(service.getConnection()
                        .executeQuery(query, options, null, null, startTime), endTime, startIndex, endIndex));
    }

    @ExceptionHandler({OutOfMemoryError.class})
    public ResponseEntity<?> handleOOMException() {
        return new ResponseEntity<>("Request is too large, try using paging", HttpStatus.BAD_REQUEST);
    }

    private static class ErrorWriter implements StreamingResponseBody, LoadingErrorListener {

        private final JsonStringEncoder encoder = new JsonStringEncoder();

        boolean empty = true;

        private final StringBuilder sb = new StringBuilder();

        @Override
        public void writeTo(OutputStream outputStream) throws IOException {
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
                writer.append('[');
                writer.append(sb.toString());
                writer.append(']');
            }
        }

        @Override
        public void onError(LoadingError e) {
            writeError(e);
        }

        public void writeError(LoadingError e) {
            if (!empty)
                sb.append(',');
            else
                empty = false;

            sb.append('{').append("\"description\":\"");
            String message = e.getMessage();
            if (message != null)
                encoder.quoteAsString(message, sb);
            sb.append("\",").append("\"error\":");
            appendError(sb, e);
            sb.append('}');
        }

        private void appendError(StringBuilder sb, Throwable ex) {
            Throwable x = ex.getCause() != null ? ex.getCause() : ex;
            StackTraceElement[] trace = x.getStackTrace();

            sb.append("\"");
            sb.append(x.getClass().getName()).append(":");
            sb.append(encoder.quoteAsString(x.getMessage()));

            if (trace != null && trace.length > 2)
                sb.append(" \n").append(trace[0]).append("\n").append(trace[1]).append("\n").append(trace[2]);

            sb.append("\"");
        }
    }

    private static class MessageSource2ResponseStream implements StreamingResponseBody {

        private final InstrumentMessageSource source;
        private final long toTimestamp;
        private final long startIndex; // inclusive
        private final long endIndex; // inclusive

        private final JSONRawMessagePrinter printer =
                new JSONRawMessagePrinter(false, true, DataEncoding.STANDARD, true, false, PrintType.FULL, "$type");

        private final StringBuilder sb = new StringBuilder();

        @SuppressWarnings({"unchecked", "unused"})
        MessageSource2ResponseStream(InstrumentMessageSource source) {
            this.source = source;
            this.toTimestamp = Long.MAX_VALUE;
            this.startIndex = 0;
            this.endIndex = Integer.MAX_VALUE;
        }

        MessageSource2ResponseStream(InstrumentMessageSource messageSource, long toTimestamp, long startIndex, long endIndex) {
            this.source = messageSource;
            this.toTimestamp = toTimestamp;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public void writeTo(@NotNull OutputStream outputStream) throws IOException {

            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
                int messageIndex = 0;
                boolean needComma = false;

                writer.append('[');
                final long limitIndex = Math.min(MAX_NUMBER_OF_RECORDS_PER_REST_RESULTSET + startIndex, endIndex); // inclusive
                while (source.next() && messageIndex <= limitIndex) {
                    if (messageIndex >= startIndex) {

                        RawMessage raw = (RawMessage) source.getMessage();
                        if (raw.getTimeStampMs() > toTimestamp)
                            break;

                        sb.setLength(0);
                        if (needComma)
                            sb.append(',');
                        else
                            needComma = true;
                        try {
                            printer.append(raw, sb);
                            writer.append(sb);
                        } catch (Throwable ex) {
                            LOGGER.error("Error sending message [%s: %s, %s]: %s")
                                    .with(source.getCurrentStreamKey())
                                    .with(raw.getSymbol())
                                    .with(raw.getTimeString())
                                    .with(ex);
                            break;
                        }
                    }

                    messageIndex++;
                }
                writer.append(']');

            } finally {
                outputStream.flush();
                Util.close(source);
            }
        }
    }

    private class MessageSource2QMSGFile implements StreamingResponseBody {
        private final InstrumentMessageSource source;
        private final long toTimestamp;
        private final long startIndex; // inclusive
        private final long endIndex; // inclusive
        private final Interval periodicity;
        private final RecordClassDescriptor[] descriptors;

        @SuppressWarnings({"unchecked", "unused"})
        MessageSource2QMSGFile(InstrumentMessageSource source) {
            this.source = source;
            this.toTimestamp = Long.MAX_VALUE;
            this.startIndex = 0;
            this.endIndex = Integer.MAX_VALUE;
            this.periodicity = null;
            this.descriptors = null;
        }

        MessageSource2QMSGFile(InstrumentMessageSource messageSource, long toTimestamp, long startIndex, long endIndex,
                               Interval periodicity, RecordClassDescriptor... descriptors) {
            this.source = messageSource;
            this.toTimestamp = toTimestamp;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.periodicity = periodicity;
            this.descriptors = descriptors;
        }

        @Override
        public void writeTo(@NotNull OutputStream outputStream) throws IOException {

            try (MessageWriter2 messageWriter = create(outputStream, periodicity, descriptors)) {
                int messageIndex = 0;// inclusive
                while (source.next() && (endIndex < 0 || messageIndex <= endIndex)) {
                    if (messageIndex >= startIndex) {
                        RawMessage raw = (RawMessage) source.getMessage();
                        if (raw.getTimeStampMs() > toTimestamp)
                            break;
                        messageWriter.send(raw);
                    }
                    messageIndex++;
                }
            } catch (ClassNotFoundException exc) {
                LOGGER.error().append("Unexpected ").append(exc).commit();
            } finally {
                exportProcesses.decrementAndGet();
            }
        }
    }

    private static ShortMessage createMessage(String message) {
        return new ShortMessage(message);
    }

    private static ShortMessage createMessage(String template, Object... args) {
        return new ShortMessage(format(template, args));
    }

    private static class ShortMessage implements StreamingResponseBody {

        private final String message;

        public ShortMessage(String message) {
            this.message = message;
        }

        @Override
        public void writeTo(OutputStream outputStream) throws IOException {
            try (PrintWriter writer = new PrintWriter(outputStream)) {
                writer.print(message);
            }
        }
    }

    private static <T extends TickStream> RecordClassDescriptor[] collectTypes(final T... streams) {
        final List<RecordClassDescriptor> types = new ArrayList<RecordClassDescriptor>();
        for (final TickStream stream : streams) {
            if (stream.isFixedType())
                types.add(stream.getFixedType());
            else
                Collections.addAll(types,
                        stream.getPolymorphicDescriptors());
        }

        return types.toArray(new RecordClassDescriptor[types.size()]);
    }

}
