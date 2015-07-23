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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

public abstract class CommandListener<T extends PircBotX> extends ListenerAdapter<T> {
    @Override
    public void onGenericMessage(GenericMessageEvent<T> event) {
        List<String> commands = new ArrayList<String>();
        Set<String> options;
        String message = event.getMessage().trim();
        while (!isNullOrEmpty(options = getSubCommands(event, commands))) {
            String firstWord = message.split(" ")[0];
            String selected = null;
            try {
                selected = selectOption(options, firstWord);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (selected == null) {
                break;
            }
            commands.add(selected);
            message = message.substring(firstWord.length());
            message = message.trim();
        }
        if (commands.size() > 0) {
            handleCommand(event, commands, message);
        }
    }

    /**
     * Returns a list of subcommands that are accepted for the given command.
     *
     * Returns the empty set if no commands are accepted, or the remaining input is freeform.
     */
    public abstract Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands);

    public abstract void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder);

    // This function tries to determine which of a set of options a user was
    // aiming for when typing a shorthand. For example, of a list of menu items,
    // this function would aim to accept "fgs" for "Flame-Grilled Steak".
    private String selectOption(Set<String> options, String selector) {
        String selUpper = selector.toUpperCase();
        Map<String, String> optionsUpper = new HashMap<>();
        Map<String, String> optionsMunged = new HashMap<>();

        // Blacklist: options that are non-unique.
        // We preserve these in the original list, for the following reason
        // Consider "Black Bart", "BlackBeard", "Black Caesar"
        // If "BB" were not kept in the original list, it would not be clear in
        // the remainder of the algorithm that "B" is an ambiguous choice.
        Set<String> blacklist = new HashSet<>();

        for (String opt : options) {
            String upper = opt.toUpperCase();
            optionsUpper.put(upper, opt);

            String capsAndNumbers = getCapsAndNumbers(opt);
            if (!capsAndNumbers.equals(upper)) {
                if (optionsMunged.put(capsAndNumbers, opt) != null) {
                    blacklist.add(capsAndNumbers);
                    continue;
                }
            }

            String caps = getCapitals(capsAndNumbers);
            if (!caps.equals(capsAndNumbers)) {
                if (optionsMunged.put(caps, opt) != null) {
                    blacklist.add(caps);
                    continue;
                }
            }
        }

        // Look for the provided value itself - we need this to handle options
        // that are prefixes of other options, such as "hi" and "hit".
        if (optionsUpper.containsKey(selUpper)) {
            return optionsUpper.get(selUpper);
        }

        // If the selector is only a subsequence of one string, return that
        String unique = getUniqueSubsequence(optionsUpper.keySet(), selUpper);
        if (unique != null) {
            return optionsUpper.get(unique);
        }

        // If it's only a prefix of one string, return that
        unique = getUniquePrefix(optionsUpper.keySet(), selUpper);
        if (unique != null) {
            return optionsUpper.get(unique);
        }

        // Try again with munged values
        unique = getUniqueSubsequence(optionsMunged.keySet(), selUpper);
        if (unique != null && !blacklist.contains(unique)) {
            return optionsMunged.get(unique);
        }
        unique = getUniquePrefix(optionsMunged.keySet(), selUpper);
        if (unique != null && !blacklist.contains(unique)) {
            return optionsMunged.get(unique);
        }

        // Give up
        return null;
    }

    private String getUniquePrefix(Set<String> options, String needle) {
        String match = null;
        for (String haystack : options) {
            if (haystack.startsWith(needle)) {
                if (match != null) { // we already found one, so this isn't unique
                    return null;
                }
                match = haystack;
            }
        }
        return match;
    }

    /**
     * Returns one of the options if and only if it is the only option of which
     * the needle is a subsequence, and the needle is at least 3 characters long.
     */
    private String getUniqueSubsequence(Set<String> options, String needle) {
        if (needle.length() < 3) {
            return null;
        }

        String match = null;
        for (String haystack : options) {
            if (isSubsequence(needle, haystack)) {
                if (match != null) { // we already found one, so this isn't unique
                    return null;
                }
                match = haystack;
            }
        }
        return match;
    }

    private boolean isSubsequence(String needle, String haystack) {
        int needleLen = needle.length();
        int hayLen = haystack.length();

        int i = 0;
        for (int j = 0; j < hayLen; j++) {
            if (needle.charAt(i) == haystack.charAt(j)) {
                i++;
            }
            if (i == needleLen) {
                return true;
            }
        }
        return false;
    }

    private String getCapsAndNumbers(String source) {
        StringBuilder ret = new StringBuilder();
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char c = source.charAt(i);
            if ((c >= 'A' && c <= 'z') || (c >= '0' && c <= '9')) {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    private String getCapitals(String source) {
        StringBuilder ret = new StringBuilder();
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char c = source.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    private boolean isNullOrEmpty(Set<?> set) {
        return set == null || set.isEmpty();
    }
}
