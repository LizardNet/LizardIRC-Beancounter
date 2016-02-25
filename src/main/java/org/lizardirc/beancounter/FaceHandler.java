package org.lizardirc.beancounter;

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
    private static final String FLIP = "TableFLIP";
    private static final String FLIP2 = "FlipTable";
    private static final String RAISE = "Raise";
    private static final Set<String> COMMANDS = ImmutableSet.of(LOOK_OF_DISAPPROVAL, LENNY, FLIP, FLIP2, RAISE);

    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789[{(<>)}].!'\",&?";
    private static final String AHPLA = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀ᗺƆᗡƎℲפHIſʞ⅂WNOԀΌᴚS⊥∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86]})><({[˙¡,„'⅋¿";
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
                remainder = new IllegalArgumentException("Invalid command").toString();
            case RAISE:
                if (remainder.isEmpty()) {
                    remainder = " your dongers";
                }
                event.respond("ヽ༼ຈل͜ຈ༽ﾉ raise" + remainder + " ヽ༼ຈل͜ຈ༽ﾉ");
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
}
