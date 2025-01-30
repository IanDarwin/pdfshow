package net.rejmi.pdfshow;

import java.io.File;
import javax.swing.JOptionPane;

public class Main {

	public static boolean debug = false;

    public static boolean isMac = false;

    public static void main(String[] args) throws Exception {

        // Configure logging
        LoggerSetup.init();

        isMac = System.getProperty("os.name").toLowerCase().startsWith("mac");
        System.out.println("isMac = " + isMac);
        // Instantiate main class
        SwingGUI instance = new SwingGUI();

        // Open files from command line, if any
        int argsUsed = 0;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                switch (arg) {
					case "-d":
						debug = true;
                        ++argsUsed;
						break;
                    case "-1":
                    case "-s":
                    case "--single":
                        instance.setMonitorMode(MonitorMode.SINGLE);
                        ++argsUsed;
                        break;
                    case "-2":
                    case "-m":
                    case "--multi-monitor":
                        instance.setMonitorMode(MonitorMode.MULTI);
                        ++argsUsed;
                        break;
                }
            }
        }
        String[] argsLeft = new String[args.length - argsUsed];
        System.arraycopy(args, argsUsed, argsLeft, 0, argsLeft.length);
        for (String arg : argsLeft) {
            final File file = new File(arg);
            if (!file.canRead()) {
                JOptionPane.showMessageDialog(instance.controlFrame,
					String.format("Can't read file %s", file));
                continue;
            }
            instance.recents.openFile(arg); // Include in Recents dropdown
        }
    }
}
