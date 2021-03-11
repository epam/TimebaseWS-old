package com.epam.deltix.tbwg.webapp.services;

import com.epam.deltix.data.stream.DXChannel;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.qsrv.hf.spi.conn.Disconnectable;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.lang.Util;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * TimebaseService Provider
 */
@Service
@ConfigurationProperties(prefix = "timebase")
public class TimebaseServiceImpl {
    private static final Log LOGGER = LogFactory.getLog(TimebaseServiceImpl.class);

    private String          url;
    private String          user;
    private String          password;
    private StreamsFilter   streams = new StreamsFilter();
    private boolean         readonly;
    private String          currencies;
    private long            flushPeriodMs = 500;

    private DXTickDB db = null;
    private String dbUrl = null;

    private final SystemMessagesService systemMessagesService;

    @Autowired
    public TimebaseServiceImpl(SystemMessagesService systemMessagesService) {
        this.systemMessagesService = systemMessagesService;
    }

    private static class EventListener implements DisconnectEventListener {
        DXTickDB db;

        public EventListener(DXTickDB db) {
            this.db = db;
        }

        @Override
        public void onDisconnected() {
            if (db instanceof Disconnectable)
                ((Disconnectable)db).removeDisconnectEventListener(this);
            Util.close(db);
        }

        @Override
        public void onReconnected() {

        }
    }

    public static class StreamsFilter {

        private String include;
        private String exclude;

        private Pattern iPattern;
        private Pattern ePattern;

        public String   getInclude() {
            return include;
        }

        public void     setInclude(String include) {
            this.include = include;
            if (include != null && include.length() > 0)
                this.iPattern = Pattern.compile(include);
        }

        public String   getExclude() {
            return exclude;
        }

        public void     setExclude(String exclude) {
            this.exclude = exclude;
            if (exclude != null && exclude.length() > 0)
                this.ePattern = Pattern.compile(exclude);
        }

        public boolean  isMatched(String name) {
            boolean matches = true;

            if (iPattern != null)
                matches = iPattern.matcher(name).find();

            if (ePattern != null)
                matches &= !ePattern.matcher(name).find();

            return matches;
        }
    }

    public synchronized DXTickDB getOrCreate(String url, String userName, String password) {
        if (!Objects.equals(url, dbUrl) || db == null || !db.isOpen()) {
            Util.close(db);
            dbUrl = url;
            String decoded = !StringUtils.isEmpty(password) ? new String(Base64Utils.decodeFromString(password)) : null;
            db = !StringUtils.isEmpty(userName) ? TickDBFactory.createFromUrl(url, userName, decoded) : TickDBFactory.createFromUrl(url);

            TickDBFactory.setApplicationName(db, "TB Web Gateway");
            LOGGER.info("Opening connection to TimeBase on %s.").with(url);
            db.open(readonly);

            if (db instanceof Disconnectable)
                ((Disconnectable) db).addDisconnectEventListener(new EventListener(db));

            if (db instanceof DBStateNotifier) {
                ((DBStateNotifier) db).addStateListener(systemMessagesService.getStateListener());
            } else {
                LOGGER.error().append("Cannot add ")
                        .append(DBStateListener.class.getSimpleName())
                        .commit();
            }
        }
        return db;
    }

    @PostConstruct
    public void logStart() {
        LOGGER.info().append("Started TimeBase service.").commit();
    }

    @PreDestroy
    public synchronized void dispose() {
        LOGGER.info("Closing TickDBClient connection to %s.")
                .with(dbUrl);
        Util.close(db);
        LOGGER.info("Closing Security metadata provider.");
    }

    public void             setUrl(String url) {
        this.url = url;
    }

    public void             setUser(String user) {
        this.user = user;
    }

    public void             setPassword(String password) {
        this.password = password;
    }

    public void             setStreams(StreamsFilter streams) {
        this.streams = streams;
    }

    public boolean          isReadonly() {
        return readonly;
    }

