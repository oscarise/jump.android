package com.janrain.android.simpledemo;

public class LinkData {
    String identifier = null;
    String domainName = null;

    public LinkData(String identifier, String domainName) {
        super();
        this.identifier = identifier;
        this.domainName = domainName;
    }

    public LinkData() {
        // TODO Auto-generated constructor stub
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domain) {
        this.domainName = domain;
    }

}
