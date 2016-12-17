package http;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;


/**
 * HttpResponse class defines the HTTP Response Status Line (method, URI, version) 
 * and Headers http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html
 */
public class HttpResponse {

	private static Logger log = Logger.getLogger(HttpResponse.class);
    private static SimpleDateFormat format =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    private String version = "HTTP/1.0";
    private Status status = Status._200;

    private boolean keepAlive = false;
    private boolean isHeadResponse = false;

    private Date lastModified = null;
    private ContentType contentType = ContentType.TXT;
    private List<String> headers = new ArrayList<String>();

    private byte[] body;

    public void setResponseHeader(String version, Status status) {
        this.version = version;
        this.status = status;
    }

    public void setPersistentConnection(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public void setBody(String body) {
        this.body = body.getBytes();
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public void setContentType(String uri) {
        try {
            String ext = uri.substring(uri.lastIndexOf(".") + 1);
            contentType = ContentType.valueOf(ext.toUpperCase());
        } catch (Exception e) {
            // ignore
            contentType = contentType.TXT;
        }
    }

    public void setHeadResponse() {
        isHeadResponse = true;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = new Date(lastModified);
    }

    public void addHeader(String name, String value) {
        headers.add(name + ": " + value);
    }

	private void writeResponseHeader(DataOutputStream output) throws IOException {
        output.writeBytes(version + " " + status.toString() + "\r\n");
    }

    private void writeCommonHeader(DataOutputStream output) throws IOException {
        String connection = keepAlive ? "Connection: keep-alive" : "Connection: close";
        String date = "Date: " + format.format(new Date());
        String server = "Server: WebServer 0.1";

        output.writeBytes(connection + "\r\n");
        output.writeBytes(server + "\r\n");
        output.writeBytes(date + "\r\n");

        if (lastModified != null) {
            String lastModified = "Last-Modified: "
                    + format.format(this.lastModified);
            output.writeBytes(lastModified + "\r\n");
        }
    }

    private void writeContentInfos(DataOutputStream output) throws IOException {
        if (body != null) {
            String contentType = this.contentType.toString();
            String contentLength = "Content-Length: " + body.length;
            output.writeBytes(contentType + "\r\n");
            output.writeBytes(contentLength + "\r\n");
        } else {
            output.writeBytes("Content-Length: 0\r\n");
        }
    }

	/**
	 * dump response
	 * @param os
	 * @throws IOException
	 */
	public void write(OutputStream os) throws IOException {
		DataOutputStream output = new DataOutputStream(os);

        writeResponseHeader(output);
        writeCommonHeader(output);
        writeContentInfos(output);

		for (String header : headers) {
			output.writeBytes(header + "\r\n");
            log.info(header);
		}
		output.writeBytes("\r\n");

		if (body != null && !isHeadResponse) {
			output.write(body);
		}

		//output.writeBytes("\r\n");
		output.flush();
	}
}