    public void             setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public long getFlushPeriodMs() {
        return flushPeriodMs;
    }

    public void setFlushPeriodMs(long flushPeriodMs) {
        this.flushPeriodMs = flushPeriodMs;
    }

    public DXTickDB         getConnection() {
        return getOrCreate(url, user, password);
    }

    public DXChannel[]      listChannels() {
        DXChannel[] streams = getConnection().listChannels();

        return Arrays.stream(streams).filter(x -> !x.getKey().contains("#")).toArray(DXChannel[]::new);
    }

    public DXTickStream     getStream(String key) {
        DXTickStream stream = getConnection().getStream(key);
        if (stream != null && streams.isMatched(key))
            return stream;

        return null;
    }

    public DXTickStream[]   listStreams() {
        Stream<DXTickStream> list = Arrays.stream(getConnection().listStreams());

        if (streams != null)
            list = list.filter(x -> streams.isMatched(x.getKey()));

        return list.toArray(DXTickStream[]::new);
    }

    public DXTickStream[]   listStreams(String filter) {
        Stream<DXTickStream> list = Arrays.stream(getConnection().listStreams());

        if (streams != null)
            list = list.filter(x -> streams.isMatched(x.getKey()));

        if (filter != null && !filter.isEmpty())
            list = list.filter(x -> isMatched(x, filter.toLowerCase()));

        return list.toArray(DXTickStream[]::new);
    }

    private boolean isMatched(DXTickStream x, String filter) {
        return x.getKey().toLowerCase().contains(filter) ||
                (x.getName() != null && x.getName().toLowerCase().contains(filter)) ||
                Arrays.stream(x.listEntities())
                        .anyMatch(entity -> entity.getSymbol().toString().toLowerCase().contains(filter));
    }

    public List<String> listStreamKeys() {
        List<String> result = new ObjectArrayList<>();
        for (DXTickStream stream : listStreams()) {
            result.add(stream.getKey());
        }
        return result;
    }

    public static long        getEndTime(DXTickStream stream) {
        long[] range = stream.getTimeRange();
        return range != null ? range[1] : System.currentTimeMillis();
    }

    public static long        getEndTime(DXTickStream[] streams) {
        long time = Long.MIN_VALUE;

        for (int i = 0; i < streams.length; i++) {
            long[] range = streams[i].getTimeRange();
            if (range != null)
                time = Math.max(time, range[1]);
        }

        return time;
    }

    public String getCurrencies() {
        return currencies;
    }

    public void setCurrencies(String currencies) {
        this.currencies = currencies;
    }

    public DXTickStream     getCurrenciesStream() {
        return currencies != null ? getStream(currencies) : null;
    }

    public ObjectToObjectHashMap<RecordClassDescriptor, List<DataField>> numericFields(DXTickStream stream) {
        ObjectToObjectHashMap<RecordClassDescriptor, List<DataField>> numericFields = new ObjectToObjectHashMap<>();
        for (ClassDescriptor classDescriptor : stream.getAllDescriptors()) {
            if (classDescriptor instanceof RecordClassDescriptor) {
                RecordClassDescriptor rcd = (RecordClassDescriptor) classDescriptor;
                ObjectArrayList<DataField> dataFields = new ObjectArrayList<>();
                numericFields.put(rcd, dataFields);
                for (DataField dataField : rcd.getFields()) {
                    if (dataField instanceof NonStaticDataField) {
                        if (isNumericField(dataField)) {
                            dataFields.add(dataField);
                        }
                    }
                }
            }
        }
        return numericFields;
    }

    private static String getShortName(ClassDescriptor cd) {
        return getShortName(cd.getName());
    }

    private static String getShortName(String name) {
        return name.substring(name.lastIndexOf(".") + 1);
    }

    private static boolean isNumericField(DataField field) {
        return field.getType() instanceof IntegerDataType || field.getType() instanceof FloatDataType;
    }

}
