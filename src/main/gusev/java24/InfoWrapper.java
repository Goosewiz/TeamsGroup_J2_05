package main.gusev.java24;

import com.fasterxml.jackson.annotation.JsonProperty;
public class InfoWrapper {
    @JsonProperty("securities")
    Securities info;

    public Securities getInfo() {
        return info;
    }

    public void setInfo(Securities info) {
        this.info = info;
    }

    public String[] getInfoModel() {
        return info.getColumns();
    }
    public String[][] getAnswer(){
        return info.getData();
    }
}
