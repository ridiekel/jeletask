package io.github.ridiekel.jeletask.mqtt.listener;

import eu.rekawek.toxiproxy.Proxy;

/**
 * A running Toxiproxy with one proxy in front of the central unit and one in front of the broker. The bridge only knows
 * the proxied ports, the observer talks to the broker directly so it keeps seeing what happens during a disturbance.
 */
interface ResilienceNetwork extends AutoCloseable {
    String host();

    int proxiedCentralPort();

    int proxiedBrokerPort();

    Proxy centralLine();

    Proxy brokerLine();

    String observerBrokerUrl();

    @Override
    void close();
}
