package tech.snaco.SplitWorld;

import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class InventoryDataType implements PersistentDataType<byte[], PlayerInventory> {
    @Override
    public @NotNull Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public @NotNull Class<PlayerInventory> getComplexType() {
        return PlayerInventory.class;
    }

    @Override
    public byte @NotNull [] toPrimitive(@NotNull PlayerInventory complex, @NotNull PersistentDataAdapterContext context) {
        try (var baos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(baos);) {
            oos.writeObject(complex);
            var bytes = baos.toByteArray();
            return bytes;
        } catch (Exception e) {}
        return new byte[0];
    }

    @Override
    public @NotNull PlayerInventory fromPrimitive(byte @NotNull [] primitive, @NotNull PersistentDataAdapterContext context) {
        try(var bais = new ByteArrayInputStream(primitive); var ois = new ObjectInputStream(bais);) {
            return (PlayerInventory) ois.readObject();
        } catch (Exception e) {}
        return null;
    }
}
