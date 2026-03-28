import java.io.*;

// Dump the annotations .ser file
// It has a List<GObject> for each slide in the deck.
// JSON might be better as it easier to edit when adding/delling a slide

import net.rejmi.pdfshow.GObject;

void main(String[] args) throws Exception {
	if (args.length == 0) {
		process("annotations.ser");
	} else {
		for (String fname : args) {
			process(fname);
		}
	}
}

void process(String fileName) throws Exception {
	try (var ois = new ObjectInputStream(new FileInputStream(fileName))) {
		Object obj = null;
		int i = 0;
		while ((obj = ois.readObject()) != null) {
			++i;
			@SuppressWarnings("unchecked")
			List<GObject> list = (List<GObject>)obj;
			if (list.size() > 0) {
				System.out.println(i + ": " + list);
			}
		}
	} catch (EOFException eof) {
		System.out.println("All done");
	}
}
