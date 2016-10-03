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

package org.lizardirc.beancounter.commands.memes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.Beancounter;
import org.lizardirc.beancounter.hooks.CommandHandler;

public class MemeHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String LOOK_OF_DISAPPROVAL = "LookOfDisapproval";
    private static final String LENNY = "Lenny";
    private static final String ANGRY_LENNY = "AngryLenny";
    private static final String LOOK_OF_LENNY = "LookOfLenny";
    private static final String FLIP = "TableFLIP";
    private static final String FLIP2 = "FlipTable";
    private static final String RAISE = "Raise";
    private static final String LOWER = "Lower";
    private static final String SMALLCAPS = "SmallCaps";
    private static final String SUPERSCRIPT = "SuperScript";
    private static final String SWEAR_TO_GOD = "SwearToGod";
    private static final String LAMBORGHINI = "lamborghini";
    private static final String IDUNNOLOL = "idunnolol";
    private static final String IDKLOL = "idklol";
    private static final Set<String> COMMANDS = ImmutableSet.of(LOOK_OF_DISAPPROVAL, LENNY, ANGRY_LENNY, LOOK_OF_LENNY,
        FLIP, FLIP2, RAISE, LOWER, SMALLCAPS, SUPERSCRIPT, SWEAR_TO_GOD, LAMBORGHINI, IDUNNOLOL, IDKLOL);

    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789[{(<>)}].!'\",&?";
    private static final String AHPLA = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀ᗺƆᗡƎℲפHIſʞ⅂WNOԀΌᴚS⊥∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86]})><({[˙¡,„'⅋¿";
    private static final String SMALL_CAPS_CHARS = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ0123456789[{(<>)}].!'\",&?";
    private static final String SUPERSCRIPTS_CHARS = "ᵃᵇᶜᵈᵉᶠᵍʰᶦʲᵏˡᵐⁿᵒᵖᑫʳˢᵗᵘᵛʷˣʸᶻᴬᴮᶜᴰᴱᶠᴳᴴᴵᴶᴷᴸᴹᴺᴼᴾǫᴿˢᵀᵁⱽᵂˣʸᶻ⁰¹²³⁴⁵⁶⁷⁸⁹[{⁽<>⁾}].!'\",&?";
    //private static final String ALT = "ɐpⅽqөʈɓµ!ɾʞꞁwuobdʁƨʇ∩٨ʍxʎzᗄᗷ⊂DEᖶ⅁HIᘃʞ⅂ʍNObⵚᖉᴤ⊥∩⋀MX⅄Z0123456789";
    //private static final String ALT = "ɐqɔpǝɟƃɥıɾʞןɯuodbɹsʇnʌʍxʎzɐqɔpǝɟƃɥıɾʞןɯuodbɹsʇnʌʍxʎz0123456789";
    //private static final String ALT = "68Ɫ95ᔭƐ210Z⅄XMᴧ∩⊥SᴚΌԀOᴎW⅂⋊ſIH⅁ℲƎ◖Ↄ𐐒∀zʎxʍʌnʇsɹbdouɯʃʞɾıɥƃɟǝpɔqɐ";
    // ^ some other mappings from other tools that might be helpful

    private final List<Lamborghini> lamborghiniAccount;
    private final Random random = new Random();

    public MemeHandler() {
        List<Lamborghini> lamborghiniAccount;

        try (InputStream lamborghiniFile = Beancounter.class.getResourceAsStream("/lamborghinis.json")) {
            Gson lamborghiniDeserializer = new Gson();
            Type lamborghiniAccountType = new TypeToken<List<Lamborghini>>(){}.getType();
            InputStreamReader lamborghiniReader = new InputStreamReader(lamborghiniFile);

            List<Lamborghini> mutableLamborghinis = lamborghiniDeserializer.fromJson(lamborghiniReader, lamborghiniAccountType);
            lamborghiniAccount = ImmutableList.copyOf(mutableLamborghinis);
        } catch (IOException | NullPointerException e) {
            System.err.println("Caught IOException or NullPointerException trying to read in lamborghinis: " + e.getMessage());
            e.printStackTrace();
            lamborghiniAccount = ImmutableList.of();
        }

        this.lamborghiniAccount = lamborghiniAccount;
    }

    @Override
    public Set<String> getSubCommands(final GenericMessageEvent<T> event, final List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(final GenericMessageEvent<T> event, final List<String> commands, String remainder) {
        String message;

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
            case SMALLCAPS:
                if (remainder.isEmpty()) {
                    remainder = "u wot m8";
                }
                event.respond(smallCaps(remainder.trim()));
                break;
            case SUPERSCRIPT:
                if (remainder.isEmpty()) {
                    remainder = "serious trouble";
                }
                event.respond(superscripts(remainder.trim()));
                break;
            case SWEAR_TO_GOD:
                String boilerplate = "I swear to god if any of you motherrfuckers %s you will be in serious trouble";

                if (remainder.isEmpty()) {
                    remainder = "copy and paste this";
                }

                message = "ヽ༼ຈل͜ຈ༽ﾉ " + superscripts(String.format(boilerplate, remainder.trim())) + " ヽ༼ຈل͜ຈ༽ﾉ";
                event.respond(message);
                break;
            case LAMBORGHINI:
                if (lamborghiniAccount.isEmpty()) {
                    event.respond("Someone withdrew all the lamborghinis from my lamborghini account!  D:");
                    // This shouldn't ever happen
                } else {
                    if (!remainder.isEmpty()) {
                        event.respond(lamborghiniAccount.get(random.nextInt(lamborghiniAccount.size())).getQuote(remainder.trim()));
                    } else {
                        event.respond(lamborghiniAccount.get(random.nextInt(lamborghiniAccount.size())).getQuote());
                    }
                }
                break;
            case IDUNNOLOL:
            case IDKLOL:
                event.respond("¯\\_(ツ)_/¯" + remainder);
                break;
        }
    }

    private static String charConvert(char[] input, String convertTo) {
        for (int i = 0; i < input.length; i++) {
            int index = ALPHA.indexOf(input[i]);
            if (index >= 0) {
                input[i] = convertTo.charAt(index);
            }
        }

        return new String(input);
    }

    private static String flip(String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length / 2; i++) {
            char tmp = chars[i];
            chars[i] = chars[chars.length - i - 1];
            chars[chars.length - i - 1] = tmp;
        }

        return charConvert(chars, AHPLA);
    }

    private static String smallCaps(String input) {
        return charConvert(input.toCharArray(), SMALL_CAPS_CHARS);
    }

    private static String superscripts(String input) {
        return charConvert(input.toCharArray(), SUPERSCRIPTS_CHARS);
    }
}
