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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandListener;
import org.lizardirc.beancounter.persistence.PersistenceManager;

public class SlapListener<T extends PircBotX> extends CommandListener<T> {
    private final PersistenceManager pm;
    private final AccessControl acl;

    private final Set<String> COMMANDS = ImmutableSet.of
        ( "slap"
        , "cfgslap"
        );
    private final List<String> actions;
    private final List<String> modifiers;
    private final List<String> items;
    private final List<String> item_mods;

    private static final Random random = new Random();

    public SlapListener(PersistenceManager pm) {
        this.pm = pm;

        HashMap<String, String> accessList = new HashMap<>();
        accessList.put("^.*!.*@lizardirc/staff/.*$", "*");
        acl = new AccessControl(accessList);

        actions = new ArrayList<>(Arrays.asList(pm.get("actions", "").split("\\|")));
        if (actions.isEmpty() || (actions.size() == 1 && actions.toArray()[0].equals(""))) {
            actions.clear();
            actions.addAll(Arrays.asList
                ( "slaps "
                , "whacks "
                ));
        }

        modifiers = new ArrayList<>(Arrays.asList(pm.get("modifiers", "").split("\\|")));
        if (modifiers.isEmpty() || (modifiers.size() == 1 && modifiers.toArray()[0].equals(""))) {
            modifiers.clear();
            modifiers.addAll(Arrays.asList
                ( ""
                , " around a bit"
                ));
        }

        items = new ArrayList<>(Arrays.asList(pm.get("items", "").split("\\|")));
        if (items.isEmpty() || (items.size() == 1 && items.toArray()[0].equals(""))) {
            items.clear();
            items.addAll(Arrays.asList
                ( "a%s trout"
                , "a%s minnow"
                , "a%s whale"
                , "a%s can of sardines"
                , "a%s leather belt"
                , "Donald Trump's%s combover"
                ));
        }

        item_mods = new ArrayList<>(Arrays.asList(pm.get("item_mods", "").split("\\|")));
        if (item_mods.isEmpty() || (item_mods.size() == 1 && item_mods.toArray()[0].equals(""))) {
            item_mods.clear();
            item_mods.addAll(Arrays.asList
                ( ""
                , " large"
                , " feisty"
                , " moderately sized"
                , " cursed [-1]"
                , " 4 str 4 stam"
                , " talking"
                , " energetic"
                ));
        }

        sync();
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals("cfgslap")) {
            return ImmutableSet.of("add", "list", "remove");
        }

        if (commands.size() == 2 && commands.get(0).equals("cfgslap")) {
            return ImmutableSet.of("actions", "modifiers", "items", "item_mods");
        }

        if (!(event instanceof GenericChannelEvent)) {
            return Collections.<String>emptySet();
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
            case "slap":
                System.out.println("Got slap command!");
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
                String completed = action + target + modifier + " with " + String.format(item, itemMod);
                event.getBot().sendIRC().action(channel, completed);
                break;

            case "cfgslap":
                if (!acl.hasPriv(event, "cfgslap")) {
                    event.respond("No u!  (You don't have the necessary permissions to use this command.)");
                    return;
                }

                if (commands.size() < 3) {
                    event.respond("Error: Too few or invalid arguments for ?cfgslap");
                    event.respond("syntax: cfgslap <add|list|remove> <actions|modifiers|items|item_mods> [what]");
                    return;
                }

                switch (commands.get(1)) {
                    case "add":
                        if (remainder == null || remainder.trim().isEmpty()) {
                            event.respond("Error: Too few arguments.  What do you want to add?");
                            event.respond("syntax: cfgslap <add|list|remove> <actions|modifiers|items|item_mods> [what]");
                            return;
                        }

                        switch (commands.get(2)) {
                            case "actions":
                                actions.add(remainder.trim() + " ");
                                sync();
                                event.respond("New action remembered!");
                                break;
                            case "modifiers":
                                modifiers.add(" " + remainder.trim());
                                sync();
                                event.respond("New modifier remembered!");
                                break;
                            case "items":
                                items.add(remainder.trim());
                                sync();
                                event.respond("New item remembered!");
                                break;
                            case "item_mods":
                                item_mods.add(" " + remainder.trim());
                                sync();
                                event.respond("New item_mod remembered!");
                                break;
                        }
                        break;

                    case "list":
                        switch (commands.get(2)) {
                            case "actions":
                                event.respond("I know the following actions");
                                for (String eachAction : actions) {
                                    event.respond(eachAction);
                                }
                                break;
                            case "modifiers":
                                event.respond("I know the following modifiers");
                                for (String eachModifier : modifiers) {
                                    event.respond(eachModifier);
                                }
                                break;
                            case "items":
                                event.respond("I know the following items");
                                for (String eachItem : items) {
                                    event.respond(eachItem);
                                }
                                break;
                            case "item_mods":
                                event.respond("I know the following item_mods");
                                for (String eachItemMod : item_mods) {
                                    event.respond(eachItemMod);
                                }
                                break;
                        }
                        break;

                    case "remove":
                        if (remainder == null || remainder.trim().isEmpty()) {
                            event.respond("Error: Too few arguments.  What do you want to remove?");
                            event.respond("syntax: cfgslap <add|list|remove> <actions|modifiers|items|item_mods> [what]");
                            return;
                        }

                        switch (commands.get(2)) {
                            case "actions":
                                if (actions.remove(remainder.trim() + " ")) {
                                    event.respond("action successfully forgotten.");
                                    sync();
                                } else {
                                    event.respond("Unable to comply: I didn't know that action in the first place!");
                                }
                                break;
                            case "modifiers":
                                if (modifiers.remove(" " + remainder.trim())) {
                                    event.respond("modifier successfully forgotten.");
                                    sync();
                                } else {
                                    event.respond("Unable to comply: I didn't know that modifier in the first place!");
                                }
                                break;
                            case "items":
                                if (items.remove(remainder.trim())) {
                                    event.respond("item successfully forgotten.");
                                    sync();
                                } else {
                                    event.respond("Unable to comply: I didn't know that item in the first place!");
                                }
                                break;
                            case "item_mods":
                                if (item_mods.remove(" " + remainder.trim())) {
                                    event.respond("item_mod successfully forgotten.");
                                    sync();
                                } else {
                                    event.respond("Unable to comply: I didn't know that item_mod in the first place!");
                                }
                                break;
                        }
                        break;
                }
                break;
        }
    }

    private void sync() {
        String actionData = actions.stream().collect(Collectors.joining("|"));
        String modifierData = modifiers.stream().collect(Collectors.joining("|"));
        String itemData = items.stream().collect(Collectors.joining("|"));
        String itemModData = item_mods.stream().collect(Collectors.joining("|"));

        pm.set("actions", actionData);
        pm.set("modifiers", modifierData);
        pm.set("items", itemData);
        pm.set("item_mods", itemModData);

        pm.sync();
    }
}
