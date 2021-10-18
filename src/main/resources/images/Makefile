all:
	@echo Ya wanna MAKE someting of it?

# Make a Windoze .ico file, cf http://www.imagemagick.org/Usage/thumbnails/#favicon
logo.ico:
	convert logo.png  -bordercolor white -border 0 \
		\( -clone 0 -resize 16x16 \) \
		\( -clone 0 -resize 32x32 \) \
		\( -clone 0 -resize 48x48 \) \
		\( -clone 0 -resize 64x64 \) \
		-delete 0 -alpha off -colors 256 logo.ico
