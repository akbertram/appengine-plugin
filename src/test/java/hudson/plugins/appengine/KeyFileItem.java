package hudson.plugins.appengine;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.net.MediaType;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.util.FileItemHeadersImpl;

import java.io.*;

public class KeyFileItem implements FileItem {


    private final byte[] json;

    public KeyFileItem() throws IOException {
        String path = System.getenv("SERVICE_ACCOUNT_KEY");
        if(Strings.isNullOrEmpty(path)) {
            throw new RuntimeException("The SERVICE_ACCOUNT_KEY environment variable must be set with the path" +
                    " to the json key for the jenkinsplugintest service account.");
        }
        json = Files.toByteArray(new File(path));

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(json);
    }

    @Override
    public String getContentType() {
        return MediaType.JSON_UTF_8.toString();
    }

    @Override
    public String getName() {
        return "key.json";
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    @Override
    public long getSize() {
        return json.length;
    }

    @Override
    public byte[] get() {
        return json;
    }

    @Override
    public String getString(String encoding) throws UnsupportedEncodingException {
        return new String(json, encoding);
    }

    @Override
    public String getString() {
        return new String(json, Charsets.UTF_8);
    }

    @Override
    public void write(File file) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFieldName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFieldName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFormField() {
        return false;
    }

    @Override
    public void setFormField(boolean state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileItemHeaders getHeaders() {
        return new FileItemHeadersImpl();
    }

    @Override
    public void setHeaders(FileItemHeaders headers) {
        
    }
}
