/**
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015 by the LizardIRC Development Team. Some rights reserved.
 *
 * License GPLv3+: GNU General Public License version 3 or later (at your choice):
 * <http://gnu.org/licenses/gpl.html>. This is free software: you are free to
 * change and redistribute it at your will provided that your redistribution, with
 * or without modifications, is also licensed under the GNU GPL. (Although not
 * required by the license, we also ask that you attribute us!) There is NO
 * WARRANTY FOR THIS SOFTWARE to the extent permitted by law.
 *
 * Note that this is an official project of the LizardIRC IRC network.  For more
 * information about LizardIRC, please visit our website at
 * <https://www.lizardirc.org>.
 *
 * This is an open source project. The source Git repositories, which you are
 * welcome to contribute to, can be found here:
 * <https://gerrit.fastlizard4.org/r/gitweb?p=LizardIRC%2FBeancounter.git;a=summary>
 * <https://git.fastlizard4.org/gitblit/summary/?r=LizardIRC/Beancounter.git>
 *
 * Gerrit Code Review for the project:
 * <https://gerrit.fastlizard4.org/r/#/q/project:LizardIRC/Beancounter,n,z>
 *
 * Alternatively, the project source code can be found on the PUBLISH-ONLY mirror
 * on GitHub: <https://github.com/LizardNet/LizardIRC-Beancounter>
 *
 * Note: Pull requests and patches submitted to GitHub will be transferred by a
 * developer to Gerrit before they are acted upon.
 */

package org.lizardirc.beancounter;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandListener;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;

