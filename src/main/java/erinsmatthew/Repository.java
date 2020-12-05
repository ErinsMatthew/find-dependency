package erinsmatthew;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Repository {
    private static final Logger log = LogManager.getLogger( Repository.class );

    private String id;
    private String name;
    private String url;

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
