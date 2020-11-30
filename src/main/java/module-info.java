/**
 * module-info for pdfshow.
 */
module pdfshow {
	exports net.rejmi.pdfshow;

	requires darwinsys.api;
	requires java.desktop;
	requires java.logging;
	requires java.prefs;
	requires org.apache.pdfbox;
}