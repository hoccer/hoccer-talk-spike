package better.cli.defaultcommands;

import better.cli.CLIContext;
import better.cli.Command;
import better.cli.CommandResult;
import better.cli.CommandResult.CommandResultType;
import better.cli.annotations.CLICommand;


/**
 * Returns an exit result type. This will exit the application when executed.
 * @author Sean
 *
 */
@CLICommand(name="exit", description="Exits the application.")
public class ExitCommand extends Command<CLIContext> {

	@Override
	public CommandResult innerExecute(CLIContext context) {
		return new CommandResult(CommandResultType.EXIT, 0);
	}

}
