/**
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015-2016 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.sed;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.EvictingQueue;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class SedListener<T extends PircBotX> extends ListenerAdapter<T> {
    // There are several substring types that we match.
    // A) any character except \
    // B) 0 or more copies of either an escaped character, or something other than \
    // C) The delimiter, as a backreference
    // D) One or more non-whitespace characters, followed by a colon and a space
    // We optionally match D. This is the target ("Alice" in `Alice: s/foo/bar`)
    // We match the mode
    // We match A. This is the delimiter.
    // We then match B. This is the search regex.
    // We match C.
    // We match B. This is the replacement string.
    // We match C again.
    // We match B. These are the options.
    private static final String REGEX_MODE = "([ys])";
    private static final String REGEX_D = "(?:([^\\s]+): )?";
    private static final String REGEX_B = "((?:\\\\.|[^\\\\])*)";
    private static final String REGEX_AB = "([^\\\\\\sA-Za-z0-9])" + REGEX_B;
    private static final String REGEX_CB = "\\3" + REGEX_B;
    private static final String REGEX_SED = REGEX_D + REGEX_MODE + REGEX_AB + REGEX_CB + REGEX_CB;
    private static final String REGEX_BAD_SED = REGEX_D + "s/" + REGEX_B + "/" + REGEX_B;
    private static final Pattern PATTERN_SED = Pattern.compile(REGEX_SED);
    private static final Pattern PATTERN_BAD_SED = Pattern.compile(REGEX_BAD_SED);

    private static final Pattern PATTERN_OPTIONS = Pattern.compile("[gi]*");

    private final LoadingCache<User, Queue<UserMessage>> windows;
    private final ExecutorService executorService;

    public SedListener(ExecutorService executorService, int windowSize) {
        windows = CacheBuilder.newBuilder()
            .build(CacheLoader.from(() -> EvictingQueue.create(windowSize)));
        this.executorService = executorService;
    }

    @Override
    public void onGenericMessage(GenericMessageEvent<T> event) throws Exception {
        if (!(event instanceof GenericChannelEvent)) {
            return;
        }
        Channel channel = ((GenericChannelEvent) event).getChannel();

        String message = event.getMessage();
        Matcher m = PATTERN_SED.matcher(message);

        final UserMessageType messageType;
        if (event instanceof ActionEvent) {
            messageType = UserMessageType.ACTION;
        } else {
            messageType = UserMessageType.MESSAGE;
        }

        if (m.matches()) {
            if (messageType != UserMessageType.MESSAGE) {
                return;
            }

            String target = m.group(1);
            String regex = m.group(4);
            String replacement = m.group(5);
            String options = m.group(6);

            User corrector = event.getUser();
            User speaker = channel.getUsers().stream()
                .filter(u -> u.getNick().equalsIgnoreCase(target))
                .findFirst()
                .orElse(corrector);

            if (!PATTERN_OPTIONS.matcher(options).matches()) {
                event.respond("Invalid options '" + options + "'");
                return;
            }

            int flags = 0;
            if (options.contains("i")) {
                flags = Pattern.CASE_INSENSITIVE;
            }

            // The mode to operate in - either regex replace (s) or transliterate (y)
            String mode = m.group(2);

            Queue<UserMessage> window = windows.getUnchecked(speaker);
            Callable<Optional<UserMessage>> callable = null;

            switch (mode) {
                case "s":
                    Pattern p;
                    try {
                        p = Pattern.compile(regex, flags);
                    } catch (PatternSyntaxException e) {
                        event.respond("Invalid regex '" + regex + "': " + e.getMessage());
                        return;
                    }

                    callable = new RegexReplacementCallable(p, replacement, options.contains("g"), window);
                    break;
                case "y":
                    if (regex.length() != replacement.length()) {
                        event.respond("Strings for 'y' command are different lengths");
                        return;
                    }

                    if (!options.isEmpty()) {
                        event.respond("extra characters after command");
                        return;
                    }

                    callable = new TransliterationCallable(regex, replacement, window);
                    break;
                default:
                    // unknown or not implemented mode.
                    return;
            }

            Future<Optional<UserMessage>> future = executorService.submit(callable);

            try {
                Optional<UserMessage> response = future.get(5, TimeUnit.SECONDS);

                response.ifPresent(s -> {
                    window.add(s);

                    StringBuilder sb = new StringBuilder();
                    switch (s.getType()) {
                        case MESSAGE:
                            sb.append(corrector.getNick());
                            if (!corrector.equals(speaker)) {
                                sb.append(" thinks ").append(speaker.getNick());
                            }
                            sb.append(" meant to say: ");
                            break;
                        case ACTION:
                            if (!corrector.equals(speaker)) {
                                sb.append(corrector.getNick());
                                sb.append(" suggests a correction: * ");
                            } else {
                                sb = new StringBuilder("Correction: * ");
                            }
                            sb.append(speaker.getNick()).append(' ');
                            break;
                    }
                    channel.send().message(sb.append(s.getMessage()).toString());
                });
            } catch (TimeoutException e) {
                event.respond("Timeout while processing replacement.");
                System.err.println("WARNING: " + corrector.getNick() + " caused regex timeout with regex " + message + '.');
                if (!future.cancel(true)) {
                    event.respond("WARNING: Attempt to cancel pending regex operations DID NOT succeed.");
                }
            }
        } else {
            m = PATTERN_BAD_SED.matcher(message);
            if (m.matches()) {
                channel.send().action("slaps " + event.getUser().getNick() + " with a copy of the sed user manual");
                event.respond("Add a slash to the end, like this: s/foo/bar/");
            } else {
                windows.getUnchecked(event.getUser()).add(new UserMessage(messageType, message));
            }
        }
    }
}
