package net.rejmi.pdfshow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
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
import java.net.URL;
import java.util.ResourceBundle;

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

/** A simpler PDF viewer
 * @author Ian Darwin
 */
public class PdfShow {

	public static void main(String[] args) throws Exception {
		final PdfShow pdfShow = new PdfShow();
		for (String arg : args) {
			final File file = new File(arg);
			if (!file.canRead()) {
				JOptionPane.showMessageDialog(PdfShow.jf, "Can't open file " + file);
				continue;
			}
			pdfShow.openPdfFile(file);
		}
	}


	/** 
	 * State is a class, not an interface, so subclasses don't
	 * have to implement every method.
	 */
	private static abstract class State {

		/** Anything to be done on entering a given state */
		public void enterState() {
			//
		}
		
		/** Called by any State when it is done, e.g., after
		 * composing and adding a GObject, currentTab.repaint(), done()
		 */
		public void done() {
			gotoState(viewState);
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
	static class ViewState extends State {
		@Override
		public void keyTyped(KeyEvent e) {
			// XXX why does this not get activated?
			System.out.println("PdfShow.ViewState.keyTyped() " + e.getKeyCode());
		}
	}
	static final ViewState viewState = new ViewState();

	/** State for adding text annotations */
	static class TextDrawState extends State {
		@Override
		public void mousePressed(MouseEvent e) {
			System.out.println("PdfShow.TextDrawState.mousePressed()");
			String text = JOptionPane.showInputDialog("Text?");
			if (text != null) {
				currentTab.addIn(new GText(e.getX(), e.getY(), text));
				done();
			}
		}
	}
	static final State textDrawState = new TextDrawState();

	/** For now, crude line-drawing: click start, click end. */
	static class LineDrawState extends State {
		int startX = -1, startY = -1;
		@Override
		public void mousePressed(MouseEvent e) {
			startX = e.getX();
			startY = e.getY();
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			currentTab.addIn(
					new GLine(startX, startY, e.getX(), e.getY()));
			currentTab.repaint();
			done();
		}
	}
	static final State lineDrawState = new LineDrawState();

	static class PolyLineDrawState extends State {
		enum Mode {IDLE, DRAWING };
		Mode mode = Mode.IDLE;
		// Static arrays to avoid multiple allocations
		static int[] x = new int[750];
		static int[] y = new int[750];
		int n = 0;
		@Override
		public void mousePressed(MouseEvent e) {
			System.out.println("PdfShow.PolyLineDrawState.mouseClicked()");
			n = 0;
			mode = Mode.DRAWING;
			addPoint(e.getX(), e.getY());
		}
		private void addPoint(int x, int y) {
			PolyLineDrawState.x[n] = x;
			PolyLineDrawState.y[n] = y;
			++n;
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			if (n >= x.length) {
				return;
			}
			int lastx = x[n-1]; int lasty = y[n-1];
			int newx = e.getX(); int newy = e.getY();
			int dx = newx - lastx;
			if (dx > -5 && dx < +5)
				return;
			int dy = newy - lasty;
			if (dy > -1 && dy < +5)
				return;
			addPoint(newx, newy);
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			System.out.println("PdfShow.PolyLineDrawState.mouseReleased()");
			int[] xPoints = new int[n];
			System.arraycopy(x, 0, xPoints, 0, n);
			int[] yPoints = new int[n];
			System.arraycopy(y, 0, yPoints, 0, n);
			currentTab.addIn(
				new GPolyLine(xPoints, yPoints));
			currentTab.repaint();
			done();
		}
	}
	static final State polyLineDrawState = new PolyLineDrawState();
	
	static class RectangleState extends State {
		int ulX = -1, ulY = -1;
		@Override
		public void mousePressed(MouseEvent e) {
			ulX = e.getX();
			ulY = e.getY();
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			currentTab.addIn(new GRectangle(ulX, ulY, e.getX(), e.getY()));
			currentTab.repaint(); // XXX Shoudl addIn() do repaint() for us?
			done();
		}
		
	}
	static final State rectangleState = new RectangleState();

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
		JOptionPane.showMessageDialog(jf, sb.toString(), 
			currentTab.file.getName(), JOptionPane.INFORMATION_MESSAGE);
	}

	static JFrame jf;
	private static JTabbedPane tabPane;
	private static DocTab currentTab;
	private static JButton upButton, downButton;
	private static JTextField pageNumTF;
	final RecentMenu recents;

	// Listeners; these get added to each DocTab in openPdfFile().
	private MouseListener ml;
	private MouseMotionListener mml;
	private KeyListener kl;

	PdfShow() {

		gotoState(viewState);

		// GUI SETUP

		jf = new JFrame("PDFShow");
		Toolkit tk = Toolkit.getDefaultToolkit();
		jf.setSize(tk.getScreenSize());
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// TABBEDPANE (main window for viewing PDFs)

		tabPane = new JTabbedPane();
		tabPane.addChangeListener(evt -> {
			currentTab = (DocTab)tabPane.getSelectedComponent();
			// This shouldn't be needed...
			if (currentTab != null)
				pageNumTF.setText(Integer.toString(currentTab.pageNumber));
		});
		jf.add(BorderLayout.CENTER, tabPane);
		// MENUS

		JMenuBar menuBar = new JMenuBar();
		jf.setJMenuBar(menuBar);
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
				JOptionPane.showMessageDialog(jf, "Can't open file: " + e1);
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
			JOptionPane.showMessageDialog(jf, "PdfShow v0.0\n" +
			"c 2020 Ian Darwin\n" +
			"https://darwinsys.com/freeware\n" +
			"Icons from the Sun JLF Image Repository (c) Sun Micro.\n"+
			"Other icons Copyright(C) 1998  by  Dean S. Jones\n" +
			"dean@gallant.com www.gallant.com/icons.htm",
			"About PdfShow(tm)",
			JOptionPane.INFORMATION_MESSAGE));
		helpMenu.add(aboutButton);
		final JMenuItem helpButton = MenuUtils.mkMenuItem(rb, "help", "help");
		helpButton.addActionListener(e->
	    	JOptionPane.showMessageDialog(jf, "Help not written yet, sorry!"));
		helpMenu.add(helpButton);

