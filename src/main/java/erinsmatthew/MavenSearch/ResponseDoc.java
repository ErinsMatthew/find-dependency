package erinsmatthew.MavenSearch;

import java.util.List;

public class ResponseDoc {
    private String id;
    private String g;
    private String a;
    private String v;
    private String p;

    private long timestamp;

    private List< String > ec;
    private List< String > tags;

    public String getGroupId() {
        return g;
    }

    public void setGroupId( String g ) {
        this.g = g;
    }

    public String getArtifactId() {
        return a;
    }

    public void setArtifactId( String a ) {
        this.a = a;
    }

    public String getVersion() {
        return v;
    }

    public void setVersion( String v ) {
        this.v = v;
    }

    public String getPackaging() {
        return p;
    }

    public void setPackaging( String p ) {
        this.p = p;
    }
}
