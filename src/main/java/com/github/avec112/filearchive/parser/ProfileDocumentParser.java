package com.github.avec112.filearchive.parser;

import com.github.avec112.filearchive.type.ProfileDocument;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class ProfileDocumentParser {

    public ProfileDocument parseDocument(Path path) throws IOException, SAXException, TikaException {
        try (FileInputStream inputstream = new FileInputStream(path.toFile())) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();
            parser.parse(inputstream, handler, metadata, context);

            String content = handler.toString();

            // Simplistically split the document. This might need adjustment based on actual document formatting.
            String[] parts = content.split("\n\n", 3);

            String title = parts.length > 0 ? parts[0].trim() : "";
            String ingress = parts.length > 1 ? parts[1].trim() : "";
            String mainContent = parts.length > 2 ? parts[2].trim() : "";

            ProfileDocument profileDocument = new ProfileDocument();
            profileDocument.setIngress(ingress);
            profileDocument.setTitle(title);
            profileDocument.setContent(mainContent);
            profileDocument.setFilePath(path.toString());

            return profileDocument;
        }
    }
}
