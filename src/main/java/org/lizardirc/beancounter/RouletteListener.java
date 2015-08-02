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
import java.util.stream.Collectors;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputChannel;

import org.lizardirc.beancounter.hooks.CommandListener;

public class RouletteListener<T extends PircBotX> extends CommandListener<T> {
    private static final Set<String> COMMANDS = ImmutableSet.of("reload", "roulette", "spin");

    private static Random random = new Random();

    private List<Boolean> loaded = new ArrayList<>();

    public RouletteListener() {
        reload(1, 6);
    }

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
        OutputChannel outChan = null;
        if (event instanceof GenericChannelEvent) {
            Channel chan = ((GenericChannelEvent) event).getChannel();
            channel = chan.getName();
            outChan = new OutputChannel(event.getBot(), chan);
        }
        switch (commands.get(0)) {
            case "reload":
                long removedBullets = loaded.stream().filter(Boolean::booleanValue).count();
                if (removedBullets > 0) {
                    String pluralized = removedBullets == 1 ? " bullet falls out." : " bullets fall out.";
                    event.getBot().sendIRC().action(channel, "empties the gun. " + removedBullets + pluralized);
                    sleep();
                }

                String[] args = remainder.split(" ");
                int bullets = 1;
                int chambers = 6;
                try {
                    bullets = Integer.parseInt(args[0]);
                    chambers = Integer.parseInt(args[1]);
                } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                    // ignore
                }

                if (chambers < 1 || chambers > 32) {
                    event.getBot().sendIRC().action(channel, "looks for a gun with " + chambers + " chambers, but can't find one.");
                    chambers = 6;
                    sleep();
                }
                event.getBot().sendIRC().action(channel, "picks up a " + chambers + "-chamber gun");
                sleep();

                if (bullets < 0) {
                    String pluralized = bullets == -1 ? " bullet" : " bullets";
                    event.getBot().sendIRC().action(channel, "takes " + (-bullets) + pluralized + " out of the empty gun and throws them away");
                    bullets = 0;
                    sleep();
                } else if (bullets > chambers) {
                    event.getBot().sendIRC().action(channel, "puts " + bullets + " bullets into the gun. " + (bullets - chambers) + " fall out.");
                    bullets = chambers;
                    sleep();
                } else if (bullets > 0) {
                    String pluralized = bullets == 1 ? " bullet" : " bullets";
                    event.getBot().sendIRC().action(channel, "puts " + bullets + pluralized + " into the gun");
                    sleep();
                }
                reload(bullets, chambers);

                event.getBot().sendIRC().action(channel, "spins the barrel");
                sleep();
                break;
            case "roulette":
                String target = event.getUser().getNick();
                event.getBot().sendIRC().message(channel, target + " puts the gun to eir head, pulls the trigger, and...");
                loaded.add(false);
                sleep();
                if (loaded.remove(0)) {
                    event.getBot().sendIRC().message(channel, "*BANG*! You're dead, Jim!");
                    if (outChan != null) {
                        outChan.kick(event.getUser(), "*BANG*! You're dead, Jim!");
                    }
                } else {
                    event.getBot().sendIRC().message(channel, "*CLICK*");
                }
                sleep();
                break;
            case "spin":
                event.getBot().sendIRC().action(channel, "spins the barrel");
                spin();
                sleep();
                break;
        }
    }

    private void reload(int bullets, int chambers) {
        loaded.clear();
        int i;
        for (i = 0; i < bullets; i++) {
            loaded.add(true);
        }
        for (; i < chambers; i++) {
            loaded.add(false);
        }
        Collections.shuffle(loaded);
    }

    private void spin() {
        for (int i = random.nextInt(loaded.size()); i > 0; i++) {
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

    public static class Provider<T extends PircBotX> implements Supplier<RouletteListener<T>> {
        public RouletteListener<T> get() {
            return new RouletteListener<T>();
        }
    }
}
