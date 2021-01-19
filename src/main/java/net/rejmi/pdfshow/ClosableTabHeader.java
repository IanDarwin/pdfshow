package net.rejmi.pdfshow;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** The header for the TabbedPane doctabs */
final class ClosableTabHeader extends JPanel {
	private static final long serialVersionUID = 1L;

	public ClosableTabHeader(Consumer<DocTab> tabCloser, DocTab docTab) {
		setLayout(new FlowLayout());
		add(new JLabel(docTab.file.getName()));
		// Leave comment out - tooltip eats mouse event so switching fails
		// See https://stackoverflow.com/questions/19910739 but applying that didn't work(?)
		// setToolTipText(docTab.file.getAbsolutePath());
		JButton xButton = new MyCloseButton();
		add(xButton);
		xButton.setPreferredSize(new Dimension(16,16));
		xButton.addActionListener(e -> tabCloser.accept(docTab));
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
