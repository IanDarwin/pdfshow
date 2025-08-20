package net.rejmi.pdfshow;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import com.darwinsys.swingui.*;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

/** 
 * Main class of "PDFShow: A simpler PDF viewer"; this class just does Swing UI.
 * Page numbers: Zero or one? In this class, all page numbers are one-origin.
 * DocTab's API is also one-based; it does the "subtract 1" dance internally.
 * @author Ian Darwin
 */
public class SwingGUI {

	public static final int CTRL_SIZE = 800;
	public static final int FULL_WIDTH = 1024;
	public static final String MENU_SLIDESHOW = "slideshow";
	public static final String ANNOTATIONS_SAVE_FILE = "annotations-";
	public static final String ANNOTATIONS_SAVE_EXT = ".ser";
	static Logger logger;
	
	static SwingGUI instance;

	/** Notify code that the current tab has been changed */
	ObservableHelper tabChangeNotifier = new ObservableHelper();
	/** Notify code that the page number (1-based) in the current tab has changed. */
	ObservableHelper pageChangeNotifier = new ObservableHelper();

	Desktop desktop;
	Properties programProps;
	Preferences prefs = Preferences.userNodeForPackage(SwingGUI.class);
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
			KEY_SAVE_ANNOS = "save annotations",
			KEY_JUMP_BACK = "jump back",
			KEY_ONESHOT = "one_shot_draw_tools",
			KEY_FILECHOOSER_DIR = "file_chooser_dir";
	final static String EMAIL_TEMPLATE = "mailto:%s?subject=PdfShow Feedback";
	final static Dimension SQUARE = new Dimension(24, 24);
	
	boolean savePageNumbers = prefs.getBoolean(KEY_SAVE_PAGENUMS, true);
	boolean jumpBack = prefs.getBoolean(KEY_JUMP_BACK, true);
	boolean oneShotDrawTools = prefs.getBoolean(KEY_ONESHOT, true);
	boolean saveAnnos = prefs.getBoolean(KEY_SAVE_ANNOS, true);

	// GUI Controls - defined here since referenced throughout
	JFrame controlFrame;
	static JFrame viewFrame;
	private final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	private final JTabbedPane tabPane = new DnDTabbedPane();
	JFrame bTimerFrame;
	JLabel emptyViewScreenLabel = new JLabel("<html><b>PDFShow Display</b><br/>" +
			"Open a file from the Control window to view.",
			JLabel.CENTER);
	DocTab currentTab;
	private final JButton
			upButton = new BoxButton(getMyImageIcon("Chevron-Up")),
			downButton = new BoxButton(getMyImageIcon("Chevron-Down"));
	private JTextField pageNumTF;
	private JTextField searchTF;
	private JLabel pageCountTF;
	private final JButton selectButton = new BoxButton(getMyImageIcon("Select")),
		textButton = new BoxButton(getMyImageIcon("Text")),
		markerButton = new BoxButton(getMyImageIcon("Marker")),
		lineButton = new BoxButton(getMyImageIcon("Line")),
		polyLineButton = new BoxButton(getMyImageIcon("PolyLine")),
		ovalButton = new BoxButton(getMyImageIcon("Oval")),
		rectangleButton = new BoxButton(getMyImageIcon("Rectangle")),
		checkButton = new BoxButton(getMyImageIcon("GreenCheck")),
		nixButton = new BoxButton(getMyImageIcon("RedX")),
		smileButton = new BoxButton(getMyImageIcon("Smile"));
	Preview previewer;
	RecentMenu recents;
	private BreakTimer breakTimer;
	ProgressBarSupport barHelper;

	// For slide show
    ExecutorService pool = Executors.newSingleThreadExecutor();
    int slideTime = 10;
    boolean slideshowDone = false;

	private MonitorMode monitorMode = MonitorMode.SINGLE;

	/**
	 * MAIN CONSTRUCTOR
 	 */
	SwingGUI() {

		instance = this;

		logger = Logger.getLogger("pdfshow.swingui");

		// Configure for macOS if possible/applicable - before JFrame() -
		// ignored on other platforms
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name",
				SwingGUI.class.getSimpleName());

		// Do not reference controlFrame or viewFrame before this call!
		pickAScreenOrTwo();
		controlFrame.setJMenuBar(makeMenus());
		createGuiAndListeners();
		barHelper = new ProgressBarSupport(viewFrame,"Working...");

