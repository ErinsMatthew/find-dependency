package erinsmatthew;

public class Dependency {
    private boolean valid;
    private String groupId;
    private String artifactId;
    private String version;
    private String sha1;

    public String getSha1() {
        return sha1;
    }

    public void setSha1( String sha1 ) {
        this.sha1 = sha1;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId( String groupId ) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId( String artifactId ) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion( String version ) {
        this.version = version;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid( boolean valid ) {
        this.valid = valid;
    }

    @Override
    public String toString() {
        return String.format( "{groupId=%s, artifactId=%s, version=%s, sha1=%s}", this.getGroupId(),
                              this.getArtifactId(), this.getVersion(), this.getSha1() );
    }
}
