package ru.ivan;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LukePodcastDownloader {
    private static final String lastDownloadedLink=
            "";
//            "http://teacherluke.co.uk/2017/03/09/431-restaurants-hotels-really-strange-tripadvisor-reviews-with-amber/";

    public static void main(String[] args) throws IOException {

        Validate.isTrue(args.length == 1, "Usage: supply local path to save");
        String remoteURL = "http://teacherluke.co.uk/archive-of-episodes-1-149/";
        String localPath = args[0];

        Map<String,String> podcasts = new ConcurrentHashMap<>();
        Document doc = Jsoup.connect(remoteURL).get();
        Elements newsPages = doc.select("a[href]");

        LocalDate lastDownloadedDay= extractDateFromURL(lastDownloadedLink);

        print("Create mp3 links list %s...", remoteURL);
        newsPages.parallelStream().forEach(page -> {
            Document newsDoc;
            try {
                final String link = page.attr("abs:href");
                if (!link.contains("teacherluke")) {
                    return;
                }
                //Если страница старая - не качаем
                if (lastDownloadedDay.compareTo(extractDateFromURL(link))>0) {
                    return;
                }

                newsDoc = Jsoup.connect(link).get();
                newsDoc.select("a[href$=mp3]").stream()
                        .forEach(mp3Link -> {
                            String mp3URL = mp3Link.attr("abs:href");
                            podcasts.put(mp3URL,"");

                        });
            } catch (IOException e) {
                print("Incorrect url for fetch: %s ", page.attr("abs:href"));
            }
            return;
        });
        print("Download mp3 begin: %s", Calendar.getInstance().getTime());

        podcasts.keySet().parallelStream().forEach(mp3Link -> {
            try {
                URL mp3URL = new URL(mp3Link);
                try (InputStream inputStream = mp3URL.openStream()) {
                    String fileName = mp3Link.substring(mp3Link.lastIndexOf('/') + 1, mp3Link.length());
                    Path targetPath = new File(localPath + File.separator + fileName).toPath();
                    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    print("%s complete", fileName);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

        });
        print("Download complete: %s", Calendar.getInstance().getTime());

    }

    private static LocalDate extractDateFromURL(String link) {

        LocalDate linkDate=LocalDate.MIN;
        String[] linkArray = link.split("/");
        try {
            int year =Integer.parseInt(linkArray[3]);
            int month =Integer.parseInt(linkArray[4]);
            int day =Integer.parseInt(linkArray[5]);
             linkDate=LocalDate.of(year,month,day);

        } catch (Exception e) {
        }
        return linkDate;
    }

    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }
}
