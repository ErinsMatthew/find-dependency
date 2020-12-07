package erinsmatthew;

public class CommandLineOption {
    private String shortName;
    private String longName;
    private boolean hasArg;
    private String argName;
    private String description;
    private boolean argOptional;
    private char valueSeparator;
    private int numArgs;

    public boolean hasArg() {
        return hasArg;
    }

    public void setHasArg( boolean hasArg ) {
        this.hasArg = hasArg;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName( String longName ) {
        this.longName = longName;
    }

    public String getArgName() {
        return argName;
    }

    public void setArgName( String argName ) {
        this.argName = argName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public boolean isArgOptional() {
        return argOptional;
    }

    public void setArgOptional( boolean argOptional ) {
        this.argOptional = argOptional;
    }

    public char getValueSeparator() {
        return valueSeparator;
    }

    public void setValueSeparator( char valueSeparator ) {
        this.valueSeparator = valueSeparator;
    }

    public int getNumArgs() {
        return numArgs;
    }

    public void setNumArgs( int numArgs ) {
        this.numArgs = numArgs;
    }
}
