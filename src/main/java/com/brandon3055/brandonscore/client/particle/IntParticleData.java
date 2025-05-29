package com.brandon3055.brandonscore.client.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brandon3055 on 22/01/2025
 */
//public class IntParticlData extends ParticleType<IntParticlData> implements ParticleOptions {
//    private final List<Integer> data;
//
//    private static final StreamCodec<ByteBuf, List<Integer>> INT_LIST = new StreamCodec<>() {
//        public List<Integer> decode(ByteBuf buf) {
//            List<Integer> result = new ArrayList<>();
//            int count = buf.readInt();
//            for (int i = 0; i < count; i++) {
//                result.add(buf.readInt());
//            }
//            return result;
//        }
//
//        public void encode(ByteBuf buf, List<Integer> list) {
//            buf.writeInt(list.size());
//            list.forEach(buf::writeInt);
//        }
//    };
//
////    private final MapCodec<IntParticlType> codec = MapCodec.unit(this::getType);
//    private final MapCodec<IntParticlData> codec = Codec.INT.listOf().xmap(intList -> new IntParticlData(type, intList), data -> data.data).fieldOf("data");
//    private final StreamCodec<RegistryFriendlyByteBuf, IntParticlData> streamCodec = StreamCodec.unit(this);
//
//    public static MapCodec<IntParticlData> codec(ParticleType<IntParticlData> type) {
//        return Codec.INT.listOf().xmap(intList -> new IntParticlData(type, intList), data -> data.data).fieldOf("data");
//    }
//
//    public static StreamCodec<? super ByteBuf, IntParticlData> streamCodec(ParticleType<IntParticlData> type) {
//        return INT_LIST.map(ints -> new IntParticlData(type, ints), data -> data.data);
//    }
//
//    public static final StreamCodec<RegistryFriendlyByteBuf, VibrationParticleOption> STREAM_CODEC = StreamCodec.composite(
//            Codec.BOOL,
//            VibrationParticleOption::getDestination,
//            ByteBufCodecs.VAR_INT,
//            VibrationParticleOption::getArrivalInTicks,
//            VibrationParticleOption::new
//    );
//
//
////    private static final Codec<PositionSource> SAFE_POSITION_SOURCE_CODEC = PositionSource.CODEC
////            .validate(
////                    p_340622_ -> p_340622_ instanceof EntityPositionSource
////                            ? DataResult.error(() -> "Entity position sources are not allowed")
////                            : DataResult.success(p_340622_)
////            );
////    public static final MapCodec<VibrationParticleOption> CODEC = RecordCodecBuilder.mapCodec(
////            p_340623_ -> p_340623_.group(
////                            SAFE_POSITION_SOURCE_CODEC.fieldOf("destination").forGetter(VibrationParticleOption::getDestination),
////                            Codec.INT.fieldOf("arrival_in_ticks").forGetter(VibrationParticleOption::getArrivalInTicks)
////                    )
////                    .apply(p_340623_, VibrationParticleOption::new)
////    );
////    public static final StreamCodec<RegistryFriendlyByteBuf, VibrationParticleOption> STREAM_CODEC = StreamCodec.composite(
////            PositionSource.STREAM_CODEC,
////            VibrationParticleOption::getDestination,
////            ByteBufCodecs.VAR_INT,
////            VibrationParticleOption::getArrivalInTicks,
////            VibrationParticleOption::new
////    );
//
//    public IntParticlData(boolean overrideLimiter, Integer... data) {
//        super(overrideLimiter);
//        this.data = List.of(data);
//    }
//
//    public IntParticlData(boolean overrideLimiter, List<Integer> data) {
//        super(overrideLimiter);
//        this.data = data;
//    }
//
//    @Override
//    public IntParticlData getType() {
//        return this;
//    }
//
//    @Override
//    public MapCodec<IntParticlData> codec() {
//        return null;
//    }
//
//    @Override
//    public StreamCodec<? super RegistryFriendlyByteBuf, IntParticlData> streamCodec() {
//        return null;
//    }
//}

public class IntParticleData implements ParticleOptions {
    private final ParticleType<IntParticleData> type;
    private final List<Integer> data;

    private static final StreamCodec<ByteBuf, List<Integer>> INT_LIST = new StreamCodec<>() {
        public List<Integer> decode(ByteBuf buf) {
            List<Integer> result = new ArrayList<>();
            int count = buf.readInt();
            for (int i = 0; i < count; i++) {
                result.add(buf.readInt());
            }
            return result;
        }

        public void encode(ByteBuf buf, List<Integer> list) {
            buf.writeInt(list.size());
            list.forEach(buf::writeInt);
        }
    };

    public static MapCodec<IntParticleData> codec(ParticleType<IntParticleData> type) {
        return Codec.INT.listOf().xmap(intList -> new IntParticleData(type, intList), data -> data.data).fieldOf("data");
    }

    public static StreamCodec<? super ByteBuf, IntParticleData> streamCodec(ParticleType<IntParticleData> type) {
        return INT_LIST.map(ints -> new IntParticleData(type, ints), data -> data.data);
    }

    public IntParticleData(ParticleType<IntParticleData> type, Integer... data) {
        this.type = type;
        this.data = List.of(data);
    }

    public IntParticleData(ParticleType<IntParticleData> type, List<Integer> data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public ParticleType<IntParticleData> getType() {
        return type;
    }

    public List<Integer> get() {
        return data;
    }
}