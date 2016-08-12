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
package org.lizardirc.beancounter.commands.url;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

abstract class UrlSummariserBase implements UrlSummariser {
    protected String getPageContent(URL url) throws IOException {
        int ttl = 16;

        while(ttl > 0) {
            URLConnection urlConnection = url.openConnection();

            if(urlConnection instanceof HttpsURLConnection)
            {
                // TEMP WORKAROUND (reject me at codereview pls)
                ((HttpsURLConnection)urlConnection).setHostnameVerifier((hostname, session) -> true);
            }

            urlConnection.connect();

            String location = urlConnection.getHeaderField("Location");
            if(location != null){
                ttl--;
                url = new URL(url, location);
                continue;
            }

            InputStream inputStream = urlConnection.getInputStream();

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];

            while (true) {
                int length = inputStream.read(buffer);

                if (length == -1) {
                    break;
                }

                result.write(buffer, 0, length);
            }

            return result.toString("UTF-8");
        }

        return "";
    }
}
