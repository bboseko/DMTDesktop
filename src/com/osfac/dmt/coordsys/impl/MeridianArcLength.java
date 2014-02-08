package com.osfac.dmt.coordsys.impl;

import com.osfac.dmt.coordsys.Spheroid;

/**
 * @author $Author: javamap $
 * @version $Revision: 4 $  <pre>
 * $Id: MeridianArcLength.java 4 2005-06-16 15:27:48Z javamap $
 * $Date: 2005-06-16 17:27:48 +0200 (jeu., 16 juin 2005) $
 *
 *
 *  $Log$
 *  Revision 1.1  2005/06/16 15:25:29  javamap
 *  *** empty log message ***
 *
 *  Revision 1.2  2005/05/03 15:23:55  javamap
 *  *** empty log message ***
 *
 *  Revision 1.2  2003/11/05 05:13:43  dkim
 *  Added global header; cleaned up Javadoc.
 *
 *  Revision 1.1  2003/09/15 20:26:12  jaquino
 *  Reprojection
 *
 *  Revision 1.2  2003/07/25 17:01:04  gkostadinov
 *  Moved classses reponsible for performing the basic projection to a new
 *  package -- base.
 *
 *  Revision 1.1  2003/07/24 23:14:44  gkostadinov
 *  adding base projection classes
 *
 *  Revision 1.1  2003/06/20 18:34:31  gkostadinov
 *  Entering the source code into the CVS.
 * </pre>
 */
public class MeridianArcLength {

    public double s, a0, a2, a4, a6, a8;

    public void compute(Spheroid spheroid, double lat, int diff) {
//  Returns the meridian arc length given the latitude
        double e2;
//  Returns the meridian arc length given the latitude
        double e4;
//  Returns the meridian arc length given the latitude
        double e6;
//  Returns the meridian arc length given the latitude
        double e8;
        double a;
        double e;
        a = spheroid.getA();
        e = spheroid.getE();
        e2 = e * e;
        e4 = e2 * e2;
        e6 = e4 * e2;
        e8 = e4 * e4;
        a0 = 1.0 - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0 - 175.0 * e8 / 16384.0;
        a2 = 3.0 / 8.0 * (e2 + e4 / 4.0 + 15.0 * e6 / 128.0 - 455.0 * e8 / 4096.0);
        a4 = 15.0 / 256.0 * (e4 + 3.0 * e6 / 4.0 - 77.0 * e8 / 128.0);
        a6 = 35.0 / 3072.0 * (e6 - 41.0 * e8 / 32.0);
        a8 = -315.0 * e8 / 131072.0;
        if (diff == 0) {
            s = a * (a0 * lat - a2 * Math.sin(2.0 * lat) + a4 * Math.sin(4.0 * lat)
                    - a6 * Math.sin(6.0 * lat) + a8 * Math.sin(8.0 * lat));
        } else {
            s = a0 * lat - 2.0 * a2 * Math.cos(2.0 * lat) + 4.0 * a4 * Math.cos(4.0 * lat)
                    - 6.0 * a6 * Math.cos(6.0 * lat) + 8.0 * a8 * Math.cos(8.0 * lat);
        }
    }
}
