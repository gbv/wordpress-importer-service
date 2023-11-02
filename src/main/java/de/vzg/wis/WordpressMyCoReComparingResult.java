package de.vzg.wis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WordpressMyCoReComparingResult {

    private List<PostInfo> notImportedPosts;

    private Map<String,PostInfo> mycoreIDPostMap;

    private Map<String, String> mycoreIDValidationMap;

    public WordpressMyCoReComparingResult() {
        this.notImportedPosts = new LinkedList<>();
        this.mycoreIDPostMap = new HashMap<>();
        this.mycoreIDValidationMap=new HashMap<>();
    }

    public List<PostInfo> getNotImportedPosts() {
        return notImportedPosts;
    }

    public Map<String,PostInfo> getMyCoReIDPostMap() {
        return mycoreIDPostMap;
    }

    public Map<String, String> getMycoreIDValidationMap() {
        return mycoreIDValidationMap;
    }
}
