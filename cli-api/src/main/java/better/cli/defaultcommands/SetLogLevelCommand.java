package better.cli.defaultcommands;

import better.cli.CLIContext;
import better.cli.Command;
import better.cli.CommandResult;
import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import better.cli.console.Console.ConsoleLevel;

import com.beust.jcommander.Parameter;

/**
 * Sets the log level of the application.
 * @author Sean
 *
 */
@CLICommand(name="conlevel", description="Sets the logging level threshold.")
public class SetLogLevelCommand extends Command<CLIContext> {
	
	@Parameter(names={"-l", "--level"}, description="The log level to set the threshold to.", required=true)
	private String _logLevel;
	
	@Override
	public CommandResult innerExecute(CLIContext context) {
		ConsoleLevel level = null;
		try {
			level = ConsoleLevel.valueOf(_logLevel.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			ConsoleLevel[] knownLevels = ConsoleLevel.values();
			StringBuilder knownLevelsStr = new StringBuilder();
			for (int i = 0; i < knownLevels.length; i++) {
				knownLevelsStr.append(knownLevels[i].name());
				if (i < knownLevels.length-1) {
					knownLevelsStr.append(", ");
				}
			}
			Console.error("Unknown log level ["+_logLevel+"]. Known levels are: " + knownLevelsStr.toString());
			return CommandResult.BAD_ARGS;
		}
		Console.setLevel(level);
		Console.info("Logging level set to ["+level.name()+"].");
		return CommandResult.OK;
	}

}
