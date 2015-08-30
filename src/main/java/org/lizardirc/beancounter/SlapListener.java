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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandListener;

public class SlapListener<T extends PircBotX> extends CommandListener<T> {
    private static final Set<String> COMMANDS = ImmutableSet.of
        ( "slap"
        );
    private static final List<String> ACTIONS = ImmutableList.of
        ( "slaps "
        , "whacks "
        );
    private static final List<String> MODIFIERS = ImmutableList.of
        ( ""
        , " around a bit"
        );
    private static final List<String> ITEMS = ImmutableList.of
        ( "a%s trout"
        , "a%s minnow"
        , "a%s whale"
        , "a%s can of sardines"
        , "a%s leather belt"
        , "Donald Trump's%s combover"
        );
    private static final List<String> ITEM_MODS = ImmutableList.of
        ( ""
        , " large"
        , " feisty"
        , " moderately sized"
        , " cursed [-1]"
        , " 4 str 4 stam"
        , " talking"
        , " energetic"
        );

    private static final Random random = new Random();

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
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
        String target = event.getUser().getNick();
        if (commands.size() >= 2) {
            target = commands.get(1);
        }
        String channel = event.getUser().getNick();
        if (event instanceof GenericChannelEvent) {
            channel = ((GenericChannelEvent) event).getChannel().getName();
        }

        String action = ACTIONS.get(random.nextInt(ACTIONS.size()));
        String modifier = MODIFIERS.get(random.nextInt(MODIFIERS.size()));
        String item = ITEMS.get(random.nextInt(ITEMS.size()));
        String itemMod = ITEM_MODS.get(random.nextInt(ITEM_MODS.size()));
        String completed = action + target + modifier + " with " + String.format(item, itemMod);
        event.getBot().sendIRC().action(channel, completed);
    }
}
