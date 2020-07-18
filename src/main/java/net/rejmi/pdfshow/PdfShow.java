package net.rejmi.pdfshow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
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
import com.darwinsys.swingui.RecentMenu;

/** A simpler PDF viewer
 * @author Ian Darwin
 */
public class PdfShow {

	/** A visual rep of one PDF document, for placing within a TabView */
	@SuppressWarnings("serial")
	private static class DocTab extends JComponent {
		/** zero-origin pageNumber, not from document's page numbering */
		private int pageNumber = 0;
		/** Total size of this document */
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

	private static class State {

		/** Anything to be done on entering a given state */
		public void enterState() {
			//
		}

		public void leaveState() {
			//
		}

		public void keyTyped(KeyEvent e) {
			// Probably want to override this
		}

		public void mouseClicked(MouseEvent e) {
			// probably want to override this
		}

		public void mousePressed(MouseEvent e) {
			// empty
		}

		public void mouseReleased(MouseEvent e) {
			// empty
		}

		public void mouseEntered(MouseEvent e) {
			// empty
		}

		public void mouseExited(MouseEvent e) {
			// Probably want to override this
		}
	}
	static class ViewState extends State {
		@Override
		public void keyTyped(KeyEvent e) {
			System.out.println("PdfShow.ViewState.keyTyped() " + e.getKeyCode());
		}
		@Override
		public void mouseEntered(MouseEvent e) {
			System.out.println("PdfShow.ViewState.mouseEntered()");
		}
	}
	static final ViewState viewState = new ViewState();

	static class TextDrawState extends State {
		//
	}
	static final TextDrawState textDrawState = new TextDrawState();

	static class LineDrawState extends State {
		//
	}
	static final LineDrawState lineDrawState = new LineDrawState();

	static State currentState;

	static void gotoState(State state) {
		if (currentState != null)	// At beginning of program
			currentState.leaveState();
		currentState = state;
		currentState.enterState();
	}

	private static JFrame jf;
	private static JTabbedPane jtp;
	private static DocTab tab;
	private static JButton upButton, downButton;
	private static JTextField pageNumTF;

	public static void main(String[] args) throws Exception {
		new PdfShow();
	}

	PdfShow() {
		jf = new JFrame("PDFShow");
		jf.setSize(1000,800);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// TABBEDPANE (main window for viewing PDFs)

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
		final RecentMenu recents = new RecentMenu(this) {
			public void loadFile(String fileName) throws IOException {
				openPdfFile(new File(fileName));
			}
		};
		miOpen.addActionListener(e -> {
			try {
				recents.openFile(chooseFile().getAbsolutePath());
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(jf, "Can't open file: " + e1);
			}
		});
		fm.add(recents);
		JMenuItem miClearRecents = MenuUtils.mkMenuItem(rb, "file", "clear_recents");
		miClearRecents.addActionListener(e -> recents.clear());
		fm.add(miClearRecents);
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

		// GENERIC VIEW LISTENERS - Just delegate directly to currentState
		gotoState(viewState);
		MouseListener ml = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				currentState.mousePressed(e);
			};
			@Override
			public void mouseClicked(MouseEvent e) {
				currentState.mouseClicked(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				currentState.mouseReleased(e);
			}
			public void mouseEntered(MouseEvent e) {
				currentState.mouseEntered(e);
			}
			@Override
			public void mouseExited(MouseEvent e) {
				currentState.mouseExited(e);
			};
		};
		jf.addMouseListener(ml);
		KeyListener kl = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				System.out.println("PdfShow.main(...).new KeyAdapter() {...}.keyPressed()");
			};
			@Override
			public void keyTyped(KeyEvent e) {
				System.out.println("PdfShow.main(...).new KeyAdapter() {...}.keyTyped()");
				currentState.keyTyped(e);
			};
		};
		jf.addKeyListener(kl);

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

	private static void openPdfFile(File file) throws IOException {
		DocTab t = new DocTab();
		t.doc = PDDocument.load(file);
		t.pageCount = t.doc.getNumberOfPages();
		t.renderer = new PDFRenderer(t.doc);

		jtp.addTab(file.getName(), t);
		jtp.setSelectedIndex(jtp.getTabCount() - 1);
		// If no exception, then set 't' to be global current tab
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
