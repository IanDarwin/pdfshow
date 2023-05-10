package net.rejmi.pdfshow;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import com.darwinsys.swingui.BreakTimer;
import com.darwinsys.swingui.MenuUtils;
import com.darwinsys.swingui.RecentMenu;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

/** 
 * Main class of "PDFShow: A simpler PDF viewer"; this class just does Swing UI.
 * Page numbers: Zero or one? In this class, all page numbers are one-origin.
 * DocTab's API is also one-based; it does the "subtract 1" dance internally.
 * @author Ian Darwin
 */
public class PdfShow {
	
	static Logger logger;

	static {
		// Configure for macOS if possible/applicable -
		// ignored on other platforms
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name",
				PdfShow.class.getSimpleName());
	}
	
	public static void main(String[] args) throws Exception {

		// Configure logging
		LoggerSetup.init();
		logger = Logger.getLogger("net.rejmi.pdfshow");
		logger.info("PdfShow Starting.");
		
		// Instantiate main program
		PdfShow.instance = new PdfShow();

		// Open files from command line, if any
		for (String arg : args) {
			final File file = new File(arg);
			if (!file.canRead()) {
				JOptionPane.showMessageDialog(controlFrame, "Can't open file " + file);
				continue;
			}
			PdfShow.instance.recents.openFile(arg); // Include in Recents dropdown
		}
	}
	
	static PdfShow instance;

	/** Notify code that the current tab has been changed */
	ObservableHelper tabChangeNotifier = new ObservableHelper();
	/** Notify code that the page number (1-based) in the current tab has changed. */
	ObservableHelper pageChangeNotifier = new ObservableHelper();

	Desktop desktop;
	Properties programProps;
	Preferences prefs = Preferences.userNodeForPackage(PdfShow.class);
	final static String PROPS_FILE_NAME = "/pdfshow.properties";
	// For programProps
	final static String KEY_FEEDBACK_URL = "feedback_general",
			KEY_BUG_ENHANCE = "feedback_bug_enhance",
			KEY_FEEDBACK_EMAIL = "feedback_email",
			KEY_SOURCE_URL = "source_url",
			KEY_HOME_URL = "home_url",
			KEY_VERSION = "version";
	// For Prefs
	final static String
			KEY_SAVE_PAGENUMS = "save pagenums",
			KEY_FILECHOOSER_DIR = "file_chooser_dir";
	final static String EMAIL_TEMPLATE = "mailto:%s?subject=PdfShow Feedback";
	
	boolean savePageNumbers = prefs.getBoolean(KEY_SAVE_PAGENUMS, true);

	// GUI Controls - defined here since referenced throughout
	static JFrame controlFrame, viewFrame;
	private final JTabbedPane tabPane = new DnDTabbedPane();
	JInternalFrame jiffy;
	JLabel emptyViewScreenLabel;
	DocTab currentTab;
	private final JButton upButton = new JButton(getMyImageIcon("Chevron-Up")),
			downButton = new JButton(getMyImageIcon("Chevron-Down"));
	private JTextField pageNumTF;
	private JLabel pageCountTF;
	private final JButton selectButton = new JButton(getMyImageIcon("Select")),
		textButton = new JButton(getMyImageIcon("Text")),
		markerButton = new JButton(getMyImageIcon("Marker")),
		lineButton = new JButton(getMyImageIcon("Line")),
		polyLineButton = new JButton(getMyImageIcon("PolyLine")),
		ovalButton = new JButton(getMyImageIcon("Oval")),
		rectangleButton = new JButton(getMyImageIcon("Rectangle"));
	Preview previewer;
	private RecentMenu recents;
	private BreakTimer breakTimer;

	// For slide show
    ExecutorService pool = Executors.newSingleThreadExecutor();
    int slideTime = 10;
    boolean done = false;

	// For "busy" popup
	private final JDialog progressDialog;

	/**
	 * MAIN CONSTRUCTOR
 	 */
	private PdfShow() throws IOException {

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		int numScreens = gs.length;
		System.out.println("Found " + numScreens + " screen(s)");
		for (GraphicsDevice curGs : gs) { // Informational
			DisplayMode dm = curGs.getDisplayMode();
			System.out.println(dm.getWidth() + " x " + dm.getHeight());
		}
		GraphicsDevice screen1 = gs[0]; // must be >= 1
		viewFrame = new JFrame("PDFShow Display");

		switch (numScreens) {
			case 1 -> {
				screen1.setFullScreenWindow(viewFrame);
				controlFrame = viewFrame;
			}
			case 2 -> {
				controlFrame = new JFrame("PDFShow Control");
				previewer = new Preview();
				controlFrame.add(previewer, BorderLayout.CENTER);
				controlFrame.setSize(800, 800);
				controlFrame.setVisible(true);
				GraphicsDevice screen2 = gs[1];
				emptyViewScreenLabel = new JLabel("<html><b>PDFShow Display</b><br/>" +
						"Open a file from the Control window to view.",
						JLabel.CENTER);
				viewFrame.add(emptyViewScreenLabel, BorderLayout.CENTER);
				screen2.setFullScreenWindow(viewFrame);
			}
			default -> {
				JOptionPane.showMessageDialog(null, "Cant handle >2 screens ATM");
				System.exit(1);
			}
		}
		
		gotoState(viewState);

		try {
			desktop=Desktop.getDesktop();
		} catch (UnsupportedOperationException ex) {
			JOptionPane.showMessageDialog(controlFrame, "Java Desktop unsupported, help &c may not work.");
			// Leave it null; check before use
		}

		controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		controlFrame.setFocusable(true);
		final Image iconImage = getImage("/images/logo.png");
		logger.fine("PdfShow.PdfShow(): " + iconImage);
		controlFrame.setIconImage(iconImage);

		final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
		progressBar.setIndeterminate(true);
		JOptionPane pane = new JOptionPane();
		pane.add(progressBar);
		progressDialog = pane.createDialog(controlFrame, "Loading...");
		progressDialog.add(progressBar);

		// If view gets resized, must re-calc scaling
		viewFrame.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				int nt = tabPane.getTabCount();
				for (int i = 0; i < nt; i++) {
					Component tabComponent = tabPane.getComponent(nt);
					if (tabPane.getComponent(nt) instanceof DocTab) {
						DocTab dt = (DocTab) tabComponent;
						dt.computeScaling(dt.doc.getPage(0).getBBox(), (JComponent) tabComponent);
					} else {
						System.out.printf("Tab %d is %s, not DocTab\n", i, tabComponent.getClass());
					}
				}
			}
		});
		// ControlFrame close -> exit
		controlFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				checkAndQuit();				
			}	
		});

		programProps = new Properties();
		InputStream propwash = getClass().getResourceAsStream(PROPS_FILE_NAME);
		if (propwash == null) {
			throw new IllegalStateException("Unable to load " + PROPS_FILE_NAME);
		}
		programProps.load(propwash);
		propwash.close();
		logger.info("PdfShow(): Properties " + programProps);

		makeTabbedPane(viewFrame);

		JMenuBar menuBar = makeMenus();
		controlFrame.setJMenuBar(menuBar);

		JPanel sidePanel = new JPanel();
		sidePanel.setPreferredSize(new Dimension(200, 800));

		JPanel navBox = makeNavBox();
		sidePanel.add(navBox);

		JPanel toolBox = makeToolbox();
		sidePanel.add(toolBox);

		JComponent stopButton = makeStopShowButton();
		sidePanel.add(stopButton);
		
        // SETTINGS
		sidePanel.add(new Settings(
				controlFrame,
				GObject.getFont(), GObject::setFont,
				GObject.getColor(), GObject::setColor,
				GObject.getLineThickness(), GObject::setLineThickness,
				slideTime, this::setSlideTime,
				savePageNumbers, this::setSavePageNumbers
			));

		controlFrame.add(BorderLayout.WEST, sidePanel);

		controlFrame.setVisible(true);
		viewFrame.setVisible(true);
	}

	/** Create a DnDTabbedPane: the main window for viewing PDFs */
	private void makeTabbedPane(JFrame frame) {

		tabPane.addChangeListener(evt -> {
			currentTab = (DocTab)tabPane.getSelectedComponent();
			if (currentTab != null) { // Avoid NPE on removal
				updatePageNumbersDisplay();
			}
		});
		frame.add(BorderLayout.CENTER, tabPane);
	}

	/** Create a JMenuBar with all the menus. */
	private JMenuBar makeMenus() {

		JMenuBar menuBar = new JMenuBar();
		ResourceBundle rb = ResourceBundle.getBundle("Menus");
		JMenu fm = MenuUtils.mkMenu(rb, "file");
		menuBar.add(fm);
		JMenuItem miOpen = MenuUtils.mkMenuItem(rb, "file", "open");
		miOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				ActionEvent.CTRL_MASK));
		fm.add(miOpen);
		recents = new RecentMenu(prefs, 10) {
			@Serial
			private static final long serialVersionUID = 828751972333590042L;
			@Override
			public void loadFile(String fileName) throws IOException {
				var file = new File(fileName);
				if (file.canRead()) {
					pool.submit( () -> {
						try {
							openPdfFile(file);
						} catch(IOException ex) {
							JOptionPane.showMessageDialog(controlFrame, "Failed to open file: " + ex);
						}
					});
				} else {
					throw new IOException("Can't read file " + file);
				}
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
				JOptionPane.showMessageDialog(controlFrame, "Can't open file: " + e1);
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
		JMenuItem deleteItemMI = MenuUtils.mkMenuItem(rb, "edit","delete");
		deleteItemMI.addActionListener(e -> {
			if (currentTab == null)
				return;
			currentTab.deleteSelected();
		});
		editMenu.add(deleteItemMI);

		final JMenu viewMenu = MenuUtils.mkMenu(rb, "view");
		menuBar.add(viewMenu);

		JMenuItem favoritesMI = MenuUtils.mkMenuItem(rb, "view","favorites");
		favoritesMI.setEnabled(false);
		viewMenu.add(favoritesMI);

		jiffy = new JInternalFrame("Timer", true, true, true, true);
		breakTimer = new BreakTimer(jiffy);

		JMenuItem breakTimerMI = MenuUtils.mkMenuItem(rb, "view","break_timer");

		breakTimerMI.addActionListener(showBreakTimer);
		viewMenu.add(breakTimerMI);

		final JMenu slideshowMenu = MenuUtils.mkMenu(rb, "slideshow");
		menuBar.add(slideshowMenu);
		final JMenuItem ssThisTabFromStartButton = MenuUtils.mkMenuItem(rb, "slideshow", "thistab_from_start");
		slideshowMenu.add(ssThisTabFromStartButton);
		ssThisTabFromStartButton.addActionListener(e -> runSlideShow(1));
		final JMenuItem ssThisTabCurButton = MenuUtils.mkMenuItem(rb, "slideshow", "thistab_from_current");
		slideshowMenu.add(ssThisTabCurButton);
		ssThisTabCurButton.addActionListener((e) -> runSlideShow(currentTab.getPageNumber()));
		final JMenuItem ssAcrossTabsButton = MenuUtils.mkMenuItem(rb, "slideshow", "across_tabs");
		ssAcrossTabsButton.addActionListener(slideshowAcrossTabsAction);
		slideshowMenu.add(ssAcrossTabsButton);
		final JMenuItem ssCustomButton = MenuUtils.mkMenuItem(rb, "slideshow", "custom");
		ssCustomButton.setEnabled(false);
		// XXX: a list of open files with a Pages textfield in each (1, 1-5, or "1,5,6")
		slideshowMenu.add(ssCustomButton);

		final JMenu helpMenu = MenuUtils.mkMenu(rb, "help");
		menuBar.add(helpMenu);
		final JMenuItem aboutButton = MenuUtils.mkMenuItem(rb, "help", "about");
		aboutButton.addActionListener(e->
				JOptionPane.showMessageDialog(controlFrame,
						String.format("PdfShow(tm) %s\n(c) 2021 Ian Darwin\n%s\n",
								programProps.getProperty(KEY_VERSION),
								programProps.getProperty(KEY_HOME_URL)),
						"About PdfShow(tm)",
						JOptionPane.INFORMATION_MESSAGE));
		helpMenu.add(aboutButton);
		final JMenuItem helpButton = MenuUtils.mkMenuItem(rb, "help", "help");
		helpButton.addActionListener(e->
				JOptionPane.showMessageDialog(controlFrame, "Help not written yet",
						"Sorry", JOptionPane.WARNING_MESSAGE));
		helpMenu.add(helpButton);
		final JMenuItem breakTimerHelpButton = MenuUtils.mkMenuItem(rb, "help", "breaktimer");
		helpMenu.add(breakTimerHelpButton);
		breakTimerHelpButton.addActionListener(e -> breakTimer.doHelp());
		final JMenuItem sourceButton = MenuUtils.mkMenuItem(rb, "help", "source_code");
		sourceButton.setIcon(getMyImageIcon("octocat"));
		sourceButton.addActionListener(e -> {
			String url = programProps.getProperty(KEY_SOURCE_URL);
			if (desktop == null) {
				JOptionPane.showMessageDialog(controlFrame,
						"Java Desktop unsupported, visit " + url + " on your own.");
			} else {
				try {
					desktop.browse(new URI(url));
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(controlFrame, "Failed to open browser to " + url);
				}
			}
		});
		helpMenu.add(sourceButton);
		final JMenuItem feedbackMI = MenuUtils.mkMenuItem(rb, "help", "feedback");
		feedbackMI.addActionListener(feedbackAction);
		helpMenu.add(feedbackMI);

		return menuBar;
	}

	/** Create the navigation box - arrows for pageup/down etc. */
	private JPanel makeNavBox() {

		JPanel navBox = new JPanel();
		navBox.setBorder(BorderFactory.createTitledBorder("Navigation"));
		navBox.setLayout(new GridLayout(0,2));

		// Row 1 - just Up button
		upButton.addActionListener(e -> moveToPage(currentTab.getPageNumber() - 1));
		navBox.add(upButton);
		downButton.addActionListener(e -> moveToPage(currentTab.getPageNumber() + 1));
		navBox.add(downButton);

		JButton firstButton = new JButton(getMyImageIcon("Rewind"));
		firstButton.addActionListener(e -> moveToPage(1));
		navBox.add(firstButton);
		JButton lastButton = new JButton(getMyImageIcon("Fast-Forward"));
		lastButton.addActionListener(e -> moveToPage(currentTab.getPageCount()));
		navBox.add(lastButton);

		// Row 2 - first page, # page, last page
		pageNumTF = new JTextField("1");
		pageNumTF.addMouseListener(new MouseAdapter() {
			// If you click in it, we select all so that you can overtype
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
				JOptionPane.showMessageDialog(controlFrame,
						String.format(
								"Could not interpret '%s' as a number, alas.", text),
						"How's that?",
						JOptionPane.ERROR_MESSAGE);
			}
		});
		pageCountTF = new JLabel("1");
		JPanel pageNumbersPanel = new JPanel();
		pageNumbersPanel.setLayout(new BoxLayout(pageNumbersPanel, BoxLayout.LINE_AXIS));
		pageNumbersPanel.add(pageNumTF);
		pageNumbersPanel.add(new JLabel(" of "));
		pageNumbersPanel.add(pageCountTF);
		navBox.add(pageNumbersPanel);

		return navBox;
	}

	/** Create the drawing-tool toolBox */
	private JPanel makeToolbox() {

		logger.info("PdfShow(): Building Toolbox");
		JPanel toolBox = new JPanel();
		toolBox.setBorder(BorderFactory.createTitledBorder("Toolbox"));
		toolBox.setLayout(new GridLayout(0, 2));

		// Mode buttons
		selectButton.addActionListener(e -> gotoState(viewState));
		toolBox.add(selectButton);

		textButton.addActionListener(e -> gotoState(textDrawState));
		textButton.setToolTipText("Add text object");
		toolBox.add(textButton);

		markerButton.addActionListener(e -> gotoState(markingState));
		markerButton.setToolTipText("Add marker");
		toolBox.add(markerButton);

		lineButton.addActionListener(e -> gotoState(lineDrawState));
		lineButton.setToolTipText("Add straight line");
		toolBox.add(lineButton);

		polyLineButton.addActionListener(e -> gotoState(polyLineDrawState));
		polyLineButton.setToolTipText("Add a polyline");
		toolBox.add(polyLineButton);

		ovalButton.addActionListener(e -> gotoState(ovalState));
		ovalButton.setToolTipText("Add oval");
		toolBox.add(ovalButton);

		rectangleButton.addActionListener(e -> gotoState(rectangleState));
		rectangleButton.setToolTipText("Add rectangle");
		toolBox.add(rectangleButton);

		// Other buttons
		final JButton clearButton = new JButton(getMyImageIcon("Trash"));
		clearButton.addActionListener(e -> currentTab.deleteAll());
		clearButton.setToolTipText("Delete ALL objects");
		toolBox.add(clearButton);

		final JButton undoButton = new JButton(getMyImageIcon("Undo"));
		undoButton.addActionListener(e -> { currentTab.removeLastIn(); currentTab.repaint(); });
		undoButton.setToolTipText("Undo last object");
		toolBox.add(undoButton);

		final JButton feedbackButton = new JButton(getMyImageIcon("Feedback"));
		feedbackButton.addActionListener(feedbackAction);
		feedbackButton.setToolTipText("Send feedback");
		toolBox.add(feedbackButton);

		final JButton starButton = new JButton(getMyImageIcon("Star"));
		starButton.addActionListener(e->
				JOptionPane.showMessageDialog(controlFrame, "Fave handling not written yet",
						"Sorry", JOptionPane.WARNING_MESSAGE));
		starButton.setToolTipText("Favorite this page");
		toolBox.add(starButton);

		final JButton timerButton = new JButton(getMyImageIcon("Timer"));
		timerButton.addActionListener(showBreakTimer);
		timerButton.setToolTipText("Open Break Timer");
		toolBox.add(timerButton);

		JButton stop_show = new JButton("Stop slide show");
		// toolBox.add(stop_show);
		stop_show.addActionListener((e -> {
			done = true;
		}));

		return toolBox;
	}

	private JComponent makeStopShowButton() {
		JButton stop_show = new JButton("Stop slide show");
		// toolBox.add(stop_show);
		stop_show.addActionListener((e -> {
			done = true;
		}));
		return stop_show;
	}

	/** Adjusts the slide show time interval */
	void setSlideTime(int n) {
	    slideTime = n;
    }

	/** Controls saving page numbers when closing file / exiting app */
	void setSavePageNumbers(boolean b) {
		prefs.putBoolean(KEY_SAVE_PAGENUMS, b);
		savePageNumbers = b;
	}

	ActionListener showBreakTimer = e ->  {
		boolean glassify = true;
		if (glassify)
			controlFrame.setGlassPane(jiffy);
		else
			controlFrame.add(jiffy);
		jiffy.setVisible(true);
	};

    /** Runs a show "across tabs" */
	ActionListener slideshowAcrossTabsAction = e -> {
        done = false;
        final DocTab tab = currentTab;
        pool.submit(() -> {
            if (tab != currentTab) {
                // user is assuming control
                return;
            }
            int n = tabPane.getSelectedIndex();
            while (!done) {
                try {
                    Thread.sleep(slideTime * 1000L);
                } catch (InterruptedException ex) {
                    done = true;
                    return;
                }
                if (done)
                	return;
                n = (n + 1) % tabPane.getTabCount();
                tabPane.setSelectedIndex(n);
            }
        });
    };

    /** Runs a show within the current tab, starting at page 'n' */
	void runSlideShow(int n) {
        done = false;
        pool.submit(() -> {
            int slideShowPageNumber = n;
            DocTab tab = currentTab;
            while (!done) {
                if (tab != currentTab) {
                    // User manually changed it, so "the show must go off"!
                	done = true;
                    return;
                }
                try {
                    Thread.sleep(slideTime * 1000L);
                } catch (InterruptedException ex) {
                    done = true;
                    return;
                }
                slideShowPageNumber = (slideShowPageNumber % tab.getPageCount()) + 1;
                tab.gotoPage(slideShowPageNumber);
            }
        });
    }

    ActionListener feedbackAction = e -> {
		String[] choices = { "Web-Comment", "Bug Report/Feature Req", "Email Team", "Cancel" }; // XXX  I18N this!
		int n = JOptionPane.showOptionDialog(controlFrame, "How to send feedback?", "Send Feedback",
					JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, 
					choices, 0);
		try {
			switch(n) {
			case 0: // Web - general
				String webStr0 = programProps.getProperty(KEY_FEEDBACK_URL);
				URI webUrl0 = new URI(webStr0);
				if (desktop == null) {
					JOptionPane.showMessageDialog(controlFrame, "Java Desktop unsupported, help unavailable.");
				} else {
					desktop.browse(webUrl0);
				}
				return;
			case 1: // Web - file bug report/enhancement request
				String webStr1 = programProps.getProperty(KEY_BUG_ENHANCE);
				URI weburl1 = new URI(webStr1);
				if (desktop == null) {
					JOptionPane.showMessageDialog(controlFrame, "Java Desktop unsupported, help unavailable.");
				} else {
					desktop.browse(weburl1); 
				}
				return;
			case 2: // Email
				String mailStr = programProps.getProperty(KEY_FEEDBACK_EMAIL);
				URI mailurl = new URI(
						String.format(EMAIL_TEMPLATE, mailStr).replaceAll(" ", "%20"));
				if (desktop == null) {
					JOptionPane.showMessageDialog(controlFrame, "Java Desktop unsupported, sending unavailable.");
				} else {
					desktop.mail(mailurl);
				}
				return;
			case 3:
				break;
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(controlFrame, "Unable to contact feedback form\n" + ex,
					"Feedback Fail!", JOptionPane.ERROR_MESSAGE);
		}
	};

	void visitCurrentPageGObjs(Consumer<GObject> consumer) {
		if (currentTab == null) {
			return;	// Try to draw before opening a file?
		}
		final List<GObject> currentPageAddIns = currentTab.getCurrentAddIns();
		if (currentPageAddIns.isEmpty()) {
			logger.fine("No annotations");
			return;
		}
		for (GObject t : currentPageAddIns) {
			consumer.accept(t);
		}
	}

	final State viewState = new ViewState(this, selectButton);
	final State textDrawState = new TextDrawState(this, textButton);
	final State markingState = new MarkingState(this, markerButton);
	final State lineDrawState = new LineDrawState(this, lineButton);
	final State polyLineDrawState = new PolyLineDrawState(this, polyLineButton);
	final State rectangleState = new RectangleState(this, rectangleButton);
	final State ovalState = new OvalState(this, ovalButton);

	// State Management

	static State currentState;

	static void gotoState(State state) {
		if (currentState != null) {	// At beginning of program
			currentState.leaveState();
		}
		currentState = state;
		currentState.enterState();
	}

	// Everything Else

	void showFileProps() {
		final PDDocumentInformation docInfo = currentTab.doc.getDocumentInformation();
		var sb = new StringBuilder();
		sb.append("Title: ").append(docInfo.getTitle()).append('\n');
		sb.append("Author: ").append(docInfo.getAuthor()).append('\n');
		sb.append("Producer: ").append(docInfo.getProducer()).append('\n');
		sb.append("Subject: ").append(docInfo.getSubject()).append('\n');
		JOptionPane.showMessageDialog(controlFrame, sb.toString(),
			currentTab.file.getName(), JOptionPane.INFORMATION_MESSAGE);
	}

	// Listeners; these get added to each DocTab in openPdfFile().
	private final MouseListener ml = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent e) {
			currentState.mousePressed(e);
		}
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
		}
	};
	private final MouseMotionListener mml = new MouseMotionListener() {

		@Override
		public void mouseDragged(MouseEvent e) {
			currentState.mouseDragged(e);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			// Ignore
		}		
	};
 
	private final KeyListener kl = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {
			currentState.keyPressed(e);
		}
	};

	/*
	 * Should be the only place we exit from.
	 */
	private void checkAndQuit() {
		if (savePageNumbers) {
			for (int i = 0; i < tabPane.getTabCount(); i++) {
				Object comp = tabPane.getComponent(i);
				if (comp instanceof DocTab)
					((DocTab)comp).close();
			}
			try {
				prefs.flush();
			} catch (BackingStoreException e) {
				JOptionPane.showMessageDialog(controlFrame, "Failed to save some prefs: " + e);
				// Nothing can be done, alas.
			}
		}
		// XXX Once we add saving, check for unsaved changes
		System.exit(0);
	}

	private File chooseFile() {
		String prevDir = prefs.get(KEY_FILECHOOSER_DIR, null);
		logger.fine("PdfShow.chooseFile(): prevDir = " + prevDir);
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
				}
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

		fc.showOpenDialog(controlFrame);
		final File selectedFile = fc.getSelectedFile();
		if (selectedFile != null) {
			logger.fine("PdfShow.chooseFile(): put: " + selectedFile.getParent());
			prefs.put(KEY_FILECHOOSER_DIR, selectedFile.getParent());
			try {
				prefs.flush();
			} catch (BackingStoreException e) {
				JOptionPane.showMessageDialog(controlFrame, "Failed to save prefs: " + e);
				e.printStackTrace();
			}
		}
		return selectedFile;
	}

	/**
	 * Opens one file.
	 * @param file A File descriptor for the file to be opened.
	 */
	private void openPdfFile(File file) throws IOException {
		if (emptyViewScreenLabel != null)
			viewFrame.remove(emptyViewScreenLabel);
		startIndefiniteProgressBar();
		DocTab t = new DocTab(file, prefs);
		t.setFocusable(true);
		t.addKeyListener(kl);
		t.addMouseListener(ml);
		t.addMouseMotionListener(mml);

		tabPane.addTab(file.getName(), currentTab = t);
		final int index = tabPane.getTabCount() - 1;
		tabPane.setSelectedIndex(index);
		ClosableTabHeader tabComponent = new ClosableTabHeader(this::closeFile, tabPane, t);
		tabPane.setTabComponentAt(index, tabComponent);
		int pageNum = savePageNumbers ? prefs.getInt("PAGE#" + file.getName(), -1) : 0;
		moveToPage(pageNum == -1 ? 0 : pageNum);
		stopIndefiniteProgressBar();
	}

	void closeFile(DocTab dt) {
		tabPane.remove(dt);
		if (tabPane.getTabCount() == 0) {
			viewFrame.add(emptyViewScreenLabel, BorderLayout.CENTER);
		}
		if (savePageNumbers) {
			prefs.putInt("PAGE#" + dt.getName(), dt.getPageNumber());
		}
		dt.close();
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
				return new ImageIcon(imageURL);
			}
		}
		throw new IllegalArgumentException("No image: " + imgName);
	}

	// Old format, still needed for JFrame icon(?)
	private Image getImage(String imgName) {
		URL imageURL = getClass().getResource(imgName);

		if (imageURL == null) {
			throw new IllegalArgumentException("No image: " + imgName);
		}
		return Toolkit.getDefaultToolkit().getImage(imageURL);
	}

	/** Show the indefinite progress bar */
	void startIndefiniteProgressBar() {
		new SwingWorker() {
			@Override
			protected Object doInBackground() {
				progressDialog.setVisible(true);
				return null;
			}
			@Override
			protected void done() {
				super.done();
			}
		}.execute();
	}

	/** Hide the indefinite progress bar */
	void stopIndefiniteProgressBar() {
		progressDialog.setVisible(false);
	}

	private class PreviewComponent extends JComponent {
		int pageNum;
		float scaleX, scaleY;

		PreviewComponent(float x, float y) {
			this.scaleX = x;
			this.scaleY = y;
		}

		void setPageNum(int pageNum) {
			this.pageNum = pageNum;
			repaint();
		}

		/** A simple draw component for slide miniatures.
		 * NB Page numbers in this code are zero based
		 * @param g the <code>Graphics</code> object to draw with
		 */
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			try {
				if (currentTab == null) {
					return;
				}
				if (pageNum < 0) {
					return;
				}
				if (pageNum >= currentTab.getPageCount()) {
					return;
				}
				currentTab.renderer.renderPageToGraphics(pageNum, (Graphics2D) g, scaleX, scaleY);
			} catch (IOException e) {
				throw new RuntimeException("Preview Rendering failed " + e, e);
			}
		}
	}

	class Preview extends JPanel {
		PreviewComponent current, prev, next;
		private int pageNumber;

		Preview() {
			setLayout(new TripartiteLayoutManager());

			current = new PreviewComponent(0.6f, 0.6f);
			add("main", current);

			prev  = new PreviewComponent(0.35f, 0.35f);
			prev.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					currentTab.gotoPage(pageNumber - 1);
				}
			});
			add("left", prev);

			next  = new PreviewComponent(0.35f, 0.35f);
			next.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					currentTab.gotoPage(pageNumber + 1);
				}
			});
			add("right", next);
		}

		void setPageNumber(int pageNum) {
			this.pageNumber = pageNum;
			pageNum--; // 1-based to 0-based
			current.setPageNum(pageNum);
			prev.setPageNum(pageNum - 1);
			next.setPageNum(pageNum + 1);
			repaint();
		}
	}
}
