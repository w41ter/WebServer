import WebServer.WebServer

/**
 * Created by Maochuan on 2016/10/13.
 */

fun main(args: Array<String>) {
    val server = WebServer(8080)
    server.run()
}