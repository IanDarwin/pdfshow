install:
	git pull
	mvn clean package assembly:single
	rm ~/lib/pdfshow*
	cp target/pdfshow-1.?.?-jar-with-dependencies.jar ~/lib
webpages:
	cd website; make
