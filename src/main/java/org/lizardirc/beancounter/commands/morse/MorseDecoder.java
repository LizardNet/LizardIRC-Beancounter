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

package org.lizardirc.beancounter.commands.morse;

import com.google.common.collect.ImmutableList;

import java.util.Map;
import java.util.regex.Pattern;

public class MorseDecoder {

    private final Map<String, String> morseMap;
    private final Pattern regexMatchesMorseWord;

    /**
     * Morse letters which are commonly used in non-morse text.
     */
    private final ImmutableList<String> ambiguousMorse;

    public MorseDecoder(Map<String, String> map) {
        this.morseMap = map;
        regexMatchesMorseWord = Pattern.compile("^[-./]+$");
        this.ambiguousMorse = ImmutableList.of("...", ".", "-");
    }

    public static class DecodeResult {
        public final String result;
        public final int detectedCharacters;

        public DecodeResult(String result, int detectedCharacters) {
            this.result = result;
            this.detectedCharacters = detectedCharacters;
        }
    }

    /**
     * Converts a string containing morse into a non-morse string
     *
     * @param message The message containing morse to decode
     * @return DecodeResult
     */
    public DecodeResult decodeMorse(String message) {
        String[] strings = message.split(" ");
        int characters = 0;

        StringBuilder sb = new StringBuilder();
        boolean lastWasMorse = false;
        String ambiguousMorseInstance = null;

        // We've taken a text string, and split it into what humans call words.
        // We iterate over those words, and see if they are actually morse sequences which are actually letters.
        // We keep track of whether or not the last "word" was a morse character so we can properly pad things
        for (String morseChar : strings) {
            // insert a space, but only if we think we're in a morse sequence
            if ("/".equals(morseChar) && lastWasMorse) {
                if (ambiguousMorseInstance != null) {
                    sb.append(this.morseMap.getOrDefault(ambiguousMorseInstance, "█"));
                    ambiguousMorseInstance = null;
                    characters++;
                }

                sb.append(" ");
                continue;
            }

            if (!regexMatchesMorseWord.matcher(morseChar).matches()) {
                // this one isn't a morse sequence, so just pass it through
                if (lastWasMorse && ambiguousMorseInstance == null) {
                    // oh, and we're transitioning from morse to non-morse, so add a space
                    sb.append(" ");
                }

                if (ambiguousMorseInstance != null) {
                    sb.append(ambiguousMorseInstance);
                    sb.append(" ");
                    ambiguousMorseInstance = null;
                }

                sb.append(morseChar);
                sb.append(" ");
                lastWasMorse = false;
                continue;
            }

            // OK, so we've verified that morseChar is actually morse.
            // Thus, at this point, it can contain only a dot, a dash, or a slash.

            // Deal with a slash as a word separator
            if (morseChar.contains("/")) {
                // definitely morse.
                if (ambiguousMorseInstance != null) {
                    // last character was ambiguous, so allow it as this is morse.
                    sb.append(this.morseMap.getOrDefault(ambiguousMorseInstance, "█"));
                    characters++;
                    ambiguousMorseInstance = null;
                }

                String[] wordSep = morseChar.split("/");
                boolean first = true;
                for (String morseSubword : wordSep) {
                    if (!first) {
                        sb.append(" ");
                    }
                    String realChar = this.morseMap.getOrDefault(morseSubword, "█");
                    characters++;
                    sb.append(realChar);
                    first = false;
                }

                lastWasMorse = true;

                continue;
            }

            // if this character is possibly ambiguous as to whether it's morse or not (such as a ... or a -), then
            // treat it as morse, but keep a note of it and add it to the list later.
            if (this.ambiguousMorse.contains(morseChar) && ambiguousMorseInstance == null && !lastWasMorse) {
                ambiguousMorseInstance = morseChar;
                lastWasMorse = true;
                continue;
            }

            // this character is now definitely morse
            if (ambiguousMorseInstance != null) {
                // last character was ambiguous, so allow it as this is morse.
                sb.append(this.morseMap.getOrDefault(ambiguousMorseInstance, "█"));
                characters++;
                ambiguousMorseInstance = null;
            }

            String realChar = this.morseMap.getOrDefault(morseChar, "█");
            characters++;
            sb.append(realChar);
            lastWasMorse = true;
        }

        // end of buffer. Was the last character ambiguous?
        if (ambiguousMorseInstance != null) {
            sb.append(ambiguousMorseInstance);
        }

        return new DecodeResult(sb.toString().trim(), characters);
    }
}
