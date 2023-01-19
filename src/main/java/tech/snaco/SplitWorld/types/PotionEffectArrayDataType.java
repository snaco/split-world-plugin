package tech.snaco.SplitWorld.types;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@SuppressWarnings("DataFlowIssue")
public class PotionEffectArrayDataType implements PersistentDataType<byte[], PotionEffect[]> {
    @Override
    public @NotNull Class<byte[]> getPrimitiveType() { return byte[].class; }

    @Override
    public @NotNull Class<PotionEffect[]> getComplexType() { return PotionEffect[].class; }

    @Override
    public byte @NotNull [] toPrimitive(PotionEffect @NotNull [] complex, @NotNull PersistentDataAdapterContext context) {
        try (var baos = new ByteArrayOutputStream(); var oos = new BukkitObjectOutputStream(baos)) {
            oos.writeObject(complex);
            oos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            System.out.println("toPrimitive");
            System.out.println(e.getMessage());
        }
        return new byte[0];
    }

    @Override
    public PotionEffect @NotNull [] fromPrimitive(byte @NotNull [] primitive, @NotNull PersistentDataAdapterContext context) {
        try(var bais = new ByteArrayInputStream(primitive); var ois = new BukkitObjectInputStream(bais)) {
            return (PotionEffect[]) ois.readObject();
        } catch (Exception e) {
            System.out.println("fromPrimitive");
            System.out.println(e.getMessage());
        }
        return null;
    }
}
