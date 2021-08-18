package net.rejmi.pdfshow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/** The header-providing wrapper for the TabbedPane doctabs */
final class ClosableTabHeader extends JPanel {
	private static final long serialVersionUID = 1L;

	public ClosableTabHeader(Consumer<DocTab> tabCloser, final JTabbedPane panel, DocTab docTab) {
		setLayout(new FlowLayout());
        JLabel label = new JLabel(docTab.file.getName());
        add(label);
		JButton xButton = new MyCloseButton();
		add(xButton);
		xButton.setPreferredSize(new Dimension(16,16));
		xButton.addActionListener(e -> tabCloser.accept(docTab));
	}

	// The X button for tabs
    private static class MyCloseButton extends JButton {
        private static final long serialVersionUID = 1L;

        public MyCloseButton() {
            super("X");
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
    }
}
