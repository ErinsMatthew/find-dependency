package erinsmatthew;

public enum OutputType {
    JSON( "JSON" ), POM( "POM" );

    private final String description;

    OutputType( String description ) {
        this.description = description;
    }
}
