/**
 * Created by Maochuan on 2016/10/13.
 */

interface Request {

    val method: String
    val url: String
    val params: Map<String, String>
    val headers: Map<String, String>
    val data: Any?
}