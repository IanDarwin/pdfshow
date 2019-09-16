package net.rejmi.pdfshow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
		private int pageCount = 0;
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

	private static class DrawState implements MouseListener, KeyListener{

		@Override
		public void keyTyped(KeyEvent e) {
			// Probably want to override this
		}

		@Override
		public void keyPressed(KeyEvent e) {
			// empty
		}

		@Override
		public void keyReleased(KeyEvent e) {
			// empty
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// probably want to override this
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// empty
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// empty
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// empty
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// Probably want to override this
		}

	}
	DrawState drawState;

	private static JFrame jf;
	private static JTabbedPane jtp;
	private static DocTab tab;
	private static JButton upButton, downButton;
	private static JTextField pageNumTF;

	public static void main(String[] args) throws Exception {

		jf = new JFrame("PDFShow");
		jf.setSize(1000,800);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// TABBEDPANE

		jtp = new JTabbedPane();
		jtp.addChangeListener(evt -> {
			tab = (DocTab)jtp.getSelectedComponent();
			// This shouldn't be needed...
			pageNumTF.setText(Integer.toString(tab.pageNumber));
		});
		jf.add(BorderLayout.CENTER, jtp);
		JPanel glassPane = (JPanel) jf.getGlassPane();
		glassPane.setLayout(null); // Displace default FlowLayout
		glassPane.setVisible(true);

		// MENUS

		JMenuBar mb = new JMenuBar();
		jf.setJMenuBar(mb);
		ResourceBundle rb = ResourceBundle.getBundle("Menus");
		JMenu fm = MenuUtils.mkMenu(rb, "file");
		mb.add(fm);
		JMenuItem miOpen = MenuUtils.mkMenuItem(rb, "file", "open");
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
		JMenuItem miRecents = MenuUtils.mkMenuItem(rb, "file", "recents");
		fm.add(miRecents);
		JMenuItem miClose = MenuUtils.mkMenuItem(rb, "file", "close");
		miClose.addActionListener(e -> {
			if (tab != null) {
				closeFile(tab);
			}
		});
		fm.add(miClose);

		fm.addSeparator();
		JMenuItem miQuit = MenuUtils.mkMenuItem(rb, "file", "exit");
		miQuit.addActionListener(e -> checkAndQuit());
		fm.add(miQuit);

		// TOOLBOX

		JPanel toolBox = new JPanel();
		toolBox.setBackground(Color.cyan);
		toolBox.setPreferredSize(new Dimension(200, 800));
		JPanel navBox = new JPanel();
		navBox.setLayout(new GridLayout(3,3));
		upButton = new JButton("Up");
		upButton.addActionListener(e -> moveToPage(tab.pageNumber - 1));
		navBox.add(new JLabel()); navBox.add(upButton); navBox.add(new JLabel());
		downButton = new JButton("Down");
		downButton.addActionListener(e -> moveToPage(tab.pageNumber + 1));
		JButton firstButton = new JButton("<<"), 
			lastButton = new JButton(">>");
		firstButton.addActionListener(e -> moveToPage(0));
		navBox.add(firstButton);
		pageNumTF = new JTextField(3);
		pageNumTF.addActionListener(e -> moveToPage(Integer.parseInt(pageNumTF.getText())));
		navBox.add(pageNumTF);
		lastButton.addActionListener(e -> moveToPage(Integer.MAX_VALUE));
		navBox.add(lastButton);
		navBox.add(new JLabel()); navBox.add(downButton); navBox.add(new JLabel());
		navBox.setPreferredSize(new Dimension(200, 200));
		toolBox.add(navBox);

		final JButton textButton = MenuUtils.mkButton(rb, "toolbox", "text");
		textButton.addActionListener(e -> {
			@SuppressWarnings("serial")
			JComponent dlg = new JComponent() {};
			glassPane.add(dlg);
			dlg.setLocation(20,20);
			dlg.setSize(50,50);
			dlg.setBackground(Color.cyan);
		});
		toolBox.add(textButton);
		final JButton lineButton = MenuUtils.mkButton(rb, "toolbox", "line");
		lineButton.addActionListener(e -> {
			JOptionPane.showMessageDialog(jf, "Line drawing not implemented yet");
		});
		toolBox.add(lineButton);

		jf.add(BorderLayout.WEST, toolBox);
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
		t.pageCount = t.doc.getNumberOfPages();
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
		int docPages = tab.pageCount;
		if (newPage < 0) {
			newPage = 0;
		}
		if (newPage >= docPages) {
			newPage = docPages - 1;
		}
		if (newPage == tab.pageNumber) {
			return;
		}
		pageNumTF.setText(Integer.toString(newPage) +  " of " + tab.pageCount);
		tab.pageNumber = newPage;
		upButton.setEnabled(tab.pageNumber > 0);
		downButton.setEnabled(tab.pageNumber < docPages);
		tab.repaint();
	}
}
