package erinsmatthew;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final Logger log = LogManager.getLogger( Config.class );

    private static final char NULL_CHAR = '\u0000';
    private static final String DEFAULT_OUTPUT_FORMAT = "JSON";

    private List< Repository > repositories;

    private Options options;

    private String directory;

    private boolean showOutput;
    private List< String > extensions;
    private boolean inspectJar;
    private int maxDepth;
    private String pomPropertiesFile;
    private String mavenJarPath;
    private String dbDriver;
    private String dbConnection;
    private String dbUser;
    private String dbPassword;
    private String selectDependencySql;
    private String addDependencySql;
    private String createDependencySql;
    private String schemaExistsSql;
    private String helpSummary;
    private OutputType outputFormat;
    private String outputFileName;
    private String groupIdAttribute;
    private String artifactIdAttribute;
    private String versionAttribute;
    private ArrayList< CommandLineOption > commandLineOptions;

    public boolean showOutput() {
        return showOutput;
    }

    public void setShowOutput( boolean showOutput ) {
        this.showOutput = showOutput;
    }

    public void parse( String[] args ) {
        options = new Options();

        //
        //  build command line options from values specified in JSON
        //
        for ( CommandLineOption opt : this.getCommandLineOptions() ) {
            Option o = new Option( opt.getShortName(), opt.getLongName(), opt.hasArg(),
                                   opt.getDescription() );

            if ( opt.hasArg() ) {
                o.setArgName( opt.getArgName() );
                o.setOptionalArg( opt.isArgOptional() );

                char valueSeparator = opt.getValueSeparator();

                if ( valueSeparator != NULL_CHAR ) {
                    o.setValueSeparator( valueSeparator );
                }

                int numArgs = opt.getNumArgs();

                if ( numArgs > 0 ) {
                    o.setArgs( numArgs );
                }
            }

            options.addOption( o );
        }

        //
        //  parse command line options
        //
        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = null;

        try {
            cmd = parser.parse( options, args );

            if ( cmd.hasOption( "d" ) ) {
                String dir = cmd.getOptionValue( "d" );

                if ( new File( dir ).isDirectory() ) {
                    this.setDirectory( dir );

                    if ( cmd.hasOption( "o" ) ) {
                        String outputFile = cmd.getOptionValue( "o" );

                        this.setShowOutput( true );

                        this.setOutputFileName( outputFile );

                        if ( cmd.hasOption( "f" ) ) {
                            String outputType = cmd.getOptionValue( "f" );

                            this.setOutputFormat( outputType );
                        } else {
                            this.setOutputFormat( DEFAULT_OUTPUT_FORMAT );
                        }
                    } else {
                        this.setShowOutput( false );
                    }
                } else {
                    log.error( "Invalid directory specified: {}.", dir );
                }
            }
        } catch ( ParseException e ) {
            log.fatal( "Error parsing options.", e );
        }
    }

    public void printHelp() {
        new HelpFormatter().printHelp( this.getHelpSummary(), this.options );
    }

    public boolean doInspectJar() {
        return inspectJar;
    }

    public void setInspectJar( boolean inspectJar ) {
        this.inspectJar = inspectJar;
    }

    public List< String > getExtensions() {
        return extensions;
    }

    public void setExtensions( List< String > extensions ) {
        this.extensions = extensions;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth( int maxDepth ) {
        this.maxDepth = maxDepth;
    }

    public String getPomPropertiesFile() {
        return pomPropertiesFile;
    }

    public void setPomPropertiesFile( String pomPropertiesFile ) {
        this.pomPropertiesFile = pomPropertiesFile;
    }

    public String getMavenJarPath() {
        return mavenJarPath;
    }

    public void setMavenJarPath( String mavenJarPath ) {
        this.mavenJarPath = mavenJarPath;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public void setOutputFileName( String outputFileName ) {
        this.outputFileName = outputFileName;
    }

    public OutputType getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat( String outputFormat ) {
        this.outputFormat = OutputType.valueOf( outputFormat );
    }

    public String getGroupIdAttribute() {
        return groupIdAttribute;
    }

    public void setGroupIdAttribute( String groupIdAttribute ) {
        this.groupIdAttribute = groupIdAttribute;
    }

    public String getArtifactIdAttribute() {
        return artifactIdAttribute;
    }

    public void setArtifactIdAttribute( String artifactIdAttribute ) {
        this.artifactIdAttribute = artifactIdAttribute;
    }

    public String getVersionAttribute() {
        return versionAttribute;
    }

    public void setVersionAttribute( String versionAttribute ) {
        this.versionAttribute = versionAttribute;
    }

    public String getAddDependencySql() {
        return addDependencySql;
    }

    public void setAddDependencySql( String addDependencySql ) {
        this.addDependencySql = addDependencySql;
    }

    public String getSelectDependencySql() {
        return selectDependencySql;
    }

    public void setSelectDependencySql( String selectDependencySql ) {
        this.selectDependencySql = selectDependencySql;
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public void setDbDriver( String dbDriver ) {
        this.dbDriver = dbDriver;
    }

    public String getDbConnection() {
        return dbConnection;
    }

    public void setDbConnection( String dbConnection ) {
        this.dbConnection = dbConnection;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser( String dbUser ) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword( String dbPassword ) {
        this.dbPassword = dbPassword;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory( String directory ) {
        this.directory = directory;
    }

    public String getHelpSummary() {
        return helpSummary;
    }

    public void setHelpSummary( String helpSummary ) {
        this.helpSummary = helpSummary;
    }

    public ArrayList< CommandLineOption > getCommandLineOptions() {
        return commandLineOptions;
    }

    public void setCommandLineOptions( ArrayList< CommandLineOption > commandLineOptions ) {
        this.commandLineOptions = commandLineOptions;
    }

    public String getCreateDependencySql() {
        return createDependencySql;
    }

    public void setCreateDependencySql( String createDependencySql ) {
        this.createDependencySql = createDependencySql;
    }

    public List< Repository > getRepositories() {
        return repositories;
    }

    public void setRepositories( List< Repository > repositories ) {
        this.repositories = repositories;
    }

    public String getSchemaExistsSql() {
        return schemaExistsSql;
    }

    public void setSchemaExistsSql( String schemaExistsSql ) {
        this.schemaExistsSql = schemaExistsSql;
    }
}
