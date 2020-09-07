/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2016 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.goat;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.security.AccessControl;

public class GoatHandler implements CommandHandler {
    private static final String CMD_GOAT = "goat";
    private static final String CMD_FGOAT = "fgoat";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_GOAT, CMD_FGOAT);

    private static final Random random = new Random();

    private final AccessControl acl;

    public GoatHandler(AccessControl acl) {
        this.acl = acl;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }
        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent event, List<String> commands, String remainder) {
        if (commands.isEmpty()) {
            return;
        }

        if (commands.size() == 1 && COMMANDS.contains(commands.get(0))) {
            final String botnick = event.getBot().getNick();
            final String user = event.getUser().getNick();
            String target;

            if (event instanceof GenericChannelEvent) {
                target = ((GenericChannelEvent) event).getChannel().getName();
            } else {
                target = user;
            }

            Consumer<String> response = s -> event.getBot().sendIRC().message(target, s);

            switch (random.nextInt(10)) {
                case 0:
                    event.respond("Do I *look* like lykos to you?");
                    break;
                case 1:
                    response.accept('\002' + botnick + "\002's goat walks by and does nothing because " + botnick + " is not \002lykos\002.");
                    break;
                case 2:
                    response.accept("Wow, such mindless violence.");
                    break;
                case 3:
                    event.respond("I may not be lykos, but I just want you to love me for what I am. :(");
                    break;
                case 4:
                    event.respond(new IllegalStateException("User has assumed that I am lykos").toString());
                    break;
                case 5:
                    remainder = remainder.trim();
                    if (remainder.isEmpty()) {
                        remainder = user;
                    }
                    response.accept('\002' + botnick + "\002's sheeps walk by and consume \002" + remainder + "\002, leaving behind nothing but a pile of bones.");
                    break;
                case 6:
                    response.accept("There is no goat, only Zuul.");
                    break;
                case 7:
                    event.respond("YOU AREN'T MY SUPERVISOR");
                    if (acl.hasPermission(event, "*")) {
                        event.respond("...Okay, maybe you are my supervisor, BUT STILL, IT'S THE PRINCIPLE OF THINGS");
                    }
                    break;
                case 8:
                    response.accept("!lynch " + user);
                    break;
                case 9:
                    response.accept("In their desperate attempt to bring goat-assisted violence upon their neighbor, \002" + user + "\002 inadvertantly triggers the heat death of the universe.");
                    response.accept("Good job, " + user + ".  I hope you have learned a valuable lesson.");
                    break;
            }
        }
    }
}
