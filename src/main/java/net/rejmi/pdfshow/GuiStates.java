package net.rejmi.pdfshow;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** 
 * GUI State class hierarchy - can control interactions differently in each state.
 * State is a class, not an interface, so it can have fields and so subclasses don't
 * have to implement every method (though the latter could now be done with 
 * interface default methods).
 */
abstract class State {
	final PdfShow parent;
	final JButton button;
	final Border active = BorderFactory.createLineBorder(Color.BLUE, 3);
	final Logger logger = PdfShow.logger;

	public State(PdfShow parent, JButton button) {
		this.parent = parent;
		this.button = button;
	}

	/** Anything to be done on entering a given state */
	public void enterState() {
		logger.fine(String.format("enterState of %s, button is %s", getClass(), button));
		if (button != null)
			button.setBorder(active);
	}

	public void leaveState() {
		logger.fine("leaveState of " + getClass());
		if (button != null)
			button.setBorder(null);
	}

	public void keyPressed(KeyEvent e) {
		logger.fine("PdfShow.State.keyPressed(" + e + ")");
		switch(e.getKeyChar()) {
		case 'j':
		case '\r':
		case '\n':
		case ' ':
		case KeyEvent.VK_UP:
			parent.currentTab.gotoNext();
			return;
		case 'k':
		case '\b':
		case KeyEvent.VK_DOWN:
			parent.currentTab.gotoPrev();
			return;
		case KeyEvent.VK_DELETE:
			parent.currentTab.deleteSelected();
			return;
		
		default:
			switch(e.getKeyCode()) {
			case KeyEvent.VK_DOWN:
				parent.currentTab.gotoNext(); return;
			case KeyEvent.VK_UP:
				parent.currentTab.gotoPrev(); return;
			}
			if (e.getKeyCode() == 'W') {
				if (e.isControlDown() || e.isMetaDown()) {
					parent.closeFile(parent.currentTab);
					return;
				}
			}
				
		}
		logger.warning("Unhandled key event: " + e);
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

	void visitCurrentPageGObjs(Consumer<GObject> consumer) {
		if (parent.currentTab == null) {
			return;	// Try to draw before opening a file?
		}
		final List<GObject> currentPageAddIns = parent.currentTab.getCurrentAddIns();
		if (currentPageAddIns.isEmpty()) {
			logger.fine("No annotations");
			return;
		}
		currentPageAddIns.forEach(gobj -> consumer.accept(gobj));
	}
}


/** State for normal viewing */
class ViewState extends State {
	boolean changed = false, found = false;

	// Default State
	ViewState(PdfShow parent, JButton button) {
		super(parent, button);
	}

	// Select an object
	@Override
	public void mouseClicked(MouseEvent e) {
		int mx = e.getX(), my = e.getY();
		logger.info(String.format(
				"PdfShow.ViewState.mouseClicked() x %d y %d", mx, my));
		changed = found = false;
		// Avoid old selection
		visitCurrentPageGObjs(gobj -> gobj.isSelected = false);

		visitCurrentPageGObjs(gobj -> {
			if (found) {
				return;	// Only select one
			}
			if (gobj.isSelected) {
				gobj.isSelected = false;
				changed = true;
			}
			if (gobj.contains(mx, my)) {
				logger.fine("HIT: " + gobj);
				gobj.isSelected = true;
				changed = true;
				found = true;
			} else {
				logger.fine("MISS: " + gobj);
			}
		});
		if (changed) {
			parent.currentTab.repaint();
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
				parent.currentTab.repaint(); // XXX expensive during drag?
			}
		});
	}
	
	@Override
	public void leaveState() {
		super.leaveState();
		visitCurrentPageGObjs(gobj->{
			if (gobj.isSelected) {
				gobj.isSelected = false;
				changed = true;
			}	
			if (changed) {
				parent.currentTab.repaint();
			}
		});
	}
}

/** State for adding text annotations */
class TextDrawState extends State {
	TextDrawState(PdfShow parent, JButton button) {
		super(parent, button);
	}
	boolean dialogClosed = false;
	@Override
	public void mousePressed(MouseEvent e) {
		JOptionPane pane = new JOptionPane("Text?",
			JOptionPane.QUESTION_MESSAGE,
			JOptionPane.DEFAULT_OPTION);
		pane.setWantsInput(true);
		JDialog dialog = pane.createDialog(PdfShow.viewFrame, "Text?");
		dialog.setLocation(e.getX(), e.getY());
		dialogClosed = false;
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dialogClosed = true;
			}
		});
		dialog.setVisible(true); // BLOCKING WAIT
		if (dialogClosed)
			return;
		String text = pane.getInputValue().toString();
		if (text == null)
			return;
		parent.currentTab.addIn(new GText(e.getX(), e.getY(), text));
		parent.currentTab.repaint();
	}
}

/** Marker: straight line: click start, click end. */
class MarkingState extends State {
	
	MarkingState(PdfShow parent, JButton button) {
		super(parent, button);
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
			ix = parent.currentTab.addIn(mark);
		} else {
			parent.currentTab.setIn(ix, 
				new GMarker(startX, startY, e.getX() - startX, e.getY() - startY));
		}
		parent.currentTab.repaint();
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		mark = null;
	}
}

/** For now, crude line-drawing: click start, drag to end. */
class LineDrawState extends State {

	LineDrawState(PdfShow parent, JButton button) {
		super(parent, button);
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
			ix = parent.currentTab.addIn(line);
		} else {
			parent.currentTab.setIn(ix, new GLine(startX, startY, e.getX() - startX, e.getY() - startY));
		}
		parent.currentTab.repaint();
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		line = null;
	}
}

/** A simple multi-straight-line poly-point line. No Bezier &c.
 * Unlike most of the other GObject types, the points in a
 * polyline are RELATIVE and get absolutized in the GPolyLine
 * drawing code.
 */
class PolyLineDrawState extends State {
	
	PolyLineDrawState(PdfShow parent, JButton button) {
		super(parent, button);
	}
	
	int n = 0, ix;
	int lastx, lasty;
	GPolyLine line;
	@Override
	public void mousePressed(MouseEvent e) {
		logger.fine("PdfShow.PolyLineDrawState.mousePressed()");
		n = 0;
		line = new GPolyLine(lastx = e.getX(), lasty = e.getY());
		ix = parent.currentTab.addIn(line);
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
		parent.currentTab.repaint();
		lastx = newx; lasty = newy;
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		logger.fine("PdfShow.PolyLineDrawState.mouseReleased()");
		parent.currentTab.repaint();
		line = null;	// We are done with it.
	}
}

class RectangleState extends State {
	
	RectangleState(PdfShow parent, JButton button) {
		super(parent, button);
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
			ix = parent.currentTab.addIn(rect);
		} else {
			parent.currentTab.setIn(ix, new GRectangle(ulX, ulY, e.getX() - ulX, e.getY() - ulY));
		}
		parent.currentTab.repaint();
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		// parent.currentTab.addIn(new GRectangle(ulX, ulY, e.getX(), e.getY()));
		parent.currentTab.repaint(); // XXX Should addIn() do repaint() for us?
	}
	
}

class OvalState extends State {
	
	OvalState(PdfShow parent, JButton button) {
		super(parent, button);
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
			ix = parent.currentTab.addIn(oval);
		} else {
			parent.currentTab.setIn(ix, new GOval(ulX, ulY, x - ulX, y - ulY));
		}
		parent.currentTab.repaint();
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		parent.currentTab.repaint(); // XXX Should addIn() do repaint() for us?
	}
}
