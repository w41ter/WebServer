import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * <code>Config</code> 默认配置文件
 * 如果不存在，默认使用 8080 端口和当前运行目录
 */
public class Config {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_ROOT = "./";

    private int mConnectTimeout = 3000;
    private int mPort = -1;
    public String mDocumentRoot = "";

    Config() { }

    public int getPort() {
        if (mPort > 0 && mPort < 65535) {
            return mPort;
        } else {
            return DEFAULT_PORT;
        }
    }

    public String getDocumentRoot() {
        if (mDocumentRoot == null || mDocumentRoot.isEmpty())
            return DEFAULT_ROOT;
        return mDocumentRoot;
    }

    public int getConnectTimeout() {
        return mConnectTimeout;
    }
}
