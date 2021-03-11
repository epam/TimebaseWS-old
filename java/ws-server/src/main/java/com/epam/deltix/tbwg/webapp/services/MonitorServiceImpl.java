package com.epam.deltix.tbwg.webapp.services;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.util.json.JSONRawMessagePrinter;
import com.epam.deltix.qsrv.util.json.JSONRawMessagePrinterFactory;
import com.epam.deltix.tbwg.webapp.utils.TBWGUtils;
import com.epam.deltix.tbwg.webapp.utils.cache.CachedMessageBufferImpl;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Service
public class MonitorServiceImpl implements MonitorService {

    private static final int MAX_TASKS = TBWGUtils.getIntProperty(MonitorServiceImpl.class.getPackage().getName() +
            ".maxMonitorTasks", 50);
    private static final int FLUSH_PERIOD_MS = TBWGUtils.getIntProperty(MonitorServiceImpl.class.getPackage().getName() +
            ".monitorFlushPeriod", 500);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(MAX_TASKS);
    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_TASKS);

    private final TimebaseServiceImpl timebase;
    private final Table<String, String, Task> table = HashBasedTable.create();

    public MonitorServiceImpl(TimebaseServiceImpl timebase) {
        this.timebase = timebase;
    }

    @Override
    public synchronized void subscribe(String sessionId, String subscriptionId, String key, long fromTimestamp, List<String> types,
                          List<String> symbols, Consumer<String> consumer) {
        BufferedConsumer bufferedConsumer = new BufferedConsumer();
        StreamConsumer streamConsumer = new StreamConsumer(timebase, fromTimestamp, key, symbols, types, bufferedConsumer);
        ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
                String messages = bufferedConsumer.messageBuffer.flush();
                if (messages != null && !messages.isEmpty()) {
                    consumer.accept(messages);
                }
            }, FLUSH_PERIOD_MS, FLUSH_PERIOD_MS, TimeUnit.MILLISECONDS
        );
        executorService.submit(streamConsumer);
        table.put(sessionId, subscriptionId, new Task(scheduledFuture, streamConsumer));
    }

    @Override
    public synchronized void unsubscribe(String sessionId, String subscriptionId) {
        Task task = table.remove(sessionId, subscriptionId);
        if (task != null)
            task.cancel();
    }

    @PreDestroy
    public synchronized void preDestroy() {
        table.values().forEach(Task::cancel);
        table.clear();
        executorService.shutdown();
        scheduledExecutorService.shutdown();
    }

    private static final class BufferedConsumer implements Consumer<RawMessage> {

        private final JSONRawMessagePrinter printer = JSONRawMessagePrinterFactory.create("$type");
        private final CachedMessageBufferImpl messageBuffer = new CachedMessageBufferImpl(printer);

        @Override
        public void accept(RawMessage rawMessage) {
            messageBuffer.append(rawMessage);
        }
    }

    private static final class Task {
        private final ScheduledFuture<?> scheduledFuture;
        private final StreamConsumer streamConsumer;

        private Task(ScheduledFuture<?> scheduledFuture, StreamConsumer streamConsumer) {
            this.scheduledFuture = scheduledFuture;
            this.streamConsumer = streamConsumer;
        }

        public void cancel() {
            TBWGUtils.cancel(scheduledFuture, true);
            streamConsumer.close();
        }
    }
}
