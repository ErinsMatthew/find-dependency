package erinsmatthew;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class DbUtil {
    private static final Logger log = LogManager.getLogger( DbUtil.class );

    public static void createSchema( Config config, Connection conn ) {
        try ( Statement stmt = conn.createStatement() ) {
            stmt.execute( config.getCreateDependencySql() );
        } catch ( SQLException e ) {
            log.fatal( "Error running SQL.", e );
        }
    }

    public static boolean schemaExists( Config config, Connection conn ) {
        boolean exists = false;

        ResultSet rs = null;

        try ( Statement stmt = conn.createStatement() ) {
            rs = stmt.executeQuery( config.getSchemaExistsSql() );

            exists = rs.next();
        } catch ( SQLException e ) {
            log.fatal( "Error running SQL.", e );
        } finally {
            DbUtil.close( rs );
        }

        return exists;
    }

    private static void close( ResultSet rs ) {
        try {
            if ( rs != null ) {
                rs.close();
            }
        } catch ( SQLException e ) {
            log.fatal( "Error closing ResultSet.", e );
        }
    }

    public static Dependency lookup( Config config, Connection conn, String sha1 ) {
        ResultSet rs = null;

        Dependency dependency = new Dependency();

        String selectString = config.getSelectDependencySql();

        try ( PreparedStatement stmt = conn.prepareStatement( selectString ) ) {
            stmt.setString( 1, sha1 );

            rs = stmt.executeQuery();

            if ( rs.next() ) {
                dependency.setGroupId( rs.getString( "groupId" ) );
                dependency.setArtifactId( rs.getString( "artifactId" ) );
                dependency.setVersion( rs.getString( "version" ) );

                dependency.setSha1( sha1 );

                dependency.setValid( true );
            }
        } catch ( SQLException e ) {
            log.fatal( "Error running SQL.", e );
        } finally {
            DbUtil.close( rs );
        }

        return dependency;
    }

    public static void save( Config config, Connection conn, Dependency d ) {
        String insertString = config.getAddDependencySql();

        try ( PreparedStatement stmt = conn.prepareStatement( insertString ) ) {
            stmt.setString( 1, d.getGroupId() );
            stmt.setString( 2, d.getArtifactId() );
            stmt.setString( 3, d.getVersion() );

            stmt.setString( 4, d.getSha1() );

            stmt.executeUpdate();

            log.debug( "Added dependency {}.", d.toString() );
        } catch ( SQLException e ) {
            log.fatal( "Error running SQL.", e );
        }
    }
}
