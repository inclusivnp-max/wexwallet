package wex.wallet.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong

class JsonRpcClient(private val rpcUrl: String) : Closeable {

    private val counter = AtomicLong(1)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(this@JsonRpcClient.json) }
    }

    suspend fun call(method: String, params: List<JsonElement> = emptyList()): JsonElement {
        val id = counter.getAndIncrement()
        val request = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("method", JsonPrimitive(method))
            put("params", buildJsonArray { params.forEach { add(it) } })
            put("id", JsonPrimitive(id))
        }

        val response: JsonElement = httpClient.post(rpcUrl) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        val obj = response.jsonObject
        val error = obj["error"]
        if (error != null && error !is JsonNull) {
            val errorObj = error.jsonObject
            val code = errorObj["code"]?.jsonPrimitive?.content ?: "unknown"
            val message = errorObj["message"]?.jsonPrimitive?.content ?: "RPC error"
            throw JsonRpcException(code, message)
        }

        return obj["result"] ?: throw JsonRpcException("null", "No result in response")
    }

    override fun close() {
        httpClient.close()
    }
}

class JsonRpcException(val code: String, override val message: String) :
    Exception("JSON-RPC error $code: $message")
