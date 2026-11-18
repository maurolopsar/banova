package dev.banova.core.platform;

import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;

/**
 * Hooks used by commands to open menus when the platform supports them.
 * All methods may be called from any thread.
 */
public interface GuiBridge {

    void openMain(CommandActor viewer);

    void openPlayer(CommandActor viewer, PlayerRecord target);

    void openHistory(CommandActor viewer, PlayerRecord target);

    void openNotes(CommandActor viewer, PlayerRecord target);

    void openPunishment(CommandActor viewer, Punishment punishment);

    void openAppeals(CommandActor viewer);

    void openList(CommandActor viewer, dev.banova.api.PunishmentType type);

    void openAudit(CommandActor viewer);

    void openAlts(CommandActor viewer, PlayerRecord target);
}
