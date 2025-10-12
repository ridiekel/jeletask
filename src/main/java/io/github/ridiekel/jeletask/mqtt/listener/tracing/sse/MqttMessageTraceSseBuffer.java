package io.github.ridiekel.jeletask.mqtt.listener.tracing.sse;

import io.github.ridiekel.jeletask.mqtt.listener.tracing.MqttMessageTrace;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class MqttMessageTraceSseBuffer {
  private final int capacity;
  private final Deque<MqttMessageTrace> buffer;

  public MqttMessageTraceSseBuffer() {
    this.capacity = Math.max(1, 10000);
    this.buffer = new ConcurrentLinkedDeque<>();
  }

  public void add(MqttMessageTrace e) {
    buffer.addFirst(e); // nieuwste bovenaan
    trim();
  }

  public List<MqttMessageTrace> findAllNewestFirst() {
    List<MqttMessageTrace> list = new ArrayList<>(buffer.size());
  list.addAll(buffer);
    return list; // al in newest-first volgorde
  }

  private void trim() {
    while (buffer.size() > capacity) {
      buffer.removeLast();
    }
  }

  public void clear() {
    buffer.clear();
  }
}
