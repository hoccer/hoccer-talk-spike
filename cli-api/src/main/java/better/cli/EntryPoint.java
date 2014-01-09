package better.cli;

import java.lang.reflect.Constructor;

import better.cli.annotations.CLIEntry;
import better.cli.console.Console;
import better.cli.utils.CLIAnnotationDiscovereryListener;


import com.impetus.annovention.ClasspathDiscoverer;
import com.impetus.annovention.Discoverer;

/**
 * The entry point contains the main entry. It is in charge of
 * locating the desired CommandLineApplication implementation, as well
 * as initializing and starting it.
 * @author Sean
 *
 */
public class EntryPoint {
	
	public static void main(String[] args) {
		
		Discoverer discoverer = new ClasspathDiscoverer();
		CLIAnnotationDiscovereryListener discoveryListener =
			new CLIAnnotationDiscovereryListener(new String[] {CLIEntry.class.getName()});
		discoverer.addAnnotationListener(discoveryListener);
		discoverer.discover();

		if (discoveryListener.getDiscoveredClasses().isEmpty()) {
			Console.severe("Startup failed: Could not find CLIEntry.");
			System.exit(1);
		}
		
		String cliEntryClassName = discoveryListener.getDiscoveredClasses().get(0);

		Console.superFine("Loading CLIEntry ["+cliEntryClassName+"].");
		
		try {
			Class<?> cliEntryClass = Class.forName(cliEntryClassName);
			if (!CommandLineApplication.class.isAssignableFrom(cliEntryClass)) {
				Console.severe("CLIEntry ["+cliEntryClassName+"] is not of type CommandLineApplication.");
				System.exit(1);
			}
			
			Constructor<?> constructor = cliEntryClass.getConstructor();
			
			@SuppressWarnings("unchecked")
			CommandLineApplication<? extends CLIContext> cla =
					(CommandLineApplication<? extends CLIContext>)constructor.newInstance();
			cla.start();
		}
		catch (ClassNotFoundException e) {
			Console.severe("Unable to find CLIEntry class ["+cliEntryClassName+"].");
			System.exit(1);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
