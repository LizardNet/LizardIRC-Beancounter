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

import junit.framework.TestCase;

import java.util.HashMap;

public class MorseDecoderTest extends TestCase {


    private HashMap<String, String> map;

    public void setUp() {
        map = new HashMap<>();
        map.put(".", "e");
        map.put("...", "s");
        map.put("-", "t");
        map.put("---", "o");

        map.put(".--.-.", "@");
    }

    public void testBulkMorseBasicNone() {
        MorseDecoder decoder = new MorseDecoder(map);

        assertEquals("test", decoder.decodeMorse("test").result);
        assertEquals("please no.", decoder.decodeMorse("please no.").result);
        assertEquals("no...", decoder.decodeMorse("no...").result);
        assertEquals("...really?", decoder.decodeMorse("...really?").result);
        assertEquals("No - don't do this - either - really no", decoder.decodeMorse("No - don't do this - either - really no").result);
    }

    public void testBulkMorseIntermixed() {
        MorseDecoder decoder = new MorseDecoder(map);

        assertEquals("test se", decoder.decodeMorse("test ... .").result);
        assertEquals("otot yay otot", decoder.decodeMorse("--- - --- - yay --- - --- - ").result);
        assertEquals("ot multi word sts eee", decoder.decodeMorse("--- - multi word ... - ... / . . .").result);
        assertEquals("no but sote", decoder.decodeMorse("no but ... --- - .").result);
    }

    public void testDecodesMorseBasic() {
        MorseDecoder decoder = new MorseDecoder(map);

        String input = "... --- ...";
        String expected = "sos";
        String actual = decoder.decodeMorse(input).result;

        assertEquals(expected, actual);
    }

    public void testDecodesMorseMultiWord() {
        MorseDecoder decoder = new MorseDecoder(map);

        String input = "... --- / .--.-. -";
        String expected = "so @t";
        String actual = decoder.decodeMorse(input).result;

        assertEquals(expected, actual);
    }

    public void testDecodesMorseIntermixed() {
        MorseDecoder decoder = new MorseDecoder(map);

        String input = "Test a ... --- hi";
        String expected = "Test a so hi";
        String actual = decoder.decodeMorse(input).result;

        assertEquals(expected, actual);
    }

    public void testDecodesMorseIntermixedMultiword() {
        MorseDecoder decoder = new MorseDecoder(map);

        String input = "Test a ... --- / - --- hi";
        String expected = "Test a so to hi";
        String actual = decoder.decodeMorse(input).result;

        assertEquals(expected, actual);
    }

    public void testDecodesMorseCondensedSpace() {
        MorseDecoder decoder = new MorseDecoder(map);

        String input = "... ---/- ---";
        String expected = "so to";
        String actual = decoder.decodeMorse(input).result;

        assertEquals(expected, actual);
    }

}
