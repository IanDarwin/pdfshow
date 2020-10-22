package net.rejmi.pdfshow;

import java.awt.*;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.darwinsys.swingui.FontChooser;

public class Settings extends JPanel {
	private static final long serialVersionUID = 1L;
	private Consumer<Font> setFont;
	private Consumer<Color> setColor;
	private Consumer<Integer> setLineThickness;

	public Settings(JFrame jf, 
			Consumer<Font> setFont,
			Consumer<Color> setColor,
			Consumer<Integer> setLineThickness) {

		// Callbacks
		this.setFont = setFont;
		this.setColor = setColor;
		this.setLineThickness = setLineThickness;

		setPreferredSize(new Dimension(130, 100));

		// GUI & Actions
		setLayout(new GridLayout(3, 1));
		JButton fontButton = new JButton("Font");
		fontButton.addActionListener(e -> {
			FontChooser fontChooser = new FontChooser(jf);
			fontChooser.setVisible(true);
			Font myNewFont = fontChooser.getSelectedFont();
			if (myNewFont != null) {
				setFont.accept(myNewFont);
			};
			fontChooser.dispose();
		});
		add(fontButton);
		JButton colorButton = new JButton("Color");
		colorButton.addActionListener(e -> {
			Color ch = JColorChooser.showDialog(
				jf,             // parent
				"Pick a Drawing Color",   // title
				getForeground());          // default
			if (ch != null) {
				setColor.accept(ch);
			}
		});
		add(colorButton);
		JButton linewidthButton = new JButton("Line");
		// XXX This could be much better - a slider with a live line preview
		linewidthButton.addActionListener(e -> {
			String ret = JOptionPane.showInputDialog("Line Width");
			if (ret == null)
				return;
			try {
				setLineThickness.accept(Integer.parseInt(ret.trim()));
			} catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(this, 
					String.format(
						"Could not interpret '%s' as a number, alas.", ret), 
					"Oops", 
					JOptionPane.ERROR_MESSAGE);
			}
		});
		add(linewidthButton);
	}

}
