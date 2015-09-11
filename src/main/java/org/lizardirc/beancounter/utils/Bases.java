/**
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015 by the LizardIRC Development Team. Some rights reserved.
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

import java.nio.charset.Charset;
import java.util.Base64;

import com.google.common.base.Charsets;

public final class Bases {
    private static final Charset UTF8 = Charsets.UTF_8;
    private static final Base64.Encoder B64E = Base64.getEncoder();
    private static final Base64.Decoder B64D = Base64.getDecoder();
    private static final Base64.Encoder MB64E = Base64.getMimeEncoder();
    private static final Base64.Decoder MB64D = Base64.getMimeDecoder();
    private static final Base64.Encoder UB64E = Base64.getUrlEncoder();
    private static final Base64.Decoder UB64D = Base64.getUrlDecoder();

    private Bases() {
        throw new IllegalStateException("Cannot instantiate this class");
    }

    public static String base64encode(String source) {
        return B64E.encodeToString(source.getBytes(UTF8));
    }

    public static String base64decode(String source) {
        return new String(B64D.decode(source), UTF8);
    }

    public static String mimeBase64encode(String source) {
        return MB64E.encodeToString(source.getBytes(UTF8));
    }

    public static String mimeBase64decode(String source) {
        return new String(MB64D.decode(source), UTF8);
    }

    public static String urlBase64encode(String source) {
        return UB64E.encodeToString(source.getBytes(UTF8));
    }

    public static String urlBase64decode(String source) {
        return new String(UB64D.decode(source), UTF8);
    }
}
