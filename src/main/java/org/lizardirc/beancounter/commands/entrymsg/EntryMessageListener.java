package org.lizardirc.beancounter.commands.entrymsg;

import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.lizardirc.beancounter.hooks.CommandHandler;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;

import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.pircbotx.hooks.types.GenericChannelEvent;

public class EntryMessageListener<T extends PircBotX> extends ListenerAdapter<T> {
    /** This must match the type of the {@link #entryMessages} field */
    private static final Type PERSISTENCE_TYPE_TOKEN = new TypeToken<Map<String, EntryMessage>>(){}.getType();

    private static final String MESSAGE_OUTPUT_FORMAT = "%s (Set by %s (%s) at %s)";

    private final PersistenceManager pm;
    private final AccessControl<T> acl;
    private final Map<String, EntryMessage> entryMessages;
    private final EntryMessageCommandHandler<T> commandHandler;

    public EntryMessageListener(PersistenceManager pm, AccessControl<T> acl) {
        this.pm = pm;
        this.acl = acl;

        // Depersist state
        Gson gson = new Gson();
        Optional<String> serializedEntryMessages = pm.get("entryMessages");
        if (serializedEntryMessages.isPresent()) {
            Map<String, EntryMessage> entryMessages = gson.fromJson(serializedEntryMessages.get(), PERSISTENCE_TYPE_TOKEN);

            // Force all key strings to be lowercase, and arbitrarily discard the "old" item in case of collision
            this.entryMessages = new HashMap<>(entryMessages.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue, (o, n) -> n)));
        } else {
            entryMessages = new HashMap<>();
        }

        this.commandHandler = new EntryMessageCommandHandler<>(this);
    }

    private synchronized void sync() {
        Gson gson = new Gson();
        pm.set("entryMessages", gson.toJson(entryMessages, PERSISTENCE_TYPE_TOKEN));
        pm.sync();
    }

    public CommandHandler<T> getCommandHandler() {
        return commandHandler;
    }

    AccessControl<T> getAccessControl() {
        return acl;
    }

    void setEntryMessage(String channel, EntryMessage entryMessage) {
        entryMessages.put(channel.toLowerCase(), entryMessage);
        sync();
    }

    EntryMessage getEntryMessage(String channel) {
        return entryMessages.get(channel.toLowerCase());
    }

    void clearEntryMessage(String channel) {
        entryMessages.remove(channel.toLowerCase());
        sync();
    }

    String generateMessageString(GenericChannelEvent<?> event) {
        EntryMessage message = getEntryMessage(event.getChannel().getName());
        if (message != null) {
            String[] settingUser = message.getSettingUser().split("!");
            return String.format(MESSAGE_OUTPUT_FORMAT, message.getMessage(), settingUser[0], settingUser[1],
                message.getTimestamp().format(DateTimeFormatter.RFC_1123_DATE_TIME));
        } else {
            return null;
        }
    }

    @Override
    public void onJoin(JoinEvent<T> event) {
        if (event.getUser().equals(event.getBot().getUserBot())) {
            return;
        }

        String response = generateMessageString(event);

        if (response != null) {
            event.respond(generateMessageString(event));
        }
    }
}
