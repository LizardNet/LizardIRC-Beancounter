/*
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

package org.lizardirc.beancounter.commands.roulette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputChannel;
import org.pircbotx.output.OutputIRC;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.utils.Strings;

public class RouletteHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final Set<String> COMMANDS = ImmutableSet.of("poulette", "reload", "roulette", "spin");
    private static final int MAX_CHAMBERS = 64;
    private static final int DEFAULT_BULLETS = 1;
    private static final int DEFAULT_CHAMBERS = 6;

    private static Random random = new Random();

    private List<Boolean> loaded = new ArrayList<>();
    private int lastBullets = DEFAULT_BULLETS;

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }
        return Collections.emptySet();
    }

    @Override
    public synchronized void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() == 0) {
            return;
        }
        String channel = event.getUser().getNick();
        OutputChannel outChan = null;
        if (event instanceof GenericChannelEvent) {
            Channel chan = ((GenericChannelEvent) event).getChannel();
            channel = chan.getName();
            outChan = new OutputChannel(event.getBot(), chan);
        }

        OutputIRC outIrc = event.getBot().sendIRC();
        String outChannel = channel;
        Consumer<String> action = s -> {
            outIrc.action(outChannel, s);
            sleep();
        };
        Consumer<String> message = s -> {
            outIrc.message(outChannel, s);
            sleep();
        };

        String target = event.getUser().getNick();

        switch (commands.get(0)) {
            case "poulette":
                message.accept(target + " picks up a chicken, points it at their head, pulls a feather, and...");
                message.accept("*BWACK*!");
                break;
            case "reload":
                reload(action, remainder.split(" "));
                break;
            case "roulette":
                if (!loaded.contains(true)) {
                    if (!loaded.isEmpty()) { // don't say this the first time playing
                        action.accept("notices the gun feels rather light. The chamber is empty!");
                    }
                    if (lastBullets > 0) {
                        reload(action);
                    } else {
                        message.accept("ಠ_ಠ " + target);
                    }
                }

                message.accept(target + " picks up the gun, points it at their head, pulls the trigger, and...");

                loaded.add(false);
                if (loaded.remove(0) || lastBullets == 0) {
                    if (lastBullets == 0) {
                        message.accept("The suspense is killing " + target + "!");
                    }

                    if (outChan != null) {
                        outChan.kick(event.getUser(), "*BANG*! You're dead, Jim!");
                    }
                    message.accept("*BANG*! You're dead, Jim!");
                } else {
                    message.accept("*CLICK*");
                }
                break;
            case "spin":
                action.accept("spins the barrel");
                spin();
                break;
        }
    }

    private void reload(Consumer<String> action, String... args) {
        long removedBullets = loaded.stream().filter(Boolean::booleanValue).count();
        if (removedBullets > 0) {
            String pluralized = removedBullets == 1 ? " bullet falls out." : " bullets fall out.";
            action.accept("empties the gun. " + removedBullets + pluralized);
        }

        int bullets = lastBullets;
        int chambers = loaded.size();
        if (chambers == 0) {
            chambers = DEFAULT_CHAMBERS;
        }
        try {
            bullets = Integer.parseInt(args[0]);
            chambers = Integer.parseInt(args[1]);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            // ignore
        }

        if (chambers < 1 || chambers > MAX_CHAMBERS) {
            action.accept("looks for a gun with " + chambers + " chambers, but can't find one.");
            chambers = 6;
        }
        action.accept((Strings.startsWithVowel(String.valueOf(chambers)) ? "grabs an " : "grabs a ") + chambers + "-chamber gun");

        if (bullets < 0) {
            if (bullets == -1) {
                action.accept("takes 1 bullet out of the empty gun and throws it away");
            } else {
                action.accept("takes " + -bullets + " bullets out of the empty gun and throws them away");
            }
            bullets = 0;
        } else if (bullets > chambers) {
            action.accept("puts " + bullets + " bullets into the gun. " + (bullets - chambers) + " fall out.");
            bullets = chambers;
        } else if (bullets > 0) {
            String pluralized = bullets == 1 ? " bullet" : " bullets";
            action.accept("puts " + bullets + pluralized + " into the gun");
        }

        lastBullets = bullets;

        loaded.clear();
        int i;
        for (i = 0; i < bullets; i++) {
            loaded.add(true);
        }
        for (; i < chambers; i++) {
            loaded.add(false);
        }
        Collections.shuffle(loaded);

        action.accept("spins the barrel");
    }

    private void spin() {
        for (int i = random.nextInt(loaded.size()); i > 0; i--) {
            loaded.add(loaded.remove(0));
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(1000 + random.nextInt(2000));
        } catch (InterruptedException e) {
            // oh well
        }
    }
}
