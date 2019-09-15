package net.rejmi.pdfshow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.darwinsys.swingui.MenuUtils;

public class PdfShow {
	
	@SuppressWarnings("serial")
	private static class DocTab extends JComponent {
		private int pageNumber = 0;
		private PDDocument doc;
		private PDFRenderer renderer;
		DocTab() {
			super();
			setSize(800, 800);
		}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			try {
				renderer.renderPageToGraphics(pageNumber, (Graphics2D) g);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(jf, "Failure: " + e);
			}
		}
	}
	private static JFrame jf;
	private static JTabbedPane jtp;
	private static DocTab tab;
	private static JButton upButton, downButton;
	private static JTextField pageNumTF;

	public static void main(String[] args) throws Exception {
		
		jf = new JFrame("PDFShow");
		jf.setSize(1000,800);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		jtp = new JTabbedPane();
		jtp.addChangeListener(evt -> {
			tab = (DocTab)jtp.getSelectedComponent();
			// This shouldn't be needed...
			pageNumTF.setText(Integer.toString(tab.pageNumber));
		});
		jf.add(BorderLayout.CENTER, jtp);
		
		JMenuBar mb = new JMenuBar();
		jf.setJMenuBar(mb);
		ResourceBundle b = ResourceBundle.getBundle("Menus");
		JMenu fm = MenuUtils.mkMenu(b, "file");
		mb.add(fm);
		JMenuItem miOpen = MenuUtils.mkMenuItem(b, "file", "open");
		miOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fm.add(miOpen);
		miOpen.addActionListener(e -> {
			try {
				openFile(chooseFile());
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(jf, "Can't open file: " + e1);
			}
		});
		JMenuItem miQuit = MenuUtils.mkMenuItem(b, "file", "exit");
		miQuit.addActionListener(e -> checkAndQuit());
		fm.addSeparator();
		fm.add(miQuit);

		JPanel toolbox = new JPanel();
		toolbox.setBackground(Color.cyan);
		toolbox.setPreferredSize(new Dimension(200, 800));
		upButton = new JButton("Up");
		upButton.addActionListener(e -> moveToPage(tab.pageNumber - 1));
		toolbox.add(upButton);
		downButton = new JButton("Down");
		downButton.addActionListener(e -> moveToPage(tab.pageNumber + 1));
		toolbox.add(downButton);
		JButton firstButton = new JButton("<<"), 
			lastButton = new JButton(">>");
		firstButton.addActionListener(e -> moveToPage(0));
		toolbox.add(firstButton);
		pageNumTF = new JTextField("0  ");
		pageNumTF.addActionListener(e -> moveToPage(Integer.parseInt(pageNumTF.getText())));
		toolbox.add(pageNumTF);
		lastButton.addActionListener(e -> moveToPage(Integer.MAX_VALUE));
		toolbox.add(lastButton);

		jf.add(BorderLayout.WEST, toolbox);
		jf.setVisible(true);
	}
	
	private static void checkAndQuit() {
		// TODO Once we add drawing, check for unsaved changes
		System.exit(0);
	}

	private static JFileChooser fc;
	
	private static File chooseFile() {
		String curDir = System.getProperty("user.dir");
		fc = new JFileChooser(curDir);
		FileFilter filter = new FileFilter() {

			/** Return true if the given file is accepted by this filter. */
			@Override
			public boolean accept(File f) {
				// Little trick: if you don't do this, only directory names
				// ending in one of the extentions appear in the window.
				if (f.isDirectory()) {
					return true;

				} else if (f.isFile()) {
					if (f.getName().endsWith(".pdf"))
						return true;
				}
				return false;
			}

			/** Return the printable description of this filter. */
			@Override
			public String getDescription() {
					return "PDF Files";
			}
	};

		fc.addChoosableFileFilter(filter);
		// XXX start in curdir
		// XXX add pdf-only filter
		File f = null;
		do {
			fc.showOpenDialog(jf);
			f = fc.getSelectedFile();
			if (f != null) {
				return f;
			} else {
				JOptionPane.showMessageDialog(jf, "Please choose a file");
			}
		} while (f != null);
		return null;
	}

	private static void openFile(File file) throws Exception {
		DocTab t = new DocTab();
		t.doc = PDDocument.load(file);
		t.renderer = new PDFRenderer(t.doc);
		
		jtp.addTab(file.getName(), t);
		jtp.setSelectedIndex(jtp.getTabCount() - 1);
		// If no exception, then change global tab
		tab = t;
		moveToPage(0);
	}

	private static void closeFile(DocTab dt) {
		// XXX
	}

	private static void moveToPage(int newPage) {
		int docPages = tab.doc.getNumberOfPages();
		if (newPage < 0) {
			newPage = 0;
		}
		if (newPage >= docPages) {
			newPage = docPages - 1;
		}
		if (newPage == tab.pageNumber) {
			return;
		}
		pageNumTF.setText(Integer.toString(newPage));
		tab.pageNumber = newPage;
		upButton.setEnabled(tab.pageNumber > 0);
		downButton.setEnabled(tab.pageNumber < docPages);
		tab.repaint();
	}
}
