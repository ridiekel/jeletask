package io.jeletask.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jeletask.parser.handler.CentralUnitLineHandler;
import io.jeletask.parser.handler.ConditionLineHandler;
import io.jeletask.parser.handler.DimmerLineHandler;
import io.jeletask.parser.handler.GeneralMoodLineHandler;
import io.jeletask.parser.handler.InputInterfaceLineHandler;
import io.jeletask.parser.handler.InputLineHandler;
import io.jeletask.parser.handler.LineHandler;
import io.jeletask.parser.handler.LocalMoodLineHandler;
import io.jeletask.parser.handler.MotorLineHandler;
import io.jeletask.parser.handler.OutputInterfaceLineHandler;
import io.jeletask.parser.handler.RelayLineHandler;
import io.jeletask.parser.handler.RoomLineHandler;
import io.jeletask.parser.handler.SensorLineHandler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Map.*;

public class PrintedFileVisitor {
    private static final PrintedFileVisitor INSTANCE = new PrintedFileVisitor();

    private static final Map<Pattern, LineHandler> LINE_HANDLER_MAP = Map.ofEntries(
            entry(RoomLineHandler.getInstance().getStartPattern(), RoomLineHandler.getInstance()),
            entry(OutputInterfaceLineHandler.getInstance().getStartPattern(), OutputInterfaceLineHandler.getInstance()),
            entry(InputInterfaceLineHandler.getInstance().getStartPattern(), InputInterfaceLineHandler.getInstance()),
            entry(CentralUnitLineHandler.getInstance().getStartPattern(), CentralUnitLineHandler.getInstance()),
            entry(InputLineHandler.getInstance().getStartPattern(), InputLineHandler.getInstance()),
            entry(RelayLineHandler.getInstance().getStartPattern(), RelayLineHandler.getInstance()),
            entry(LocalMoodLineHandler.getInstance().getStartPattern(), LocalMoodLineHandler.getInstance()),
            entry(MotorLineHandler.getInstance().getStartPattern(), MotorLineHandler.getInstance()),
            entry(GeneralMoodLineHandler.getInstance().getStartPattern(), GeneralMoodLineHandler.getInstance()),
            entry(DimmerLineHandler.getInstance().getStartPattern(), DimmerLineHandler.getInstance()),
            entry(ConditionLineHandler.getInstance().getStartPattern(), ConditionLineHandler.getInstance()),
            entry(SensorLineHandler.getInstance().getStartPattern(), SensorLineHandler.getInstance())
    );

    private PrintedFileVisitor() {
    }

    public void visit(Consumer consumer, final InputStream stream) throws IOException {
        List<String> lines = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
        for (ListIterator<String> iterator = lines.listIterator(); iterator.hasNext(); ) {
            String line = iterator.next();
            LineHandler handler = this.getLineHandler(line);
            if (handler != null) {
                handler.handle(line, consumer, iterator);
            }
        }
    }

    private LineHandler getLineHandler(String line) {
        LineHandler handler = null;
        for (Iterator<Map.Entry<Pattern, LineHandler>> iterator = LINE_HANDLER_MAP.entrySet().iterator(); iterator.hasNext() && handler == null; ) {
            Map.Entry<Pattern, LineHandler> entry = iterator.next();
            Matcher matcher = entry.getKey().matcher(line);
            if (matcher.matches()) {
                handler = entry.getValue();
            }
        }
        return handler;
    }

    public static PrintedFileVisitor getInstance() {
        return INSTANCE;
    }

    public static void main(String[] args) throws IOException {
//        FullProprietaryModelConsumerImpl consumer = new FullProprietaryModelConsumerImpl();
        InterestingNbtModelConsumerImpl consumer = new InterestingNbtModelConsumerImpl();
        getInstance().visit(consumer, new FileInputStream("/home/ridiekel/Projects/git/Teletask-api/backend/config-nbt-export/src/main/resources/centrale.ttt"));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(System.out, consumer.getCentralUnit());
    }
}
