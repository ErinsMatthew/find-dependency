package erinsmatthew;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HashUtil {
    private static final Logger log = LogManager.getLogger( HashUtil.class );

    @SuppressWarnings( "deprecation" )
    public static String calculateSHA1( Path file ) {
        HashCode hashCode = null;

        try {
            if ( Files.isReadable( file ) ) {
                byte[] bytes = Files.readAllBytes( file );

                hashCode = Hashing.sha1()
                                  .hashBytes( bytes );
            } else {
                log.info( "Unable to read contents of file {}.", file.toString() );
            }
        } catch ( IOException e ) {
            log.fatal( String.format( "Error calculating hash for %s", file.toString() ), e );
        }

        return hashCode != null ? hashCode.toString() : "";
    }
}
