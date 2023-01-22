package io.github.ridiekel.jeletask.mqtt;

import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class TeletaskServiceTest extends TeletaskTestSupport {
    @Test
    void relayStateChange() {
        teletask().relay(1).turnOff();
        mqtt().expect().relay(1).lastStateMessage().toHave().state().off();
        teletask().relay(1).turnOn();
        mqtt().expect().relay(1).lastStateMessage().toHave().state().on();
//        ha().with().relay(1).asLight().toHave().state().on();
        try {
            ha().with().relay(1).asLight().set().off();
        } catch (Exception e) {
        }
//        mqtt().expect().relay(1).lastSetMessage().toHave().state().on();
//        mqtt().expect().relay(1).lastStateMessage().toHave().state().on();


        this.ha().openBrowser();
    }
}
