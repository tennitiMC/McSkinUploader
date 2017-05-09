package io.monchi.mcskinuploader;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Mon_chi on 2017/05/09.
 */
public class Main {

    public static Log CONSOLE_LOG = LogFactory.getLog("ConsoleLogger");

    public static void main(String[] args) {
        if (args.length < 2){
            CONSOLE_LOG.error("Invalid usage! run \"java -jar McSkinUploader.jar <UUID> <ACCESS_TOKEN> [Interval(Minute)]\".");
            return;
        }

        String uuid = args[0].replaceAll("-", "");
        String token = args[1];
        int interval = 3;
        if (args.length > 2){
            try{
                interval = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException e){
                CONSOLE_LOG.warn("An interval must be integer. It has been set to 10 minutes (Default).");
            }
        }

        CONSOLE_LOG.info("UUID: " + uuid);
        CONSOLE_LOG.info("TOKEN: " + StringUtils.repeat('*', token.length() - 4) + token.substring(token.length() - 4));
        CONSOLE_LOG.info("INTERVAL: " + interval + " minutes");


        Timer timer = new Timer();
        try {
            TimerTask task = new UploadTask(uuid, token);
            timer.scheduleAtFixedRate(task, 0, 1000 * 60 * interval);
        } catch (FileNotFoundException e) {
            CONSOLE_LOG.warn("No skins are found.");
            System.exit(0);
        } catch (IOException e) {
            CONSOLE_LOG.error("An error has been occurred.");
            e.printStackTrace();
            System.exit(0);
        }

    }
}
