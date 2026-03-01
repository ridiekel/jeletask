package io.github.ridiekel.jeletask.end2end;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Selectors.shadowDeepCss;
import static com.codeborne.selenide.Selenide.$$;

@SuppressWarnings("resource")
class End2EndSuggestedAreaTest extends MockingTeletaskTestSupport {
    @Test
    void suggestedArea() {
        Assertions.assertThat($$(Selectors.shadowDeepCss("hui-entities-card div.name")).texts()).contains(
                "Living Room Area"
        );

        SelenideElement livingRoomArea = $$(shadowDeepCss("hui-entities-card div.name")).stream().filter(
                e -> e.text().equals("Living Room Area")
        ).findAny().get();

        SelenideElement card = livingRoomArea.parent().parent();

        card.$(shadowDeepCss("div.info"))
                .shouldHave(exactText("Light in living room"));
    }
}
