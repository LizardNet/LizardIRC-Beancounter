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

import java.util.List;
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
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.EvictingQueue;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import org.lizardirc.beancounter.utils.InterruptibleCharSequence;

public class SedListener<T extends PircBotX> extends ListenerAdapter<T> {
    // There are several substring types that we match.
    // A) any character except \
    // B) 0 or more copies of either an escaped character, or something other than \
    // C) The delimiter, as a backreference
    // D) One or more non-whitespace characters, followed by a colon and a space
    // We optionally match D. This is the target ("Alice" in `Alice: s/foo/bar`)
    // We match s, followed by A. This is the delimiter.
    // We then match B. This is the search regex.
    // We match C.
    // We match B. This is the replacement string.
    // We match C again.
    // We match B. These are the options.
    private static final String REGEX_D = "(?:([^\\s]+): )?";
    private static final String REGEX_B = "((?:\\\\.|[^\\\\])*)";
    private static final String REGEX_AB = "s([^\\\\\\sA-Za-z0-9])" + REGEX_B;
    private static final String REGEX_CB = "\\2" + REGEX_B;
    private static final String REGEX_SED = REGEX_D + REGEX_AB + REGEX_CB + REGEX_CB;
    private static final String REGEX_BAD_SED = REGEX_D + REGEX_AB + REGEX_CB;
    private static final Pattern PATTERN_SED = Pattern.compile(REGEX_SED);
    private static final Pattern PATTERN_BAD_SED = Pattern.compile(REGEX_BAD_SED);

    private static final Pattern PATTERN_OPTIONS = Pattern.compile("[gi]*");

    private final LoadingCache<User, Queue<String>> windows;
    private final ExecutorService executorService;

    public SedListener(ExecutorService executorService, int windowSize) {
        windows = CacheBuilder.newBuilder()
            .build(CacheLoader.from(() -> EvictingQueue.create(windowSize)));
        this.executorService = executorService;
    }

    @Override
    public void onMessage(MessageEvent<T> event) throws Exception {
        String message = event.getMessage();
        Matcher m = PATTERN_SED.matcher(message);
        if (m.matches()) {
            String target = m.group(1);
            String regex = m.group(3);
            String replacement = m.group(4);
            String options = m.group(5);

            User corrector = event.getUser();
            User speaker = event.getChannel().getUsers().stream()
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

            Pattern p;
            try {
                p = Pattern.compile(regex, flags);
            } catch (PatternSyntaxException e) {
                event.respond("Invalid regex '" + regex + "': " + e.getMessage());
                return;
            }

            Queue<String> window = windows.getUnchecked(speaker);

            Callable<Optional<String>> callable = new SedListenerCallable(p, replacement, options.contains("g"), window);
            Future<Optional<String>> future = executorService.submit(callable);

            try {
                Optional<String> response = future.get(5, TimeUnit.SECONDS);

                response.ifPresent(s -> {
                    window.add(s);

                    StringBuilder sb = new StringBuilder(corrector.getNick());
                    if (!corrector.equals(speaker)) {
                        sb.append(" thinks ").append(speaker.getNick());
                    }
                    sb.append(" meant to say: ").append(s);
                    event.getChannel().send().message(sb.toString());
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
                event.getChannel().send().action("slaps " + event.getUser().getNick() + " with a copy of the sed user manual");
            } else {
                windows.getUnchecked(event.getUser()).add(message);
            }
        }
    }

    private static class SedListenerCallable implements Callable<Optional<String>> {
        private final Pattern p;
        private final String replacement;
        private final boolean global;
        private final List<CharSequence> window;

        public SedListenerCallable(Pattern p, String replacement, boolean global, Queue<String> window) {
            this.p = p;
            this.replacement = replacement;
            this.global = global;
            this.window = window.stream()
                .map(InterruptibleCharSequence::new)
                .collect(Collectors.toList());
        }

        @Override
        public Optional<String> call() throws Exception {
            return window.stream()
                .map(p::matcher)
                .filter(Matcher::find)
                .reduce((x, y) -> y) // findLast()
                .map(matcher -> {
                    if (global) {
                        return matcher.replaceAll(replacement);
                    } else {
                        return matcher.replaceFirst(replacement);
                    }
                });
        }
    }
}
