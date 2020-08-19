package net.rejmi.pdfshow;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JButton;
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

import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import com.darwinsys.swingui.MenuUtils;
import com.darwinsys.swingui.RecentMenu;

/** 
 * A simpler PDF viewer
 * @author Ian Darwin
 */
public class PdfShow {

	public static void main(String[] args) throws Exception {
		PdfShow.instance = new PdfShow();
		for (String arg : args) {
			final File file = new File(arg);
			if (!file.canRead()) {
				JOptionPane.showMessageDialog(PdfShow.frame, "Can't open file " + file);
				continue;
			}
			PdfShow.instance.openPdfFile(file);
		}
	}
	
	static PdfShow instance;

	Desktop desktop=Desktop.getDesktop();
	Properties programProps = new Properties();
	final static String PROPS_FILE_NAME = "/pdfshow.properties";
	final static String KEY_FEEDBACK_URL = "feedback_url",
			KEY_FEEDBACK_EMAIL = "feedback_email",
			KEY_SOURCE_URL = "github_url";
	final static String EMAIL_TEMPLATE = "mailto:%s?subject=PdfShow Feedback";
	
	// GUI Controls - defined here since referenced throughout
	static JFrame frame;
	private JTabbedPane tabPane;
	private DocTab currentTab;
	private JButton upButton, downButton; // Do not move into constr
	private JTextField pageNumTF;		 // Nor me.
	// These can't be final due to constructor operation ordering
	private /*final*/ JButton selectButton, textButton, markerButton,
		lineButton, polyLineButton, ovalButton, rectangleButton; // Me three
	final RecentMenu recents;
	
	// MAIN CONSTRUCTOR

