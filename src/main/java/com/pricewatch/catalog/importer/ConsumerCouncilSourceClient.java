package com.pricewatch.catalog.importer;

import com.pricewatch.catalog.CatalogImportException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;

@Component
public class ConsumerCouncilSourceClient {
    public static final int MAX_RESPONSE_BYTES = 10 * 1024 * 1024;
    private static final int TIMEOUT_MILLIS = 30_000;

    private final URI sourceUri;

    public ConsumerCouncilSourceClient(@Value("${pricewatch.source-url}") URI sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String sourceUrl() {
        return sourceUri.toString();
    }

    public byte[] fetch() {
        HttpURLConnection connection = null;
        try {
            URLConnection opened = sourceUri.toURL().openConnection();
            if (!(opened instanceof HttpURLConnection http)) {
                throw new CatalogImportException("The configured price source must use HTTP or HTTPS.");
            }
            connection = http;
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "PriceWatch-Student-Prototype/1.0");
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new CatalogImportException("Price source returned HTTP " + responseCode + ".");
            }
            int contentLength = connection.getContentLength();
            if (contentLength > MAX_RESPONSE_BYTES) {
                throw new CatalogImportException("The price source response exceeds the 10 MB limit.");
            }
            try (InputStream body = connection.getInputStream()) {
                return readLimited(body);
            }
        } catch (CatalogImportException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new CatalogImportException("Could not retrieve prices from the Consumer Council source.", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private byte[] readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > MAX_RESPONSE_BYTES) {
                throw new CatalogImportException("The price source response exceeds the 10 MB limit.");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
