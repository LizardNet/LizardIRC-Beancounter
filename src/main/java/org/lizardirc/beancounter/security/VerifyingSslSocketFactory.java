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

package org.lizardirc.beancounter.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;

public class VerifyingSslSocketFactory extends SSLSocketFactory {
    private static final HostnameVerifier verifier = new DefaultHostnameVerifier();
    private final String hostname;
    private final SSLSocketFactory underlyingFactory;

    public VerifyingSslSocketFactory(String hostname) {
        this.hostname = hostname;
        underlyingFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    public VerifyingSslSocketFactory(String hostname, SSLSocketFactory underlyingFactory) {
        this.hostname = hostname;
        this.underlyingFactory = underlyingFactory;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return underlyingFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return underlyingFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        SSLSocket ret = (SSLSocket) underlyingFactory.createSocket(socket, s, i, b);
        verify(ret);
        return ret;
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException {
        verifyHostname(s);
        SSLSocket ret = (SSLSocket) underlyingFactory.createSocket(s, i);
        verify(ret);
        return ret;
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException {
        verifyHostname(s);
        SSLSocket ret = (SSLSocket) underlyingFactory.createSocket(s, i, inetAddress, i1);
        verify(ret);
        return ret;
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        SSLSocket ret = (SSLSocket) underlyingFactory.createSocket(inetAddress, i);
        verify(ret);
        return ret;
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        SSLSocket ret = (SSLSocket) underlyingFactory.createSocket(inetAddress, i, inetAddress1, i1);
        verify(ret);
        return ret;
    }

    private void verifyHostname(String s) throws SSLException {
        if (!s.equals(hostname)) {
            System.err.println("Rejecting; bad host " + s + " where we expected " + hostname);
            throw new SSLHandshakeException("Attempting to connect to hostname other than that specified");
        }
    }

    private void verify(SSLSocket socket) throws SSLException {
        SSLSession session = socket.getSession();
        if (!verifier.verify(hostname, session)) {
            System.err.println("Rejecting; hostname verification failed");
            throw new SSLPeerUnverifiedException("Failed to verify hostname: certificate mismatch");
        }
    }
}