	PdfShow() throws IOException {

		gotoState(viewState);

		// GUI SETUP

		frame = new JFrame("PDFShow");
		Toolkit tk = Toolkit.getDefaultToolkit();
		frame.setSize(tk.getScreenSize());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setFocusable(true);
		final Image iconImage = getImage("/images/logo.png");
		// System.out.println("PdfShow.PdfShow(): " + iconImage);
		frame.setIconImage(iconImage);

		programProps = new Properties();
		InputStream propwash = getClass().getResourceAsStream(PROPS_FILE_NAME);
		if (propwash == null) {
			throw new IllegalStateException("Unable to load " + PROPS_FILE_NAME);
		}
		programProps.load(propwash);
		propwash.close();
		// System.out.println("PdfShow.PdfShow(): Properties " + programProps);

		// TABBEDPANE (main window for viewing PDFs)

		tabPane = new JTabbedPane();
		tabPane.addChangeListener(evt -> {
			currentTab = (DocTab)tabPane.getSelectedComponent();
			// This shouldn't be needed...
			if (currentTab != null)
				pageNumTF.setText(Integer.toString(currentTab.getPageNumber()));
		});
		frame.add(BorderLayout.CENTER, tabPane);
		// MENUS

		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		ResourceBundle rb = ResourceBundle.getBundle("Menus");
		JMenu fm = MenuUtils.mkMenu(rb, "file");
		menuBar.add(fm);
		JMenuItem miOpen = MenuUtils.mkMenuItem(rb, "file", "open");
		miOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fm.add(miOpen);
		recents = new RecentMenu(this) {
			private static final long serialVersionUID = 1L;
			@Override
			public void loadFile(String fileName) throws IOException {
				openPdfFile(new File(fileName));
			}
		};
		miOpen.addActionListener(e -> {
			try {
				recents.openFile(chooseFile().getAbsolutePath());
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(frame, "Can't open file: " + e1);
			}
		});
		fm.add(recents);
		JMenuItem miClearRecents = MenuUtils.mkMenuItem(rb, "file", "clear_recents");
		miClearRecents.addActionListener(e -> recents.clear());
		fm.add(miClearRecents);
		JMenuItem miClose = MenuUtils.mkMenuItem(rb, "file", "close");
		miClose.addActionListener(e -> {
			if (currentTab != null) {
				closeFile(currentTab);
			}
		});
		fm.add(miClose);

		final JMenuItem infoButton = MenuUtils.mkMenuItem(rb, "file", "properties");
		infoButton.addActionListener(e -> showFileProps());
		fm.add(infoButton);

		fm.addSeparator();
		JMenuItem miQuit = MenuUtils.mkMenuItem(rb, "file", "exit");
		miQuit.addActionListener(e -> checkAndQuit());
		fm.add(miQuit);

		final JMenu editMenu = MenuUtils.mkMenu(rb, "edit");
		menuBar.add(editMenu);
		final JMenuItem optionsMI = MenuUtils.mkMenuItem(rb, "edit", "options");
		optionsMI.setEnabled(false);
		// XXX should launch chooser for font, color, line width, etc.
		editMenu.add(optionsMI);

		final JMenu helpMenu = MenuUtils.mkMenu(rb, "help");
		menuBar.add(helpMenu);
		final JMenuItem aboutButton = MenuUtils.mkMenuItem(rb, "help", "about");
		aboutButton.addActionListener(e->
			JOptionPane.showMessageDialog(frame, "PdfShow v0.0\n" +
			"c 2020 Ian Darwin\n" +
			"https://darwinsys.com/freeware\n" +
			"Most icons from feathericons.com; a few by the author.",
			"About PdfShow(tm)",
			JOptionPane.INFORMATION_MESSAGE));
		helpMenu.add(aboutButton);
		final JMenuItem helpButton = MenuUtils.mkMenuItem(rb, "help", "help");
		helpButton.addActionListener(e->
	    	JOptionPane.showMessageDialog(frame, "Help not written yet, sorry!"));
		helpMenu.add(helpButton);
		final JMenuItem sourceButton = new JMenuItem("Source Code");
		sourceButton.setIcon(getMyImageIcon("octocat"));
		sourceButton.addActionListener(e -> {
			String url = programProps.getProperty(KEY_SOURCE_URL);
			try {
				desktop.browse(new URI(url));
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(frame, "Failed to open browser to " + url);
			}
		});
		helpMenu.add(sourceButton);

		// NAV BOX
		// System.out.println("PdfShow.PdfShow(): Building Nav Box");

		JPanel sidePanel = new JPanel();
		sidePanel.setPreferredSize(new Dimension(200, 800));
		
		JPanel navBox = new JPanel();
		navBox.setLayout(new GridLayout(3,3));
		
		// Row 1 - just Up button
		upButton = new JButton(getMyImageIcon("Chevron-Up"));
		upButton.addActionListener(e -> moveToPage(currentTab.getPageNumber() - 1));
		navBox.add(new JLabel());	// Placeholder for grid
		navBox.add(upButton); 
		navBox.add(new JLabel());	// Ditto

		// Row 2 - first page, # page, last page
		JButton firstButton = new JButton(getMyImageIcon("Rewind")), 
			lastButton = new JButton(getMyImageIcon("Fast-Forward"));
		firstButton.addActionListener(e -> moveToPage(0));
		navBox.add(firstButton);
		pageNumTF = new JTextField(3);
		pageNumTF.addMouseListener(new MouseAdapter() {
			// If you click in it, select all so you can overtype
			@Override
			public void mouseClicked(MouseEvent e) {
				pageNumTF.selectAll();
			}			
		});
		pageNumTF.addActionListener(e -> {
			String text = pageNumTF.getText();
			try {
				final int pgNum = Integer.parseInt(text);
				moveToPage(pgNum);
			} catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(frame,
					String.format(
						"Could not interpret '%s' as a number, alas.", text),
					"How's that?",
					JOptionPane.ERROR_MESSAGE);
			}
		});
		navBox.add(pageNumTF);
		lastButton.addActionListener(e -> moveToPage(Integer.MAX_VALUE));
		navBox.add(lastButton);
		
		// Row 3 - just down button
		navBox.add(new JLabel());
		downButton = new JButton(getMyImageIcon("Chevron-Down"));
		downButton.addActionListener(e -> moveToPage(currentTab.getPageNumber() + 1));
		navBox.add(downButton);
		navBox.add(new JLabel());
		navBox.setPreferredSize(new Dimension(200, 200));
		sidePanel.add(navBox);

		// END NAV BOX

		// TOOL BOX
		// System.out.println("PdfShow.PdfShow(): Building Toolbox");

		JPanel toolBox = new JPanel();
		toolBox.setLayout(new GridLayout(0, 2));

		// Mode buttons

		selectButton = new JButton(getMyImageIcon("Select"));
		selectButton.addActionListener(e -> gotoState(viewState));
		toolBox.add(selectButton);

