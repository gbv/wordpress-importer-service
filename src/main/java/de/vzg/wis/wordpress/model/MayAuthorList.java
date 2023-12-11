package de.vzg.wis.wordpress.model;

import java.util.ArrayList;
import java.util.List;

public class MayAuthorList {
    public MayAuthorList(){
        setAuthorIds(new ArrayList<>());
        setAuthorNames(new ArrayList<>());
    }

    private List<Integer> authorIds;

    private List<String> authorNames;

    public List<Integer> getAuthorIds() {
        return authorIds;
    }

    public void setAuthorIds(List<Integer> authorIds) {
        this.authorIds = authorIds;
    }

    public List<String> getAuthorNames() {
        return authorNames;
    }

    public void setAuthorNames(List<String> authorNames) {
        this.authorNames = authorNames;
    }

}
