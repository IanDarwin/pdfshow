package net.rejmi.pdfshow;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/** A visual rep of one PDF document, for placing within a TabView */
@SuppressWarnings("serial")
class DocTab extends JComponent {
	/** zero-origin pageNumber, not from document's page numbering */
	int pageNumber = 0;
	/** Total size of this document */
	int pageCount = 0;
	PDDocument doc;
	PDFRenderer renderer;
	List<GObject>[] addIns;
	
	DocTab(PDDocument document) {
		super();
		this.doc = document;
		pageCount = doc.getNumberOfPages();
		renderer = new PDFRenderer(doc);
		addIns = new List[pageCount];
		addIn(new GText(50, 50, "Hello World of Kludgery"));
		addIn(new GLine(100, 100, 400, 400));
		setSize(800, 800);
	}

	void addIn(GObject gobj) {
		if (addIns[pageNumber] == null) {
			addIns[pageNumber] = new ArrayList<GObject>();
		}
		addIns[pageNumber].add(gobj);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		try {
			renderer.renderPageToGraphics(pageNumber, (Graphics2D) g);
			if (addIns[pageNumber] != null) {
				for (GObject obj : addIns[pageNumber]) {
					obj.render(g);
				}
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Failure: " + e);
		}
	}
}