		// NAV BOX

		JPanel sidePanel = new JPanel();
		sidePanel.setBackground(Color.cyan);
		sidePanel.setPreferredSize(new Dimension(200, 800));
		
		JPanel navBox = new JPanel();
		navBox.setLayout(new GridLayout(3,3));
		upButton = new JButton(getJLFImageIcon("navigation/Up"));
		upButton.addActionListener(e -> moveToPage(currentTab.pageNumber - 1));
		navBox.add(new JLabel()); navBox.add(upButton); navBox.add(new JLabel());
		downButton = new JButton(getJLFImageIcon("navigation/Down"));
		downButton.addActionListener(e -> moveToPage(currentTab.pageNumber + 1));
		JButton firstButton = new JButton(getJLFImageIcon("media/Rewind")), 
			lastButton = new JButton(getJLFImageIcon("media/FastForward"));
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
		pageNumTF.addActionListener(e -> moveToPage(Integer.parseInt(pageNumTF.getText())));
		navBox.add(pageNumTF);
		lastButton.addActionListener(e -> moveToPage(Integer.MAX_VALUE));
		navBox.add(lastButton);
		navBox.add(new JLabel()); navBox.add(downButton); navBox.add(new JLabel());
		navBox.setPreferredSize(new Dimension(200, 200));
		sidePanel.add(navBox);

		// TOOL BOX

		JPanel toolBox = new JPanel();
		toolBox.setLayout(new BoxLayout(toolBox, BoxLayout.PAGE_AXIS));
		// Mode buttons
		toolBox.add(new JButton("...")); // Needed??
		
		final JButton textButton = new JButton(getMyImageIcon("Text"));
		textButton.addActionListener(e -> gotoState(textDrawState));
		toolBox.add(textButton);

		final JButton lineButton = new JButton(getMyImageIcon("Line"));
		lineButton.addActionListener(e -> gotoState(lineDrawState));
		toolBox.add(lineButton);
		
		final JButton polyLineButton = new JButton(getMyImageIcon("PolyLine"));
		polyLineButton.addActionListener(e -> gotoState(polyLineDrawState));
		toolBox.add(polyLineButton);
		
		final JButton rectangleButton = new JButton(getMyImageIcon("Rectangle"));
		rectangleButton.addActionListener(e -> gotoState(rectangleState));
		toolBox.add(rectangleButton);
		
		final JButton clearButton = new JButton(getJLFImageIcon("general/Delete"));
		clearButton.addActionListener(e -> currentTab.deleteAll());
		toolBox.add(clearButton);
		
		final JButton undoButton = new JButton(getJLFImageIcon("general/Undo"));
		undoButton.setEnabled(false);
		toolBox.add(undoButton);
		
		sidePanel.add(toolBox);

		// GENERIC VIEW LISTENERS - Just delegate directly to currentState
		ml = new MouseAdapter() {
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
			}
			@Override
			public void mouseExited(MouseEvent e) {
				currentState.mouseExited(e);
			};
		};

		mml = new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				currentState.mouseDragged(e);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				// Ignore
			}		
		};

		kl = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				System.out.println("PdfShow.main(...).new KeyAdapter() {...}.keyPressed()");
			};
			@Override
			public void keyTyped(KeyEvent e) {
				System.out.println("PdfShow.main(...).new KeyAdapter() {...}.keyTyped()");
				currentState.keyTyped(e);
			};
		};

		jf.add(BorderLayout.WEST, sidePanel);
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

	private void openPdfFile(File file) throws IOException {
		DocTab t = new DocTab(file);
		t.addKeyListener(kl);
		t.addMouseListener(ml);
		t.addMouseMotionListener(mml);

		tabPane.addTab(file.getName(), currentTab = t);
		tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
		moveToPage(0);
	}

	private static void closeFile(DocTab dt) {
		dt.close();
		tabPane.remove(dt);
	}

	private static void moveToPage(int newPage) {
		int docPages = currentTab.pageCount;
		if (newPage < 0) {
			newPage = 0;
		}
		if (newPage >= docPages) {
			newPage = docPages - 1;
		}
		if (newPage == currentTab.pageNumber) {
			return;
		}
		pageNumTF.setText(Integer.toString(newPage) +  " of " + currentTab.pageCount);
		currentTab.pageNumber = newPage;
		upButton.setEnabled(currentTab.pageNumber > 0);
		downButton.setEnabled(currentTab.pageNumber < docPages);
		currentTab.repaint();
	}
	
	// Graphics helpers
	
	/** Convenience routine to get an application-local image */
	private ImageIcon getMyImageIcon(String name) {
		String fullName = "/images" + '/' + name + ".gif";
		return getImageIcon(fullName);
	}
	
	/** Convenience routine to get a JLF-standard image */
	private ImageIcon getJLFImageIcon(String name) {
		String imgLocation = "/toolbarButtonGraphics/" + name + "24.gif";
		return getImageIcon(imgLocation);
	}

	private ImageIcon getImageIcon(String imgName) {
		URL imageURL = getClass().getResource(imgName);

		if (imageURL == null) {
			throw new IllegalArgumentException("No image: " + imgName);
		}
		ImageIcon ii = new ImageIcon(imageURL);
		return ii;
	}

}
