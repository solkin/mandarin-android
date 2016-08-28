package com.tomclaw.mandarin.util;

import java.io.Serializable;

/**
 * Created by ivsolkin on 27.08.16.
 */
public class NameValuePair implements Serializable {

    private String name = null;
    private String value = null;

    public NameValuePair() {
        this(null, null);
    }

    public NameValuePair(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return ("name=" + name + ", " + "value=" + value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NameValuePair that = (NameValuePair) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return value != null ? value.equals(that.value) : that.value == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
