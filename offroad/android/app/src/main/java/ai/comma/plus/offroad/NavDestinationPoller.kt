package ai.comma.plus.offroad

import ai.comma.openpilot.cereal.Log
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableNativeMap
import org.capnproto.MessageReader
import org.capnproto.Serialize
import org.zeromq.ZMQ
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Created by batman on 12/4/17.
 */

data class Destination(
        val title: String,
        val lat: Double,
        val lng: Double,
        val house: String,
        val address: String,
        val street: String,
        val city: String,
        val state: String,
        val country: String
) {
    fun toWritableMap(): WritableMap {
        val map = WritableNativeMap()
        map.putString("title", title)
        map.putDouble("lat", lat)
        map.putDouble("lng", lng)
        map.putString("house", house)
        map.putString("address", address)
        map.putString("street", street)
        map.putString("city", city)
        map.putString("state", state)
        map.putString("country", country)

        return map
    }

    companion object {
        fun readFromAddress(reader: Log.NavStatus.Address.Reader): Destination {
            return Destination(
                    reader.title.toString(),
                    reader.lat,
                    reader.lng,
                    reader.house.toString(),
                    reader.address.toString(),
                    reader.street.toString(),
                    reader.city.toString(),
                    reader.state.toString(),
                    reader.country.toString()
            )
        }
    }
}

interface NavDestinationPollerDelegate {
    fun onDestinationReceived(destination: Destination)
}

class NavDestinationPoller(val delegate: NavDestinationPollerDelegate) {
    val zmqCtx: ZMQ.Context
    val navStatusThreadHandle: Thread
    var navStatusSock: ZMQ.Socket? = null
    var running: Boolean = false
    var lastDestination: Destination? = null

    init {
        navStatusThreadHandle = Thread(Runnable {
            statusThread()
        })
        zmqCtx = ZMQ.context(1)
    }

    fun statusThread() {
        while (true) {
            if (!running) break

            val msg = navStatusSock!!.recv()
            if (msg == null || msg.size < 4) {
                continue
            }
            val msgbuf = ByteBuffer.wrap(msg)
            var reader: MessageReader
            try {
                reader = Serialize.read(msgbuf)
            } catch (e: IOException) {
                android.util.Log.e("offroad", "read")
                continue
            }

            val log = reader.getRoot(Log.Event.factory)
            assert(log.isNavStatus)

            val address = log.navStatus.currentAddress
            try {
                val destination = Destination.readFromAddress(address)
                if (destination != lastDestination) {
                    delegate.onDestinationReceived(destination)
                }

                lastDestination = destination
            } catch(e: IOException) {
                android.util.Log.e("offroad", "bad destination", e)
            }
        }
    }

    fun start() {
        running = true
        navStatusSock = zmqCtx.socket(ZMQ.SUB)
        navStatusSock!!.connect("tcp://127.0.0.1:8038")
        navStatusSock!!.subscribe("")
        navStatusThreadHandle.start()
    }

    fun stop() {
        running = false
        navStatusSock!!.disconnect("tcp://127.0.0.1:8038")
    }
}