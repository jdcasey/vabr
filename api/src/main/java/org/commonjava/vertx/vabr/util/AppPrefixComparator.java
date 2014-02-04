package org.commonjava.vertx.vabr.util;

import java.util.Comparator;

import org.commonjava.vertx.vabr.ApplicationRouter;

/**
 * Comparator to sort null or shorter prefixes to the back of the line, 
 * so the most specific prefixes are examined first when routing a request.
 * 
 * NOTE: If there are a large number of applications, this approach is likely
 * to be inefficient, since more specific path prefixes naturally means they're
 * less likely to match a request (all else being equal).
 * 
 * @author jdcasey
 */
public class AppPrefixComparator
    implements Comparator<ApplicationRouter>
{

    @Override
    public int compare( final ApplicationRouter o1, final ApplicationRouter o2 )
    {
        final String o1p = o1.getPrefix();
        final String o2p = o2.getPrefix();

        if ( o1p == null && o2p == null )
        {
            return 0;
        }
        if ( o1p == null && o2p != null )
        {
            return 1;
        }
        if ( o1p != null && o2p == null )
        {
            return -1;
        }

        return o2p.compareTo( o1p );
    }

}
