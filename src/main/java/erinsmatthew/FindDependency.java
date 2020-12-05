package erinsmatthew;

import com.google.gson.Gson;
import erinsmatthew.MavenSearch.Response;
import erinsmatthew.MavenSearch.ResponseDetails;
import erinsmatthew.MavenSearch.ResponseDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class FindDependency {
    private static final Logger log = LogManager.getLogger( FindDependency.class );

    private static final String CONFIG_JSON = "find-dependency.json";

    private static Config cfg;

    public static void main( String[] args ) {
        //
        //  load configuration from JSON and command line
        //
        loadConfig( args );

        String dir = cfg.getDirectory();

        if ( dir != null ) {
            Path directory = Paths.get( cfg.getDirectory() );

            try {
                Class.forName( cfg.getDbDriver() );
            } catch ( ClassNotFoundException e ) {
                log.fatal( "Error loading database driver.", e );
            }

            try ( Connection conn = DriverManager.getConnection( cfg.getDbConnection(),
                                                                 cfg.getDbUser(),
                                                                 cfg.getDbPassword() ) ) {
                if ( !DbUtil.schemaExists( cfg, conn ) ) {
                    DbUtil.createSchema( cfg, conn );
                }

                //
                //  process each file in specified directory;
                //  use walk() instead of list() for recursive
                //
                // TODO: Filter files by extension? Allow for config.
                Files.list( directory )
                     .filter( Files::isRegularFile )
                     .forEach( f -> processArchive( conn, f ) );
            } catch ( SQLException e ) {
                log.fatal( "Error running SQL.", e );
            } catch ( IOException e ) {
                log.error( e );
            }
        } else {
            cfg.printHelp();

            log.error( "Directory is not specified." );
        }
    }

    private static void loadConfig( String[] args ) {
        //
        //  read initial configuration from JSON file
        //
        Gson gson = new Gson();

        try ( final InputStream stream = FindDependency.class.getClassLoader()
                                                             .getResourceAsStream( CONFIG_JSON );
              final Reader reader = new InputStreamReader( stream, StandardCharsets.UTF_8 ) ) {
            cfg = gson.fromJson( reader, Config.class );
        } catch ( IOException e ) {
            log.fatal( "Error reading JSON.", e );
        }

        //
        //  supplement and override configuration with command line
        //
        cfg.parse( args );
    }

    private static void processArchive( Connection conn, Path file ) {
        //
        //  calculate SHA1 of file; SHA1 is what Maven uses for
        //  file integrity checks
        //
        String sha1 = HashUtil.calculateSHA1( file );

        log.debug( "{} (SHA1: {})", file.getFileName(), sha1 );

        //
        //  check database for cached dependency information
        //
        Dependency dependency = DbUtil.lookup( cfg, conn, sha1 );

        if ( !dependency.isValid() ) {
            //
            //  check JAR file for pom.properties
            //
            if ( cfg.doInspectJar() ) {
                dependency = inspectJar( file, sha1 );
            }

            //
            //  search Maven repositories
            //
            if ( !dependency.isValid() ) {
                dependency = searchRepos( sha1 );
            }

            //
            //  store results in database
            //
            if ( dependency.isValid() ) {
                DbUtil.save( cfg, conn, dependency );
            }
        }

        //
        //  output to POM or other locations
        //
        log.info( dependency );
    }

    private static Dependency inspectJar( Path file, String sha1 ) {
        Dependency dependency = new Dependency();

        //
        //  check JAR file for pom.properties information.
        //  https://maven.apache.org/shared/maven-archiver/#pom-properties-content
        //
        try ( FileSystem fs = FileSystems.newFileSystem( file, null ) ) {
            Path mavenDirectory = fs.getPath( "/META-INF/maven" );

            if ( Files.exists( mavenDirectory ) ) {
                //
                //  look for pom.properties file
                //
                Optional< Path > pomProperties =
                        Files.find( mavenDirectory, cfg.getMaxDepth(), ( f, attr ) -> {
                            if ( Files.isDirectory( f ) || !Files.isReadable( f ) ) {
                                return false;
                            }

                            return f.getFileName()
                                    .toString()
                                    .equalsIgnoreCase( cfg.getPomPropertiesFile() );
                        } )
                             .findFirst();

                if ( pomProperties.isPresent() ) {
                    //
                    //  read pom.properties
                    //
                    try ( BufferedReader reader = Files.newBufferedReader( pomProperties.get(),
                                                                           StandardCharsets.UTF_8 ) ) {
                        Properties properties = new Properties();

                        properties.load( reader );

                        dependency.setGroupId( properties.getProperty( "groupId" ) );
                        dependency.setArtifactId( properties.getProperty( "artifactId" ) );
                        dependency.setVersion( properties.getProperty( "version" ) );

                        dependency.setSha1( sha1 );

                        dependency.setValid( true );
                    }
                }
            }
        } catch ( IOException e ) {
            log.fatal( "Error reading JAR file.", e );
        }

        return dependency;
    }

    private static Dependency searchRepos( String sha1 ) {
        //return all results, or one and done?
        List< Dependency > results = cfg.getRepositories()
                                        .stream()
                                        .map( r -> searchBySHA1( sha1, r ) )
                                        .collect( Collectors.toList() );

        Dependency dependency;

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

        HttpResponse< String > response;

        try {
            response = client.send( request, HttpResponse.BodyHandlers.ofString() );

            if ( response != null ) {
                log.debug( response.body() );

                Gson gson = new Gson();

                Response mvnResponse = gson.fromJson( response.body(), Response.class );

                ResponseDetails details = mvnResponse.getResponse();

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
            log.fatal( "Error searching Maven.", e );
        }

        return dependency;
    }
}