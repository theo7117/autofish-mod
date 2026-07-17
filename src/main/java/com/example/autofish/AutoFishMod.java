package com.example.autofish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.regex.Pattern;

public class AutoFishMod implements ClientModInitializer {

    /** Etat: l'autofish est-il actif ? */
    public static boolean enabled = false;

    /** La canne était-elle en train de pêcher (bobber lancé) au tick précédent ? */
    private static boolean wasFishing = false;

    /** Cooldown après une capture, avant de relancer la canne */
    private static int cooldownTicks = 0;

    /**
     * Vrai pendant les quelques ticks où c'est LE MOD qui simule un clic droit
     * (récupération du poisson + relance de la canne). Permet au mixin de ne pas
     * confondre cette action avec un vrai clic du joueur.
     */
    private static boolean internalAction = false;

    /**
     * Regex tolérante pour détecter le message d'action bar de morsure du poisson,
     * du style "Tu as 2 secondes pour pêcher ce poisson !"
     */
    private static final Pattern BITE_PATTERN = Pattern.compile(
            "(?i).*\\bsecondes?\\b.*\\bp[êe]cher\\b.*|.*\\bp[êe]cher\\b.*\\bsecondes?\\b.*"
    );

    @Override
    public void onInitializeClient() {
        // Ecoute des messages d'action bar (au-dessus de la barre d'XP)
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) {
                handleActionBarMessage(message);
            }
            return true;
        });

        // Tick client : gère le cooldown, la relance de canne, et la désactivation
        // automatique si le joueur retire la canne lui-même.
        ClientTickEvents.END_CLIENT_TICK.register(AutoFishMod::onClientTick);
    }

    private void handleActionBarMessage(Text message) {
        if (!enabled) return;

        String content = message.getString();
        if (BITE_PATTERN.matcher(content).matches()) {
            triggerFishCatch();
        }
    }

    /** Simule le clic droit pour récupérer le poisson, puis programme la relance */
    private void triggerFishCatch() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        if (cooldownTicks > 0) return;

        internalAction = true;
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        internalAction = false;

        // On attend quelques ticks (le temps que l'animation de récupération se
        // termine et que le bobber disparaisse) avant de relancer la canne.
        cooldownTicks = 6;
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.player == null) {
            wasFishing = false;
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            if (cooldownTicks == 0 && enabled) {
                relaunchRod(client);
            }
            wasFishing = isPlayerFishing(client);
            return;
        }

        if (!enabled) {
            wasFishing = false;
            return;
        }

        // NB: la désactivation suite à un retrait manuel de la canne est gérée
        // directement dans le mixin (ClientPlayerInteractionManagerMixin) pour
        // une réaction immédiate dès le clic ; ce tick ne fait que garder l'état
        // "wasFishing" synchronisé pour les autres vérifications.
        wasFishing = isPlayerFishing(client);
    }

    /** Relance la canne à l'eau après capture (simule un nouveau clic droit) */
    private static void relaunchRod(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;
        ItemStack heldItem = client.player.getMainHandStack();
        if (heldItem.getItem() instanceof FishingRodItem) {
            internalAction = true;
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            internalAction = false;
        }
    }

    /** Vrai si le joueur a actuellement un hameçon (bobber) lancé dans le monde */
    private static boolean isPlayerFishing(MinecraftClient client) {
        if (client.player == null) return false;
        FishingBobberEntity bobber = client.player.fishHook;
        return bobber != null && bobber.isAlive();
    }

    public static boolean isInternalAction() {
        return internalAction;
    }

    public static void markUserToggleOn(MinecraftClient client) {
        wasFishing = false; // la canne vient d'être lancée par le clic vanilla
        notifyStatus(client, true);
    }

    public static void markUserToggleOff(MinecraftClient client) {
        notifyStatus(client, false);
    }

    private static void notifyStatus(MinecraftClient client, boolean isEnabled) {
        if (client.player == null) return;
        String msg = isEnabled ? "§a[AutoFish] Activé - la canne va pêcher automatiquement" : "§c[AutoFish] Désactivé";
        client.player.sendMessage(Text.literal(msg), true); // true = action bar
    }
}
