package net.rejmi.pdfshow;

import java.io.File;
import javax.swing.JOptionPane;

public class Main {

	public static boolean debug = false;

    public static boolean isMac = false;

    public static void main(String[] args) throws Exception {

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
                    case "-s":
                    case "--single":
                        instance.setMonitorMode(MonitorMode.SINGLE);
                        ++argsUsed;
                        break;
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
                        "Can't open file " + file);
                continue;
            }
            instance.recents.openFile(arg); // Include in Recents dropdown
        }
    }
}
