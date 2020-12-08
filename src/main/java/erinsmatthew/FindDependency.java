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
import java.util.AbstractMap;
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
                Files.list( directory )
                     .parallel()
                     .filter( Files::isRegularFile )
                     .filter( FindDependency::isValidExtension )
                     .forEach( f -> processArchive( conn, f ) );

                if ( cfg.showOutput() ) {
                    //
                    //  build a list of dependencies to show
                    //
                    List< Dependency > dependencies = Files.list( directory )
                                                           .parallel()
                                                           .filter( Files::isRegularFile )
                                                           .filter(
                                                                   FindDependency::isValidExtension )
                                                           .map( f -> new AbstractMap.SimpleEntry<>(
                                                                   f,
                                                                   HashUtil.calculateSHA1( f ) ) )
                                                           .map( se -> {
                                                               Dependency d =
                                                                       DbUtil.lookup( cfg, conn,
                                                                                      se.getValue() );

                                                               if ( d.isValid() ) {
                                                                   d.setOriginalFileName(
                                                                           se.getKey()
                                                                             .getFileName()
                                                                             .toString() );
                                                               }

                                                               return d;
                                                           } )
                                                           .filter( Dependency::isValid )
                                                           .collect( Collectors.toList() );

                    //
                    //  write dependencies to a file
                    //
                    switch ( cfg.getOutputFormat() ) {
                        case JSON:
                        default:
                            try ( FileWriter writer = new FileWriter( cfg.getOutputFileName() ) ) {
                                new Gson().toJson( dependencies, writer );
                            } catch ( IOException e ) {
                                log.fatal( "Error writing JSON to file.", e );
                            }
                            break;

                        case POM:
                            try ( FileWriter writer = new FileWriter( cfg.getOutputFileName() ) ) {
                                dependencies.stream()
                                            .map( d -> {
                                                return String.format(
                                                        "%n<!-- File: %s -->%n<dependency>%n    <groupId>%s</groupId>%n    <artifactId>%s</artifactId>%n    <version>%s</version>%n</dependency>%n",
                                                        d.getOriginalFileName(), d.getGroupId(),
                                                        d.getArtifactId(), d.getVersion() );
                                            } )
                                            .forEach( s -> {
                                                try {
                                                    writer.write( s );
                                                } catch ( IOException e ) {
                                                    log.fatal( "Error writing dependency to POM.",
                                                               e );
                                                }
                                            } );
                            } catch ( IOException e ) {
                                log.fatal( "Error writing POM to file.", e );
                            }
                            break;
                    }
                }
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

    private static boolean isValidExtension( Path file ) {
        boolean matches = false;

        for ( String extension : cfg.getExtensions() ) {
            if ( file.getFileName()
                     .toString()
                     .toLowerCase()
                     .endsWith( extension.toLowerCase() ) ) {
                matches = true;

                break;
            }
        }

        return matches;
    }

    private static void loadConfig( String[] args ) {
        //
        //  read initial configuration from JSON file
        //
        Gson gson = new Gson();

        try ( final InputStream stream = FindDependency.class.getClassLoader()
                                                             .getResourceAsStream( CONFIG_JSON ) ) {
            assert stream != null;
            try ( final Reader reader = new InputStreamReader( stream, StandardCharsets.UTF_8 ) ) {
                cfg = gson.fromJson( reader, Config.class );
            }
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
        } else {
            log.info( "Found {} in cache.", file.getFileName() );
        }
    }

    private static Dependency inspectJar( Path file, String sha1 ) {
        Dependency dependency = new Dependency();

        log.info( "Inspecting {} for pom.properties.", file.getFileName() );

        //
        //  check JAR file for pom.properties information.
        //  https://maven.apache.org/shared/maven-archiver/#pom-properties-content
        //
        try ( FileSystem fs = FileSystems.newFileSystem( file, null ) ) {
            Path mavenDirectory = fs.getPath( cfg.getMavenJarPath() );

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
                    log.info( "Found Maven pom.properties file for {}.", file.getFileName() );

                    //
                    //  read pom.properties
                    //
                    try ( BufferedReader reader = Files.newBufferedReader( pomProperties.get(),
                                                                           StandardCharsets.UTF_8 ) ) {
                        Properties properties = new Properties();

                        properties.load( reader );

                        dependency.setGroupId(
                                properties.getProperty( cfg.getGroupIdAttribute() ) );
                        dependency.setArtifactId(
                                properties.getProperty( cfg.getArtifactIdAttribute() ) );
                        dependency.setVersion(
                                properties.getProperty( cfg.getVersionAttribute() ) );

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
        Dependency dependency;

        List< Dependency > results = cfg.getRepositories()
                                        .stream()
                                        .map( r -> searchBySHA1( sha1, r ) )
                                        .collect( Collectors.toList() );

        if ( results.size() == 1 ) {
            dependency = results.get( 0 );
        } else {
            log.debug( "Unable to identity a singular dependency with this SHA1." );

            dependency = new Dependency();
        }

        return dependency;
    }

    private static Dependency searchBySHA1( String sha1, Repository repo ) {
        Dependency dependency = new Dependency();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder( URI.create( repo.getUrl()
                                                                      .replace(
                                                                              repo.getReplacementToken(),
                                                                              sha1 ) ) )
                                         .build();

        HttpResponse< String > response;

        try {
            log.info( "Searching Maven repository {} for {}.", repo.getName(), sha1 );

            response = client.send( request, HttpResponse.BodyHandlers.ofString() );

            if ( response != null ) {
                log.debug( "Maven Response = {}", response.body() );

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