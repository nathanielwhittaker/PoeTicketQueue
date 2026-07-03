package com.poeticketqueue.poe.api;

public class PathOfExileTradeApiResponse {

    private String id;
    private String[] result;
    private int total;

    public PathOfExileTradeApiResponse() {}

    public PathOfExileTradeApiResponse(String id, String[] result, int total) {
        this.id = id;
        this.result = result;
        this.total = total;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String[] getResult() { return result; }
    public void setResult(String[] result) { this.result = result; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}
