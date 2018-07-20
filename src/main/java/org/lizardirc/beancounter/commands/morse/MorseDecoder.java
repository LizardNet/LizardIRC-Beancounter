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

import java.util.Map;
import java.util.regex.Pattern;

public class MorseDecoder {

    private Map<String, String> morseMap;
    private final Pattern regexMessage;

    public MorseDecoder(Map<String, String> map) {
        this.morseMap = map;
        regexMessage = Pattern.compile("^[-./]+$");
    }

    public class DecodeResult {
        public final String result;
        public final int detectedCharacters;

        public DecodeResult(String result, int detectedCharacters) {
            this.result = result;
            this.detectedCharacters = detectedCharacters;
        }
    }

    public DecodeResult decodeMorse(String message) {
        String[] strings = message.split(" ");
        int characters = 0;

        StringBuilder sb = new StringBuilder();
        boolean lastWasMorse = false;

        for (String morseChar : strings) {
            if ("/".equals(morseChar) && lastWasMorse) {
                sb.append(" ");
                continue;
            }

            if (!regexMessage.matcher(morseChar).matches()) {
                // isn't a regex sequence
                if(lastWasMorse){
                    sb.append(" ");
                }

                sb.append(morseChar);
                sb.append(" ");
                lastWasMorse = false;
                continue;
            }

            if (morseChar.contains("/")) {
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

            String realChar = this.morseMap.getOrDefault(morseChar, "█");
            characters++;
            sb.append(realChar);
            lastWasMorse = true;
        }

        return new DecodeResult(sb.toString().trim(), characters);
    }
}
