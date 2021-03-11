package com.epam.deltix.tbwg.webapp.services.charting.transformations;

import com.epam.deltix.streamtransformer.transformations.api.AbstractTransformation;
import com.epam.deltix.tbwg.messages.Message;

import java.util.ArrayList;
import java.util.List;

public abstract class ChartTransformation<Downstream, Upstream> extends AbstractTransformation<Object, Object> {
    private final List<Class<?>> inputClasses;
    private final List<Class<?>> outputClasses;

    public ChartTransformation(List<Class<? extends Upstream>> inputClasses, List<Class<? extends Downstream>> outputClasses) {
        this.inputClasses = new ArrayList<>(inputClasses);
        this.inputClasses.add(Message.class);
        this.outputClasses = new ArrayList<>(outputClasses);
        this.outputClasses.add(Message.class);
    }

    @Override
    public List<Class<?>> getInputClasses() {
        return inputClasses;
    }

    @Override
    public List<Class<?>> getOutputClasses() {
        return outputClasses;
    }

    @Override
    public final void onNext(Object o) {
        if (o instanceof Message) {
            onMessage((Message) o);
        } else {
            onNextPoint((Upstream) o);
        }
    }

    protected abstract void onMessage(final Message message);

    protected abstract void onNextPoint(final Upstream point);
}
