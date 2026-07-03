package com.poeticketqueue.poe.item;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum StatGroupType {
    @JsonProperty("and")   AND,
    @JsonProperty("not")   NOT,
    @JsonProperty("count") COUNT;

    public String toApiValue() { return name().toLowerCase(); }
}