		textButton = new JButton(getMyImageIcon("Text"));
		textButton.addActionListener(e -> gotoState(textDrawState));
		toolBox.add(textButton);

		markerButton = new JButton(getMyImageIcon("Marker"));
		markerButton.addActionListener(e -> gotoState(markingState));
		toolBox.add(markerButton);

		lineButton = new JButton(getMyImageIcon("Line"));
		lineButton.addActionListener(e -> gotoState(lineDrawState));
		toolBox.add(lineButton);
		
		polyLineButton = new JButton(getMyImageIcon("PolyLine"));
		polyLineButton.addActionListener(e -> gotoState(polyLineDrawState));
		toolBox.add(polyLineButton);
		
		ovalButton = new JButton(getMyImageIcon("Oval"));
		ovalButton.addActionListener(e -> gotoState(ovalState));
		toolBox.add(ovalButton);
		
		rectangleButton = new JButton(getMyImageIcon("Rectangle"));
		rectangleButton.addActionListener(e -> gotoState(rectangleState));
		toolBox.add(rectangleButton);
		
		// Other buttons
		final JButton clearButton = new JButton(getMyImageIcon("Trash"));
		clearButton.addActionListener(e -> currentTab.deleteAll());
		toolBox.add(clearButton);
		
		final JButton undoButton = new JButton(getMyImageIcon("Undo"));
		undoButton.addActionListener(e -> { currentTab.removeLastIn(); currentTab.repaint(); });
		toolBox.add(undoButton);

		final JButton feedbackButton = new JButton(getMyImageIcon("Feedback"));
		feedbackButton.addActionListener(e -> {
			String[] choices = { "Web", "Email", "Cancel" };
			int n = JOptionPane.showOptionDialog(frame, "How to send feedback?", "Send Feedback", 
						JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, 
						choices, 0);
			try {
				switch(n) {
				case 0: // Web
					String webStr = programProps.getProperty(KEY_FEEDBACK_URL);
					URI weburl = new URI(webStr);
					desktop.browse(weburl); 
					return;
				case 1: // Email
					String mailStr = programProps.getProperty(KEY_FEEDBACK_EMAIL);
					URI mailurl = new URI(
							String.format(EMAIL_TEMPLATE, mailStr).replaceAll(" ", "%20"));
					desktop.mail(mailurl);
					return;
				case 2:
					return;
				}
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(frame, "Unable to contact feedback form\n" + ex,
						"Feedback Fail!", JOptionPane.ERROR_MESSAGE);
			}
		});
		toolBox.add(feedbackButton);
		
		sidePanel.add(toolBox);

		// END TOOL BOX

