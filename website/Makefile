GENERATED = index.html release-notes.html
all:		$(GENERATED)
index.html:	index.adoc
	asciidoctor index.adoc
release-notes.html:	release-notes.adoc
	asciidoctor release-notes.adoc
install:	$(GENERATED)
	git pull
	cp -r * /var/www/htdocs/darwinsys.com/pdfshow
