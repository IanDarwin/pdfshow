package net.rejmi.pdfshow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

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

		JPanel toolbox = new JPanel();
		toolbox.setBackground(Color.cyan);
		toolbox.setPreferredSize(new Dimension(200, 800));
		upButton = new JButton("Up");
		upButton.addActionListener(e -> moveToPage(tab.pageNumber - 1));
		toolbox.add(upButton);
		downButton = new JButton("Down");
		downButton.addActionListener(e -> moveToPage(tab.pageNumber + 1));
		toolbox.add(downButton);
		pageNumTF = new JTextField("0  ");
		pageNumTF.addActionListener(e -> moveToPage(Integer.parseInt(pageNumTF.getText())));
		toolbox.add(pageNumTF);

		openFile(new File("/Users/ian/lt2771", "lt2771add.pdf"));
		moveToPage(0);

		jf.add(BorderLayout.WEST, toolbox);
		jf.setVisible(true);
	}
	
	private static void openFile(File file) throws Exception {
		tab = new DocTab();
		tab.doc = PDDocument.load(file);
		tab.renderer = new PDFRenderer(tab.doc);

		jtp.addTab(file.getName(), tab);
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
