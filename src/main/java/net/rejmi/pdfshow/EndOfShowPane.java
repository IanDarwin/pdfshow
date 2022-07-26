package net.rejmi.pdfshow;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class EndOfShowPane extends JPanel {

	private String message;
	private JButton closeButton;

	EndOfShowPane() {
		this("You have reached the end of the show.\n" +
			"You can either go back, or close this tab.");
	}

	EndOfShowPane(String message) {
		this.message = message;
		setLayout(new GridLayout(2, 1));
		add(new JLabel(message));
		add(closeButton = new JButton("Close"));
		closeButton.addActionListener(e -> {
			JOptionPane.showMessageDialog(EndOfShowPane.this, "Not implemented, sorry.");
		});
	}
}
