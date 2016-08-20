/*
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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.Beancounter;

public final class Miscellaneous {
    private Miscellaneous() {
        throw new IllegalStateException("Cannot instantiate this class");
    }

    public static boolean isChannelLike(GenericMessageEvent<?> event, String arg) {
        // Determine if the argument string appears to be a channel - i.e., does it start with a character the network
        // recognizes as a channel name (usually # and &)?
        Integer firstChar = arg.codePointAt(0);

        return event.getBot().getServerInfo().getChannelTypes().chars()
            .anyMatch(firstChar::equals);
    }

    public static String getStringRepresentation(Collection<String> set) {
        return getStringRepresentation(set, ", ");
    }

    public static String getStringRepresentation(Collection<String> set, String separator) {
        if (set.isEmpty()) {
            return "(none)";
        } else {
            return set.stream().collect(Collectors.joining(separator));
        }
    }

    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<>(c);
        Collections.sort(list);
        return list;
    }

    public static String generateHttpUserAgent() {
        String artifactVersion = Beancounter.class.getPackage().getImplementationVersion();
        if (artifactVersion == null) {
            artifactVersion = "";
        } else {
            artifactVersion = '/' + artifactVersion;
        }

        String projectName = Beancounter.PROJECT_NAME;
        projectName = projectName.replace('/', '-');

        return projectName + artifactVersion + " (compatible; +" + Beancounter.PROJECT_URL + ")";
    }

    public static String getHttpData(URI url) throws IOException {
        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", generateHttpUserAgent());
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder.setSSLHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        HttpClient httpClient = httpClientBuilder.build();
        HttpResponse response = httpClient.execute(request);

        return EntityUtils.toString(response.getEntity());
    }
}
