package http;

/**
 * ContentType enum uses the file extension to loosely map the available content type based on common media types:
 * http://en.wikipedia.org/wiki/Internet_media_type
 */
public enum ContentType {
	JS("JS"),
	CSS("CSS"), //
	GIF("GIF"), //
	HTM("HTM"), //
	HTML("HTML"), //
	ICO("ICO"), //
	JPG("JPG"), //
	JPEG("JPEG"), //
	PNG("PNG"), //
	TXT("TXT"), //
	XML("XML"); //

	private final String extension;

	ContentType(String extension) {
		this.extension = extension;
	}

	@Override
	public String toString() {
		switch (this) {
			case JS:
				return "Content-Type: application/x-javascript";
			case CSS:
				return "Content-Type: text/css";
			case GIF:
				return "Content-Type: image/gif";
			case HTM:
			case HTML:
				return "Content-Type: text/html; charset=UTF-8";
			case ICO:
				return "Content-Type: image/gif";
			case JPG:
			case JPEG:
				return "Content-Type: image/jpeg";
			case PNG:
				return "Content-Type: image/png";
			case TXT:
				return "Content-type: text/plain; charset=gbk";
			case XML:
				return "Content-type: text/xml";
			default:
                return "Content-type: text/plain; charset=gbk";
		}
	}
}
