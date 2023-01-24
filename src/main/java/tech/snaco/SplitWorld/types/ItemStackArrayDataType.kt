package tech.snaco.SplitWorld.types

import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ItemStackArrayDataType : PersistentDataType<ByteArray, Array<ItemStack>> {
    override fun getPrimitiveType(): Class<ByteArray> {
        return ByteArray::class.java
    }

    override fun getComplexType(): Class<Array<ItemStack>> {
        return Array<ItemStack>::class.java
    }

    override fun toPrimitive(complex: Array<ItemStack>, context: PersistentDataAdapterContext): ByteArray {
        try {
            ByteArrayOutputStream().use { output_stream ->
                BukkitObjectOutputStream(output_stream).use { oos ->
                    oos.writeObject(complex)
                    oos.flush()
                    return output_stream.toByteArray()
                }
            }
        } catch (e: Exception) {
            println("toPrimitive")
            println(e.message)
        }
        return ByteArray(0)
    }

    override fun fromPrimitive(primitive: ByteArray, context: PersistentDataAdapterContext): Array<ItemStack> {
        try {
            ByteArrayInputStream(primitive).use { input_stream -> BukkitObjectInputStream(input_stream).use { ois ->
                @Suppress("UNCHECKED_CAST")
                return ois.readObject() as Array<ItemStack>
            } }
        } catch (e: Exception) {
            println("fromPrimitive")
            println(e.message)
        }
        return arrayOf()
    }
}