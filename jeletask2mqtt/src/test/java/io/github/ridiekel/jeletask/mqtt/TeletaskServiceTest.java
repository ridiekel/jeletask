package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import io.github.ridiekel.jeletask.server.TeletaskTestServer;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static io.github.ridiekel.jeletask.server.ExpectationBuilder.ExpectationResponseBuilder.state;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.groupGet;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.set;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.get;

@ActiveProfiles("test")
@SpringBootTest
class TeletaskServiceTest {
    private static final GenericContainer MQTT = new GenericContainer(DockerImageName.parse("eclipse-mosquitto:latest"))
            .withExposedPorts(1883)
            .withNetworkAliases("mqtt")
            .withCommand("mosquitto -c /mosquitto-no-auth.conf")
            .withNetwork(Network.newNetwork());

    //    private static final GenericContainer HOME_ASSISTANT = new GenericContainer(DockerImageName.parse("ghcr.io/home-assistant/home-assistant:stable"))
//            .withExposedPorts(8123)
//            .withNetwork(MQTT.getNetwork());
//
    static {
        MQTT.start();
//        HOME_ASSISTANT.start();
    }

//    private static final ServerSocket TT;
//    static {
//        MQTT.start();
//        HOME_ASSISTANT.start();
//        try {
//            TT = new ServerSocket(1234);
//
//            byte[] read = new byte[20];
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        Socket accept = TT.accept();
//                        byte[] bytes = new byte[256];
//                        int read;
//                        while ((read = accept.getInputStream().read(bytes)) != -1) {
//                            System.out.println("***************** read = " + Bytes.bytesToHex(read, bytes));
//                        }
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }, "mock-server").start();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    @Autowired
    private TeletaskTestServer server;

    @Autowired
    private MqttProcessor mqttProcessor;

    @Autowired
    private TeletaskClient teletaskClient;


    @DynamicPropertySource
    public static void setupProperties(DynamicPropertyRegistry registry) {
        registry.add("teletask.mqtt.port", MQTT::getFirstMappedPort);
    }

    @Test
    void one() throws InterruptedException {
//        System.out.println("port = " + MQTT.getFirstMappedPort());
//        System.out.println("port = " + HOME_ASSISTANT.getFirstMappedPort());

        Thread.sleep(10000);

        String broker = "tcp://localhost:" + MQTT.getFirstMappedPort();

        System.out.println("broker = " + broker);

        this.server.mock(e -> {
                    e.when(set(Function.RELAY, 1, "ON")).thenRespond(state(Function.RELAY, 1, "ON"));
                    e.when(get(Function.RELAY, 1)).thenRespond(state(Function.RELAY, 1, "ON"));
                    e.when(groupGet(Function.RELAY, 1, 2)).thenRespond(
                            state(Function.RELAY, 1, "ON"),
                            state(Function.RELAY, 2, "OFF")
                    );
                }
        );


        try {
            MqttClient mqttClient = new MqttClient(broker, this.getClass().getName(), new MemoryPersistence());

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setMaxInflight(100000);

            connOpts.setCleanSession(true);

            connOpts.setAutomaticReconnect(true);

            try {
                mqttClient.connect(connOpts);
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }

            mqttClient.subscribe(this.mqttProcessor.getPrefix() + "/" + this.mqttProcessor.getTeletaskIdentifier() + "/+/+/state", 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("topic = " + topic);
                    System.out.println("message = " + message.toString());
                }
            });

            System.out.println("Waiting for connected");

            Awaitility.await()
                    .pollDelay(100, TimeUnit.MILLISECONDS)
                    .atMost(10, TimeUnit.SECONDS)
                    .until(mqttClient::isConnected);

            this.teletaskClient.groupGet();

            Thread.sleep(10000);

        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }
}

