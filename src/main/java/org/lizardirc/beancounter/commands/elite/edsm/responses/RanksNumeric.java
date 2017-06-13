/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.elite.edsm.responses;

import com.google.gson.annotations.SerializedName;

public class RanksNumeric {
    public static final int ELITE = 8;
    public static final int MAX_NAVAL_RANK = 14;

    @SerializedName("Combat") protected int combat;
    @SerializedName("Trade") protected int trade;
    @SerializedName("Explore") protected int exploration;
    @SerializedName("CQC") protected int cqc;
    @SerializedName("Federation") protected int federalNavy;
    @SerializedName("Empire") protected int imperialNavy;

    public int getCombat() {
        return combat;
    }

    public int getTrade() {
        return trade;
    }

    public int getExploration() {
        return exploration;
    }

    public int getCqc() {
        return cqc;
    }

    public int getFederalNavy() {
        return federalNavy;
    }

    public int getImperialNavy() {
        return imperialNavy;
    }
}
