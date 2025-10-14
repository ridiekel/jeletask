package io.github.ridiekel.jeletask.client;

import io.github.ridiekel.jeletask.client.listener.StateChangeListener;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;

public interface TeletaskClient {
    void registerStateChangeListener(StateChangeListener listener);

    void set(ComponentSpec component, State<?> state, SuccessConsumer onSuccess, FailureConsumer onFailed);

    void set(Function function, int number, State<?> state, SuccessConsumer onSucccess, FailureConsumer onFailed);

    void displaymessage(ComponentSpec component, DisplayMessageState state, SuccessConsumer onSuccess, FailureConsumer onFailed);

    void displaymessage(int number, DisplayMessageState state, SuccessConsumer onSucccess, FailureConsumer onFailed);

    void get(Function function, int number, SuccessConsumer onSucccess, FailureConsumer onFailed);

    void get(ComponentSpec component, SuccessConsumer onSuccess, FailureConsumer onFailed);

    void groupGet();

    CentralUnit getCentralUnit();

    boolean isConnected();

    void disconnect();
}
