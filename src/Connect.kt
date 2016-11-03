/**
 * Created by Maochuan on 2016/10/13.
 */

package WebServer;

import java.net.ServerSocket
import java.net.Socket
import java.util.LinkedList

open class Connect(val port: Int) : Runnable {

    private val serverSocket = ServerSocket(port)
    private val connectPool = LinkedList<Socket>()

    var exists = false;

    override fun run() {
        log("Socket Connect thread start")
        while (!exists) {
            put(serverSocket.accept())
        }
        log("Socket Connect thread shutdown")
    }

    fun get() : Socket? {
        synchronized(this) {
            if (connectPool.size == 0)
                return null
            return connectPool.pop()
        }
    }

    fun put(socket: Socket) {
        synchronized(this) {
            connectPool.push(socket)
        }
    }

    fun size() : Int {
        synchronized(this) {
            return connectPool.size
        }
    }
}