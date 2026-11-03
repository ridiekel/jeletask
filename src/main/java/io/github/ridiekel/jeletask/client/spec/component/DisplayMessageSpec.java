package io.github.ridiekel.jeletask.client.spec.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.config.ConfigProblems;
import lombok.Getter;
import lombok.Setter;

/**
 * A message shown on one or more displays (e.g. an AURUS). Each display is a pair of a bus number and an address number.
 */
@Getter
@Setter
public class DisplayMessageSpec extends ComponentSpec {
    private static final String BUS_NUMBERS = "bus_numbers";
    private static final String ADDRESS_NUMBERS = "address_numbers";

    // Comma separated, e.g. "1,1"
    @JsonProperty(BUS_NUMBERS)
    private String busNumbers;

    // Comma separated, e.g. "13,5"; the n-th address belongs to the n-th bus
    @JsonProperty(ADDRESS_NUMBERS)
    private String addressNumbers;

    @Override
    public Function getFunction() {
        return Function.DISPLAYMESSAGE;
    }

    @Override
    public void validate(ConfigProblems problems) {
        super.validate(problems);
        boolean busNumbersValid = validateNumbers(problems, BUS_NUMBERS, this.busNumbers);
        boolean addressNumbersValid = validateNumbers(problems, ADDRESS_NUMBERS, this.addressNumbers);
        if (busNumbersValid && addressNumbersValid && this.busNumbers.split(",").length != this.addressNumbers.split(",").length) {
            problems.error(ADDRESS_NUMBERS, "needs one address for every bus, but there are " + this.busNumbers.split(",").length + " bus numbers ('" + this.busNumbers + "') and " + this.addressNumbers.split(",").length + " address numbers ('" + this.addressNumbers + "')");
        }
    }

    private static boolean validateNumbers(ConfigProblems problems, String property, String numbers) {
        if (numbers == null || numbers.isBlank()) {
            problems.error(property, "is required, a comma separated list of numbers like \"1,2\"");
            return false;
        }
        for (String number : numbers.split(",")) {
            if (!isByte(number)) {
                problems.error(property, "'" + number + "' in '" + numbers + "' is not a number between 0 and " + Byte.MAX_VALUE);
                return false;
            }
        }
        return true;
    }

    // The numbers are sent as one signed byte each, see DisplayMessage
    private static boolean isByte(String number) {
        try {
            return Byte.parseByte(number) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
