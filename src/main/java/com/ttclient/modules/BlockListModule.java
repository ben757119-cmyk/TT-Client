package com.ttclient.modules;

import java.util.List;
import java.util.Set;

/** Modules that expose a searchable block list in ClickGUI (BlockESP, XRay, …). */
public interface BlockListModule {
    String getUiSearch();
    void setUiSearch(String s);
    boolean isUiSearchFocused();
    void setUiSearchFocused(boolean focused);

    List<String> searchRegistry(String query, int limit);
    boolean addBlock(String id);
    boolean removeBlock(String id);
    Set<String> getSelected();

    default String listTitle() { return "blocks"; }
}
