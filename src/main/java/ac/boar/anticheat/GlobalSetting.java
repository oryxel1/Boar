package ac.boar.anticheat;

// https://github.com/Mojang/bedrock_protocol_docs/blob/main/additional_docs/AntiCheatServer.properties
public class GlobalSetting {
    public static boolean REWIND_INFO_DEBUG = true;
    public static int REWIND_HISTORY_SIZE_TICKS = 40;
    public static int TICKS_TILL_FORCE_REWIND = 4;
    public static double PLAYER_POSITION_ACCEPTANCE_THRESHOLD = 1.0E-4;
}
