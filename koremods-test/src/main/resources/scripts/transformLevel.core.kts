import codes.som.koffee.insns.jvm.getstatic
import codes.som.koffee.insns.jvm.lreturn
import codes.som.koffee.types.long

transformers { 
    method(
        "net.minecraft.world.level.Level",
        "m_46468_", // getDayTime
        constructMethodDescriptor(long),
        ::transformGetDayTime
    )
}

fun transformGetDayTime(node: MethodNode) {
    node.insert { 
        getstatic("wtf/gofancy/koremods/test/KoremodsGameTests", "EXPECTED_DAY_TIME", long)
        lreturn
    }
}