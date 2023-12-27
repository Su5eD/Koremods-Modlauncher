package wtf.gofancy.koremods.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder(KoremodsTestMod.MODID)
public class KoremodsGameTests {
    public static final long EXPECTED_DAY_TIME = 123456;

    @GameTest(template = "dummy")
    public void testInjectIntoMinecraftCode(GameTestHelper helper) {
        long dayTime = helper.getLevel().getDayTime();
        helper.assertTrue(dayTime == EXPECTED_DAY_TIME, "Day time doesn't match expected time " + EXPECTED_DAY_TIME);
        helper.succeed();
    }
}
