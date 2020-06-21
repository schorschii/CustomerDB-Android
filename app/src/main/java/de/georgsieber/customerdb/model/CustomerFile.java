package de.georgsieber.customerdb.model;

public class CustomerFile {
    public String mName;
    public byte[] mContent;

    public CustomerFile(String _name, byte[] _content) {
        mName = _name;
        mContent = _content;
    }
}
