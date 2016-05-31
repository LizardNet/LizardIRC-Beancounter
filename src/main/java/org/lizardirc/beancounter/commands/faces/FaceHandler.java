/**
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

package org.lizardirc.beancounter.commands.faces;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;

public class FaceHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String LOOK_OF_DISAPPROVAL = "LookOfDisapproval";
    private static final String LENNY = "Lenny";
    private static final String ANGRY_LENNY = "AngryLenny";
    private static final String LOOK_OF_LENNY = "LookOfLenny";
    private static final String FLIP = "TableFLIP";
    private static final String FLIP2 = "FlipTable";
    private static final String RAISE = "Raise";
    private static final String LOWER = "Lower";
    private static final Set<String> COMMANDS = ImmutableSet.of(LOOK_OF_DISAPPROVAL, LENNY, ANGRY_LENNY, LOOK_OF_LENNY, FLIP, FLIP2, RAISE, LOWER);

    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789[{(<>)}].!'\",&?";
    private static final String AHPLA = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀ᗺƆᗡƎℲפHIſʞ⅂WNOԀΌᴚS⊥∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86]})><({[˙¡,„'⅋¿";
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ0123456789[{(<>)}].!'\",&?";
    //private static final String ALT = "ɐpⅽqөʈɓµ!ɾʞꞁwuobdʁƨʇ∩٨ʍxʎzᗄᗷ⊂DEᖶ⅁HIᘃʞ⅂ʍNObⵚᖉᴤ⊥∩⋀MX⅄Z0123456789";
    //private static final String ALT = "ɐqɔpǝɟƃɥıɾʞןɯuodbɹsʇnʌʍxʎzɐqɔpǝɟƃɥıɾʞןɯuodbɹsʇnʌʍxʎz0123456789";
    //private static final String ALT = "68Ɫ95ᔭƐ210Z⅄XMᴧ∩⊥SᴚΌԀOᴎW⅂⋊ſIH⅁ℲƎ◖Ↄ𐐒∀zʎxʍʌnʇsɹbdouɯʃʞɾıɥƃɟǝpɔqɐ";
    // ^ some other mappings from other tools that might be helpful

    @Override
    public Set<String> getSubCommands(final GenericMessageEvent<T> event, final List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }
        return Collections.emptySet();
    }

    @Override
    public void handleCommand(final GenericMessageEvent<T> event, final List<String> commands, String remainder) {
        if (!remainder.isEmpty()) {
            remainder = ' ' + remainder.trim();
        }
        switch (commands.get(0)) {
            case LOOK_OF_DISAPPROVAL:
                event.respond("ಠ_ಠ" + remainder);
                break;
            case LENNY:
                event.respond("( ͡° ͜ʖ ͡°)" + remainder);
                break;
            case ANGRY_LENNY:
                event.respond("( ͠° ͟ʖ ͡°)" + remainder);
                break;
            case LOOK_OF_LENNY:
                event.respond("( ͡ಠ ʖ̯ ͡ಠ)" + remainder);
                break;
            case FLIP:
            case FLIP2:
                if (remainder.isEmpty()) {
                    remainder = "┻━┻";
                } else {
                    remainder = flip(remainder.trim());
                }
                event.respond("(╯°□°）╯︵" + remainder);
                break;
            default:
                remainder = ' ' + new IllegalArgumentException("Invalid command").toString();
            case RAISE:
                String message;
                String lowercase = remainder.toLowerCase();

                if (remainder.isEmpty()) {
                    message = "ヽ༼ຈل͜ຈ༽ﾉ raise your dongers ヽ༼ຈل͜ຈ༽ﾉ";
                } else if (lowercase.endsWith("mellowcraze")) {
                    message = "ᕕ༼✿•̀︿•́༽ᕗ RAISE" + remainder.toUpperCase() + " ᕕ༼✿•̀︿•́༽ᕗ";
                } else if (lowercase.endsWith("dagero")) {
                    message = "ヽ༼ຈل͜O༽ﾉ ʀᴀɪs" + smallCaps(remainder) + " ヽ༼ຈل͜___ຈ༽ﾉ";
                } else if (lowercase.endsWith("deniro") || lowercase.endsWith("de niro")) {
                    message = "ヽ༼$ل͜$༽ﾉ ʀᴀɪsᴇ" + smallCaps(remainder) + " ヽ༼$ل͜$༽ﾉ";
                } else if (lowercase.endsWith("dogers")) {
                    message = "ヽ༼⚆̂ᴥ⚆̚༽ﾉ ʀᴀɪsᴇ" + smallCaps(remainder) + " ヽ༼⚆̂ᴥ⚆̚༽ﾉ";
                } else {
                    message = "ヽ༼ຈل͜ຈ༽ﾉ raise" + remainder + " ヽ༼ຈل͜ຈ༽ﾉ";
                }

                event.respond(message);
                break;
            case LOWER:
                if (remainder.isEmpty()) {
                    remainder = " your dongers";
                }
                event.respond("┌༼ຈل͜ຈ༽┐ lower" + remainder + " ┌༼ຈل͜ຈ༽┐");
                break;
        }
    }

    private static String flip(String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length / 2; i++) {
            char tmp = chars[i];
            chars[i] = chars[chars.length - i - 1];
            chars[chars.length - i - 1] = tmp;
        }

        for (int i = 0; i < chars.length; i++) {
            int index = ALPHA.indexOf(chars[i]);
            if (index >= 0) {
                chars[i] = AHPLA.charAt(index);
            }
        }

        return new String(chars);
    }

    private static String smallCaps(String input) {
        char[] chars = input.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            int index = ALPHA.indexOf(chars[i]);
            if (index >= 0) {
                chars[i] = SMALL_CAPS.charAt(index);
            }
        }

        return new String(chars);
    }
}
