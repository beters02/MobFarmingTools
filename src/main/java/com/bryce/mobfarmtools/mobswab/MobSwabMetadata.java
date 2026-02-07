package com.bryce.mobfarmtools.mobswab;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;

public class MobSwabMetadata {

    public static final String KEY = "SwabbedMob";
    public static final BuilderCodec<MobSwabMetadata> CODEC = BuilderCodec.builder(MobSwabMetadata.class, MobSwabMetadata::new)
            .append(new KeyedCodec<>("MobId", Codec.STRING),
                    (m, v) -> m.mobId = v,
                    m -> m.mobId)
            .add()
            .append(new KeyedCodec<>("EntitySize", Codec.DOUBLE_ARRAY),
                    (m, v) -> m.entitySize = v,
                    m -> m.entitySize)
            .add()
            .build();
    public static final KeyedCodec<MobSwabMetadata> KEYED_CODEC = new KeyedCodec<>(KEY, CODEC);

    private String mobId = "None";
    private double[] entitySize;

    public String getMobId() {
        return this.mobId;
    }

    public Vector3d getEntitySize() {
        if (entitySize.length < 2) {
            return new Vector3d();
        }

        return new Vector3d(entitySize[0], entitySize[1], entitySize[2]);
    }

    public Vector3i getFixedEntitySize() {
        Vector3d sizeVec = getEntitySize();
        return new Vector3i(
                (int) Math.ceil(sizeVec.x),
                (int) Math.ceil(sizeVec.y),
                (int) Math.ceil(sizeVec.z)
        );
    }

    public void setMobId(String mobId) { this.mobId = mobId; }
    public void setEntitySize(Vector3d size) {
        entitySize = new double[]{size.x, size.y, size.z};
    }

}
