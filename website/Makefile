GENERATED = index.html release-notes.html
all:		$(GENERATED)
index.html:	index.adoc
	git pull
	asciidoctor index.adoc
release-notes.html:	release-notes.adoc
	git pull
	asciidoctor release-notes.adoc
install:	$(GENERATED)
	git pull
	cp -r * /var/www/htdocs/darwinsys.com/pdfshow