		gotoState(viewState);
	}

	private void pickAScreenOrTwo() {
		DisplayMode dm = null;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		int numScreens = gs.length;
		logger.info("PdfShow Starting. Found %d screen(s)".formatted(numScreens));
		for (GraphicsDevice curGs : gs) { // Informational
			dm = curGs.getDisplayMode();
			logger.info(dm.getWidth() + " x " + dm.getHeight());
		}
		assert dm != null : "Could not find DM";
		viewFrame = new JFrame("PDFShow Display");
		viewFrame.setSize(new Dimension(FULL_WIDTH, CTRL_SIZE));
		// But start maximized anyway, as it's more impressive.
		viewFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

		switch (monitorMode) {
			case SINGLE:
				controlFrame = viewFrame;
				break;
			case MULTI:
				switch (numScreens) {
					case 1:
						JOptionPane.showMessageDialog(controlFrame,
								"You asked for multi-monitor mode but we only found one screen!",
								"Who's confused?", JOptionPane.WARNING_MESSAGE);
						break;
					case 2:
						controlFrame = new JFrame("PDFShow Control");
						previewer = new Preview(this);
						controlFrame.add(previewer, BorderLayout.CENTER);
						controlFrame.setSize(CTRL_SIZE, CTRL_SIZE);
						controlFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
						controlFrame.setVisible(true);
						GraphicsDevice screen2 = gs[1];
						viewFrame.setLocation(screen2.getDefaultConfiguration().getBounds().x, controlFrame.getY());
						break;
					default:
						JOptionPane.showMessageDialog(null, "Cant handle >2 screens ATM");
						System.exit(1);
				}
		}

		try {
			desktop = Desktop.getDesktop();
		} catch (UnsupportedOperationException ex) {
			JOptionPane.showMessageDialog(controlFrame, "Java Desktop unsupported, help &c may not work.");
			// Leave it null; check before use
		}

		controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		controlFrame.setFocusable(true);
		final Image iconImage = getImage("/images/logo.png");
		logger.finest("SwingGUI.SwingGUI(): Logo Image = " + iconImage);
		controlFrame.setIconImage(iconImage);
	}

	private boolean skipMoveWhileMouseWheeling = false;

	/// Simple JButton subclass to force getPreferredSize on many buttons.
	private static class BoxButton extends JButton {
		BoxButton() {
			super();
		}
		BoxButton(Icon img) {
			super(img);
		}
		@Override
		public Dimension getPreferredSize() {
			return SQUARE;
		}
	}

	void createGuiAndListeners() {

		viewFrame.addMouseWheelListener(evt -> {

			// WheelRotation is 1 for down, -1 for up
			// Skip is b/c we get two events for every move, awt bug?
			if (currentTab != null) {
				if (skipMoveWhileMouseWheeling) {
					if (Main.debug)
						System.out.println("Skip wheeling");
				} else {
                    moveToPage(currentTab.getPageNumber() + evt.getWheelRotation());
				}
				skipMoveWhileMouseWheeling = !skipMoveWhileMouseWheeling;
			}
		});

		// ControlFrame close -> exit
		controlFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				checkAndQuit();				
			}	
		});
		controlFrame.addKeyListener(handleKeys);

		programProps = new Properties();
		try (InputStream propwash = getClass().getResourceAsStream(PROPS_FILE_NAME)) {
			if (propwash == null) {
				throw new IllegalStateException("Unable to load " + PROPS_FILE_NAME);
			}
			programProps.load(propwash);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(controlFrame,
					"Failed to load Application Properties " + ex);
			System.exit(1);
		}
		logger.info("SwingGUI(): Properties " + programProps);

		tabPane.addChangeListener(e -> {
			currentTab = (DocTab)tabPane.getSelectedComponent();
			if (currentTab != null) { // Avoid NPE on removal
				updatePageNumbersDisplay();
			}
		});
		// If view gets resized, must re-calc scaling
		tabPane.addComponentListener(new ComponentAdapter() { // cannot lambdafy
			public void componentResized(ComponentEvent e) {
				int nt = tabPane.getTabCount();
				for (int i = 0; i < nt; i++) {
					Component tabComponent = tabPane.getComponent(nt);
					if (tabPane.getComponent(nt) instanceof DocTab) {
						DocTab dt = (DocTab) tabComponent;
						dt.computeScaling(dt.doc.getPage(0).getBBox(), (JComponent) tabComponent);
					} else {
						logger.finest(String.format("Tab %d is %s, not DocTab", i, tabComponent.getClass()));
					}
				}
			}
		});

		viewFrame.setContentPane(splitPane);

		JPanel sidePanel = new JPanel();
		sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
		sidePanel.setPreferredSize(new Dimension(150, CTRL_SIZE));

		JPanel navBox = makeNavBox();
		sidePanel.add(navBox);

		JPanel pageNumPanel = makePageCount();
		sidePanel.add(pageNumPanel);

		JPanel toolBox = makeToolbox();
		sidePanel.add(toolBox);

		JPanel searchPanel = new JPanel();
		searchTF = new JTextField(10);
		searchTF.addActionListener(e -> doSearch(searchTF.getText()));
		searchTF.setBorder(BorderFactory.createTitledBorder("Search"));
		searchPanel.add(searchTF);
		final JButton searchButton = (JButton)searchPanel.add(new BoxButton(getMyImageIcon("Search")));
		searchButton.addActionListener(e -> doSearch(searchTF.getText()));
		sidePanel.add(searchPanel);

		sidePanel.add(new ColorPanel(GObject::setLineColor));

        // SETTINGS
		sidePanel.add(new Settings(
				controlFrame,
				new SettingHandler("fontButton", SettingType.FONT, "Font", GObject.getFont(), GObject::setFont),
				new SettingHandler("lineWidthButton", SettingType.INTEGER, "Line Thickness", GObject.getLineThickness(), GObject::setLineThickness),
				new SettingHandler("slideDelayButton", SettingType.INTEGER, "Slide Show Interval", slideTime, this::setSlideTime),
				new SettingHandler("saveAnnoBox.label", SettingType.BOOLEAN, "", saveAnnos, this::setSaveAnnos),
				new SettingHandler("memoryBox.label", SettingType.BOOLEAN, "", savePageNumbers, this::setSavePageNumbers),
				new SettingHandler("jumpbackBox.label", SettingType.BOOLEAN, "", jumpBack, this::setJumpBack)
				));

		splitPane.add(sidePanel, JSplitPane.LEFT);
		splitPane.add(emptyViewScreenLabel, JSplitPane.RIGHT);

		controlFrame.setVisible(true);
		viewFrame.setVisible(true);
	}

	private void doSearch(String searchStr) {
		if (currentTab != null) {
			OptionalInt found = currentTab.doSearch(searchStr);
			if (found.isPresent()) {
				moveToPage(found.getAsInt());
			} else {
				JOptionPane.showMessageDialog(viewFrame, "Could not find '" + searchStr + "'");
			}
		}
	}

	/** Create a JMenuBar with all the menus. */
	private JMenuBar makeMenus() {

		JMenuBar menuBar = new JMenuBar();
		ResourceBundle rb = ResourceBundle.getBundle("Menus");
		JMenu fileMenu = MenuUtils.mkMenu(rb, "file");
		menuBar.add(fileMenu);
		JMenuItem miOpen = MenuUtils.mkMenuItem(rb, "file", "open");
		miOpen.setAccelerator(KeyStroke.getKeyStroke(
				Main.isMac ? "meta O" : "control O"));
		fileMenu.add(miOpen);
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
							JOptionPane.showMessageDialog(controlFrame,
                                    "Failed to open file: " + ex);
						}
					});
				} else {
					throw new IOException("Can't read file " + file);
				}
			}
		};
		miOpen.addActionListener(e -> {
			final File chosenFile = chooseFile();
			if (chosenFile == null) {
				return;
			}
			try {
				recents.openFile(chosenFile.getAbsolutePath());
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(controlFrame,
					String.format("Can't open file %s: %s", chosenFile, e1));
			}
		});
		fileMenu.add(recents);
		JMenuItem miClearRecents = MenuUtils.mkMenuItem(rb, "file", "clear_recents");
		miClearRecents.addActionListener(e -> recentsClear());
		fileMenu.add(miClearRecents);
		JMenuItem miSaveAnnos = MenuUtils.mkMenuItem(rb, "file", "save_annos");
		miSaveAnnos.addActionListener(e -> saveAnnotations(false));
		fileMenu.add(miSaveAnnos);
		JMenuItem miLoadAnnos = MenuUtils.mkMenuItem(rb, "file", "load_annos");
		miLoadAnnos.addActionListener(e -> loadAnnotations());
		fileMenu.add(miLoadAnnos);
		JMenuItem miClose = MenuUtils.mkMenuItem(rb, "file", "close");
		miClose.addActionListener(e -> {
			if (currentTab != null) {
				closeFile(currentTab);
			}
		});
		fileMenu.add(miClose);

		final JMenuItem infoButton = MenuUtils.mkMenuItem(rb, "file", "properties");
		infoButton.addActionListener(e -> showFileProps());
		fileMenu.add(infoButton);

		fileMenu.addSeparator();
		JMenuItem miPrint = MenuUtils.mkMenuItem(rb, "file", "print");
		miPrint.setAccelerator(KeyStroke.getKeyStroke(
				Main.isMac ? "meta P" : "control P"));
		miPrint.addActionListener(e ->
			JOptionPane.showMessageDialog(controlFrame, "Sorry, not implemented yet"));
		fileMenu.add(miPrint);

		fileMenu.addSeparator();
		JMenuItem miQuit = MenuUtils.mkMenuItem(rb, "file", "exit");
		miQuit.addActionListener(e -> checkAndQuit());
		fileMenu.add(miQuit);

		final JMenu editMenu = MenuUtils.mkMenu(rb, "edit");
		menuBar.add(editMenu);
		JMenuItem newPageMI = MenuUtils.mkMenuItem(rb, "edit","newpage");
		newPageMI.addActionListener(e -> {
			if (currentTab == null)
				return;
			currentTab.insertNewPage();
			if (saveAnnos) {
				saveAnnotations(true);
			}
		});
		editMenu.add(newPageMI);
		editMenu.addSeparator();
		JMenuItem deleteItemMI = MenuUtils.mkMenuItem(rb, "edit","delete");
		deleteItemMI.addActionListener(e -> {
			if (currentTab == null)
				return;
			currentTab.deleteSelected();
		});
		editMenu.add(deleteItemMI);

		JMenuItem miFind = MenuUtils.mkMenuItem(rb, "edit", "find");
		miFind.setAccelerator(KeyStroke.getKeyStroke(
				Main.isMac ? "meta F" : "control F"));
		miFind.addActionListener(e -> {
			String search = JOptionPane.showInputDialog("Find", searchTF.getText());
			if (search == null || search.isEmpty()) {
				return;
			}
			searchTF.setText(search);	// stash for re-use
			doSearch(search);
		});
		editMenu.add(miFind);

		final JMenu viewMenu = MenuUtils.mkMenu(rb, "view");
		menuBar.add(viewMenu);

		JMenuItem favoritesMI = MenuUtils.mkMenuItem(rb, "view","favorites");
		favoritesMI.setEnabled(false);
		viewMenu.add(favoritesMI);

		setupBreakTimer(rb, viewMenu);

		final JMenu slideshowMenu = MenuUtils.mkMenu(rb, MENU_SLIDESHOW);
		menuBar.add(slideshowMenu);
		final JMenuItem ssThisTabFromStartButton = MenuUtils.mkMenuItem(rb, MENU_SLIDESHOW, "thistab_from_start");
		slideshowMenu.add(ssThisTabFromStartButton);
		ssThisTabFromStartButton.addActionListener(e -> runSlideShow(1));
		final JMenuItem ssThisTabCurButton = MenuUtils.mkMenuItem(rb, MENU_SLIDESHOW, "thistab_from_current");
		slideshowMenu.add(ssThisTabCurButton);
		ssThisTabCurButton.addActionListener(e -> runSlideShow(currentTab.getPageNumber()));
		final JMenuItem ssAcrossTabsButton = MenuUtils.mkMenuItem(rb, MENU_SLIDESHOW, "across_tabs");
		ssAcrossTabsButton.addActionListener(slideshowAcrossTabsAction);
		slideshowMenu.add(ssAcrossTabsButton);
		final JMenuItem ssCustomButton = MenuUtils.mkMenuItem(rb, MENU_SLIDESHOW, "custom");
		ssCustomButton.addActionListener(e -> {
			if (currentTab == null) {
				JOptionPane.showMessageDialog(controlFrame, "Must be in an open tab",
						"Try again", JOptionPane.ERROR_MESSAGE);
				return;
			}
			JTextField firstSlide = new JTextField(10),
					lastSlide = new JTextField(5)
;			Object[] msg = {"Start (1):", firstSlide, "End:", lastSlide};

			var result = JOptionPane.showConfirmDialog(
					controlFrame,
					msg,
					"Custom Slide Show Pages",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);

			if (result == JOptionPane.YES_OPTION) {
				int start = firstSlide.getText().isEmpty() ? 1 : Integer.parseInt(firstSlide.getText());
				int end = Integer.parseInt(lastSlide.getText());
				System.out.printf("SlideShow from %s to %s".formatted(start, end));
				runSlideShow(start, end);
			} else {
				System.out.println("Custom Show Canceled");
			}
		});
		// XXX: a list of open files with a Pages textfield in each (1, 1-5, or "1,5,6")
		slideshowMenu.add(ssCustomButton);

		final JMenu helpMenu = MenuUtils.mkMenu(rb, "help");
		menuBar.add(helpMenu);
		final JMenuItem aboutButton = MenuUtils.mkMenuItem(rb, "help", "about");
		aboutButton.addActionListener(e->
				JOptionPane.showMessageDialog(controlFrame,
						String.format("PDFShow(tm) %s\n(c) 2021-2024 Ian Darwin\n%s\n",
								programProps.getProperty(KEY_VERSION),
								programProps.getProperty(KEY_HOME_URL)),
						"About PDFShow(tm)",
						JOptionPane.INFORMATION_MESSAGE));
		helpMenu.add(aboutButton);
		final JMenuItem helpButton = MenuUtils.mkMenuItem(rb, "help", "help");
		helpButton.addActionListener(e -> {
			String url = programProps.getProperty(KEY_HOME_URL);
			showWebPage(url);
		});
		helpMenu.add(helpButton);
		final JMenuItem breakTimerHelpButton = MenuUtils.mkMenuItem(rb, "help", "breaktimer");
		helpMenu.add(breakTimerHelpButton);
		breakTimerHelpButton.addActionListener(e -> breakTimer.doHelp());
		final JMenuItem sourceButton = MenuUtils.mkMenuItem(rb, "help", "source_code");
		sourceButton.setIcon(getMyImageIcon("octocat"));
		sourceButton.addActionListener(e -> {
			String url = programProps.getProperty(KEY_SOURCE_URL);
			showWebPage(url);
		});
		helpMenu.add(sourceButton);
		final JMenuItem feedbackMI = MenuUtils.mkMenuItem(rb, "help", "feedback");
		feedbackMI.addActionListener(feedbackAction);
		helpMenu.add(feedbackMI);

		return menuBar;
	}

	private void showWebPage(String url) {
		if (desktop == null) {
			JOptionPane.showMessageDialog(controlFrame,
					"Java Desktop unsupported, visit %s on your own.".formatted(url));
		} else {
			try {
				desktop.browse(new URI(url));
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(controlFrame,
						"Failed to open browser to " + url);
			}
		}
	}

	private void saveAnnotations(boolean silent) {
		if (currentTab == null)
			return;
		int count = 0;
		String fileName = ANNOTATIONS_SAVE_FILE + 
			currentTab.getName().replaceAll("[./]","_") + ANNOTATIONS_SAVE_EXT;
		try (ObjectOutputStream os =
					 new ObjectOutputStream(
							 Files.newOutputStream(Path.of(fileName)))) {
            for (List<GObject> gobjs : currentTab.addIns) {
                os.writeObject(gobjs);
				count += gobjs.size();
            }
			if (!silent) {
				JOptionPane.showMessageDialog(viewFrame,
						"Saved " + count + " annotations to " + fileName);
			}
        } catch (IOException ex){
			JOptionPane.showMessageDialog(viewFrame, "Write to " + fileName + " failed: " + ex);
		}
	}

	private void loadAnnotations() {
		if (currentTab == null)
			return;
		int count = 0;
		String fileName = ANNOTATIONS_SAVE_FILE + 
			currentTab.getName().replaceAll("[./]","_") + ANNOTATIONS_SAVE_EXT;
		try (ObjectInputStream ois =
					 new ObjectInputStream(
							 Files.newInputStream(Path.of(fileName)))) {
			for (List<GObject> gobjs : currentTab.addIns) {
				Object obj = ois.readObject();
				var list = (Collection<GObject>) obj;
				count += list.size();
				gobjs.addAll(list);
			}
			currentTab.repaint();
			JOptionPane.showMessageDialog(viewFrame,
					"Loaded " + count + " annotations from " + fileName);
		} catch (EOFException eof) {
			JOptionPane.showMessageDialog(viewFrame, "All done.");
		} catch (IOException ex){
			JOptionPane.showMessageDialog(viewFrame, "Load from " + fileName + " failed: " + ex);
		} catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

	//
	// BREAK TIMER STUFF
	//
	private void setupBreakTimer(ResourceBundle rb, JMenu viewMenu) {

		// First, find "any" (up to 9) background images for timer;
		// images can be in ~/.pdfshow/images or on classpath (e.g., in Jar file).
		List<Image> all = new ArrayList<>();
		var tilde = System.getProperty("user.home");
		var myDir = tilde + "/" + ".pdfshow";
		if (Files.isDirectory(Path.of(myDir))) {
			System.out.println("User's images dir = " + myDir);
			IntStream.rangeClosed(1, 9)
					.mapToObj(n ->
							myDir + "/images/break-background" + n + ".png")
					.filter(s -> Path.of(s).toFile().canRead())
					.map(path -> new ImageIcon(path).getImage())
					.forEach(all::add);
		}
		List<Image> res = new ArrayList<>();
		IntStream.rangeClosed(1,9)
				.mapToObj(n->"/images/break-background"+n+".png")
				.map(fn->getClass().getResource(fn))
				.filter(Objects::nonNull)
				.forEach(url -> {
                     try {
						Image image = ImageIO.read(url);
						res.add(image);
					} catch (IOException e) {
						System.out.println("Image didn't load!");
					}
				});
		all.addAll(res);
		System.out.println("Total BreakTimer Images = " + all.size());

		// Now some GUI stuff
		bTimerFrame = new JFrame("Timer");
		// Set size and location for when non-maximized
		bTimerFrame.setLocation(100, 100);
		bTimerFrame.setSize(new Dimension(CTRL_SIZE, CTRL_SIZE));
		// But start maximized anyway, as it's more impressive.
		bTimerFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		breakTimer = new BreakTimer(bTimerFrame, all);

		JMenuItem breakTimerMI = MenuUtils.mkMenuItem(rb, "view","break_timer");
		breakTimerMI.addActionListener(e ->  {
			bTimerFrame.setVisible(true);
		});
		viewMenu.add(breakTimerMI);
	}

	void recentsClear() {
		recents.clear();
		// XXX remove from preferences too!
	}

	/** Create the navigation box - arrows for pageup/down etc. */
	private JPanel makeNavBox() {

		JPanel navBox = new JPanel();
		navBox.setBorder(BorderFactory.createTitledBorder("Navigation"));
		navBox.setLayout(new GridLayout(0, 2));

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

		return navBox;
	}

	JPanel makePageCount() {
		JPanel pageNumbersPanel = new JPanel();
		pageNumbersPanel.setMaximumSize(new Dimension(300, 100));
		pageNumTF = new JTextField("001", JTextField.RIGHT);
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
		pageNumbersPanel.setLayout(new BoxLayout(pageNumbersPanel, BoxLayout.LINE_AXIS));
		pageNumbersPanel.add(new JLabel("Slide Number "));
		pageNumbersPanel.add(pageNumTF);
		pageNumbersPanel.add(new JLabel(" of "));
		pageNumbersPanel.add(pageCountTF);

		return pageNumbersPanel;
	}

	/** Create the drawing-tool toolBox */
	private JPanel makeToolbox() {

		logger.info("SwingGUI(): Building Toolbox");
		JPanel toolBox = new JPanel();
		toolBox.setBorder(BorderFactory.createTitledBorder("Toolbox"));
		toolBox.setLayout(new GridLayout(0, 2));

		// Mode buttons
		selectButton.addActionListener(e -> gotoState(viewState));
		toolBox.add(selectButton);

		textButton.addActionListener(e -> gotoState(textDrawState));
		textButton.setToolTipText("Add text object (t)");
		toolBox.add(textButton);

		markerButton.addActionListener(e -> gotoState(markingState));
		markerButton.setToolTipText("Add marker (m)");
		toolBox.add(markerButton);

		lineButton.addActionListener(e -> gotoState(lineDrawState));
		lineButton.setToolTipText("Add straight line (l)");
		toolBox.add(lineButton);

		polyLineButton.addActionListener(e -> gotoState(polyLineDrawState));
		polyLineButton.setToolTipText("Add a polyline (w)");
		toolBox.add(polyLineButton);

		ovalButton.addActionListener(e -> gotoState(ovalState));
		ovalButton.setToolTipText("Add oval (o)");
		toolBox.add(ovalButton);

		rectangleButton.addActionListener(e -> gotoState(rectangleState));
		rectangleButton.setToolTipText("Add rectangle (r)");
		toolBox.add(rectangleButton);

		smileButton.addActionListener(e-> gotoState(iconStateSmile));
		toolBox.add(smileButton);

		checkButton.addActionListener(e -> gotoState(iconStateCheck));
		toolBox.add(checkButton);

		nixButton.addActionListener(e -> gotoState(iconStateNix));
		toolBox.add(nixButton);

		// Other buttons
		final JButton clearButton = new JButton(getMyImageIcon("Trash"));
		clearButton.addActionListener(e -> currentTab.deleteAll());
		clearButton.setToolTipText("Delete ALL objects");
		toolBox.add(clearButton);

		final JButton undoButton = new JButton(getMyImageIcon("Undo"));
		// Have to give a name if give an icon, but we only want the icon.
		Action performUndo = new AbstractAction("",
				getMyImageIcon("Undo")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				currentTab.removeLastIn(); currentTab.repaint();
			}
		};
		undoButton.setAction(performUndo);
		undoButton.getActionMap().put("performUndo", performUndo);
		undoButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(
						Main.isMac ? "meta Z" : "control Z"),
				"performUndo");
		undoButton.setToolTipText("Undo last object");
		toolBox.add(undoButton);

		final JButton feedbackButton = new JButton(getMyImageIcon("Feedback"));
		feedbackButton.addActionListener(feedbackAction);
		feedbackButton.setToolTipText("Send feedback");
		toolBox.add(feedbackButton);

		final JButton stopButton = new JButton(getMyImageIcon("StopSign"));
		stopButton.addActionListener(e -> slideshowDone = true);
		stopButton.setToolTipText("Stop Slide Show");
		toolBox.add(stopButton);

		final JButton timerButton = new JButton(getMyImageIcon("Timer"));
		timerButton.addActionListener(e1 ->  {
			bTimerFrame.setVisible(true);
		});
		timerButton.setToolTipText("Open Break Timer");
		toolBox.add(timerButton);

		return toolBox;
	}

	/** Adjusts the slide show time interval */
	void setSlideTime(Object n) {
	    slideTime = (int) n;
    }

	/** Controls saving page numbers when closing file / exiting app */
	void setSavePageNumbers(Object b) {
		prefs.putBoolean(KEY_SAVE_PAGENUMS, (boolean)b);
		savePageNumbers = (boolean) b;
	}

	void setSaveAnnos(Object b) {
		prefs.putBoolean(KEY_SAVE_ANNOS, (boolean)b);
		saveAnnos = (boolean) b;
	}

	/** Controls whether we "jump back" to Select mode after each text or draw op */
	void setJumpBack(Object b) {
		prefs.putBoolean(KEY_JUMP_BACK, (boolean)b);
		jumpBack = (boolean) b;
	}

	/** Runs a show "across tabs" */
	ActionListener slideshowAcrossTabsAction = e -> {
        slideshowDone = false;
        final DocTab tab = currentTab;
        pool.submit(() -> {
            if (tab != currentTab) {
                // user is assuming control
                return;
            }
            int n = tabPane.getSelectedIndex();
            while (!slideshowDone) {
                try {
                    Thread.sleep(slideTime * 1000L);
                } catch (InterruptedException ex) {
                    slideshowDone = true;
                    return;
                }
                if (slideshowDone)
                	return;
                n = (n + 1) % tabPane.getTabCount();
                tabPane.setSelectedIndex(n);
            }
        });
    };

	/** Runs a show within the current tab, starting at page 'start' to the end, wrap around */
	void runSlideShow(int n) {
		runSlideShow(n, currentTab.getPageCount());

	}
	/** Runs a show within the current tab, from page 'start' to 'end', wrap around */
	void runSlideShow(int start, int end) {
        slideshowDone = false;
        pool.submit(() -> {
            int slideShowPageNumber = start;
            DocTab tab = currentTab;
			tab.gotoPage(slideShowPageNumber);
			while (!slideshowDone) {
                if (tab != currentTab) {
                    // User manually changed it, so "the show must go off"!
                	slideshowDone = true;
                    return;
                }
                try {
                    Thread.sleep(slideTime * 1000L);
                } catch (InterruptedException ex) {
                    slideshowDone = true;
                    return;
                }
				if (++slideShowPageNumber > end)
                	slideShowPageNumber = start;
                tab.gotoPage(slideShowPageNumber);
            }
        });
    }

    ActionListener feedbackAction = e -> {
		String[] choices = { "Web-Comment", "Bug Report/Feature Req", "Email Team", "Cancel" }; // XXX I18N this!
		int n = JOptionPane.showOptionDialog(controlFrame, "How to send feedback?", "Send Feedback",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
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
	final State iconStateSmile = new IconState(this, "Smile");
	final State iconStateCheck = new IconState(this,  "GreenCheck");
	final State iconStateNix = new IconState(this,  "RedX");

	// State Management

	static State currentState;

	static void gotoState(State state) {
		if (currentState != null) {	// At beginning of program
			currentState.leaveState();
		}
		currentState = state;
		currentState.enterState();
	}

	/** Keys that should work almost anywhere */
	KeyListener handleKeys = new KeyListener() {
		@Override
		public void keyTyped(KeyEvent e) {
			// Empty
		}

		@Override
		public void keyReleased(KeyEvent e) {
			// Empty
		}

		public void keyPressed(KeyEvent e) {
			System.out.println("keyPressed: " + e.getKeyChar());
			switch (e.getKeyChar()) {
				case 's', 'v':
					gotoState(viewState);
					break;
				case 't':
					gotoState(textDrawState);
					break;
				case 'm':
					gotoState(markingState);
					break;
				case 'l':
					gotoState(lineDrawState);
					break;
				case 'w':
					gotoState(polyLineDrawState);
					break;
				case 'o':
					gotoState(ovalState);
					break;
				case 'r':
					gotoState(rectangleState);
					break;
                default:
					if (Main.debug) {
						System.out.println("Unhandled key " + e.getKeyChar());
					}
			}
		}
	};

	void returnToViewState() {
		if (jumpBack)
			SwingGUI.gotoState(viewState);
	}

	// Everything Else

	void showFileProps() {
		final PDDocumentInformation docInfo = currentTab.doc.getDocumentInformation();
		var sb = new StringBuilder();
		sb.append("Document Properties:").append('\n');
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
		if (saveAnnos) {
			saveAnnotations(true);
		}
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
		logger.fine("SwingGUI.chooseFile(): prevDir = " + prevDir);
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
			logger.fine("SwingGUI.chooseFile(): put: " + selectedFile.getParent());
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
		if (splitPane.getComponent(2) instanceof JLabel) {
			splitPane.remove(emptyViewScreenLabel);
			splitPane.add(tabPane, JSplitPane.RIGHT);
		}
		barHelper.runWithProgressBar( () -> {
			DocTab t;
			try {
				t = new DocTab(file, prefs);
			} catch (IOException ex) {
				throw new RuntimeException("Failed to load " + file, ex);
			}
			t.setFocusable(true);
			t.addKeyListener(kl);
			t.addMouseListener(ml);
			t.addMouseMotionListener(mml);

			tabPane.addTab(file.getName(), currentTab = t);
			final int index = tabPane.getTabCount() - 1;
			tabPane.setSelectedIndex(index);
			ClosableTabHeader tabComponent = new ClosableTabHeader(this::closeFile, tabPane, t);
			tabPane.setTabComponentAt(index, tabComponent);
			int pageNum = savePageNumbers ?
					prefs.getInt("PAGE#" + file.getName(), -1)
					: 0;
			moveToPage(pageNum == -1 ? 0 : pageNum);
		}, () -> logger.fine("Done"));
	}

	void closeFile(DocTab dt) {
		tabPane.remove(dt);
		if (tabPane.getTabCount() == 0) {
			splitPane.remove(2); // The right hand panel
			splitPane.add(emptyViewScreenLabel, JSplitPane.RIGHT);
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
		currentTab.repaint();
	}

	void updatePageNumbersDisplay() {
		final int currentPageNumber = currentTab.getPageNumber();
		final int currentPageCount = currentTab.getPageCount();
		pageNumTF.setText(Integer.toString(currentPageNumber));
		pageCountTF.setText(Integer.toString(currentPageCount));
		upButton.setEnabled(currentPageNumber > 1);
		downButton.setEnabled(currentPageNumber < currentPageCount);
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

	public void setMonitorMode(MonitorMode monitorMode) {
		this.monitorMode = monitorMode;
	}

}
