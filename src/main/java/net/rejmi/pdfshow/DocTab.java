package net.rejmi.pdfshow;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/** A visual rep of one PDF document, for placing within a TabView */
@SuppressWarnings("serial")
class DocTab extends JPanel {
	private JScrollBar sbar;
	private JComponent pdfComponent;
	/** zero-origin pageNumber, not from document's page numbering */
	private int pageNumber = 0;
	/** Total size of this document */
	int pageCount = 0;
	/** The current PDF */
	PDDocument doc;
	/** The disk file we got it from */
	File file;
	/** The PdfBox renderer for it */
	PDFRenderer renderer;
	/** Our user's annotations for this doc, indexed by page# */
	List<GObject>[] addIns;
	
	DocTab(File file) throws IOException {
		super();

		// PDF stuff
		this.file = file;
		this.doc = PDDocument.load(file);
		pageCount = doc.getNumberOfPages();
		renderer = new PDFRenderer(doc);
		addIns = new List[pageCount];

		// GUI stuff
		setLayout(new BorderLayout());
		pdfComponent = new MainComponent();
		add(pdfComponent, BorderLayout.CENTER);
		sbar = new JScrollBar(JScrollBar.VERTICAL,1, 1, 0, pageCount - 1);
		sbar.addAdjustmentListener(e -> {
			if (e.getValueIsAdjusting())
				return;
			setPageNumber(e.getValue());
		});
		add(sbar, BorderLayout.EAST);
		// setSize(800, 800);
	}

	void setPageNumber(int page) {
		pageNumber = page;
		sbar.setValue(pageNumber);
		pdfComponent.repaint();
	}

	int getPageNumber() {
		return pageNumber;
	}
	
	void gotoNext() {
		if (pageNumber == pageCount)
			return;
		setPageNumber(pageNumber + 1);
	}
	void gotoPrev() {
		if (pageNumber == 0) {
			return;
		}
		setPageNumber(pageNumber - 1);
	}

	void addIn(GObject gobj) {
		if (addIns[pageNumber] == null) {
			addIns[pageNumber] = new ArrayList<GObject>();
		}
		addIns[pageNumber].add(gobj);
	}

	void delete(GObject gobj) {
		// not implemented yet
		repaint();
	}

	void deleteAll() {
		addIns[pageNumber] = null;
		repaint();
	}

	void close() {
		try {
			doc.close();
		} catch (IOException e) {
			e.printStackTrace();	// Nobody wants to listen to your chuntering.
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
			renderer.renderPageToGraphics(pageNumber, (Graphics2D) g);
			// 3) Our annotations, if any
			if (addIns[pageNumber] != null) {
				for (GObject obj : addIns[pageNumber]) {
					obj.render(g);
				}
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(PdfShow.jf, "Failure: " + e);
		}
	}
	}
}
