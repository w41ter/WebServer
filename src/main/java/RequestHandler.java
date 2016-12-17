import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import http.*;

import org.apache.log4j.Logger;

/**
 * Class <code>RequestHandler</code> -
 * Thread class that answer the requests in the socket
 */
public class RequestHandler implements Runnable {

    // for debug.
    private static Long socketCounter = 0l;

	private static Logger log = Logger.getLogger(RequestHandler.class);

	private Socket socket;
    private Long currentCounter = 0l;

	public RequestHandler(Socket socket) {
		this.socket = socket;
        accessSocket(socket);
	}

	public void run() {
		try {
			while (true) {
				HttpRequest req = new HttpRequest(socket.getInputStream());

                if (!req.parse())
                    /* no more data, close it */
                    break;

                HttpRequestLog(req);
                HttpResponse res = process(req);
                res.write(socket.getOutputStream());

				// if need close
                if (!req.isPersistentConnection())
                    break;
			}

			closeSocket(socket);
		} catch (Exception e) {
			log.error("Error occupied at accept socket", e);
		}
	}

	private void HttpRequestLog(HttpRequest req) {
        if (req.isAvailable())
		    log.info("\t" + req.method.toString() + " "
                    + req.uri + " "
                    + req.version);
	}

	private void accessSocket(Socket socket) {
        synchronized (socketCounter) {
            socketCounter++;
            currentCounter = socketCounter;
        }
        log.info("---====== Access socket " + currentCounter + " ======---");
    }

    private void closeSocket(Socket socket) throws IOException {
        socket.close();
        log.info("---+ Close socket " + currentCounter);
    }

	private HttpResponse process(HttpRequest request) {
        if (!request.isAvailable())
            return doBadRequest(request);

		switch (request.method) {
			case HEAD:
				return doHead(request);
			case GET:
				return doGet(request);
            case POST:
                return doPost(request);
            case OPTIONS:
                return doOptions(request);
			case UNRECOGNIZED:
				return doUnrecognized(request);
			default:
				return doUnsupported(request);
		}
	}

	private void setCookie(HttpResponse response, HttpRequest request) {
        if (request.cookie != null && request.cookie.equals(""))
            response.addHeader("Set-Cookie", request.cookie);
    }

	private HttpResponse doHead(HttpRequest request) {
        HttpResponse response = doGet(request);
        response.setHeadResponse();
        return response;
	}

	private HttpResponse doPost(HttpRequest request) {
        return doGet(request);
    }

	private HttpResponse doGet(HttpRequest request) {
		HttpResponse response = new HttpResponse();
		response.setResponseHeader(request.version, Status._200);
		response.setPersistentConnection(request.isPersistentConnection());
        setCookie(response, request);

		String root = ".";

		try {
			File file = new File(root + request.resource);
			if (file.isDirectory()) {
				// support index.html
				File indexFile = new File(file, "index.html");
				if (! indexFile.isFile())
					dumpDirectory(response, request.resource, file);
				else
					sendFile(response, indexFile, request);
			} else if (file.isFile()) {
				sendFile(response, file, request);
			} else {
				log.info("File not found:" + request.resource);
				return doNotFound(request);
			}
			return response;
		} catch (Exception e) {
			log.error("Response Error", e);
			return doNotFound(request);
		}
	}

	private void dumpDirectory(HttpResponse response, String uri, File file) {
		response.setContentType(ContentType.HTML);
		StringBuilder result = new StringBuilder("<html><head><title>Index of ");
		result.append(uri);
		result.append("</title></head><body><h1>Index of ");
		result.append(uri);
		result.append("</h1><hr><pre>");

		// add parent
		result.append(" <a href=\"" + "..\\" + "\">..\\</a>\n");

		File[] files = file.listFiles();
		for (File child : files) {
			String name = child.getName();
			if (child.isDirectory())
				name += '\\';
			result.append(" <a href=\"" + name + "\">" + name + "</a>\n");
		}
		result.append("<hr></pre></body></html>");
		response.setBody(result.toString());
	}

