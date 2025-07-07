package net.rejmi.pdfshow;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;

/** 
 * GUI State class hierarchy - can control interactions differently in each state.
 * State is a class, not an interface, so it can have state and so subclasses don't
 * have to implement every method (though the latter could now be done with 
 * interface default methods).
 */
abstract class State {
	final SwingGUI parent;
	final JButton button;
	final Border active = BorderFactory.createLineBorder(Color.BLUE, 3);

	public State(SwingGUI parent, JButton button) {
		this.parent = parent;
		this.button = button;
	}

	/** Anything to be done on entering a given state */
	public void enterState() {
		SwingGUI.logger.fine(String.format("enterState of %s, button is %s", getClass(), button));
		if (button != null)
			button.setBorder(active);
	}

	public void leaveState() {
		SwingGUI.logger.fine("leaveState of " + getClass());
		if (button != null)
			button.setBorder(null);
	}

	public void keyPressed(KeyEvent e) {
		SwingGUI.logger.fine("SwingGUI.State.keyPressed(" + e + ")");
		switch(e.getKeyChar()) {
		case 'j':
		case '\r':
		case '\n':
		case ' ':
			parent.currentTab.gotoNext();
			return;
		case 'k':
		case '\b':
			parent.currentTab.gotoPrev();
			return;

		// OK, not keychar, try keyCode
		default:
			switch(e.getKeyCode()) {
				case KeyEvent.VK_DOWN:
					parent.currentTab.gotoNext(); return;
				case KeyEvent.VK_UP:
					parent.currentTab.gotoPrev(); return;
				case KeyEvent.VK_DELETE:
					parent.currentTab.deleteSelected();
					return;
			}
			if (e.getKeyCode() == 'W') {
				if (e.isControlDown() || e.isMetaDown()) {
					parent.closeFile(parent.currentTab);
					return;
				}
			}
		}
		// Delegate
		SwingGUI.instance.handleKeys.keyPressed(e);
	}

	public void mouseClicked(MouseEvent e) {
		// most will probably want to override this
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
			SwingGUI.logger.fine("No annotations");
			return;
		}
		currentPageAddIns.forEach(gobj -> consumer.accept(gobj));
	}
}


/** State for normal viewing */
class ViewState extends State {
	boolean changed = false, found = false;

	// Default State
	ViewState(SwingGUI parent, JButton button) {
		super(parent, button);
	}

	// Select an object
	@Override
	public void mouseClicked(MouseEvent e) {
		int mx = e.getX(), my = e.getY();
		SwingGUI.logger.info(String.format(
				"SwingGUI.ViewState.mouseClicked() x %d y %d", mx, my));
		changed = found = false;

		if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
			System.out.println("double clicked");
			visitCurrentPageGObjs(gobj -> {
				if (gobj.isSelected && gobj instanceof GText) {
					System.out.println("Editable Text gobj = " + ((GText) gobj).text);
					String newText = JOptionPane.showInputDialog("Edit Text", ((GText) gobj).text);
					if (newText == null || newText.isEmpty()) {
						return;
					}
					((GText) gobj).text = newText;
				}
			});
			parent.currentTab.repaint();
			return;
		}
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
				SwingGUI.logger.fine("HIT: " + gobj);
				gobj.isSelected = true;
				changed = true;
				found = true;
			} else {
				SwingGUI.logger.fine("MISS: " + gobj);
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
	TextDrawState(SwingGUI parent, JButton button) {
		super(parent, button);
	}
	boolean dialogClosed = false;
	@Override
	public void mousePressed(MouseEvent e) {
		// Not sure why this isn't just JOptionPane.showInputDialog()
		JOptionPane pane = new JOptionPane("Text?",
			JOptionPane.QUESTION_MESSAGE,
			JOptionPane.DEFAULT_OPTION);
		pane.setWantsInput(true);
		JDialog dialog = pane.createDialog(SwingGUI.viewFrame, "Text?");
		dialog.setLocation(e.getX(), e.getY());
		dialogClosed = false;
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dialogClosed = true;
			}
		});
		dialog.setVisible(true); // BLOCKING WAIT
		if (dialogClosed) {
			parent.returnToViewState();
			return;
		}
		String text = pane.getInputValue().toString();
		if ("uninitializedValue".equals(text)) {
			parent.returnToViewState();
			return;
		}
		if (text == null || text.isEmpty()) {
			parent.returnToViewState();
			return;
		}
		parent.currentTab.addIn(new GText(e.getX(), e.getY(), text));
		parent.currentTab.repaint();
		parent.returnToViewState();
	}
}

/** Marker: straight line: click start, click end. */
class MarkingState extends State {
	
	MarkingState(SwingGUI parent, JButton button) {
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

	LineDrawState(SwingGUI parent, JButton button) {
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
		parent.returnToViewState();
	}
}

/** A simple multi-straight-line poly-point line. No Bezier &c.
 * Unlike most of the other GObject types, the points in a
 * polyline are RELATIVE and get absolutized in the GPolyLine
 * drawing code.
 */
class PolyLineDrawState extends State {
	
	PolyLineDrawState(SwingGUI parent, JButton button) {
		super(parent, button);
	}
	
	int n = 0, ix;
	int lastx, lasty;
	GPolyLine line;
	@Override
	public void mousePressed(MouseEvent e) {
		SwingGUI.logger.fine("SwingGUI.PolyLineDrawState.mousePressed()");
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
		SwingGUI.logger.fine("SwingGUI.PolyLineDrawState.mouseReleased()");
		parent.currentTab.repaint();
		line = null;	// We are done with it.
		parent.returnToViewState();
	}
}

class RectangleState extends State {
	
	RectangleState(SwingGUI parent, JButton button) {
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
		parent.returnToViewState();
	}
	
}

class OvalState extends State {
	
	OvalState(SwingGUI parent, JButton button) {
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
		parent.returnToViewState();
	}
}

class IconState extends State {
	String iconName;
	GIcon icon;
	IconState(SwingGUI parent, String iconName) {
        super(parent, null);
        this.iconName = iconName;
	}

	public void mousePressed(MouseEvent e) {
        icon= new GIcon(e.getX(), e.getY(), iconName);
		parent.currentTab.addIn(icon);
		parent.currentTab.repaint();
		parent.returnToViewState();
	}
}