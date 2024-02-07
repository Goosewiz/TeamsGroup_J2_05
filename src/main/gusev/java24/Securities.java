package main.gusev.java24;

import com.fasterxml.jackson.annotation.JsonProperty;
public class Securities {
    @JsonProperty("columns")
    private String[] columns;
    @JsonProperty("data")
    private String[][] data;

    public String[] getColumns() {
        return columns;
    }

    public void setColumns(String[] columns) {
        this.columns = columns;
    }

    public String[][] getData() {
        return data;
    }

    public void setData(String[][] data) {
        this.data = data;
    }
}
