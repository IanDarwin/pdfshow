install:
	mvn clean package assembly:single
	cp target/pdfshow-1.?.?-jar-with-dependencies.jar ~/lib
webpages:
	cd website; make