public class SlapListener<T extends PircBotX> extends CommandListener<T> {
    private static final String CMD_SLAP = "slap";
    private static final String CMD_CFG = "cfgslap";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_SLAP, CMD_CFG);

    private static final String CMD_CFG_ADD = "add";
    private static final String CMD_CFG_LIST = "list";
    private static final String CMD_CFG_REMOVE = "remove";
    private static final Set<String> CFG_OPERATIONS = ImmutableSet.of(CMD_CFG_ADD, CMD_CFG_LIST, CMD_CFG_REMOVE);

    private static final String CMD_CFG_ACTIONS = "actions";
    private static final String CMD_CFG_MODIFIERS = "modifiers";
    private static final String CMD_CFG_ITEMS = "items";
    private static final String CMD_CFG_ITEM_MODS = "item_mods";
    private static final Set<String> CFG_TARGETS = ImmutableSet.of(CMD_CFG_ACTIONS, CMD_CFG_MODIFIERS, CMD_CFG_ITEMS, CMD_CFG_ITEM_MODS);

    private static final String PERSIST_ACTIONS = "actions";
    private static final String PERSIST_MODIFIERS = "modifiers";
    private static final String PERSIST_ITEMS = "items";
    private static final String PERSIST_ITEM_MODS = "item_mods";

    private static final String PRIV_CFG = "cfgslap";

    private static final Random random = new Random();

    private final PersistenceManager pm;
    private final AccessControl acl;

    private final List<String> actions;
    private final List<String> modifiers;
    private final List<String> items;
    private final List<String> item_mods;

    public SlapListener(PersistenceManager pm, AccessControl acl) {
        this.pm = pm;
        this.acl = acl;

        actions = pm.getList(PERSIST_ACTIONS);
        if (actions.isEmpty()) {
            actions.add("slaps");
            actions.add("whacks");
        }

        modifiers = pm.getList(PERSIST_MODIFIERS);
        if (modifiers.isEmpty()) {
            modifiers.add("");
            modifiers.add("around a bit");
        }

        items = pm.getList(PERSIST_ITEMS);
        if (items.isEmpty()) {
            items.add("a%s trout");
            items.add("a%s minnow");
            items.add("a%s whale");
            items.add("a%s can of sardines");
            items.add("a%s leather belt");
            items.add("Donald Trump's%s combover");
        }

        item_mods = pm.getList(PERSIST_ITEM_MODS);
        if (item_mods.isEmpty()) {
            item_mods.add("");
            item_mods.add("large");
            item_mods.add("feisty");
            item_mods.add("moderately sized");
            item_mods.add("cursed [-1]");
            item_mods.add("4 str 4 stam");
            item_mods.add("talking");
            item_mods.add("energetic");
        }
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }

        if (CMD_CFG.equals(commands.get(0))) {
            switch (commands.size()) {
                case 1:
                    return CFG_OPERATIONS;
                case 2:
                    return CFG_TARGETS;
                default:
                    return Collections.emptySet();
            }
        }

        if (!(event instanceof GenericChannelEvent)) {
            return Collections.emptySet();
        }
        GenericChannelEvent gce = (GenericChannelEvent) event;
        return gce.getChannel().getUsers().stream()
            .map(User::getNick)
            .collect(Collectors.toSet());
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() == 0) {
            return;
        }

        switch (commands.get(0)) {
            case CMD_SLAP:
                String target = event.getUser().getNick();
                if (commands.size() >= 2) {
                    target = commands.get(1);
                }
                String channel = event.getUser().getNick();
                if (event instanceof GenericChannelEvent) {
                    channel = ((GenericChannelEvent) event).getChannel().getName();
                }

                String action = actions.get(random.nextInt(actions.size()));
                String modifier = modifiers.get(random.nextInt(modifiers.size()));
                String item = items.get(random.nextInt(items.size()));
                String itemMod = item_mods.get(random.nextInt(item_mods.size()));
                // TODO support "a feisty minnow", "an energetic minnow", "Donald Trump's energetic minnow"
                String completed = action + " " + target + " " + modifier + " with " + String.format(item, " " + itemMod);
                event.getBot().sendIRC().action(channel, completed.replaceAll(" +", " "));
                break;
            case CMD_CFG:
                handleCfgCommand(event, commands, remainder);
                break;
        }
    }

    private void sync() {
        pm.setList(PERSIST_ACTIONS, actions);
        pm.setList(PERSIST_MODIFIERS, modifiers);
        pm.setList(PERSIST_ITEMS, items);
        pm.setList(PERSIST_ITEM_MODS, item_mods);
        pm.sync();
    }

    private synchronized void handleCfgCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (!acl.hasPriv(event, PRIV_CFG)) {
            event.respond("No u!  (You don't have the necessary permissions to use this command.)");
            return;
        }

        if (commands.size() < 3) {
            event.respond("Error: Too few or invalid arguments for ?cfgslap");
            event.respond("syntax: cfgslap <add|list|remove> <actions|modifiers|items|item_mods> [what]");
            return;
        }

        if (remainder == null) {
            remainder = "";
        } else {
            remainder = remainder.trim();
        }

        List<String> list;
        switch (commands.get(2)) {
            case CMD_CFG_ACTIONS:
                list = actions;
                break;
            case CMD_CFG_MODIFIERS:
                list = modifiers;
                break;
            case CMD_CFG_ITEMS:
                list = items;
                break;
            case CMD_CFG_ITEM_MODS:
                list = item_mods;
                break;
            default:
                event.respond("Error: Invalid argument.  What list do you want to operate on?");
                event.respond("Syntax: cfgslap <add|list|remove> <actions|modifiers|items|item_mods> [what]");
                return;
        }

        String targetName = commands.get(2);
        targetName = targetName.substring(0, targetName.length() - 1);

        switch (commands.get(1)) {
            case CMD_CFG_ADD:
                if (remainder.isEmpty()) {
                    event.respond("Error: Too few arguments.  What do you want to add?");
                    event.respond("syntax: cfgslap <add|list|remove> <actions|modifiers|items|item_mods> [what]");
                    return;
                }

                list.add(remainder);
                sync();
                event.respond("New " + targetName + " remembered!");
                break;
            case CMD_CFG_LIST:
                event.respond("I know the following " + targetName + "s");
                list.forEach(event::respond);
                break;
            case CMD_CFG_REMOVE:
                if (remainder.isEmpty()) {
                    event.respond("Error: Too few arguments.  What do you want to remove?");
                    event.respond("syntax: cfgslap <add|list|remove> <actions|modifiers|items|item_mods> [what]");
                    return;
                }
                if (list.remove(remainder)) {
                    event.respond(targetName + " successfully forgotten.");
                    sync();
                } else {
                    event.respond("Unable to comply: I didn't know that " + targetName + " in the first place!");
                }
                break;
        }
    }
}
