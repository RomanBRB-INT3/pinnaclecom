package com.pinnaclecom;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by roman on 5/24/17.
 */
public class CatsParser implements Runnable {

    private static String pinnacleUrl = "https://www.pinnacle.com";
    private Document htmlDom;
    private ConcurrentHashMap<String,ArrayList<MenuObject>> sportsMenu = new ConcurrentHashMap();

    public void run()
    {
        while(true) {
            try {
                while(true){
                    String responseBody = pinnaclecom.getHtmlPage(pinnacleUrl+"/en");
                    htmlDom = Jsoup.parse(responseBody);

                    sportsMenu = new ConcurrentHashMap();
                    createMenuObj("li.level-1.live","live");
                    createMenuObj("li.level-1.no-live","no-live");
                    createMenuObj("li.level-1.future","future");

                    if(!sportsMenu.isEmpty()) {
                        pinnaclecom.pagesQueue.add(sportsMenu);
                    }

                    Thread.sleep(pinnaclecom.catsRefreshTimeOutMills);
                }
            } catch (Exception $e) {
                System.out.println($e);
            }
        }
    }



    private void createMenuObj(String rootElement,String hashKey)
    {
        Elements elems = htmlDom.select(rootElement).select("li.level-2");
        for (Element elem:elems) {
            String sportName =  elem.select("span.sport-icon").attr("title");

            ArrayList newArr = sportsMenu.get(hashKey);
            if(newArr == null){
                newArr = new ArrayList<>();
                sportsMenu.put(hashKey,newArr);
            }

            //level-2
            Elements lielems = elem.select("ul").first().children();
            for(Element li:lielems){
                MenuObject menuObj = new MenuObject();
                menuObj.sport = sportName;
                menuObj.key = hashKey;
                newArr.add(menuObj);
                String todayUrl = li.child(0).attr("href");
                if(!todayUrl.isEmpty()){
                    menuObj.league = li.select("a.text").text();
                    //System.out.println(menuObj.league);
                    menuObj.pageUrl = todayUrl;
                    if(!todayUrl.startsWith("http")) {
                        menuObj.pageUrl = CatsParser.pinnacleUrl + todayUrl;
                    }
                }
                else {
                    menuObj.country = li.select("span.trigger").attr("title");
                    menuObj.pageUrl = li.select("ul > li > a").attr("href");
                    if(!menuObj.pageUrl.startsWith("http")){
                        menuObj.pageUrl = CatsParser.pinnacleUrl+menuObj.pageUrl;
                    }
                    menuObj.league = li.select("ul > li > a").text();
                }
            }

        }
    }



}

class MenuObject
{
    public String country;
    public String pageUrl;
    public String league;
    public String resultUrl;
    public String sport;
    public String key;
    public Runnable worker;
    public long latResTime;
}
