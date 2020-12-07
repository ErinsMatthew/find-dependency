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
                dependency.setGroupId( rs.getString( config.getGroupIdAttribute() ) );
                dependency.setArtifactId( rs.getString( config.getArtifactIdAttribute() ) );
                dependency.setVersion( rs.getString( config.getVersionAttribute() ) );

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

    public static void save( Config config, Connection conn, Dependency dep ) {
        String insertString = config.getAddDependencySql();

        try ( PreparedStatement stmt = conn.prepareStatement( insertString ) ) {
            stmt.setString( 1, dep.getGroupId() );
            stmt.setString( 2, dep.getArtifactId() );
            stmt.setString( 3, dep.getVersion() );

            stmt.setString( 4, dep.getSha1() );

            stmt.executeUpdate();

            log.debug( "Added dependency {} to cache.", dep.toString() );
        } catch ( SQLException e ) {
            log.fatal( "Error running SQL.", e );
        }
    }
}
