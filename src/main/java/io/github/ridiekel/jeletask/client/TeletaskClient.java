package io.github.ridiekel.jeletask.client;

import io.github.ridiekel.jeletask.client.listener.StateChangeListener;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;

public interface TeletaskClient {
    void registerStateChangeListener(StateChangeListener listener);

    /**
     * Sets the state of a component. For a {@link Function#DISPLAYMESSAGE} the state is a
     * {@link io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState} that is shown on the displays.
     */
    void set(ComponentSpec component, State<?> state, SuccessConsumer onSuccess, FailureConsumer onFailed);

    void set(Function function, int number, State<?> state, SuccessConsumer onSucccess, FailureConsumer onFailed);

    void get(Function function, int number, SuccessConsumer onSucccess, FailureConsumer onFailed);

    void get(ComponentSpec component, SuccessConsumer onSuccess, FailureConsumer onFailed);

    void groupGet();

    CentralUnit getCentralUnit();

    boolean isConnected();

    void disconnect();
}
