package erinsmatthew;

public class Repository {
    private String id;
    private String name;
    private String url;
    private String replacementToken;

    public String getReplacementToken() {
        return replacementToken;
    }

    public void setReplacementToken( String replacementToken ) {
        this.replacementToken = replacementToken;
    }

    public String getId() {
        return id;
    }

    public void setId( String id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl( String url ) {
        this.url = url;
    }
}
