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
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.google.common.base.Supplier;
import com.google.common.collect.EvictingQueue;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

public class SedListener<T extends PircBotX> extends ListenerAdapter<T> {
    // There are several substring types that we match.
    // A) any character except \
    // B) 0 or more copies of either an escaped character, or something other than \
    // C) The delimiter, as a backreference
    // We match s, followed by A. This is the delimiter.
    // We then match B. This is the search regex.
    // We match C.
    // We match B. This is the replacement string.
    // We match C again.
    // We match B. These are the options.
    private static final String REGEX_B = "((\\\\.|[^\\\\])*)";
    private static final String REGEX_AB = "s([^\\\\])" + REGEX_B;
    private static final String REGEX_CB = "\\1" + REGEX_B;
    private static final String REGEX_ABCBCB = REGEX_AB + REGEX_CB + REGEX_CB;
    private static final Pattern PATTERN_SED = Pattern.compile(REGEX_ABCBCB);

    private final Queue<String> window;

    public SedListener(int windowSize) {
        window = EvictingQueue.create(windowSize);
    }

    @Override
    public void onMessage(MessageEvent<T> event) {
        String message = event.getMessage();
        Matcher m = PATTERN_SED.matcher(message);
        if (m.matches()) {
            String regex = m.group(2);
            String replacement = m.group(4);
            String options = m.group(6);
            int flags = 0;
            if (options.contains("i")) {
                flags = Pattern.CASE_INSENSITIVE;
            }

            Pattern p;
            try {
                p = Pattern.compile(regex, flags);
            } catch (PatternSyntaxException e) {
                event.respond("Invalid regex '" + regex + "': " + e.getMessage());
                return;
            }

            window.stream()
                    .map(p::matcher)
                    .filter(Matcher::find)
                    .reduce((x, y) -> y) // findLast()
                    .map(x -> {
                        StringBuilder sb = new StringBuilder(event.getUser().getNick());
                        sb.append(" meant to say: ");
                        if (options.contains("g")) {
                            sb.append(x.replaceAll(replacement));
                        } else {
                            sb.append(x.replaceFirst(replacement));
                        }
                        return sb.toString();
                    })
                    .ifPresent(event.getChannel().send()::message);
        } else {
            window.add(message);
        }
    }

    public static class Provider<T extends PircBotX> implements Supplier<SedListener<T>> {
        private final int windowSize;

        public Provider() {
            windowSize = 10;
        }

        public Provider(int windowSize) {
            this.windowSize = windowSize;
        }

        public SedListener<T> get() {
            return new SedListener<>(windowSize);
        }
    }
}
