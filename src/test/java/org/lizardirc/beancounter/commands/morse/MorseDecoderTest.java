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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.HashMap;

@RunWith(Parameterized.class)
public class MorseDecoderTest extends TestCase {
    private HashMap<String, String> map;

    private final String expected;
    private final String actual;
    private final int expectedCharacters;

    public MorseDecoderTest(@SuppressWarnings("unused") String type, String expected, String actual, int expectedCharacters) {
        this.expected = expected;
        this.actual = actual;
        this.expectedCharacters = expectedCharacters;
    }

    @Before
    public void setUp() {
        map = new HashMap<>();
        map.put(".", "e");
        map.put("..", "i");
        map.put("...", "s");
        map.put("-", "t");
        map.put("---", "o");

        map.put(".--.-.", "@");
    }

    @Parameterized.Parameters(name = "{index}: Test {0}, input: \"{2}\", output: \"{1}\", chars: {3}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"no-op", "test", "test", 0},
            {"no-op", "please no.", "please no.", 0},
            {"no-op", "no...", "no...", 0},
            {"no-op", "no ...", "no ...", 0},
            {"no-op", "...really?", "...really?", 0},
            {"no-op", "... really?", "... really?", 0},
            {"no-op", "No - don't do this - either - really no", "No - don't do this - either - really no", 0},
            {"basic", "sos", "... --- ...", 3},
            {"basic", "test", "- . ... -", 4},
            {"intermixed", "test se", "test ... .", 2},
            {"intermixed", "otot yay otot", "--- - --- - yay --- - --- - ", 8},
            {"intermixed", "ot multi word sts eee", "--- - multi word ... - ... / . . .", 8},
            {"intermixed", "no but sote", "no but ... --- - .", 4},
            {"intermixed", "Test a so hi", "Test a ... --- hi", 2},
            {"intermixed", "Test a so to hi", "Test a ... --- / - --- hi", 4},
            {"intermixed", "Old MacDonald had a farm, e i e i o And on this farm he had a pig...", "Old MacDonald had a farm, . / .. / . / .. / --- And on this farm he had a pig...", 5},
            {"multi-word", "so @t", "... --- / .--.-. -", 4},
            {"multi-word", "s s t o", "... / ... / - / ---", 4},
            {"condensed space", "so to", "... ---/- ---", 4},
        });
    }

    @Test
    public void testMorseDecode() {
        MorseDecoder decoder = new MorseDecoder(map);
        MorseDecoder.DecodeResult decodeResult = decoder.decodeMorse(this.actual);
        assertEquals(this.expected, decodeResult.result);
    }

    @Test
    public void testMorseDecodeCharCount() {
        MorseDecoder decoder = new MorseDecoder(map);
        MorseDecoder.DecodeResult decodeResult = decoder.decodeMorse(this.actual);
        assertEquals(this.expectedCharacters, decodeResult.detectedCharacters);
    }
}
