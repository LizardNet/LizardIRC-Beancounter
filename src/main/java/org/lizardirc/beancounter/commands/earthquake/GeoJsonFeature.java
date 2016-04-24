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

package org.lizardirc.beancounter.commands.earthquake;

import java.util.Objects;
import java.util.stream.Stream;

class GeoJsonFeature {
    public GeoJsonFeatureProperty properties;
    public GeoJsonFeatureGeometry geometry;
    public String id;

    /**
     * Returns all IDs that identify this GeoJsonFeature, making no distinction between primary and alternate IDs.
     *
     * @return A stream of ID strings that identify this GeoJsonFeature
     */
    public Stream<String> getAllIds() {
        return Stream.concat(properties.getAlternateIds(), Stream.of(id))
            .distinct();
    }

    // hashCode() and equals() below should only check fields that we report to IRC in FeedChecker.run(), AND the id field.

    @Override
    public int hashCode() {
        // Note that properties.magnitude and geometry.coordinates[2], despite being used for equality comparison in equals(),
        // are deliberately not used here in hashCode() because hash codes are explicitly allowed to collide, and I don't
        // want to deal with floating point arithmetic nonsense here.  In any case, since we expect id to be unique to
        // every event, that should ensure that hash codes are unique (enough) between events.
        // properties.eventTime is divided by 1000 here because we don't care about microseconds.
        return Objects.hash(id.toLowerCase(), properties.type, properties.magType, properties.place, properties.eventTime / 1000L,
            Math.round(properties.reportedIntensity), Math.round(properties.measuredIntensity), properties.tsunami, properties.status.toLowerCase(),
            properties.url.toLowerCase());
    }

    public boolean canEqual(Object other) {
        return other instanceof GeoJsonFeature;
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof GeoJsonFeature) {
            GeoJsonFeature that = (GeoJsonFeature) other;
            result = that.canEqual(this)
                && this.id.equalsIgnoreCase(that.id)
                && this.properties.type.equals(that.properties.type)
                && this.properties.magType.equals(that.properties.magType)
                && Math.abs(this.properties.magnitude - that.properties.magnitude) < 0.01F // We only care about two decimals of precision
                && this.properties.place.equals(that.properties.place)
                && Math.abs(this.properties.eventTime - that.properties.eventTime) < 1000L // We don't care about the microseconds here
                && Math.abs(this.geometry.coordinates[2] - that.geometry.coordinates[2]) < 0.01F // We only care about two decimals of precision
                && Math.round(this.properties.reportedIntensity) == Math.round(that.properties.reportedIntensity) // We only care about the rounded value
                && Math.round(this.properties.measuredIntensity) == Math.round(that.properties.measuredIntensity) // We only care about the rounded value
                && this.properties.tsunami == that.properties.tsunami
                && this.properties.status.equalsIgnoreCase(that.properties.status)
                && this.properties.url.equalsIgnoreCase(that.properties.url);
        }

        return result;
    }
}
