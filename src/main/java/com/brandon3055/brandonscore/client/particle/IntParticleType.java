//package com.brandon3055.brandonscore.client.particle;
//
//import net.minecraft.core.particles.ParticleType;
//import net.minecraft.network.RegistryFriendlyByteBuf;
//import net.minecraft.network.codec.StreamCodec;
//
///**
// * Created by brandon3055 on 27/2/20.
// */
////Replace with ParticleType<IntParticleData>
//public class IntParticleType extends ParticleType<IntParticleData> {
////    private final MapCodec<IntParticleType> codec = MapCodec.unit(this::getType);
//    private final StreamCodec<RegistryFriendlyByteBuf, IntParticleType> streamCodec = StreamCodec.unit(this);
//
//
////    private static ParticleOptions.Deserializer<IntParticleData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
////        @Override
////        public IntParticleData fromCommand(ParticleType<IntParticleData> particleTypeIn, StringReader reader) throws CommandSyntaxException {
////            List<Integer> list = new ArrayList<>();
////            while (reader.peek() == ' ') {
////                reader.expect(' ');
////                list.add((int) reader.readInt());
////            }
////
////            return new IntParticleData(particleTypeIn, DataUtils.toPrimitive(list.toArray(new Integer[0])));
////        }
////
////        @Override
////        public IntParticleData fromNetwork(ParticleType<IntParticleData> particleTypeIn, FriendlyByteBuf buffer) {
////            return new IntParticleData(particleTypeIn, buffer.readByte());
////        }
////    };
////
////    public IntParticleType(boolean alwaysShow) {
////        super(alwaysShow, DESERIALIZER);
////    }
////
////    @Override
////    public Codec<IntParticleData> codec() {
////        return null;
////    }
////
////    @Override
////    public StreamCodec<? super RegistryFriendlyByteBuf, IntParticleData> streamCodec() {
////        return null;
////    }
//
//}