	private void sendStaticFile(HttpResponse response, File file, HttpRequest request) throws IOException {
		if (request.ifModifiedSince != null
				&& request.ifModifiedSince.getTime() >= file.lastModified()) {
			onNotModified(response, request);
			return;
		}
		response.setContentType(file.getPath());
		response.setLastModified(file.lastModified());
		response.setBody(getBytes(file));
	}

	private void insertParamsInto(Map<String, String> values,
		Map<String, List<String>> array, String key, String val) {
		if (array.containsKey(key)) {
			array.get(key).add(val);
		} else if (values.containsKey(key)) {
			List<String> list = new ArrayList<>();
			list.add(val);
			array.put(key, list);
			values.remove(key);
		} else {
			values.put(key, val);
		}
	}

	private Execute.ExecuteParams getParams(HttpRequest request) throws IOException {
        Execute.ExecuteParams params = new Execute.ExecuteParams();
        Map<String, String> values = new HashMap<>();
        Map<String, List<String>> array = new HashMap<>();

		HttpRequest.FormData callback = new HttpRequest.FormData() {
			@Override
			public void value(String name, String value) {
				insertParamsInto(values, array, name, value);
			}

			@Override
			public void file(String name, String path) {
				params.putFile(name, path);
			}
		};

		if (request.query != null) {
            request.foreachUrlParams(callback);
            values.forEach(params::putGet);
            array.forEach(params::putGet);
        }

        values.clear();
        array.clear();
        if (request.method == Method.POST && request.contentLength != 0) {
            if (request.isFormUrlEncoded()) {
				request.foreachUrlEncoded(callback);
                values.forEach(params::putPost);
                array.forEach(params::putPost);
            } else {
				request.foreachFromData(callback);
				values.forEach(params::putPost);
				array.forEach(params::putPost);
            }
        }
        return params;
    }

	private void sendDynamicFile(HttpResponse response, File file, HttpRequest request) {
		response.setContentType(".html");
		try {
            Execute.ExecuteParams params = getParams(request);
            byte[] body = Execute.exec(file, params).getBytes();
			response.setBody(body);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendFile(HttpResponse response, File file, HttpRequest request) throws IOException {
		String filename = file.getName().toLowerCase();
		int index = filename.lastIndexOf('.');
		if (index != -1 && filename.substring(index + 1).equals("php")) {
			sendDynamicFile(response, file, request);
		} else {
			sendStaticFile(response, file, request);
		}
	}

	private byte[] getBytes(File file) throws IOException {
		int length = (int) file.length();
		byte[] array = new byte[length];
		InputStream in = new FileInputStream(file);
		int offset = 0;
		while (offset < length) {
			int count = in.read(array, offset, (length - offset));
			offset += count;
		}
		in.close();
		return array;
	}

	private void onNotModified(HttpResponse response, HttpRequest request) {
        response.setResponseHeader(request.version, Status._304);
    }

	private HttpResponse doUnrecognized(HttpRequest request) {
		HttpResponse response = new HttpResponse();
		response.setResponseHeader(request.version, Status._400);
		response.setPersistentConnection(false);
        setCookie(response, request);
        return response;
	}

	private HttpResponse doOptions(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        response.setResponseHeader(request.version, Status._200);
        response.setPersistentConnection(false);
        response.addHeader("Allow", "GET, HEAD, OPTIONS");
        setCookie(response, request);
        return response;
    }

	private HttpResponse doUnsupported(HttpRequest request) {
		HttpResponse response = new HttpResponse();
		response.setResponseHeader(request.version, Status._501);
		response.setPersistentConnection(false);
        setCookie(response, request);
        return response;
	}

	private HttpResponse doNotFound(HttpRequest request) {
		HttpResponse response = new HttpResponse();
		response.setResponseHeader(request.version, Status._404);
		response.setPersistentConnection(false);
        setCookie(response, request);
        return response;
	}

	private HttpResponse doBadRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        response.setResponseHeader(request.version, Status._400);
        response.setPersistentConnection(false);
        setCookie(response, request);
        return response;
    }

}
