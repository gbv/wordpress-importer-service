package de.vzg.wis.mycore;

import java.io.IOException;
import java.io.StringReader;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class DocumentTypeAdapter extends TypeAdapter<Document> {

    @Override
    public void write(JsonWriter out, Document value) throws IOException {
        final String xml = new XMLOutputter(Format.getCompactFormat()).outputString(value);
        out.value(xml);
    }

    @Override
    public Document read(JsonReader in) throws IOException {
        final String xml = in.nextString();
        try {
            return new SAXBuilder().build(new StringReader(xml));
        } catch (JDOMException e) {
            throw new IOException(e);
        }
    }
}
