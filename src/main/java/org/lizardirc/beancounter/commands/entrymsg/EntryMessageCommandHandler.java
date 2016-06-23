package org.lizardirc.beancounter.commands.entrymsg;

import java.util.List;
import java.util.Set;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;

class EntryMessageCommandHandler<T extends PircBotX> implements CommandHandler<T> {
    private final EntryMessageListener<T> parentListener;

    public EntryMessageCommandHandler(EntryMessageListener<T> parentListener) {
        this.parentListener = parentListener;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        return null;
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {

    }
}
