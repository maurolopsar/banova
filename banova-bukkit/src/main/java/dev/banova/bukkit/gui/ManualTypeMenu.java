package dev.banova.bukkit.gui;

import dev.banova.api.PunishmentType;
import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.service.PunishmentService;
import org.bukkit.entity.Player;

final class ManualTypeMenu extends Menu {
    private final PlayerRecord target;

    ManualTypeMenu(BanovaBukkit plugin, Player viewer, Menu parent, PlayerRecord target) {
        super(plugin, viewer, parent);
        this.target = target;
    }

    @Override protected int rows() { return 3; }
    @Override protected String title() { return msg("gui.route.manual-sanction"); }

    @Override protected void render() {
        int slot = 10;
        for (var type : new PunishmentType[]{PunishmentType.WARN, PunishmentType.MUTE, PunishmentType.KICK, PunishmentType.BAN}) {
            if (PunishmentService.canCreate(actor, type, true, false) && (type != PunishmentType.KICK || core.platform().player(target.uuid()) != null)) {
                set(slot, Items.item(Icons.material(type), core.formatter().typeName(type), java.util.List.of(target.name())),
                        click -> new CustomMenu(plugin, viewer, this, target, type).open());
            }
            slot += 2;
        }
        backButton(22);
        fillEmpty();
    }
}
