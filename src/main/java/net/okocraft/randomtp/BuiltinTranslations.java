package net.okocraft.randomtp;

import com.google.common.collect.ImmutableMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.TranslationStore;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

final class BuiltinTranslations {

    public static @NotNull TranslationStore<MessageFormat> createRegistry() {
        var registry = TranslationStore.messageFormat(Key.key("randomtp", "languages"));

        registry.registerAll(Locale.ENGLISH, en());
        registry.registerAll(Locale.JAPAN, jp());

        return registry;
    }

    private static @NotNull Map<String, MessageFormat> en() {
        return ImmutableMap.<String, MessageFormat>builder()
                .put("randomtp.command.not-player", en("This command can only be executed by the player."))
                .put("randomtp.no-permission", en("You don''t have the permission: {0}"))
                .put("randomtp.seconds", en("{0}s"))
                .put("randomtp.in-cooldown", en("Random teleportation is on cool down for {0}."))
                .put("randomtp.already-teleporting", en("You are already teleporting."))
                .put("randomtp.generating-random-location", en("Searching for a teleport destination, please wait a moment..."))
                .put("randomtp.safe-location-not-found", en("No safe location found. Please try again."))
                .put("randomtp.teleported", en("Random teleported!"))
                .put("randomtp.cannot-teleport", en("Something prevented teleportation. Please try again."))
                .put("randomtp.countdown", en("Teleports after {0}... Please do not move."))
                .put("randomtp.cancelled-due-to-move", en("Random teleportation was cancelled due to movement."))
                .put("randomtp.cancelled-due-to-change-world", en("Random teleportation was cancelled due to world change."))
                .build();
    }


    private static @NotNull Map<String, MessageFormat> jp() {
        return ImmutableMap.<String, MessageFormat>builder()
                .put("randomtp.command.not-player", jp("このコマンドはプレイヤーのみ実行できます。"))
                .put("randomtp.no-permission", jp("権限 {0} がありません。"))
                .put("randomtp.seconds", jp("{0}秒"))
                .put("randomtp.in-cooldown", jp("ランダムテレポートは{0}後に使用できます。"))
                .put("randomtp.already-teleporting", jp("すでにテレポート中です。"))
                .put("randomtp.generating-random-location", en("テレポート先を探しています...少しお待ちください..."))
                .put("randomtp.safe-location-not-found", jp("安全なテレポート先が見つかりませんでした。再度お試しください。"))
                .put("randomtp.teleported", jp("テレポートしました!"))
                .put("randomtp.cannot-teleport", jp("何かが原因でテレポートできませんでした。再度お試しください。"))
                .put("randomtp.countdown", jp("{0}後にテレポートします。動かないでください。"))
                .put("randomtp.cancelled-due-to-move", jp("動いたためランダムテレポートがキャンセルされました。"))
                .put("randomtp.cancelled-due-to-change-world", en("ワールドを移動したためランダムテレポートがキャンセルされました。"))
                .build();
    }

    private static @NotNull MessageFormat en(@NotNull String msg) {
        return new MessageFormat(msg, Locale.ENGLISH);
    }

    private static @NotNull MessageFormat jp(@NotNull String msg) {
        return new MessageFormat(msg, Locale.JAPAN);
    }

    private BuiltinTranslations() {
        throw new UnsupportedOperationException();
    }
}
