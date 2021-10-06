package net.rejmi.pdfshow;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * A visual rep of one PDF document, for placing within a TabView
 * Page numbers in methods are one-based, so subtract 1 for PDTree
 */
@SuppressWarnings("serial")
class DocTab extends JPanel {

	static Logger logger = Logger.getLogger("pdfshow.doctab");

	private JScrollBar sbar;
	private JComponent pdfComponent;
	/** one--based pageNumber, same as from document's page numbering */
	private int pageNumber = 1;
	/** The current PDF */
	PDDocument doc;
	/** The disk file we got it from */
	File file;
	/** The PdfBox renderer for it */
	PDFRenderer renderer;
	/** Scaling to make the document fit */
	float scaleX, scaleY;
	/** Our user's annotations for this doc, indexed by page#-1 to get list */
	List<List<GObject>> addIns;
	private PDPage deletedPage;
	private Preferences prefs;
	
	/** Construct one Document Tab */
	DocTab(File file, Preferences prefs) throws IOException {
		super();

		this.prefs = prefs;
		
		// PDF stuff
		this.file = file;
		// Start of "Should be done in a background thread"
		this.doc = PDDocument.load(file);

		renderer = new PDFRenderer(doc);
		addIns = new ArrayList<>(getPageCount());
		for (int i = 0; i < getPageCount(); i++) {
			addIns.add(new ArrayList<>());
		}
		// End of "Should be done in a background thread"

		// GUI stuff
		setDoubleBuffered(true);
		setLayout(new BorderLayout());
		pdfComponent = new MainComponent();
		add(pdfComponent, BorderLayout.CENTER);
		sbar = new JScrollBar(JScrollBar.VERTICAL, 1, 1, 1, getPageCount() + 1);
		sbar.addAdjustmentListener(e -> {
			if (e.getValueIsAdjusting())
				return;
			gotoPage(e.getValue());
		});
		add(sbar, BorderLayout.EAST);
	}

	/** Get the filename (without a care for the directory) */
	public String getName() {
		return file.getName();
	}

	/**
	 * Scale PDF to fit - after main window is fully sized.
	 * Scaling is quick & dirty: works for slide decks, not for books(!).
	 * XXX Must either constrain ratio or allow to manually adjust.
	 */
	public void computeScaling() {
		final PDRectangle pdfBBox = doc.getPage(0).getBBox();
		logger.fine("BBox: " + pdfBBox);
		final Dimension compSize = pdfComponent.getSize();
		logger.fine("Component size = " + compSize);
		scaleX = (float)(compSize.getWidth() / pdfBBox.getWidth());
		scaleY = (float)(compSize.getHeight() / pdfBBox.getHeight());
		logger.fine(String.format("Computed scaling %s as %f x %f",
				file.getName(), scaleX, scaleY));
	}

	void gotoPage(int page) {
		pageNumber = page;
		sbar.setValue(pageNumber);
		pdfComponent.repaint();
		PdfShow.instance.updatePageNumbersDisplay();
	}

	int getPageNumber() {
		return pageNumber;
	}
	
	/** @return Total size of this document */
	int getPageCount() {
		return doc.getNumberOfPages();
	}
	
	List<GObject> getCurrentAddIns() {
		return addIns.get(pageNumber - 1);
	}
	
	public File getFile() {
		return file;
	}

	void gotoNext() {
		if (pageNumber == getPageCount())
			return;
		gotoPage(pageNumber + 1);
	}

	void gotoPrev() {
		if (pageNumber == 1) {
			return;
		}
		gotoPage(pageNumber - 1);
	}

	/** Insert a new, blank page after the current page */
	void insertNewPage() {
		final PDPageTree pageTree = doc.getPages();
		PDPage curPage = pageTree.get(pageNumber - 1);
		PDPage newPage = new PDPage();
		pageTree.insertAfter(newPage, curPage);
		sbar.setMaximum(getPageCount() + 1);
		addIns.add(pageNumber, new ArrayList<GObject>());
		gotoNext();
	}
	
	/** Delete the given page
	 * @param index the one-origin page number
	 */
	void deletePage(int index) {
		final PDPageTree pageTree = doc.getPages();
		PDPage curPage = pageTree.get(pageNumber - 1);
		deletedPage = curPage;	// for undo
		pageTree.remove(curPage);
		sbar.setMaximum(getPageCount() + 1);
		gotoPrev();
	}

	int addIn(GObject gobj) {
		int ix = getCurrentAddIns().size();
		getCurrentAddIns().add(gobj);
		return ix;
	}

	/** Replace an object (for rubber-banding) */
	void setIn(int ix, GObject gobj) {
		if (getCurrentAddIns() == null)
			throw new IllegalStateException("setIn with no list!");
		getCurrentAddIns().set(ix, gobj);
	}

	void removeLastIn() {
		List<GObject> l = getCurrentAddIns();
		if (l != null && !l.isEmpty())
			l.remove(l.size() - 1);
	}

	void removeIn(int ix) {
		getCurrentAddIns().remove(ix);
	}

	void deleteAll() {
		getCurrentAddIns().clear();
		repaint();
	}

	void deleteSelected() {
		for (GObject obj : getCurrentAddIns()) {
			if (obj.isSelected) {
				getCurrentAddIns().remove(obj);
				repaint();
				break;
			}
		}
	}

	void close() {
		try {
			prefs.putInt("PAGE#" + getName(), getPageNumber());
			if (doc != null)
				doc.close();
			doc = null;
		} catch (IOException e) {
			System.err.printf("IOException %s, but: Nobody wants to listen to your chuntering.\n", e);
		}
	}

	class MainComponent extends JComponent {
		/**
		 * Draw the stuff on this page, in the correct 1-2-3 order
		 */
		@Override
		protected void paintComponent(Graphics g) {
			// 1) Super
			super.paintComponent(g);
			try {
				// 2) PdfBox - whole page
				if (scaleX == 0) {
					computeScaling();
				}
				renderer.renderPageToGraphics(pageNumber - 1, (Graphics2D) g, scaleX, scaleY);
				// 3) Our annotations, if any
				for (GObject obj : getCurrentAddIns()) {
					obj.draw(g);
				}
			} catch (IOException e) {
				JOptionPane.showMessageDialog(PdfShow.frame, "Failure: " + e);
			}
		}
	}
}
