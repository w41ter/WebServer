package http;

import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;
import util.ByteString;

/**
 * HttpRequest class parses the HTTP Request Line (method, URI, version) 
 * and Headers http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html
 */
public class HttpRequest {

	private static Logger log = Logger.getLogger(HttpRequest.class);
    private static SimpleDateFormat format =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    private static SimpleDateFormat filenameFormat =
            new SimpleDateFormat("yyyyMMddHHMMSSE", Locale.US);

    private boolean isAvailable = false;    // whether request?
    private boolean connect;                // persistent connection?

    private BufferedInputStream bis;

	public Map<String, String> headers = new HashMap<String, String>();

    // Request header
    public Method method;
	public String uri;
	public String version;

    public String resource;
    public String query;
    public String frag;

    // Common header
    public int contentLength = 0;
    public String host;
    public String cookie;
    public String userAgent;
    public String accept;
    public String acceptLanguage;
    public String acceptEncoding;
    public String contentType;
    public Date ifModifiedSince; //If-Modified-Since

    // Body
    public byte[] body;

    public HttpRequest(InputStream is) throws IOException {
//        is.read()
//		reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
//        new InputStreamReader(is);
        bis = new BufferedInputStream(is);
	}

	public boolean parse() throws IOException {
        String requestLine = getRequestLine();
        if (requestLine == null)
            return false;

        isAvailable = true;

        parseRequestLine(requestLine);

        if (!isAvailable)
            return true;

        // Notice: call before request headers
        setPersistentContentionByVersion();

        if (!isAvailable)
            return true;

        parseRequestHeaders();

        if (!isAvailable)
            return true;

        // if Content-Length != zero.
        tryParseRequestBody();

        return true;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public boolean isPersistentConnection() {
        return connect;
    }

    public boolean isFormUrlEncoded() {
        return contentType != null && contentType.contains("application/x-www-form-urlencoded");
    }

    public boolean isMultipartFormData() {
        return contentType != null && contentType.contains("multipart/form-data");
    }

    private String getMultipartBoundary() {
        if (!isMultipartFormData())
             return "";
        String id = "boundary=";
        return contentType.substring(contentType.indexOf(id)+id.length());
    }

    private String readLine() throws IOException {
        StringBuilder builder = new StringBuilder();
        int c;
        while ((c = bis.read()) != -1) {
            if (c == '\n')
                break;
            if (c == '\r')
                continue;
            assert c < 256;
            builder.append((char)c);
        }
        return builder.toString();
    }

    private void parseFromData(ByteString part, FormData callback) throws IOException {
        String data = new String(part.getBytes());
        String name = null, filename = null;
        int at = 0;
        for (;;) {
            int next = data.indexOf("\r\n", at);
            assert next != -1;
            String line = data.substring(at, next);
            at = next + 2;

            if (line.length() == 0)
                break;

            // FIXME:
            String[] header = line.split(": ");
            if (header[0].equals("Content-Disposition")) {
                int b = header[1].indexOf("name=\"") + "name=\"".length();
                int e = header[1].indexOf("\"", b);
                name = header[1].substring(b, e);

                b = header[1].indexOf("filename=\"");
                if (b == -1)
                    continue;
                b += "filename=\"".length();
                e = header[1].indexOf("\"", b);
                filename = header[1].substring(b, e);
            }
        }

        // FIXME:
        ByteString body = part.substring(at);

        if (filename != null) {
            int fileExtIdx = filename.lastIndexOf('.') + 1;
            String fileExt = fileExtIdx == 0 ? "" : filename.substring(fileExtIdx);
            filename = filenameFormat.format(new Date()) + "." + fileExt;

            File file = new File("./tmp/" + filename);
            file.setWritable(true);
            file.setReadable(true);

            FileOutputStream output = new FileOutputStream(file);
            output.write(body.getBytes());
            output.close();
            callback.file(name, "/tmp/" + filename);
        } else {
            callback.value(name, new String(body.getBytes(), "ascii"));
        }
    }

    public void foreachFromData(FormData callback) throws IOException {
        String tb = "--" + getMultipartBoundary();
        ByteString boundary = new ByteString(tb.getBytes("ascii"));
        ByteString content = new ByteString(body);

        int start = content.indexOf(boundary) + boundary.length(),
                end = content.indexOf(boundary, start);

        while (end != -1) {
            // FIXME: \r\n
            ByteString part = content.substring(start + 2, end);
            parseFromData(part, callback);
            start = end + boundary.length();
            end = content.indexOf(boundary, start);
        }
    }

    public void foreachUrlEncoded(FormData callback) {
        // www-form-urlencoded: a=1&b=2
        String body = new String(this.body);
        String[] queryList = body.split("&");
        for (String pair : queryList) {
            String []kv = pair.split("=");
            callback.value(kv[0], kv.length == 1 ? "" : kv[1]);
        }
    }

    public void foreachUrlParams(FormData callback) {
        // www-form-urlencoded: a=1&b=2
        String[] queryList = query.split("&");
        for (String pair : queryList) {
            String []kv = pair.split("=");
            callback.value(kv[0], kv.length == 1 ? "" : kv[1]);
        }
    }

	private String getRequestLine() throws IOException {
        String requestLine;

        try {
            requestLine = readLine();
        } catch (SocketTimeoutException e) {
            // timeout, so that close it.
            connect = false;
            return null;
        }

        if (requestLine == null || requestLine.isEmpty()) {
            connect = false;
            return null;
        }

        return requestLine;
    }

    private void splitUri() {
        resource = uri;

        int indexOfFrag = resource.indexOf('#');
        if (indexOfFrag != -1) {
            frag = resource.substring(indexOfFrag + 1);
            resource = resource.substring(0, indexOfFrag);
        } else {
            frag = "";
        }

        int indexOfQuery = resource.indexOf('?');
        if (indexOfQuery != -1) {
            query = resource.substring(indexOfQuery + 1);
            resource = resource.substring(0, indexOfQuery);
        }
    }

	private void parseRequestLine(String str) {
        String[] split = str.split("\\s+");

        if (split.length != 3) {
            /* request line isn't pattern like
               GET / HTTP/1.1
             */
            isAvailable = false;
            return;
        }

		try {
			method = Method.valueOf(split[0]);
		} catch (Exception e) {
			method = Method.UNRECOGNIZED;
		}

		uri = split[1];
		version = split[2].toUpperCase();

        splitUri();
	}

	private String getRequestHeaderLine() throws IOException {
        // FIXME: headerLine can be multi-line, and next line
        //  begin with ' ' or '\t'.
        return readLine();
    }

	private void parseRequestHeader(String str) {
        String[] split = str.split(": ");
        assert (split.length == 2);
        switch (split[0]) {
            case "host":
                host = split[1];
                break;
            case "Connection":
                connect = !split[1].equals("close");
                break;
            case "User-Agent":
                userAgent = split[1];
                break;
            case "Accept":
                accept = split[1];
                break;
            case "Accept-Language":
                acceptLanguage = split[1];
                break;
            case "Accept-Encoding":
                acceptEncoding = split[1];
                break;
            case "Content-Length":
                contentLength = Integer.parseInt(split[1]);
                break;
            case "Content-Type":
                contentType = split[1];
                break;
            case "Cookie":
                cookie = split[1];
                break;
            case "If-Modified-Since":
                try {
                    ifModifiedSince = format.parse(split[1]);
                } catch (ParseException e) {
                    // ignore
                }
                break;
            default:
                headers.put(split[0], split[1]);
                break;
        }
	}

	private void parseRequestHeaders() throws IOException {
        String str = getRequestHeaderLine();
        while (!str.equals("")) {
            parseRequestHeader(str);
            str = readLine();
        }
    }

    private void tryParseRequestBody() throws IOException {
        if (contentLength == 0)
            return;

        assert contentLength > 0;

        body = new byte[contentLength];
        byte[] data = new byte[4096];
        int has = 0, size = 0;
        while ((size = bis.read(data)) != -1) {
            System.arraycopy(data, 0, body, has, size);
            has += size;
            if (has >= contentLength)
                break;
        }
    }

    private void setPersistentContentionByVersion() {
        // http 1.0中默认是关闭的
        // http 1.1中默认启用Keep-Alive
        connect = version.equals("HTTP/1.1");
    }

    public interface FormData {
        void value(String name, String value);
        void file(String name, String path);
    }
}
