package io.valhala.tigerlearning.app;

public class Student {

    private String[] delimiter = {";", "?"};
    private String id;
    private String[] reason;

    public Student(String id, String[] reason) {
        this.id = id;
        this.reason = reason;
    }

    public Student() {}

    public void setId(String id) {this.id = id;}

    public String getId() {return id;}

    public void setReason(String[] reason) {
        this.reason = reason;
    }

    public String getReason() {
        for (int x = 0; x > reason.length; x++) {
            return reason[x];
        }
        return "";
    }

}
