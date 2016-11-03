/**
 * Created by Maochuan on 2016/10/13.
 */

package WebServer;

class WebServer(val port: Int) {

    private val connect = Connect(port)

    fun run() {
        log("Web Server is start")
        connect.run()

        while (true) {
            if (connect.size() == 0) {
                Thread.sleep(1)
                continue
            }
            val client = connect.get()

        }

        log("Web Server is shutdown")
    }
}