		frame.add(BorderLayout.WEST, sidePanel);
		frame.setVisible(true);
	}


	/** 
	 * State is a class, not an interface, so subclasses don't
	 * have to implement every method.
	 */
	private abstract class State {

		protected JButton button;

		public State(JButton button) {
			this.button = button;
		}
		/** Anything to be done on entering a given state */
		public void enterState() {
			//
		}
		
		/** Called by any State when it is done, e.g., after
		 * composing and adding a GObject, currentTab.repaint(), done()
		 */
		public void done() {
			// gotoState(viewState);
		}

		public void leaveState() {
			//
		}

		public void keyPressed(KeyEvent e) {
			// System.out.println("PdfShow.State.keyPressed(" + e + ")");
			switch(e.getKeyChar()) {
			case 'j':
			case '\n':
			case ' ':
			case KeyEvent.VK_UP:
				currentTab.gotoNext();
				return;
			case 'k':
			case '\b':
			case KeyEvent.VK_DOWN:
				currentTab.gotoPrev();
				return;
			default:
				switch(e.getKeyCode()) {
				case KeyEvent.VK_DOWN:
					currentTab.gotoNext(); return;
				case KeyEvent.VK_UP:
					currentTab.gotoPrev(); return;
				}
			}
			System.out.println("Unhandled key event: " + e);
		}

		public void mouseClicked(MouseEvent e) {
			// probably want to override this
		}

		public void mousePressed(MouseEvent e) {
			// empty
		}

		public void mouseDragged(MouseEvent e) {
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

	/** State for normal viewing */
	class ViewState extends State {
		// Nothing to do - is default State
		ViewState(JButton button) {
			super(button);
		}
	}
	final State viewState = new ViewState(selectButton);

	/** State for adding text annotations */
	class TextDrawState extends State {
		TextDrawState(JButton button) {
			super(button);
		}
		@Override
		public void mousePressed(MouseEvent e) {
			String text = JOptionPane.showInputDialog(frame, "Text?");
			if (text != null) {
				currentTab.addIn(new GText(e.getX(), e.getY(), text));
				currentTab.repaint();
				done();
			}
		}
	}
	final State textDrawState = new TextDrawState(textButton);

	/** Marker: straight line: click start, click end. */
	class MarkingState extends State {
		
		MarkingState(JButton button) {
			super(button);
		}
		
		int startX = -1, startY = -1, ix;
		GMarker mark;
		@Override
		public void mousePressed(MouseEvent e) {
			startX = e.getX();
			startY = e.getY();
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			if (mark == null) {
				mark = new GMarker(startX, startY, e.getX(), e.getY());
				ix = currentTab.addIn(mark);
			} else {
				currentTab.setIn(ix, new GMarker(startX, startY, e.getX(), e.getY()));
			}
			currentTab.repaint();
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			mark = null;
			done();
		}
	}
	final State markingState = new MarkingState(markerButton);

	/** For now, crude line-drawing: click start, click end. */
	class LineDrawState extends State {

		LineDrawState(JButton button) {
			super(button);
		}

		int startX = -1, startY = -1, ix;
		GLine line;
		@Override
		public void mousePressed(MouseEvent e) {
			startX = e.getX();
			startY = e.getY();
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			if (line == null) {
				line = new GLine(startX, startY, e.getX(), e.getY());
				ix = currentTab.addIn(line);
			} else {
				currentTab.setIn(ix, new GLine(startX, startY, e.getX(), e.getY()));
			}
			currentTab.repaint();
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			line = null;
			done();
		}
	}
	final State lineDrawState = new LineDrawState(lineButton);

	class PolyLineDrawState extends State {
		
		PolyLineDrawState(JButton button) {
			super(button);
		}
		
		int n = 0, ix;
		int lastx, lasty;
		GPolyLine line;
		@Override
		public void mousePressed(MouseEvent e) {
			// System.out.println("PdfShow.PolyLineDrawState.mousePressed()");
			n = 0;
			line = new GPolyLine(e.getX(), e.getY());
			ix = currentTab.addIn(line);
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			int newx = e.getX(); int newy = e.getY();
			int dx = newx - lastx;
			if (dx > -5 && dx < +5)
				return;
			int dy = newy - lasty;
			if (dy > -5 && dy < +5)
				return;
			line.addPoint(newx, newy);
			currentTab.repaint();
			lastx = newx; lasty = newy;
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			// System.out.println("PdfShow.PolyLineDrawState.mouseReleased()");
			currentTab.repaint();
			line = null;	// We are done with it.
			done();
		}
	}
	final State polyLineDrawState = new PolyLineDrawState(polyLineButton);

	class RectangleState extends State {
		
		RectangleState(JButton button) {
			super(button);
		}
		
		int ulX = -1, ulY = -1;
		GRectangle rect;
		int ix;
		@Override
		public void mousePressed(MouseEvent e) {
			ulX = e.getX();
			ulY = e.getY();
			rect = null;
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			if (rect == null) {
				rect = new GRectangle(ulX, ulY, e.getX(), e.getY());
				ix = currentTab.addIn(rect);
			} else {
				currentTab.setIn(ix, new GRectangle(ulX, ulY, e.getX(), e.getY()));
			}
			currentTab.repaint();
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			// currentTab.addIn(new GRectangle(ulX, ulY, e.getX(), e.getY()));
			currentTab.repaint(); // XXX Should addIn() do repaint() for us?
			done();
		}
		
	}
	final State rectangleState = new RectangleState(rectangleButton);
	
	class OvalState extends State {
		
		OvalState(JButton button) {
			super(button);
		}
		
		int ulX = -1, ulY = -1;
		GOval oval;
		int ix;
		@Override
		public void mousePressed(MouseEvent e) {
			ulX = e.getX();
			ulY = e.getY();
			oval = null;
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			if (oval == null) {
				oval = new GOval(ulX, ulY, e.getX(), e.getY());
				ix = currentTab.addIn(oval);
			} else {
				currentTab.setIn(ix, new GOval(ulX, ulY, e.getX(), e.getY()));
			}
			currentTab.repaint();
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			// currentTab.addIn(new GRectangle(ulX, ulY, e.getX(), e.getY()));
			currentTab.repaint(); // XXX Should addIn() do repaint() for us?
			done();
		}
		
	}
	final State ovalState = new OvalState(ovalButton);

	static State currentState;

	static void gotoState(State state) {
		if (currentState != null)	// At beginning of program
			currentState.leaveState();
		currentState = state;
		currentState.enterState();
	}

	void showFileProps() {
		final PDDocumentInformation docInfo = currentTab.doc.getDocumentInformation();
		StringBuilder sb = new StringBuilder();
		sb.append("Title: ").append(docInfo.getTitle()).append('\n');
		sb.append("Author: ").append(docInfo.getAuthor()).append('\n');
		sb.append("Producer: ").append(docInfo.getProducer()).append('\n');
		sb.append("Subject: ").append(docInfo.getSubject()).append('\n');
		JOptionPane.showMessageDialog(frame, sb.toString(), 
			currentTab.file.getName(), JOptionPane.INFORMATION_MESSAGE);
	}

	// Listeners; these get added to each DocTab in openPdfFile().
	private MouseListener ml = new MouseAdapter() {
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
		@Override
		public void mouseEntered(MouseEvent e) {
			currentState.mouseEntered(e);
			currentTab.requestFocus();
		}
		@Override
		public void mouseExited(MouseEvent e) {
			currentState.mouseExited(e);
		};
	};
	private MouseMotionListener mml = new MouseMotionListener() {

		@Override
		public void mouseDragged(MouseEvent e) {
			currentState.mouseDragged(e);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			// Ignore
		}		
	};
 
	private KeyListener kl = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {
			currentState.keyPressed(e);
		};
	};


	void pageNumberChanged() {
		pageNumTF.setText(String.format("%d of %d", currentTab.getPageNumber(), currentTab.pageCount));
	}

	private static void checkAndQuit() {
		// TODO Once we add saving, check for unsaved changes
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
			fc.showOpenDialog(frame);
			f = fc.getSelectedFile();
			if (f != null) {
				return f;
			} else {
				JOptionPane.showMessageDialog(frame, "Please choose a file");
			}
		} while (f != null);
		return null;
	}

	private void openPdfFile(File file) throws IOException {
		DocTab t = new DocTab(file);
		t.setFocusable(true);
		t.addKeyListener(kl);
		t.addMouseListener(ml);
		t.addMouseMotionListener(mml);

		tabPane.addTab(file.getName(), currentTab = t);
		tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
		moveToPage(0);
	}

	private void closeFile(DocTab dt) {
		dt.close();
		tabPane.remove(dt);
	}

	private void moveToPage(int newPage) {
		int docPages = currentTab.pageCount;
		if (newPage < 0) {
			newPage = 0;
		}
		if (newPage >= docPages) {
			newPage = docPages - 1;
		}
		if (newPage == currentTab.getPageNumber()) {
			return;
		}
		pageNumTF.setText(Integer.toString(newPage) +  " of " + currentTab.pageCount);
		currentTab.setPageNumber(newPage);
		upButton.setEnabled(currentTab.getPageNumber() > 0);
		downButton.setEnabled(currentTab.getPageNumber() < docPages);
		currentTab.repaint();
	}

	// Graphics helpers

	/** Convenience routine to get an application-local image */
	private ImageIcon getMyImageIcon(String name) {
		String fullName = "/images/" + name;
		return getImageIcon(fullName);
	}

	/** Grab an image off the resource path, which might be
	 * in one of several formats (png, jpg, gif, etc.).
	 * @param imgName The name with no extension, e.g., "/images/Line"
	 * @return an ImageIcon to display the thing.
	 */
	private ImageIcon getImageIcon(String imgName) {
		for (String ext : new String[] {".png", ".jpg" }) {			
			URL imageURL = getClass().getResource(imgName  + ext);
			if (imageURL != null) {
				ImageIcon ii = new ImageIcon(imageURL);
				return ii;
			}
		}
		throw new IllegalArgumentException("No image: " + imgName);
	}

	// Old format, still needed for JFrame(?)
	protected Image getImage(String imgName) {
		URL imageURL = getClass().getResource(imgName);

		if (imageURL == null) {
			throw new IllegalArgumentException("No image: " + imgName);
		}
		return Toolkit.getDefaultToolkit().getImage(imageURL);
	}
}
