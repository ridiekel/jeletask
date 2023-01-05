package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.server.TeletaskTestServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

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


    @DynamicPropertySource
    public static void setupProperties(DynamicPropertyRegistry registry) {
        registry.add("teletask.mqtt.port", MQTT::getFirstMappedPort);
    }

    @Test
    void one() {
//        System.out.println("port = " + MQTT.getFirstMappedPort());
//        System.out.println("port = " + HOME_ASSISTANT.getFirstMappedPort());
    }
}

