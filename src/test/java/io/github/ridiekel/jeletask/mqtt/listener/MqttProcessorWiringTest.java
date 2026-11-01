package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

// Reads the constructors reflectively, which the native test image has no metadata for.
@DisabledInNativeImage
class MqttProcessorWiringTest {
    private final DefaultListableBeanFactory beanFactory = beanFactory();

    private static DefaultListableBeanFactory beanFactory() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        AutowiredAnnotationBeanPostProcessor autowired = new AutowiredAnnotationBeanPostProcessor();
        autowired.setBeanFactory(beanFactory);
        beanFactory.addBeanPostProcessor(autowired);
        beanFactory.registerSingleton("centralUnit", MqttProcessorFixtures.centralUnit());
        beanFactory.registerSingleton("teletaskClient", new MqttProcessorFixtures.FakeTeletaskClient());
        beanFactory.registerSingleton("configuration", MqttProcessorFixtures.configuration());
        beanFactory.registerBeanDefinition("mqttProcessor", new RootBeanDefinition(MqttProcessor.class));
        return beanFactory;
    }

    @Test
    void springInstantiatesThePublicConstructor() {
        assertThat(beanFactory.getBean(MqttProcessor.class).getTeletaskIdentifier()).isEqualTo(MqttProcessorFixtures.CENTRAL_ID);
    }

    @Test
    void aotResolvesThePublicConstructorForTheNativeImage() throws NoSuchMethodException {
        Constructor<?> expected = MqttProcessor.class.getConstructor(CentralUnit.class, TeletaskClient.class, Teletask2MqttConfigurationProperties.class);

        assertThat(RegisteredBean.of(beanFactory, "mqttProcessor").resolveInstantiationDescriptor().executable()).isEqualTo(expected);
    }
}
