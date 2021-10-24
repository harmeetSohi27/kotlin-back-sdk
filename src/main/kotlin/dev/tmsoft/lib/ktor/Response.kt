@file:UseSerializers(ResponseErrorSerializer::class)

package dev.tmsoft.lib.ktor

import dev.tmsoft.lib.query.ContinuousList
import dev.tmsoft.lib.query.ContinuousListSerializer
import dev.tmsoft.lib.serialization.resolveSerializer
import dev.tmsoft.lib.validation.Error
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.ApplicationSendPipeline
import io.ktor.response.respondFile
import io.ktor.routing.Route
import kotlin.collections.set
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
sealed class Response {
    @Serializable
    class Error(val error: dev.tmsoft.lib.validation.Error) : Response()

    @Serializable(with = ResponseOkSerializer::class)
    object Ok : Response()

    @Serializable(with = ResponseDataSerializer::class)
    class Data<T : Any>(val data: T) : Response()

    class File(val file: java.io.File) : Response()

    @Serializable
    class Errors(val errors: List<dev.tmsoft.lib.validation.Error>) : Response()

    @Serializable(with = ResponseListingSerializer::class)
    class Listing<T : Any>(val list: ContinuousList<T>) : Response()

    @Serializable(with = ResponseEitherSerializer::class)
    class Either<TL : Response, TR : Response>(val data: dev.tmsoft.lib.structure.Either<TL, TR>) : Response()
}

class RouteResponseInterceptor : Interceptor() {
    override fun intercept(route: Route) {
        route.sendPipeline.intercept(ApplicationSendPipeline.Before) {
            if (it is Response.File) {
                context.response.status(it.status())
                call.respondFile(it.file)
                finish()
            }
        }
        route.sendPipeline.intercept(ApplicationSendPipeline.Transform) {
            if (it is Response) {
                context.response.status(it.status())
                proceedWith(it)
            }
        }
    }
}

fun Response.status(): HttpStatusCode {
    return when (this) {
        is Response.Error -> HttpStatusCode.UnprocessableEntity
        is Response.Errors -> HttpStatusCode.UnprocessableEntity
        is Response.Either<*, *> -> this.data.fold({ it.status() }, { it.status() }) as HttpStatusCode
        else -> HttpStatusCode.OK
    }
}

object ResponseEitherSerializer : KSerializer<Response.Either<out Response, out Response>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseEitherSerializerDescriptor")

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Response.Either<out Response, out Response>) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
        val anon = { response: Response ->
            output.json.encodeToJsonElement(
                resolveSerializer(response) as KSerializer<Response>,
                response
            )
        }
        val tree: JsonElement = value.data.fold(anon, anon) as JsonObject

        output.encodeJsonElement(tree)
    }

    override fun deserialize(decoder: Decoder): Response.Either<*, *> {
        throw NotImplementedError()
    }
}

object ResponseErrorSerializer : KSerializer<Error> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseErrorSerializerDescriptor")

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Error) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
        val error: MutableMap<String, JsonElement> = mutableMapOf("message" to JsonPrimitive(value.message))
        if (value.property != null && value.property.isNotBlank()) error["property"] = JsonPrimitive(value.property)
        if (value.value != null)
            error["value"] = output.json.encodeToJsonElement(
                resolveSerializer(value.value) as KSerializer<Any>,
                value.value
            )

        val tree = JsonObject(error)
        output.encodeJsonElement(tree)
    }

    override fun deserialize(decoder: Decoder): Error {
        throw NotImplementedError()
    }
}

object ResponseOkSerializer : KSerializer<Response.Ok> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseOkSerializerDescriptor")

    override fun serialize(encoder: Encoder, value: Response.Ok) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
        output.encodeJsonElement(JsonObject(mapOf("data" to JsonPrimitive("ok"))))
    }

    override fun deserialize(decoder: Decoder): Response.Ok {
        throw NotImplementedError()
    }
}

object ResponseDataSerializer : KSerializer<Response.Data<Any>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseDataSerializerDescriptor")

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Response.Data<Any>) {
        // toDo bug with inline classed and encodeToJsonElement
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
        val encoded = output.json.encodeToJsonElement(
            resolveSerializer(value.data) as KSerializer<Any>,
            value.data
        )
        output.encodeJsonElement(
            JsonObject(
                mapOf(
                    "data" to encoded
                )
            )
        )
    }

    override fun deserialize(decoder: Decoder): Response.Data<Any> {
        throw NotImplementedError()
    }
}

object ResponseListingSerializer : KSerializer<Response.Listing<Any>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ResponseListingSerializerDescriptor")

    override fun serialize(encoder: Encoder, value: Response.Listing<Any>) {
        // toDo bug with inline classed and encodeToJsonElement
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
        output.encodeJsonElement(output.json.encodeToJsonElement(ContinuousListSerializer, value.list))
    }

    override fun deserialize(decoder: Decoder): Response.Listing<Any> {
        throw NotImplementedError()
    }
}
