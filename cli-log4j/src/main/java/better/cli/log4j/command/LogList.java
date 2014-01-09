package better.cli.log4j.command;

import better.cli.CLIContext;
import better.cli.Command;
import better.cli.CommandResult;
import better.cli.annotations.CLICommand;
import better.cli.utils.PrintUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;

@CLICommand(name = "loglist", description = "List loggers")
public class LogList extends Command<CLIContext> {
    @Override
    protected CommandResult innerExecute(CLIContext context) {
        try {
            Logger rootLogger = Logger.getRootLogger();
            LoggerRepository repository = rootLogger.getLoggerRepository();
            Enumeration<Logger> loggerEnum = repository.getCurrentLoggers();
            ArrayList<Logger> loggers = new ArrayList<Logger>();
            while(loggerEnum.hasMoreElements()) {
                loggers.add(loggerEnum.nextElement());
            }
            Collections.sort(loggers, new Comparator<Logger>() {
                @Override
                public int compare(Logger o1, Logger o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            loggers.add(0, rootLogger);
            String[] titles = new String[] {"name","effLevel","level"};
            String[][] lines = new String[1 + loggers.size()][];
            lines[0] = titles;
            for(int i = 0; i < loggers.size(); i++) {
                Logger logger = loggers.get(i);
                String[] columns = new String[] {"","", ""};

                columns[0] = logger.getName();

                columns[1] = logger.getEffectiveLevel().toString();

                columns[2] = "";
                Level level = logger.getLevel();
                if(level != null) {
                    columns[2] = level.toString();
                }

                lines[i+1] = columns;
            }
            PrintUtils.printTable(lines);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return CommandResult.OK;
    }
}
