package com.example.autofish.mixin;

import com.example.autofish.AutoFishMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepte les clics droits (interactItem) du joueur pour piloter l'état
 * ON/OFF de l'AutoFish, sans jamais annuler le clic vanilla (la canne se
 * lance/se ramène normalement, le mod se contente d'observer).
 *
 * Règle :
 *  - clic droit alors que l'AutoFish est OFF et la canne pas encore à l'eau
 *    -> le clic lance la canne comme d'habitude ET active l'AutoFish.
 *  - clic droit alors que l'AutoFish est ON et la canne est à l'eau (donc
 *    c'est le joueur qui la retire lui-même, pas le mod) -> désactive
 *    l'AutoFish immédiatement.
 *  - clics simulés par le mod lui-même (récupération du poisson / relance
 *    automatique) -> ignorés grâce à AutoFishMod.isInternalAction().
 */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "interactItem", at = @At("HEAD"))
    private void autofish$onInteractItem(net.minecraft.entity.player.PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        // Si ce clic droit est simulé par le mod lui-même (récupération du poisson
        // ou relance de la canne), on ne touche surtout pas à l'état enabled.
        if (AutoFishMod.isInternalAction()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || hand != Hand.MAIN_HAND) return;

        ItemStack stack = client.player.getMainHandStack();
        if (!(stack.getItem() instanceof FishingRodItem)) return;

        boolean bobberActive = client.player.fishHook != null && client.player.fishHook.isAlive();

        if (!AutoFishMod.enabled && !bobberActive) {
            // La canne n'est pas encore à l'eau et l'autofish est OFF :
            // ce clic droit va lancer la canne (vanilla, non annulé) ET activer l'autofish.
            AutoFishMod.enabled = true;
            AutoFishMod.markUserToggleOn(client);
        } else if (AutoFishMod.enabled && bobberActive) {
            // L'autofish est ON et la canne est à l'eau : c'est le joueur qui
            // retire lui-même la canne -> on désactive l'autofish immédiatement.
            AutoFishMod.enabled = false;
            AutoFishMod.markUserToggleOff(client);
        }
    }
}
