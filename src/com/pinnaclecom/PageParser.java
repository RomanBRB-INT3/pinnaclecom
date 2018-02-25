package com.pinnaclecom;

import java.util.regex.Matcher;


import static com.pinnaclecom.pinnaclecom.ngControllerPattern;
import static com.pinnaclecom.pinnaclecom.resultsQueue;

/**
 * Created by roman on 5/24/17.
 */
public class PageParser implements Runnable
{
    private MenuObject parseObj;


    public PageParser(MenuObject obj)
    {
        parseObj = obj;
    }

    @Override
    public void run() {
        try {
            System.out.println("PageParser started, thread name = " + Thread.currentThread().getName());
            String responseBody = pinnaclecom.getHtmlPage(parseObj.pageUrl);

            Matcher matcher = ngControllerPattern.matcher(responseBody);
            String paramsUrl = "";
            if (!matcher.find()) {
                return;
            }
            System.out.println("group 1: " + matcher.group(1));
            String[] callParams = matcher.group(1).split(",");
            int paramsCount = 0;
            for (String param : callParams) {
                param = param.trim();
                if (paramsCount >= 2 && !isInteger(param)) {
                    break;
                }
                paramsCount++;
                paramsUrl += param + "/";
            }

            if (paramsUrl.isEmpty()) {
                return;
            }
            paramsUrl = paramsUrl.substring(0, paramsUrl.length() - 1);

            String whenUrl = "";
            switch (parseObj.key) {
                case "no-live": {
                    whenUrl = "NonLive";
                    break;
                }
                case "Today": {
                    whenUrl = "Today";
                    break;
                }
                case "live": {
                    whenUrl = "Live";
                    break;
                }
            }
            if (parseObj.league.equals("Today")) {
                whenUrl = "Today";
            }

            String resultUrl = "https://www.pinnacle.com/webapi/1.15/api/v1/GuestLines/" + whenUrl + "/" + paramsUrl + "?callback=angular.callbacks._0";
            parseObj.resultUrl = resultUrl;
            System.out.println("JSONAPI_Url=" + resultUrl);
            resultsQueue.add(parseObj);

        } catch (Exception $e) {
            System.out.println($e);
        }
    }


    public boolean isInteger( String input )
    {
        try
        {
            Integer.parseInt( input );
            return true;
        }
        catch( Exception e)
        {
            return false;
        }
    }
}
