package net.rejmi.pdfshow;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.Serial;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.darwinsys.swingui.FontChooser;
import com.darwinsys.swingui.I18N;

enum SettingType { STRING, INTEGER, BOOLEAN, FONT, COLOR }

record SettingHandler(String buttonName,
					  SettingType type,
					  Object value,
					  Consumer<Object> callback){}

/**
 * The Settings panel 
 */
public class Settings extends JPanel {
	@Serial
	private static final long serialVersionUID = 1L;
	Color curColor;

	/**
	 * Create the GUI for the Settings pane
	 * @param jf The JFrame ready to receive the items
	 * @param handlers Array of SettingHandlers
	 */
	Settings(JFrame jf, SettingHandler... handlers) {

		setBorder(BorderFactory.createTitledBorder("Settings"));
		ResourceBundle rb = ResourceBundle.getBundle("Menus");
		setLayout(new GridLayout(0, 1));

		for (SettingHandler handler : handlers) {
			if (handler instanceof SettingHandler(
					String name, SettingType type,
					Object value, Consumer<Object> mHandler)) {
				switch (type) {
					case STRING:
						throw new UnsupportedOperationException("Not written yet");
					case INTEGER:
						JButton lineWidthButton = I18N.mkButton(rb, name);
						// XXX This could be done better - a slider with a live preview?
						lineWidthButton.addActionListener(_ -> {
							String ret = JOptionPane.showInputDialog("Line Thickness");
							if (ret == null)
								return;
							try {
								 mHandler.accept(Integer.decode(ret.trim()));
							} catch (NumberFormatException nfe) {
								JOptionPane.showMessageDialog(this,
										String.format(
												"Could not interpret '%s' as a number, alas.", ret),
										"Oops",
										JOptionPane.ERROR_MESSAGE);
							}
						});
						add(lineWidthButton);
						break;
					case BOOLEAN:
						final JCheckBox memoryBox = new JCheckBox(I18N.getString(rb, name, "Boolean"));
						memoryBox.setSelected((Boolean)value);
						memoryBox.addItemListener(_ -> mHandler.accept(memoryBox.isSelected()));
						add(memoryBox);
						break;
					case FONT:
						JButton fontButton = I18N.mkButton(rb, name);
						fontButton.addActionListener(_ -> {
							FontChooser fontChooser = new FontChooser(jf);
							fontChooser.setSelectedFont((Font) handler.value());
							fontChooser.setVisible(true);
							Font myNewFont = fontChooser.getSelectedFont();
							if (myNewFont != null) {
								 mHandler.accept(myNewFont);
							}
							fontChooser.dispose();
						});
						add(fontButton);
						break;
					case COLOR:
						JButton colorButton = I18N.mkButton(rb, name);
						colorButton.addActionListener(_ -> {
							Color ch = JColorChooser.showDialog(
									jf,             // parent
									"Pick a Drawing Color",   // title
									this.curColor);
							if (ch != null) {
								 mHandler.accept(ch);
							}
						});
						add(colorButton);
						break;
					default:
						throw new IllegalStateException(STR."Unknown Handler Type \{handler.type()}");
				}
			}
		}
	}
}
