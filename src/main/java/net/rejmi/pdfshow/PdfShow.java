package net.rejmi.pdfshow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.darwinsys.swingui.MenuUtils;

public class PdfShow {
	
	@SuppressWarnings("serial")
	private static class DocTab extends JComponent {
		private int pageNumber = 0;
		private PDDocument doc;
		private PDFRenderer renderer;
		DocTab() {
			super();
			setSize(800, 800);
		}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			try {
				renderer.renderPageToGraphics(pageNumber, (Graphics2D) g);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(jf, "Failure: " + e);
			}
		}
	}
	private static JFrame jf;
	private static JTabbedPane jtp;
	private static DocTab tab;
	private static JButton upButton, downButton;
	private static JTextField pageNumTF;

	public static void main(String[] args) throws Exception {
		
		jf = new JFrame("PDFShow");
		jf.setSize(1000,800);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		jtp = new JTabbedPane();
		jf.add(BorderLayout.CENTER, jtp);
		
		JMenuBar mb = new JMenuBar();
		jf.setJMenuBar(mb);
		ResourceBundle b = ResourceBundle.getBundle("Menus");
		JMenu fm = MenuUtils.mkMenu(b, "file");
		mb.add(fm);
		JMenuItem miOpen = MenuUtils.mkMenuItem(b, "file", "open");
		fm.add(miOpen);
		miOpen.addActionListener(e -> {
			try {
				openFile(chooseFile());
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(jf, "Can't open file: " + e1);
			}
		});

		JPanel toolbox = new JPanel();
		toolbox.setBackground(Color.cyan);
		toolbox.setPreferredSize(new Dimension(200, 800));
		upButton = new JButton("Up");
		upButton.addActionListener(e -> moveToPage(tab.pageNumber - 1));
		toolbox.add(upButton);
		downButton = new JButton("Down");
		downButton.addActionListener(e -> moveToPage(tab.pageNumber + 1));
		toolbox.add(downButton);
		JButton firstButton = new JButton("<<"), 
			lastButton = new JButton(">>");
		firstButton.addActionListener(e -> moveToPage(0));
		toolbox.add(firstButton);
		pageNumTF = new JTextField("0  ");
		pageNumTF.addActionListener(e -> moveToPage(Integer.parseInt(pageNumTF.getText())));
		toolbox.add(pageNumTF);
		lastButton.addActionListener(e -> moveToPage(Integer.MAX_VALUE));
		toolbox.add(lastButton);

		openFile(new File("/Users/ian/lt2771", "lt2771add.pdf"));
		moveToPage(0);

		jf.add(BorderLayout.WEST, toolbox);
		jf.setVisible(true);
	}
	
	private static File chooseFile() {
		final JFileChooser fc = new JFileChooser();
		// XXX start in curdir
		// XXX add pdf-only filter
		File f = null;
		do {
			fc.showOpenDialog(jf);
			f = fc.getSelectedFile();
			if (f != null) {
				return f;
			} else {
				JOptionPane.showMessageDialog(jf, "Please choose a file");
			}
		} while (f != null);
		return null;
	}

	private static void openFile(File file) throws Exception {
		DocTab t = new DocTab();
		t.doc = PDDocument.load(file);
		t.renderer = new PDFRenderer(t.doc);
		
		jtp.addTab(file.getName(), t);
		// If no exception, then change global tab
		tab = t;
	}

	private static void closeFile(DocTab dt) {
		// XXX
	}

	private static void moveToPage(int newPage) {
		int docPages = tab.doc.getNumberOfPages();
		if (newPage < 0) {
			newPage = 0;
		}
		if (newPage >= docPages) {
			newPage = docPages - 1;
		}
		if (newPage == tab.pageNumber) {
			return;
		}
		pageNumTF.setText(Integer.toString(newPage));
		tab.pageNumber = newPage;
		upButton.setEnabled(tab.pageNumber > 0);
		downButton.setEnabled(tab.pageNumber < docPages);
		tab.repaint();
	}
}
