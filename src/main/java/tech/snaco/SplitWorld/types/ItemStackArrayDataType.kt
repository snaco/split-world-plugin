package tech.snaco.SplitWorld.types;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;

@SuppressWarnings({"DataFlowIssue"})
public class ItemStackArrayDataType implements PersistentDataType<byte[], ItemStack[]> {
    @Override
    public @NotNull Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public @NotNull Class<ItemStack[]> getComplexType() {
        return ItemStack[].class;
    }

    @Override
    public byte @NotNull [] toPrimitive(ItemStack @NotNull [] complex, @NotNull PersistentDataAdapterContext context) {
        try (var baos = new ByteArrayOutputStream(); var oos = new BukkitObjectOutputStream(baos)) {
            oos.writeObject(complex);
            oos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            System.out.println("toPrimitive");
            System.out.println(e.getMessage());
        }
        return new byte[0];
    }

    @Override
    public ItemStack @NotNull [] fromPrimitive(byte @NotNull [] primitive, @NotNull PersistentDataAdapterContext context) {
        try(var bais = new ByteArrayInputStream(primitive); var ois = new BukkitObjectInputStream(bais)) {
            return (ItemStack[]) ois.readObject();
        } catch (Exception e) {
            System.out.println("fromPrimitive");
            System.out.println(e.getMessage());
        }
        return null;
    }
}
