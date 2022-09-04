package io.github.ridiekel.jeletask.client;

import io.github.ridiekel.jeletask.client.listener.StateChangeListener;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

public interface TeletaskClient {
    void registerStateChangeListener(StateChangeListener listener);

    void set(ComponentSpec component, ComponentState state, SuccessConsumer onSuccess, FailureConsumer onFailed);

    void set(Function function, int number, ComponentState state, SuccessConsumer onSucccess, FailureConsumer onFailed);

    void get(Function function, int number, SuccessConsumer onSucccess, FailureConsumer onFailed);

    void get(ComponentSpec component, SuccessConsumer onSuccess, FailureConsumer onFailed);

    TeletaskClient start();

    void restart();

    void stop();

    CentralUnit getConfig();

    void groupGet();
}
