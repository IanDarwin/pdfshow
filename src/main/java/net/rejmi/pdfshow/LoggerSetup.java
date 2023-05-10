package net.rejmi.pdfshow;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Set up the Logger configuration so main doesn't have to.
 */
public class LoggerSetup {
	public static void init() {};
	static {
		InputStream stream = LoggerSetup.class.getClassLoader().
				getResourceAsStream("logging.properties");
		try {
			LogManager.getLogManager().readConfiguration(stream);
		} catch (IOException e) {
			System.err.println("Could not load logging.properties");
		}
	}
}
