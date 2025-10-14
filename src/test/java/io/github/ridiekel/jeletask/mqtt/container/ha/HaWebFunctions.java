package io.github.ridiekel.jeletask.mqtt.container.ha;

import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.InputStateCalculator;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class HaWebFunctions {

    private final CentralUnit centralUnit;

    public HaWebFunctions(Teletask2MqttConfigurationProperties config, CentralUnit centralUnit) {
        this.centralUnit = centralUnit;
    }

    public HaOnOffWebElementFunctions relay(int number) {
        return new HaOnOffWebElementFunctions(centralUnit, Function.RELAY, number);
    }

    public HaOnOffWebElementFunctions localmood(int number) {
        return new HaOnOffWebElementFunctions(centralUnit, Function.LOCMOOD, number);
    }

    public HaOnOffWebElementFunctions generalmood(int number) {
        return new HaOnOffWebElementFunctions(centralUnit, Function.GENMOOD, number);
    }

    public HaSceneWebElementFunctions generalmoodScene(int number) {
        return new HaSceneWebElementFunctions(centralUnit, Function.GENMOOD, number);
    }

    public HaSceneWebElementFunctions localmoodScene(int number) {
        return new HaSceneWebElementFunctions(centralUnit, Function.LOCMOOD, number);
    }

    public HaOnOffWebElementFunctions condition(int number) {
        return new HaOnOffWebElementFunctions(centralUnit, Function.COND, number);
    }

    public HaMotorWebElementFunctions motor(int number) {
        return new HaMotorWebElementFunctions(centralUnit, Function.MOTOR, number);
    }

    public HaInputWebElementFunctions input(int number) {
        return new HaInputWebElementFunctions(centralUnit, Function.INPUT, number);
    }

    public HaSensorWebElementFunctions sensor(int number) {
        return new HaSensorWebElementFunctions(centralUnit, Function.SENSOR, number);
    }

    public HaOnOffWebElementFunctions flag(int number) {
        return new HaOnOffWebElementFunctions(centralUnit, Function.FLAG, number);
    }

    public HaDimmerWebElementFunctions dimmer(int number) {
        return new HaDimmerWebElementFunctions(centralUnit, Function.DIMMER, number);
    }

    public static class HaMotorWebElementFunctions extends HaOnOffWebElementFunctionsSupport<HaMotorWebElementFunctions> {
        public HaMotorWebElementFunctions(CentralUnit centralUnit, Function function, int number) {
            super(centralUnit, function, number);
        }

        //<ha-control-slider vertical="" min="1" max="100" aria-label="Brightness" style="--control-slider-color: var(--state-light-on-color, var(--state-light-active-color, var(--state-active-color))); --control-slider-background: var(--state-light-on-color, var(--state-light-active-color, var(--state-active-color)));" role="slider" tabindex="0" aria-valuemin="1" aria-valuemax="100" aria-orientation="vertical" aria-valuenow="44"> </ha-control-slider>
        protected SelenideElement sliderElement() {
            return $(Selectors.shadowDeepCss("more-info-content ha-control-slider"));
        }

        public HaMotorWebElementFunctions shouldHaveValidSliderRange() {
            this.sliderElement().shouldHave(Condition.attribute("min", "0"));
            this.sliderElement().shouldHave(Condition.attribute("max", "100"));
            return this.self();
        }

        public HaMotorWebElementFunctions shouldHaveSliderState(String value) {
            this.sliderElement().shouldHave(Condition.attribute("aria-valuenow", value));
            return this.self();
        }

        public HaMotorWebElementFunctions slideToValue44() {
            SelenideElement element = this.sliderElement();

            element.click(ClickOptions.withOffset(20, 20));

            return this.self();
        }
    }

    public static class HaDimmerWebElementFunctions extends HaOnOffWebElementFunctionsSupport<HaDimmerWebElementFunctions> {
        public HaDimmerWebElementFunctions(CentralUnit centralUnit, Function function, int number) {
            super(centralUnit, function, number);
        }

        //<ha-control-slider vertical="" min="1" max="100" aria-label="Brightness" style="--control-slider-color: var(--state-light-on-color, var(--state-light-active-color, var(--state-active-color))); --control-slider-background: var(--state-light-on-color, var(--state-light-active-color, var(--state-active-color)));" role="slider" tabindex="0" aria-valuemin="1" aria-valuemax="100" aria-orientation="vertical" aria-valuenow="44"> </ha-control-slider>
        protected SelenideElement sliderElement() {
            return $(Selectors.shadowDeepCss("more-info-content ha-control-slider"));
        }

        public HaDimmerWebElementFunctions shouldHaveValidSliderRange() {
            this.sliderElement().shouldHave(Condition.attribute("min", "1"));
            this.sliderElement().shouldHave(Condition.attribute("max", "100"));
            return this.self();
        }

        public HaDimmerWebElementFunctions shouldHaveSliderState(String value) {
            this.sliderElement().shouldHave(Condition.attribute("aria-valuenow", value));
            return this.self();
        }

        public HaDimmerWebElementFunctions slideToValue45() {
            SelenideElement element = this.sliderElement();

            element.click(ClickOptions.withOffset(20, 20));

            return this.self();
        }
    }

    public static class HaWebElementFunctions<F extends HaWebElementFunctions<F>> {
        private static final Logger LOG = LogManager.getLogger();

        private static final Map<Function, java.util.function.Function<ComponentSpec, String>> FUNTION_TO_ROWTYPE = Map.of(
                Function.DIMMER, c -> "hui-toggle-entity-row",
                Function.RELAY, c -> "hui-toggle-entity-row",
                Function.FLAG, c -> "hui-toggle-entity-row",
                Function.GENMOOD, c -> Objects.equals(c.getType(), "scene") ? "ha-assist-chip" : "hui-toggle-entity-row",
                Function.LOCMOOD, c -> Objects.equals(c.getType(), "scene") ? "ha-assist-chip" : "hui-toggle-entity-row",
                Function.TIMEDMOOD, c -> "hui-toggle-entity-row",
                Function.MOTOR, c -> "hui-cover-entity-row",
                Function.COND, c -> "hui-simple-entity-row",
                Function.SENSOR, c -> "hui-sensor-entity-row",
                Function.INPUT, c -> "hui-sensor-entity-row"
        );

        protected final int index;
        protected final CentralUnit centralUnit;
        protected final String baseCss;

        public HaWebElementFunctions(CentralUnit centralUnit, Function function, int number) {
            this.centralUnit = centralUnit;
            this.baseCss = FUNTION_TO_ROWTYPE.get(function).apply(centralUnit.getComponent(function, number)) + " ";
            this.index = getIndexOf(function, number);
        }

        protected int getIndexOf(Function function, int number) {
            String elementToFind = baseCss + "div[title]";
            String description = this.centralUnit.getComponent(function, number).getDescription();

            List<@Nullable String> titles = $$(Selectors.shadowDeepCss(elementToFind))
                    .attributes("title");

            int index = titles.indexOf(description);

            if (index < 0) {
                throw new IllegalStateException(String.format("'%s' not found in: '%s' - List of possible values: \n%s", description, elementToFind, titles.stream().map(s -> String.format("\t'%s'", s)).collect(Collectors.joining("\n"))));
            }

            return index;
        }

        @SuppressWarnings("unchecked")
        public F self() {
            return (F) this;
        }

        protected SelenideElement popupStateTextElement() {
            return $(Selectors.shadowDeepCss("more-info-content ha-more-info-state-header p.state"));
        }

        public F shouldHaveHeaderStateText(String text) {
            this.popupStateTextElement().shouldHave(Condition.text(text));
            return this.self();
        }

        protected SelenideElement iconElement() {
            return $$(Selectors.shadowDeepCss(baseCss + "ha-state-icon")).get(index);
        }

        protected SelenideElement titleElement() {
            return $$(Selectors.shadowDeepCss(baseCss + "div[title]")).get(index);
        }

        protected SelenideElement closeButtonElement() {
            return $$(Selectors.shadowDeepCss("ha-icon-button[dialogaction='cancel']")).get(index);
        }

        public void openDetails() {
            this.titleElement().click();
        }

        public void closeDetails() {
            this.closeButtonElement().click();
        }

        public F shouldHaveLightIcon() {
            this.iconElement().shouldHave(Condition.attribute("data-domain", "light"));
            return this.self();
        }

        public F shouldHaveSwitchIcon() {
            this.iconElement().shouldHave(Condition.attribute("data-domain", "switch"));
            return this.self();
        }

        public F shouldHaveBinarySensorIcon() {
            this.iconElement().shouldHave(Condition.attribute("data-domain", "binary_sensor"));
            return this.self();
        }

        public F shouldHaveSensorIcon() {
            this.iconElement().shouldHave(Condition.attribute("data-domain", "sensor"));
            return this.self();
        }
    }

    public static class HaOnOffWebElementFunctions extends HaOnOffWebElementFunctionsSupport<HaOnOffWebElementFunctions> {
        public HaOnOffWebElementFunctions(CentralUnit centralUnit, Function function, int number) {
            super(centralUnit, function, number);
        }
    }

    public static class HaSceneWebElementFunctions extends HaWebElementFunctions<HaSceneWebElementFunctions> {
        public HaSceneWebElementFunctions(CentralUnit centralUnit, Function function, int number) {
            super(centralUnit, function, number);
        }

        protected SelenideElement controlElement() {
            return $$(Selectors.shadowDeepCss(baseCss + "button")).get(index);
        }

        public void click() {
            this.controlElement().click();
        }

        protected int getIndexOf(Function function, int number) {
            String elementToFind = baseCss + ".label";
            String description = this.centralUnit.getComponent(function, number).getDescription();

            List<@Nullable String> titles = $$(Selectors.shadowDeepCss(elementToFind))
                    .texts();

            int index = titles.indexOf(description);

            if (index < 0) {
                throw new IllegalStateException(String.format("'%s' not found in: '%s' - List of possible values: \n%s", description, elementToFind, titles.stream().map(s -> String.format("\t'%s'", s)).collect(Collectors.joining("\n"))));
            }

            return index;
        }
    }

    public static class HaOnOffWebElementFunctionsSupport<F extends HaOnOffWebElementFunctionsSupport<F>> extends HaWebElementFunctions<F> {
        public HaOnOffWebElementFunctionsSupport(CentralUnit centralUnit, Function function, int number) {
            super(centralUnit, function, number);
        }

        protected SelenideElement controlElement() {
            return $$(Selectors.shadowDeepCss(baseCss + "input")).get(index);
        }

        protected SelenideElement textElement() {
            return $$(Selectors.shadowDeepCss(baseCss)).get(index);
        }

        public void toggle() {
            this.controlElement().click();
        }

        public F shouldBeChecked() {
            this.controlElement().shouldHave(Condition.attribute("checked", "true"));
            return this.self();
        }

        public F shouldNotBeChecked() {
            this.controlElement().shouldHave(Condition.attribute("checked", ""));
            return this.self();
        }

        public F shouldHaveStateTextOn() {
            this.textElement().shouldHave(Condition.text("On"));
            return this.self();
        }

        public F shouldHaveStateTextOff() {
            this.textElement().shouldHave(Condition.text("Off"));
            return this.self();
        }

        public F shouldHaveIconStateOn() {
            this.iconElement().shouldHave(Condition.attribute("data-state", "on"));
            return this.self();
        }

        public F shouldHaveIconStateOff() {
            this.iconElement().shouldHave(Condition.attribute("data-state", "off"));
            return this.self();
        }
    }

    public static class HaSensorWebElementFunctions extends HaWebElementFunctions<HaSensorWebElementFunctions> {
        public HaSensorWebElementFunctions(CentralUnit centralUnit, Function function, int number) {
            super(centralUnit, function, number);
        }

        protected SelenideElement textElement() {
            return $$(Selectors.shadowDeepCss(baseCss)).get(index);
        }

        public HaSensorWebElementFunctions shouldHaveState(String value) {
            this.textElement().shouldHave(Condition.text(value));
            return this.self();
        }

//        public HaSensorWebElementFunctions shouldHaveIconStateOpen() {
//            this.iconElement().shouldHave(Condition.attribute("data-state", InputStateCalculator.ValidInputState.OPEN.toString()));
//            return this.self();
//        }
//
//        public HaSensorWebElementFunctions shouldHaveIconStateClosed() {
//            this.iconElement().shouldHave(Condition.attribute("data-state", InputStateCalculator.ValidInputState.CLOSED.toString()));
//            return this.self();
//        }
//
//        public HaSensorWebElementFunctions shouldHaveIconStateNotPressed() {
//            this.iconElement().shouldHave(Condition.attribute("data-state", InputStateCalculator.ValidInputState.NOT_PRESSED.toString()));
//            return this.self();
//        }
//
//        public HaSensorWebElementFunctions shouldHaveIconStateShortPress() {
//            this.iconElement().shouldHave(Condition.attribute("data-state", InputStateCalculator.ValidInputState.SHORT_PRESS.toString()));
//            return this.self();
//        }
    }

    public static class HaInputWebElementFunctions extends HaWebElementFunctions<HaInputWebElementFunctions> {
        public HaInputWebElementFunctions(CentralUnit centralUnit, Function function, int number) {
            super(centralUnit, function, number);
        }

        protected SelenideElement textElement() {
            return $$(Selectors.shadowDeepCss(baseCss)).get(index);
        }

        public HaInputWebElementFunctions shouldHaveStateTextOpen() {
            this.textElement().shouldHave(Condition.text(InputStateCalculator.ValidInputState.OPEN.toString()));
            return this.self();
        }

        public HaInputWebElementFunctions shouldHaveStateTextShortPress() {
            this.textElement().shouldHave(Condition.text(InputStateCalculator.ValidInputState.SHORT_PRESS.toString()));
            return this.self();
        }

        public HaInputWebElementFunctions shouldHaveStateTextClosed() {
            this.textElement().shouldHave(Condition.text(InputStateCalculator.ValidInputState.CLOSED.toString()));
            return this.self();
        }

        public HaInputWebElementFunctions shouldHaveStateTextNotPressed() {
            this.textElement().shouldHave(Condition.text(InputStateCalculator.ValidInputState.NOT_PRESSED.toString()));
            return this.self();
        }

        public HaInputWebElementFunctions shouldHaveIconStateOpen() {
            this.iconElement().shouldHave(Condition.attribute("data-state", InputStateCalculator.ValidInputState.OPEN.toString()));
            return this.self();
        }

        public HaInputWebElementFunctions shouldHaveIconStateClosed() {
            this.iconElement().shouldHave(Condition.attribute("data-state", InputStateCalculator.ValidInputState.CLOSED.toString()));
            return this.self();
        }

        public HaInputWebElementFunctions shouldHaveIconStateNotPressed() {
            this.iconElement().shouldHave(Condition.attribute("data-state", InputStateCalculator.ValidInputState.NOT_PRESSED.toString()));
            return this.self();
        }

        public HaInputWebElementFunctions shouldHaveIconStateShortPress() {
            this.iconElement().shouldHave(Condition.attribute("data-state", InputStateCalculator.ValidInputState.SHORT_PRESS.toString()));
            return this.self();
        }
    }
}
