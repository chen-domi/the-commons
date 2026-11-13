package com.thecommons.backend.nlp;

public interface ItemDescriptionModel {
    boolean isAvailable();
    String extract(String description);
}
