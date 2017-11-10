package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputLineHandler extends LineHandlerSupport {
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("(\\d)\\s*([^\\s]*)\\s*(.*)");

    private static final InputLineHandler INSTANCE = new InputLineHandler();

    private static final Pattern START_PATTERN = Pattern.compile("\\s*I\\s\\-\\sINTERFACES\\s*(\\d*)\\s*(\\w)([^\\s]*)");

    private static final Pattern INPUT_LINE_PATTERN = Pattern.compile("Input: (\\d*):\\s(.*)");
    private static final Pattern ACTION_LINE_PATTERN = Pattern.compile("[^:]*:\\s(\\w*)\\s(\\d*).*");

    private InputLineHandler() {
    }

    public static InputLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter) {
        Matcher matcher = this.getStartPattern().matcher(startLine);
        if (matcher.find()) {
            String autobusId = matcher.group(1);
            String autobusType = matcher.group(2);
            String autobusNumber = matcher.group(3);

            Matcher inputMatcher = INPUT_LINE_PATTERN.matcher(line);
            if (inputMatcher.find()) {
                String id = inputMatcher.group(1);
                String name = inputMatcher.group(2);

                Action shortAction = this.getAction(iterator.next());
                Action longAction = this.getAction(iterator.next());

                consumer.input(autobusId, autobusType, autobusNumber, id, name, shortAction.getType(), shortAction.getId(), longAction.getType(), longAction.getId());
            }

        }
    }

    private Action getAction(String actionLine) {
        Action action = null;
        Matcher matcher = ACTION_LINE_PATTERN.matcher(actionLine);
        if (matcher.find()) {
            action = new Action(matcher.group(1), matcher.group(2));
        } else {
            action = new Action(null, null);
        }
        return action;
    }

    private static class Action {
        private final String type;
        private final String id;

        private Action(String type, String id) {
            this.type = type;
            this.id = id;
        }

        public String getType() {
            return this.type;
        }

        public String getId() {
            return this.id;
        }
    }
}
