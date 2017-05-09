package io.monchi.mcskinuploader;

import com.google.api.client.http.*;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Created by Mon_chi on 2017/05/09.
 */
public class UploadTask extends TimerTask {

    private String uuid;
    private String token;

    private Gson gson;
    private HttpRequestFactory factory;

    private File outputFile;
    private List<File> files;

    public UploadTask(String uuid, String token) throws IOException {
        this.uuid = uuid;
        this.token = token;

        this.gson = new Gson();
        ApacheHttpTransport transport = new ApacheHttpTransport();
        this.factory = transport.createRequestFactory();

        this.outputFile = new File("output.log");
        if (!outputFile.exists())
            outputFile.createNewFile();
        this.files = new ArrayList<>();
        File dir = new File("skins");
        if (!dir.exists())
            dir.mkdir();
        if (dir.listFiles() == null || dir.listFiles().length == 0){
            this.cancel();
            throw new FileNotFoundException();
        }
        files.addAll(Arrays.asList(dir.listFiles()));

        Main.CONSOLE_LOG.info(files.size() + " skin files are found.");
    }

    @Override
    public void run() {
        File skinFile = files.remove(0);
        if (!skinFile.exists()) {
            Main.CONSOLE_LOG.warn(skinFile.getName() + " has been skipped because it is not found.");
            return;
        }

        MultipartContent content = new MultipartContent();
        content.addPart(new MultipartContent.Part(new FileContent("image/png", skinFile)).setHeaders(new HttpHeaders().set("Content-Disposition", "form-data; name=\"file\"; filename=\"" + skinFile.getName() + "\"")));
        content.addPart(new MultipartContent.Part(new ByteArrayContent(null, "".getBytes())).setHeaders(new HttpHeaders().set("Content-Disposition", "form-data; name=\"model\"")));

        String uploadResponse = null;
        try {
            HttpRequest uploadRequest = factory.buildPutRequest(new GenericUrl("https://api.mojang.com/user/profile/" + uuid + "/skin"), content);
            uploadRequest.setHeaders(new HttpHeaders().setAuthorization("Bearer " + token));
            uploadResponse = uploadRequest.execute().parseAsString();
        } catch (IOException e) {
            Main.CONSOLE_LOG.error("An error has been occurred while uploading " + skinFile.getName());
            e.printStackTrace();
            return;
        }
        if (!uploadResponse.isEmpty()){
            Main.CONSOLE_LOG.error("Failed to upload " + skinFile.getName());
            return;
        }

        String getResponse;
        try {
            HttpRequest getRequest = factory.buildGetRequest(new GenericUrl("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid));
            getRequest.setHeaders(new HttpHeaders().setAuthorization("Bearer " + token));
            getResponse = getRequest.execute().parseAsString();
        } catch (IOException e) {
            Main.CONSOLE_LOG.error("An error has been occurred while checking a URL of " + skinFile.getName());
            e.printStackTrace();
            return;
        }

        String decoded = new String(Base64.getDecoder().decode(gson.fromJson(getResponse, JsonObject.class).getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString()));
        String url = gson.fromJson(decoded, JsonObject.class).getAsJsonObject("textures").get("SKIN").getAsJsonObject().get("url").getAsString();
        try {
            FileUtils.write(outputFile, skinFile.getName() + ": " + url, "UTF-8", true);
        } catch (IOException e) {
            Main.CONSOLE_LOG.error("An error has been occurred while saving a URL of " + skinFile.getName());
            e.printStackTrace();
        }
        Main.CONSOLE_LOG.info(skinFile.getName() + " has successfully been uploaded.");

        if (files.size() == 0){
            Main.CONSOLE_LOG.info("All processes are finished.");
            System.exit(0);
        }
    }
}
