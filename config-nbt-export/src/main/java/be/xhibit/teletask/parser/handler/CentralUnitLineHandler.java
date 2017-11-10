package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.text.DecimalFormat;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CentralUnitLineHandler extends LineHandlerSupport {
    private static final DecimalFormat ID_FORMATTER = new DecimalFormat("00");

    private static final CentralUnitLineHandler INSTANCE = new CentralUnitLineHandler();

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("([^:]*):\\s(.*)");

    private static final Pattern START_PATTERN = Pattern.compile("\\s*CENTRAL\\sUNITS");

    private CentralUnitLineHandler() {
    }

    public static CentralUnitLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter) {
        Matcher matcher = PROPERTY_PATTERN.matcher(line);
        if (matcher.find()) {
            String property = matcher.group(1);
            String value = matcher.group(2);

            switch (property) {
                case "Type": consumer.type(value); break;
                case "Name": consumer.name(value); break;
                case "Serial Number": consumer.serialNumber(value); break;
                case "IP Address": consumer.ipAddress(value); break;
                case "Port Number": consumer.portNumber("55957"); break; //Cannot change port number, this is hardcoded
                case "MAC Address": consumer.macAddress(value); break;
            }
        }
    }
}
