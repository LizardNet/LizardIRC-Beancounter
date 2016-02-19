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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;

public class DiceHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final Set<String> COMMANDS = ImmutableSet.of("coin", "dice", "more", "roll");
    private static final Pattern DESCRIPTOR = Pattern.compile("(?:([0-9]+)?d)?([0-9]+)");

    private static Random random = new Random();

    private String moreInfo = null;

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }
        return Collections.<String>emptySet();
    }

    @Override
    public synchronized void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() == 0) {
            return;
        }
        String channel = event.getUser().getNick();
        if (event instanceof GenericChannelEvent) {
            channel = ((GenericChannelEvent) event).getChannel().getName();
        }
        String action;
        String requestor = event.getUser().getNick();
        switch (commands.get(0)) {
            case "coin":
                String result;
                int flip = random.nextInt(11);

                if (flip <= 4) {
                    result = "heads";
                } else if (flip <= 9) {
                    result = "tails";
                } else {
                    result = "FILE_NOT_FOUND";
                }

                action = "flips a coin for " + requestor + ". It lands on " + result + '.';
                event.getBot().sendIRC().action(channel, action);
                break;
            case "dice":
            case "roll":
                try {
                    List<Integer> dieSizes = parseDieSizes(remainder);
                    switch (dieSizes.size()) {
                        case 0:
                            action = "rolls no dice for %s. Their total is 0.";
                            break;
                        case 1:
                            action = "rolls the die for %s. It lands on %d.";
                            break;
                        default:
                            action = "rolls the dice for %s. Their total is %d.";
                            break;
                    }
                    int total = rollDice(dieSizes);
                    event.getBot().sendIRC().action(channel, String.format(action, requestor, total));
                } catch (IllegalArgumentException e) {
                    event.respond(e.getMessage());
                }
                break;
            case "more":
                if (moreInfo != null) {
                    event.respond(moreInfo);
                    moreInfo = null;
                }
                break;
        }
    }

    private List<Integer> parseDieSizes(String descriptors) {
        List<Integer> ret = new ArrayList<>();

        if (descriptors.trim().isEmpty()) {
            ret.add(6);
            return ret;
        }

        for (String descriptor : descriptors.split(" ")) {
            descriptor = descriptor.trim();
            if (descriptor.isEmpty()) {
                continue;
            }

            Matcher m = DESCRIPTOR.matcher(descriptor);
            if (!m.matches()) {
                throw new IllegalArgumentException("I don't know what kind of die '" + descriptor + "' is.");
            }

            int dieCount = 1;
            int dieSize = Integer.parseInt(m.group(2));
            if (m.group(1) != null) {
                dieCount = Integer.parseInt(m.group(1));
            }

            for (int i = 0; i < dieCount; i++) {
                ret.add(dieSize);
            }
        }

        return ret;
    }

    private int rollDice(List<Integer> sizes) {
        StringBuilder moreBuilder = new StringBuilder("I rolled these numbers: ");
        boolean first = true;
        int total = 0;

        for (Integer size : sizes) {
            if (first) {
                first = false;
            } else {
                moreBuilder.append(", ");
            }

            int roll = random.nextInt(size) + 1;
            moreBuilder.append(roll);
            total += roll;
        }

        moreInfo = moreBuilder.toString();
        return total;
    }
}
