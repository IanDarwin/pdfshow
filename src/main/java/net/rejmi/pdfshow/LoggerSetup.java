package net.rejmi.pdfshow;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

public class LoggerSetup {
	public static void init() {};
	static {
		InputStream stream = LoggerSetup.class.getClassLoader().
				getResourceAsStream("logging.properties");
		try {
			LogManager.getLogManager().readConfiguration(stream);
		} catch (IOException e) {
			Error ex = new ExceptionInInitializerError("Could not load logging.properties");
			ex.setStackTrace(e.getStackTrace());
		}
	}
}
