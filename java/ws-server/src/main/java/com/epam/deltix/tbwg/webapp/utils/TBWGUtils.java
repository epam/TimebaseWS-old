package com.epam.deltix.tbwg.webapp.utils;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.stream.MessageWriter2;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.tbwg.webapp.services.TimebaseServiceImpl;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.Interval;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

public class TBWGUtils {

    private static final Log LOG = LogFactory.getLog(TBWGUtils.class);

    public static IdentityKey[] match(DXTickStream stream, String... symbols) {
        if (symbols != null) {
            HashSet<String> set = new HashSet<>(Arrays.asList(symbols));

            // match entities
            IdentityKey[] entities = stream.listEntities();
            return Stream.of(entities).filter(x -> set.contains(x.getSymbol().toString())).toArray(IdentityKey[]::new);
        }

        return null;
    }

    public static IdentityKey[] match(DXTickStream stream, List<String> symbols) {
        if (symbols != null) {
            HashSet<String> set = new HashSet<>(symbols);

            // match entities
            IdentityKey[] entities = stream.listEntities();
            return Stream.of(entities).filter(x -> set.contains(x.getSymbol().toString())).toArray(IdentityKey[]::new);
        }

        return null;
    }

    public static DXTickStream[] match(TimebaseServiceImpl tb, String... streams) {
        if (streams != null) {
            HashSet<String> set = new HashSet<>(Arrays.asList(streams));
            return Stream.of(tb.listStreams()).filter(x -> set.contains(x.getKey())).toArray(DXTickStream[]::new);
        }

        return null;
    }

    public static MessageWriter2 create(OutputStream os, Interval periodicity, RecordClassDescriptor... cds) throws IOException, ClassNotFoundException {
        os = new GZIPOutputStream(os, 1 << 16 / 2);
        try {
            MessageWriter2 wr = new MessageWriter2(os, periodicity, null, cds);
            os = null;
            return wr;
        } finally {
            Util.close(os);
        }
    }

    public static CharSequence[] collectCharSequence(IdentityKey[] instruments) {
        if (instruments != null) {
            return Arrays.stream(instruments).map(IdentityKey::getSymbol).toArray(CharSequence[]::new);
        }

        return null;
    }

    public static IdentityKey[] collect(HashSet<IdentityKey> instruments) {
        if (instruments != null)
            return instruments.toArray(new IdentityKey[instruments.size()]);

        return null;
    }

    public static IdentityKey[] collect(HashSet<IdentityKey> instruments, boolean live) {
        if (instruments != null)
            return instruments.toArray(new IdentityKey[instruments.size()]);

        return live ? new IdentityKey[0]: null;
    }

    public static int getIntProperty(String property, int def) {
        String value = System.getProperty(property);
        int result;
        if (value != null) {
            try {
                result = Integer.parseInt(value);
            } catch (NumberFormatException exc) {
                result = def;
            }
        } else {
            result = def;
        }
        return result;
    }

    public static void cancel(Future<?> future, boolean mayInterruptIfRunning) {
        try {
            future.cancel(mayInterruptIfRunning);
        } catch (Exception exc) {
            LOG.error().append("Exception while canceling Future: ").append(exc)
                    .commit();
        }
    }
}
