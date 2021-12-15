package de.vzg.service.wordpress.model;

import java.util.ArrayList;

public class MayAuthorList {
    public MayAuthorList(){
        setAuthorIds(new ArrayList<>(0));
    }

    public MayAuthorList(ArrayList<Integer> authorIds) {
        this.authorIds = authorIds;
    }

    private ArrayList<Integer> authorIds;

    public ArrayList<Integer> getAuthorIds() {
        return authorIds;
    }

    public void setAuthorIds(ArrayList<Integer> authorIds) {
        this.authorIds = authorIds;
    }
}
