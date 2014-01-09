package better.cli.log4j.command;

import better.cli.CLIContext;
import better.cli.Command;
import better.cli.CommandResult;
import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.List;

@CLICommand(name = "loglevel", description = "Set the level of a logger")
public class LogLevel extends Command<CLIContext> {

    @Parameter(description = "Pairs of logger and level or single level for root logger")
    List<String> pLogger;

    @Override
    protected CommandResult innerExecute(CLIContext context) {
        if(pLogger.size() == 1) {
            Logger rootLogger = Logger.getRootLogger();
            Level level = Level.toLevel(pLogger.get(0));
            if(level != null) {
                rootLogger.setLevel(level);
            }
            return CommandResult.OK;
        }

        if(pLogger.size() % 2 != 0) {
            System.err.println("Must provide pairs of loggers and levels");
            return CommandResult.ERROR;
        }

        for(int i = 0; i < pLogger.size(); i += 2) {
            String loggerString = pLogger.get(i);
            String levelString = pLogger.get(i+1);
            Logger logger = Logger.getLogger(loggerString);
            Level level = Level.toLevel(levelString);
            if(level != null) {
                logger.setLevel(level);
            }
        }

        return CommandResult.OK;
    }
}
