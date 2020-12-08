package erinsmatthew.MavenSearch;

import java.util.List;

public class ResponseDetails {
    private int numFound;
    private int start;
    private List< ResponseDoc > docs;

    public int getNumFound() {
        return numFound;
    }

    public void setNumFound( int numFound ) {
        this.numFound = numFound;
    }

    public int getStart() {
        return start;
    }

    public void setStart( int start ) {
        this.start = start;
    }

    public List< ResponseDoc > getDocs() {
        return docs;
    }

    public void setDocs( List< ResponseDoc > docs ) {
        this.docs = docs;
    }
}
