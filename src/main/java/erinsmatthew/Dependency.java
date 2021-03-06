package erinsmatthew;

public class Dependency {
    private transient boolean valid;

    private String groupId;
    private String artifactId;
    private String version;

    private transient String sha1;

    private String originalFileName;

    @Override
    public String toString() {
        return String.format( "{groupId=%s, artifactId=%s, version=%s, sha1=%s}", this.getGroupId(),
                              this.getArtifactId(), this.getVersion(), this.getSha1() );
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName( String originalFileName ) {
        this.originalFileName = originalFileName;
    }

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
}
