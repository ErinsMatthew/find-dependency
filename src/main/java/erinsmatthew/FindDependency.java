package erinsmatthew;

import com.google.gson.Gson;
import erinsmatthew.MavenSearch.Response;
import erinsmatthew.MavenSearch.ResponseDetails;
import erinsmatthew.MavenSearch.ResponseDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FindDependency {
    private static final Logger log = LogManager.getLogger( FindDependency.class );

    private static final String CONFIG_JSON = "find-dependency.json";

    private static Config cfg;

    public static void main( String[] args ) {
        readConfigFile( args );

        try ( Connection conn = DriverManager.getConnection( cfg.getDbConnection(), cfg.getDbUser(),
                                                             cfg.getDbPassword() ) ) {
            Class.forName( cfg.getDbDriver() );

            if ( !DbUtil.schemaExists( cfg, conn ) ) {
                DbUtil.createSchema( cfg, conn );
            }

            //
            //  process each file in specified directory
            //
            Files.list( Paths.get( cfg.getDirectory() ) )
                 .filter( Files::isRegularFile )
                 .forEach( d -> processArchive( conn, d ) );
        } catch ( ClassNotFoundException e ) {
            log.fatal( "Error loading database driver.", e );
        } catch ( SQLException e ) {
            log.fatal( "Error running SQL.", e );
        } catch ( IOException e ) {
            log.error( e );
        }
    }

    private static void readConfigFile( String[] args ) {
        //
        //  read initial configuration from JSON file
        //
        Gson gson = new Gson();

        try ( final InputStream stream = FindDependency.class.getClassLoader()
                                                             .getResourceAsStream(
                                                                     CONFIG_JSON ); final Reader reader = new InputStreamReader(
                stream ) ) {
            cfg = gson.fromJson( reader, Config.class );
        } catch ( IOException e ) {
            log.fatal( "Error reading JSON.", e );
        }

        cfg.parse( args );
    }

    private static void processArchive( Connection conn, Path file ) {
        log.info( file.toString() );

        //
        //  calculate SHA1
        //
        String sha1 = HashUtil.calculateSHA1( file );

        log.debug( "{} (SHA1: {}", file.getFileName(), sha1 );

        //
        //  check database for cached information
        //
        Dependency d = DbUtil.lookup( cfg, conn, sha1 );

        if ( d == null || !d.isValid() ) {
            //
            //  check JAR file for pom.properties information.
            //  https://maven.apache.org/shared/maven-archiver/#pom-properties-content
            //
            if ( cfg.doInspectJar() ) {

            }

            //
            //  search Maven repositories
            //
            d = searchRepos( sha1 );

            //
            //  store results in database
            //
            if ( d.isValid() ) {
                DbUtil.save( cfg, conn, d );
            }
        }

        //
        //  output to POM or other locations
        //
    }

    private static Dependency searchRepos( String sha1 ) {
        //return all results, or one and done?
        List< Dependency > results = cfg.getRepositories()
                                        .stream()
                                        .map( r -> searchBySHA1( sha1, r ) )
                                        .collect( Collectors.toList() );

        Dependency dependency = null;

        if ( results.size() == 1 ) {
            dependency = results.get( 0 );
        } else {
            dependency = new Dependency();
        }

        return dependency;
    }

    private static Dependency searchBySHA1( String sha1, Repository repo ) {
        Dependency dependency = new Dependency();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder( URI.create( repo.getUrl()
                                                                      .replace( "{}", sha1 ) ) )
                                         .build();

        HttpResponse< String > response = null;

        try {
            response = client.send( request, HttpResponse.BodyHandlers.ofString() );

            if ( response != null ) {
                log.debug( response.body() );

                Gson gson = new Gson();

                Response mvnResponse = gson.fromJson( response.body(), Response.class );

                ResponseDetails details = mvnResponse.getResponse();
                log.debug( details.getNumFound() );
                if ( details.getNumFound() == 1 ) {
                    Optional< ResponseDoc > doc = details.getDocs()
                                                         .stream()
                                                         .findFirst();

                    if ( doc.isPresent() ) {
                        ResponseDoc d = doc.get();

                        dependency.setGroupId( d.getGroupId() );
                        dependency.setArtifactId( d.getArtifactId() );
                        dependency.setVersion( d.getVersion() );
                        dependency.setSha1( sha1 );

                        dependency.setValid( true );
                    }
                }
            }
        } catch ( IOException | InterruptedException e ) {
            e.printStackTrace();
        }

        return dependency;
    }
}
