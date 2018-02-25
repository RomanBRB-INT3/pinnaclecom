package com.pinnaclecom;

import java.io.File;
import java.io.PrintWriter;

import static com.pinnaclecom.pinnaclecom.getHtmlPage;

/**
 * Created by roman on 5/24/17.
 */
public class ResultParser implements Runnable {

    private MenuObject resObj;

    public ResultParser(MenuObject Obj) {
        this.resObj = Obj;
    }

    @Override
    public void run() {
        try {
            long startTime = System.currentTimeMillis();
            String resultStr = getHtmlPage(resObj.resultUrl);
            resultStr = resultStr.substring("angular.callbacks._0(".length() + 1, resultStr.length() - 2);
            String AddInfo = String
                    .format("{\"live\":\"%s\",\"country\":\"%s\",\"league\":\"%s\",\"Sport\":\"%s\",", resObj.key,
                            resObj.country, resObj.league, resObj.sport);
            resultStr = AddInfo + resultStr;

            long endTime = System.currentTimeMillis();
            System.out.println("Parsing JSONAPI URL = " + resObj.resultUrl +
                    "\nParsing time (NET+parsing) : " + (endTime - startTime) + " milliseconds\n");

            File logFile = new File("/tmp/pinnaclecom/" +
                    java.security.MessageDigest.getInstance("MD5").digest(resObj.resultUrl.getBytes()) + ".txt");
            new File("/tmp/pinnaclecom/").mkdir();
            logFile.createNewFile();
            PrintWriter writer = new PrintWriter(logFile, "UTF-8");
            writer.println(resultStr);
            writer.close();

        } catch (Exception $e) {
            System.out.println($e);
        }
    }
}

