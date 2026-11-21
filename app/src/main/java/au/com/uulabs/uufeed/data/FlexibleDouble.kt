package au.com.uulabs.uufeed.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * Google encodes a whole-number rating as `4`, not `4.0`. kotlinx.serialization
 * refuses to put that JSON integer into a Double, which used to fail the whole
 * response.
 */
object FlexibleDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }

    override fun deserialize(decoder: Decoder): Double {
        val json = decoder as? JsonDecoder
            ?: return decoder.decodeDouble()
        val primitive = json.decodeJsonElement() as? JsonPrimitive
            ?: error("Expected a JSON number")
        return primitive.doubleOrNull
            ?: primitive.content.toDouble()
    }
}
