package com.pinnaclecom;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;

import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class pinnaclecom {

    public static int maxResultParsers = 100;
    public static int maxPagesParsers = 100;
    public static long resultsRefreshTimeoutMills = 100;
    public static long catsRefreshTimeOutMills = 100;
    public static ConcurrentLinkedQueue pagesQueue = new ConcurrentLinkedQueue<MenuObject>();
    public static LinkedBlockingQueue resultsQueue = new LinkedBlockingQueue();
    public static Pattern ngControllerPattern = Pattern
                                .compile("<div\\sng-controller=\"GuestLinesController\"\\sng-init=\"[^\\(]*\\(([^\\)]*)");


    public static void main(String[] args)
    { 
        try {
            System.out.println(System.getProperty("user.dir"));
            System.out.println("Starting pinnacle.com parser");

            try {
                Properties settings = new Properties();
                settings.load(new FileInputStream("settings.ini"));
                maxResultParsers = Integer.parseInt(settings.getProperty("maxResultParsers").trim());
                maxPagesParsers = Integer.parseInt(settings.getProperty("maxPagesParsers").trim());
                resultsRefreshTimeoutMills = Integer.parseInt(settings.getProperty("resultsRefreshTimeoutMills").trim());
                catsRefreshTimeOutMills = Integer.parseInt(settings.getProperty("catsRefreshTimeOutMills").trim());
            }
            catch(Exception e){
                System.out.println("Configuration file settings.ini not found");
            }

            new Thread(new CatsParser()).start();
            ExecutorService PageParserExecutor = Executors.newFixedThreadPool(maxPagesParsers);
            ExecutorService ResultParserExecutor = Executors.newFixedThreadPool(maxResultParsers);

            while(true) {
                ConcurrentHashMap<String, ArrayList<MenuObject>> betMenu = (ConcurrentHashMap) pagesQueue.poll();
                if (betMenu != null) {
                    for (Map.Entry<String, ArrayList<MenuObject>> obj : betMenu.entrySet()) {
                        ArrayList obj4 = obj.getValue();

                        obj4.forEach(e -> {
                            MenuObject obj5 = (MenuObject) e;
                            Runnable worker = new PageParser(obj5);
                            PageParserExecutor.execute(worker);
                        });
                    }
                }

                Iterator<MenuObject> resultsIter = resultsQueue.iterator();
                while (resultsIter.hasNext()) {
                    MenuObject resObj = resultsIter.next();
                    if (resObj.worker == null || System.currentTimeMillis() - resObj.latResTime >= resultsRefreshTimeoutMills) {
                        resObj.worker = new ResultParser(resObj);
                        ResultParserExecutor.execute(resObj.worker);
                    }
                }

                Thread.sleep(100);
            }
            

        }catch (Exception e){
            System.out.println(e);
        }
    }

    public static void addHttpHeaders(HttpGet httpRequest)
    {
        httpRequest.addHeader("User-Agent","Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:53.0) Gecko/20100101 Firefox/53.0");
        httpRequest.addHeader("Accept","*/*");
        httpRequest.addHeader("Accept-Language","en-US,en;q=0.5");
        httpRequest.addHeader("Accept-Encoding","gzip, deflate, br");
    }

    public static String getHtmlPage(String targetUrl) throws Exception
    {
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD).build())
            .setRedirectStrategy(new LaxRedirectStrategy()).build();
        String responseBody;

        try {
            HttpGet httpRequest = new HttpGet(targetUrl);//
            pinnaclecom.addHttpHeaders(httpRequest);

            System.out.println("Executing request " + httpRequest.getRequestLine());

            responseBody = httpclient.execute(httpRequest, response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            });
        } finally {
            httpclient.close();
        }

        return responseBody;
    }

}

