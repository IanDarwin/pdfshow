package net.rejmi.pdfshow;

import java.io.File;
import javax.swing.JOptionPane;

public class Main {

    public static void main(String[] args) throws Exception {

        // Instantiate main class
        PdfShow instance = new PdfShow();

        // Open files from command line, if any
        for (String arg : args) {
            if (arg.startsWith("-")) {
                switch(arg) {
                    case "-s":
                        instance.setMonitorMode(MonitorMode.SINGLE);
                        break;
                    case "-m":
                        instance.setMonitorMode(MonitorMode.MULTI);
                        break;
                }
            }
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
