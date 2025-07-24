package com.example.zenohtest

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenoh.Config
import io.zenoh.Session
import io.zenoh.Zenoh
import io.zenoh.keyexpr.KeyExpr
import io.zenoh.keyexpr.intoKeyExpr
import io.zenoh.pubsub.Subscriber
import io.zenoh.sample.Sample
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random

class MainViewModel : ViewModel() {
    private val droneSensorKeyExpr: KeyExpr = "drone/sensor".intoKeyExpr().getOrElse {
        throw IllegalArgumentException("Invalid publisherKeyExprString: ${it.message}", it)
    }
    private val joystickOperationKeyExpr: KeyExpr = "joystick/operation".intoKeyExpr().getOrElse {
        throw IllegalArgumentException("Invalid subscriberKeyExprString: ${it.message}", it)
    }

    private val _gyroValues = mutableStateOf(floatArrayOf(0f, 0f, 0f))
    val gyroValues: State<FloatArray> = _gyroValues

    private val _operation = mutableStateOf("STOP")
    val operation: State<String> = _operation

    private var currentRole = Role.NONE
    private var session: Session? = null
    private var publisherJob: Job? = null
    private val peerConfig: String = """
            {
                mode: "peer",
                scouting: {
                    multicast: {
                        enabled: true
                    }
                }
            }
            """.trimIndent()


    fun startSession(role: Role) {
        viewModelScope.launch {
            try {
                currentRole = role
                val config = Config.default()
//                val config = Config.fromJson(peerConfig).getOrThrow()
                Zenoh.open(config).onSuccess {
                    session = it
                    if (role == Role.DRONE) {
                        publishGyroData()
                        subscribeOperation()
                    } else if (role == Role.JOYSTICK) {
                        subscribeGyroData()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun subscribeOperation() {
        viewModelScope.launch {
            var subscriber: Subscriber<Channel<Sample>>? = null
            try {
                subscriber =
                    session?.declareSubscriber(joystickOperationKeyExpr, Channel())?.getOrThrow()
                        ?: return@launch
                while (isActive) {
                    val sample = subscriber.receiver.receive()
                    _operation.value = sample.payload.toString()
                }
            } catch (e: Exception) {
                _operation.value = e.message.toString()
            } finally {
                subscriber?.close()
            }
        }
    }

    fun publishOperation(operation: Operation) {
        if (publisherJob?.isActive == true) return

        publisherJob = viewModelScope.launch {
            val publisher = session?.declarePublisher(joystickOperationKeyExpr) ?: return@launch

            try {
                val payload = operation.name
                publisher.onSuccess {
                    it.put(payload)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun subscribeGyroData() {
        viewModelScope.launch {
            var subscriber: Subscriber<Channel<Sample>>? = null
            try {
                subscriber = session?.declareSubscriber(droneSensorKeyExpr, Channel())?.getOrThrow()
                    ?: return@launch

                while (isActive) {
                    val floatArray: FloatArray = try {
                        val sample = subscriber.receiver.receive()
                        val payloadString = sample.payload.tryToString().getOrDefault("")
                        val json = JSONObject(payloadString)
                        floatArrayOf(
                            json.getString("x").toFloat(),
                            json.getString("y").toFloat(),
                            json.getString("z").toFloat()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        floatArrayOf(0f, 0f, 0f)
                    }
                    _gyroValues.value = floatArray
                }
            } catch (e: Exception) {
                _gyroValues.value = floatArrayOf(0f, 0f, 0f)
            } finally {
                subscriber?.close()
            }
        }
    }

    private fun publishGyroData() {
        if (publisherJob?.isActive == true) return

        publisherJob = viewModelScope.launch {
            val publisher = session?.declarePublisher(droneSensorKeyExpr) ?: return@launch
            while (this.isActive) {
                try {
                    val x = Random.nextFloat() * 10f - 5f
                    val y = Random.nextFloat() * 10f - 5f
                    val z = Random.nextFloat() * 10f - 5f
                    val jsonObject = JSONObject()
                    jsonObject.put("x", x.toString())
                    jsonObject.put("y", y.toString())
                    jsonObject.put("z", z.toString())
                    val payload = jsonObject.toString()
                    publisher.onSuccess { it.put(payload) }
                    delay(1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        publisherJob?.cancel()
        try {
            session?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}