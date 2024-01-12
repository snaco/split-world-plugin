package tech.snaco.SplitWorld.types

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class PotionEffectArrayDataType : PersistentDataType<ByteArray, Array<PotionEffect>> {
    override fun getPrimitiveType(): Class<ByteArray> {
        return ByteArray::class.java
    }

    override fun getComplexType(): Class<Array<PotionEffect>> {
        return Array<PotionEffect>::class.java
    }

    override fun toPrimitive(complex: Array<PotionEffect>, context: PersistentDataAdapterContext): ByteArray {
        try {
            ByteArrayOutputStream().use { output_stream ->
                BukkitObjectOutputStream(output_stream).use { oos ->
                    oos.writeObject(complex)
                    oos.flush()
                    return output_stream.toByteArray()
                }
            }
        } catch (e: IOException) {
            println("toPrimitive")
            println(e.message)
        }
        return ByteArray(0)
    }

    override fun fromPrimitive(primitive: ByteArray, context: PersistentDataAdapterContext): Array<PotionEffect> {
        try {
            ByteArrayInputStream(primitive).use { input_stream -> BukkitObjectInputStream(input_stream).use { ois ->
                @Suppress("UNCHECKED_CAST")
                return ois.readObject() as Array<PotionEffect>
            } }
        } catch (e: Exception) {
            println("fromPrimitive")
            println(e.message)
        }
        return arrayOf()
    }
}