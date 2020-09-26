package net.rejmi.pdfshow;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
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
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
 * A simpler PDF viewer.
 * Zero or one? In this class, all page numbers are one-origin.
 * DocTab's API is also one-based; it must subtract 1 internally.
 * @author Ian Darwin
 */
public class PdfShow {

	public static void main(String[] args) throws Exception {

		// Configure for macOS if possible/applicable
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name",
			PdfShow.class.getSimpleName());

		// Instantiate main program
		PdfShow.instance = new PdfShow();
		PdfShow.instance.setVisible(true);

		// Open files from command line, if any
		for (String arg : args) {
			final File file = new File(arg);
			if (!file.canRead()) {
				JOptionPane.showMessageDialog(PdfShow.frame, "Can't open file " + file);
				continue;
			}
			PdfShow.instance.recents.openFile(arg); // Include in Recents dropdown
		}
	}
	
	static PdfShow instance;

	Desktop desktop=Desktop.getDesktop();
	Properties programProps = new Properties();
	Preferences prefs = Preferences.userNodeForPackage(PdfShow.class);
	final static String PROPS_FILE_NAME = "/pdfshow.properties";
	final static String KEY_FEEDBACK_URL = "feedback_url",
			KEY_FEEDBACK_EMAIL = "feedback_email",
			KEY_SOURCE_URL = "github_url";
	final static String KEY_FILECHOOSER_DIR = "file_chooser_dir";
	final static String EMAIL_TEMPLATE = "mailto:%s?subject=PdfShow Feedback";
	
	// GUI Controls - defined here since referenced throughout
	static JFrame frame;
	private JTabbedPane tabPane;
	private DocTab currentTab;
	private JButton upButton, downButton; // Do not move into constr
	private JTextField pageNumTF;		 // Nor me.
	private JLabel pageCountTF;		// Me neither.
	// These can't be final due to constructor operation ordering:
	private /*final*/ JButton selectButton, textButton, markerButton,
		lineButton, polyLineButton, ovalButton, rectangleButton; // Me three
	final RecentMenu recents;
	
	// MAIN CONSTRUCTOR

	PdfShow() throws IOException {

		gotoState(viewState);

		// GUI SETUP

		frame = new JFrame("PDFShow");
		Toolkit tk = Toolkit.getDefaultToolkit();
		final Dimension screenSize = tk.getScreenSize();
		final Dimension windowSize = 
			new Dimension(screenSize.width, screenSize.height - 50);
		frame.setSize(windowSize);
		frame.setLocation(0, 0);
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
		recents = new RecentMenu(prefs) {
			private static final long serialVersionUID = 1L;
			@Override
			public void loadFile(String fileName) throws IOException {
				openPdfFile(new File(fileName));
			}
		};
		miOpen.addActionListener(e -> {
			try {
				final File chosenFile = chooseFile();
				if (chosenFile == null) {
					return;
				}
				recents.openFile(chosenFile.getAbsolutePath());
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
		JMenuItem newPageMI = MenuUtils.mkMenuItem(rb, "edit","newpage");
		newPageMI.addActionListener(e -> {
			if (currentTab == null)
				return;
			currentTab.insertNewPage();
		});
		editMenu.add(newPageMI);

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
		final JMenuItem feedbackMI = new JMenuItem("Feedback");
		feedbackMI.addActionListener(feedbackAction);
		helpMenu.add(feedbackMI);

		// NAV BOX

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
		JButton firstButton = new JButton(getMyImageIcon("Rewind")); 
		firstButton.addActionListener(e -> moveToPage(1));
		navBox.add(firstButton);
		pageNumTF = new JTextField("1");
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
				final int pgNum = Integer.parseInt(text.trim());
				moveToPage(pgNum);
			} catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(frame,
					String.format(
						"Could not interpret '%s' as a number, alas.", text),
					"How's that?",
					JOptionPane.ERROR_MESSAGE);
			}
		});
		pageCountTF = new JLabel("1");
		JPanel pageNumsPanel = new JPanel();
		pageNumsPanel.setLayout(new BoxLayout(pageNumsPanel, BoxLayout.PAGE_AXIS));
		pageNumsPanel.add(pageNumTF);
		pageNumsPanel.add(new JLabel("of"));
		pageNumsPanel.add(pageCountTF);
		navBox.add(pageNumsPanel);
		JButton lastButton = new JButton(getMyImageIcon("Fast-Forward"));
		lastButton.addActionListener(e -> moveToPage(currentTab.getPageCount()));
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
		feedbackButton.addActionListener(feedbackAction);
		toolBox.add(feedbackButton);
		
		sidePanel.add(toolBox);
		
		// Finally the font/color/etc settings
		sidePanel.add(new Settings(
				frame,
				GObject::setFont,
				GObject::setColor,
				GObject::setLineThickness));

		frame.add(BorderLayout.WEST, sidePanel);

		// END TOOL BOX
	}

	ActionListener feedbackAction = e -> {
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
	};

	void setVisible(boolean vis) {
		frame.setVisible(vis);
	}

	// ALL STATE CLASSES HERE

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

		public void leaveState() {
			//
		}

		public void keyPressed(KeyEvent e) {
			// System.out.println("PdfShow.State.keyPressed(" + e + ")");
			switch(e.getKeyChar()) {
			case 'j':
			case '\r':
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
				if (e.getKeyCode() == 'W') {
					if (e.isControlDown() || e.isMetaDown()) {
						closeFile(currentTab);
						return;
					}
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
	
	void visitCurrentPageGObjs(Consumer<GObject> consumer) {
		if (currentTab == null) {
			return;	// Try to draw before opening a file?
		}
		final List<GObject> currentPageAddIns = currentTab.getCurrentAddIns();
		if (currentPageAddIns.isEmpty()) {
			// System.out.println("No annotations");
			return;
		}
		currentPageAddIns.forEach(gobj -> consumer.accept(gobj));
	}

	boolean changed = false, found = false;
	
	/** State for normal viewing */
	class ViewState extends State {
		// Default State
		ViewState(JButton button) {
			super(button);
		}
		
		// Select an object
		@Override
		public void mouseClicked(MouseEvent e) {
			int mx = e.getX(), my = e.getY();
			System.out.printf(
					"PdfShow.ViewState.mouseClicked() x %d y %d\n", mx, my);
			changed = found = false;
			visitCurrentPageGObjs(gobj -> {
				if (found) {
					return;	// Only select one
				}
				if (gobj.isSelected) {
					gobj.isSelected = false;
					changed = true;
				}
				// gobj.contains(mx, my)
				if (mx >= gobj.x && mx <= gobj.x + gobj.width &&
					my >= gobj.y && my <= gobj.y + gobj.height) {
					System.out.println("HIT: " + gobj);
					gobj.isSelected = true;
					changed = true;
					found = true;
				} else {
					System.out.println("MISS: " + gobj);
				}
			});
			if (changed) {
				currentTab.repaint();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			mouseClicked(e);
		}

		// Move the selected object
		@Override
		public void mouseDragged(MouseEvent e) {
			final int mx = e.getX(), my = e.getY();
			visitCurrentPageGObjs(gobj -> {
				if (gobj.isSelected) {
					// XXX Adjust for mousex - x
					gobj.x = mx; gobj.y = my;
					currentTab.repaint(); // XXX expensive during drag?
				}
			});
		}
		
		@Override
		public void leaveState() {
			visitCurrentPageGObjs(gobj->{
				if (gobj.isSelected) {
					gobj.isSelected = false;
					changed = true;
				}	
				if (changed) {
					currentTab.repaint();
				}
			});
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
				mark = 
					new GMarker(startX, startY, e.getX() - startX, e.getY() - startY);
				ix = currentTab.addIn(mark);
			} else {
				currentTab.setIn(ix, 
					new GMarker(startX, startY, e.getX() - startX, e.getY() - startY));
			}
			currentTab.repaint();
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			mark = null;
		}
	}
	final State markingState = new MarkingState(markerButton);

	/** For now, crude line-drawing: click start, drag to end. */
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
				line = new GLine(startX, startY, e.getX() - startX, e.getY() - startY);
				ix = currentTab.addIn(line);
			} else {
				currentTab.setIn(ix, new GLine(startX, startY, e.getX() - startX, e.getY() - startY));
			}
			currentTab.repaint();
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			line = null;
		}
	}
	final State lineDrawState = new LineDrawState(lineButton);

	/** A simple multi-straight-line poly-point line. No Bezier &c.
	 * Unlike most of the other GObject types, the points in a
	 * polyline are RELATIVE and get absolutized in the GPolyLine
	 * drawing code.
	 */
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
			line = new GPolyLine(lastx = e.getX(), lasty = e.getY());
			ix = currentTab.addIn(line);
		}
		/** We get a stream of events; skip over trivial moves */
		@Override
		public void mouseDragged(MouseEvent e) {
			int newx = e.getX();
			int newy = e.getY();
			int dx = newx - lastx;
			int dy = newy - lasty;
			int thresh = 2;
			if (dx > -thresh && dx < +thresh &&
					dy > -thresh && dy < +thresh)
				return;
			line.addPoint(dx, dy);
			currentTab.repaint();
			lastx = newx; lasty = newy;
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			// System.out.println("PdfShow.PolyLineDrawState.mouseReleased()");
			currentTab.repaint();
			line = null;	// We are done with it.
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
				rect = new GRectangle(ulX, ulY, e.getX() - ulX, e.getY() - ulY);
				ix = currentTab.addIn(rect);
			} else {
				currentTab.setIn(ix, new GRectangle(ulX, ulY, e.getX() - ulX, e.getY() - ulY));
			}
			currentTab.repaint();
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			// currentTab.addIn(new GRectangle(ulX, ulY, e.getX(), e.getY()));
			currentTab.repaint(); // XXX Should addIn() do repaint() for us?
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
			int x = e.getX(), y = e.getY();
			if (oval == null) {
				oval = new GOval(ulX, ulY, x - ulX, y - ulY);
				ix = currentTab.addIn(oval);
			} else {
				currentTab.setIn(ix, new GOval(ulX, ulY, x - ulX, y - ulY));
			}
			currentTab.repaint();
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			currentTab.repaint(); // XXX Should addIn() do repaint() for us?
		}
	}

	final State ovalState = new OvalState(ovalButton);

	// State Management

	static State currentState;

	static void gotoState(State state) {
		if (currentState != null)	// At beginning of program
			currentState.leaveState();
		currentState = state;
		currentState.enterState();
	}

	// Everything Else

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

	private static void checkAndQuit() {
		// TODO Once we add saving, check for unsaved changes
		System.exit(0);
	}

	private File chooseFile() {
		String prevDir = prefs.get(KEY_FILECHOOSER_DIR, null);
		System.out.println("PdfShow.chooseFile(): prevDir = " + prevDir);
		String dir = prevDir != null ? prevDir :
				System.getProperty("user.home");
		JFileChooser fc = new JFileChooser(dir);
		FileFilter filter = new FileFilter() {

			/** Return true if the given file is accepted by this filter. */
			@Override
			public boolean accept(File f) {
				// Little trick: if you don't do this, directory names not
				// ending in one of the extensions don't show up!
				if (f.isDirectory()) {
					return true;
				};
				return (f.isFile() && f.getName().endsWith(".pdf"));
			}

			/** Return the printable description of this filter. */
			@Override
			public String getDescription() {
				return "PDF Files";
			}
		};

		fc.addChoosableFileFilter(filter);
		fc.setAcceptAllFileFilterUsed(false);

		fc.showOpenDialog(frame);
		final File selectedFile = fc.getSelectedFile();
		if (selectedFile != null) {
			System.out.println("PdfShow.chooseFile(): put: " + selectedFile.getParent());
			prefs.put(KEY_FILECHOOSER_DIR, selectedFile.getParent());
			try {
				prefs.flush();
			} catch (BackingStoreException e) {
				JOptionPane.showMessageDialog(frame, "Failed to save prefs: " + e);
				e.printStackTrace();
			}
		}
		return selectedFile;
	}

	private void openPdfFile(File file) throws IOException {
		DocTab t = new DocTab(file);
		t.setFocusable(true);
		t.addKeyListener(kl);
		t.addMouseListener(ml);
		t.addMouseMotionListener(mml);

		tabPane.addTab(file.getName(), currentTab = t);
		final int index = tabPane.getTabCount() - 1;
		tabPane.setSelectedIndex(index);
		tabPane.setTabComponentAt(index, new ClosableTabHeader(this, t));
		moveToPage(0);
	}

	/** The header for the TabbedPane doctabs */
	final class ClosableTabHeader extends JPanel {
		private static final long serialVersionUID = 1L;

		public ClosableTabHeader(PdfShow pdfShow, DocTab docTab) {
			setLayout(new FlowLayout());
			add(new JLabel(docTab.file.getName()));
			JButton xButton = new MyCloseButton();
			add(xButton);
			xButton.setPreferredSize(new Dimension(16,16));
			xButton.addActionListener(e -> pdfShow.closeFile(docTab));
		}

		// The X button for tabs
		class MyCloseButton extends JButton {
			private static final long serialVersionUID = 1L;

			public MyCloseButton() {
			    super("x");
			    setBorder(BorderFactory.createEmptyBorder());
			    setFocusPainted(false);
			    setBorderPainted(false);
			    setContentAreaFilled(false);
			    setRolloverEnabled(false);
			  }
			  @Override
			  public Dimension getPreferredSize() {
			    return new Dimension(16, 16);
			  }
			};
	}

	private void closeFile(DocTab dt) {
		dt.close();
		tabPane.remove(dt);
	}

	private void moveToPage(int newPage) {
		int currentPageNumber = currentTab.getPageNumber();
		final int currentPageCount = currentTab.getPageCount();
		if (newPage == currentPageNumber) {
			return;
		}
		if (newPage < 1) {
			newPage = 1;
		}
		if (newPage > currentPageCount) {
			newPage = currentPageCount;
		}
		currentTab.gotoPage(newPage);
		updatePageNumbersDisplay();
		currentPageNumber = currentTab.getPageNumber();
		upButton.setEnabled(currentPageNumber > 1);
		downButton.setEnabled(currentPageNumber < currentPageCount);
		currentTab.repaint();
	}

	void updatePageNumbersDisplay() {
		pageNumTF.setText(Integer.toString(currentTab.getPageNumber()));
		pageCountTF.setText(Integer.toString(currentTab.getPageCount()));
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
	private Image getImage(String imgName) {
		URL imageURL = getClass().getResource(imgName);

		if (imageURL == null) {
			throw new IllegalArgumentException("No image: " + imgName);
		}
		return Toolkit.getDefaultToolkit().getImage(imageURL);
	}
}
