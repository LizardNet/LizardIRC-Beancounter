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

package org.lizardirc.beancounter.commands.elite.eddn.schema;

/**
 * No, not the real-life aeronautical navaid.  Go away.
 */
public final class Localizer {
    private Localizer() {
        throw new IllegalStateException("gtfo");
    }

    public static String localizeSystemSecurity(String input) {
        input = input.toLowerCase().replaceAll(";", "");

        switch (input) {
            case "$system_security_high":
                return "High Security";
            case "$system_security_low":
                return "Low Security";
            case "$system_security_medium":
                return "Medium Security";
            case "$galaxy_map_info_state_anarchy":
                return "Anarchy";
            case "$galaxy_map_info_state_lawless":
                return "Lawless";
            default:
                return input;
        }
    }

    public static String localizeSystemEconomy(String input) {
        input = input.toLowerCase().replaceAll(";", "");

        switch (input) {
            case "$economy_agri":
                return "Agriculture";
            case "$economy_colony":
                return "Colony";
            case "$economy_construction":
                return "Construction";
            case "$economy_corporate":
                return "Corporate";
            case "$economy_educational":
                return "Educational";
            case "$economy_engineer":
                return "Engineer";
            case "$economy_extraction":
                return "Extraction";
            case "$economy_financial":
                return "Financial";
            case "$economy_governmental":
                return "Governmental";
            case "$economy_health":
                return "Health";
            case "$economy_hightech":
                return "High Tech";
            case "$economy_hospitality":
                return "Hospitality";
            case "$economy_industrial":
                return "Industrial";
            case "$economy_military":
                return "Military";
            case "$economy_monastic":
                return "Monastic";
            case "$economy_none":
                return "None";
            case "$economy_penal":
                return "Penal Colony";
            case "$economy_refinery":
                return "Refinery";
            case "$economy_research":
                return "Research";
            case "$economy_retail":
                return "Retail";
            case "$economy_service":
                return "Service";
            case "$economy_terraforming":
                return "Terraforming";
            case "$economy_tourism":
                return "Tourism";
            case "$economy_undefined":
                return "";
            default:
                return input;
        }
    }

    public static String localizeSystemGovernment(String input) {
        input = input.toLowerCase().replaceAll(";", "");

        switch (input) {
            case "$government_anarchy":
                return "Anarchy";
            case "$government_colony":
                return "Colony";
            case "$government_communism":
                return "Communism";
            case "$government_confederacy":
                return "Confederacy";
            case "$government_cooperative":
                return "Cooperative";
            case "$government_corporate":
                return "Corporate";
            case "$government_democracy":
                return "Democracy";
            case "$government_dictatorship":
                return "Dictatorship";
            case "$government_engineer":
                return "Engineer";
            case "$government_feudal":
                return "Feudal";
            case "$government_imperial":
                return "Imperial";
            case "$government_none":
                return "None";
            case "$government_patronage":
                return "Patronage";
            case "$government_prisoncolony":
                return "Prison Colony";
            case "$government_theocracy":
                return "Theocracy";
            case "$government_undefined":
                return "";
            case "$government_unknown":
                return "Unknown";
            default:
                return input;
        }
    }
}
