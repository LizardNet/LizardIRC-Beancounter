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
 *
 * ============================================================================
 *
 * The code in this file is based on the EDDN example code for Java, which can be
 * found at <https://github.com/AnthorNet/EDDN/tree/master/examples/Java>
 */

package org.lizardirc.beancounter.commands.elite.eddn;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class EddnStreamer implements Runnable {
    private final EddnHandler<?> parent;
    private volatile boolean interrupt = false;

    public EddnStreamer(EddnHandler<?> parent) {
        this.parent = parent;
    }

    @Override
    public void run() {
        ZContext ctx = new ZContext();
        ZMQ.Socket client = ctx.createSocket(ZMQ.SUB);
        client.subscribe("".getBytes());
        client.setReceiveTimeOut(30000);

        client.connect(EddnHandler.ZEROMQ_ENDPOINT);
        System.out.println("EDDN Endpoint connected");
        ZMQ.Poller poller = ctx.createPoller(2);
        poller.register(client, ZMQ.Poller.POLLIN);
        byte[] output = new byte[256 * 1024];
        while (true) {
            if (interrupt) {
                return;
            }

            int poll = poller.poll(10);

            if (poll == ZMQ.Poller.POLLIN) {
                ZMQ.PollItem item = poller.getItem(poll);

                if (poller.pollin(0)) {
                    byte[] recv = client.recv(ZMQ.NOBLOCK);
                    if (recv.length > 0) {
                        // decompress
                        Inflater inflater = new Inflater();
                        inflater.setInput(recv);
                        try {
                            int outlen = inflater.inflate(output);
                            String outputString = new String(output, 0, outlen, "UTF-8");
                            // outputString contains a json message

                            parent.handleMessage(outputString);
                        } catch (DataFormatException | IOException e) {
                            parent.signalError(e);
                        }
                    }
                }
            }
        }
    }

    synchronized void interrupt() {
        interrupt = true;
    }
}
