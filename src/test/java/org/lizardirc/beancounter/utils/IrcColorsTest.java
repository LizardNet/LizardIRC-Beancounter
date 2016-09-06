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

package org.lizardirc.beancounter.utils;

import junit.framework.TestCase;

public class IrcColorsTest extends TestCase {
    private final String COLOUR_BASE = "\003";

    public void testStripFormattingNoOp() {
        // Arrange
        String input = "test";
        String expected = "test";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingColour() {
        // Arrange
        String input = "test" + COLOUR_BASE + "4test";
        String expected = "testtest";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingColour2() {
        // Arrange
        String input = "test" + COLOUR_BASE + "04test";
        String expected = "testtest";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingColourWithNumber() {
        // Arrange
        String input = "test" + COLOUR_BASE + "045test";
        String expected = "test5test";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingReset() {
        // Arrange
        String input = "test" + IrcColors.RESET + "test";
        String expected = "testtest";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingBackground() {
        // Arrange
        String input = "test" + COLOUR_BASE + "4,5test";
        String expected = "testtest";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingBackgroundTest() {
        // Arrange
        String input = "test" + COLOUR_BASE + "04,05test";
        String expected = "testtest";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingBackgroundTestNumber() {
        // Arrange
        String input = "test" + COLOUR_BASE + "04,051test";
        String expected = "test1test";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingBold() {
        // Arrange
        String input = "test" + IrcColors.BOLD + "test";
        String expected = "testtest";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingItalic() {
        // Arrange
        String input = "test" + IrcColors.ITALIC + "test";
        String expected = "testtest";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingUnderline() {
        // Arrange
        String input = "test" + IrcColors.UNDERLINE + "test";
        String expected = "testtest";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingRv() {
        // Arrange
        String input = "test" + IrcColors.REVERSEVIDEO + "test";
        String expected = "testtest";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingRandomlyChosenColour() {
        // Arrange
        // Chosen by fair dice roll, guaranteed to be random.
        String input = "test" + IrcColors.RED + ", but test";
        String expected = "test, but test";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }

    public void testStripFormattingAwkwardComma() {
        // Arrange
        String input = "test" + IrcColors.RED + ", but test";
        String expected = "test, but test";

        // Act
        String actual = IrcColors.stripFormatting(input);

        // Assert
        assertEquals(expected, actual);
    }
}
