package ru.ivan;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LukePodcastDownloader {

    public static void main(String[] args) throws IOException {
        Validate.isTrue(args.length == 1, "usage: supply url to fetch");
        String remoteURL = "http://teacherluke.co.uk/archive-of-episodes-1-149/";
        String localPath = args[0];

        Queue<String> podcasts = new ConcurrentLinkedQueue<>();
        Document doc = Jsoup.connect(remoteURL).get();
        Elements newsPages = doc.select("a[href]");

        print("Create mp3 links list %s...", remoteURL);
        newsPages.parallelStream().forEach(page -> {
            Document newsDoc;
            try {
                final String link = page.attr("abs:href");
                if (!link.contains("teacherluke")) {
                    return;
                }
//                //Если страница старая - не качаем
//                if (page.compareTo("http://teacherluke.co.uk/2017/03/09/")<0) {
//                    return;
//                }

                newsDoc = Jsoup.connect(link).get();
                newsDoc.select("a[href$=mp3]").stream()
                        .forEach(mp3Link -> {
                            String mp3URL = mp3Link.attr("abs:href");
                            podcasts.add(mp3URL);

                        });
            } catch (IOException e) {
                print("Incorrect url for fetch: %s ", page.attr("abs:href"));
            }
            return;
        });

        print("Downloads mp3 from list");
        podcasts.parallelStream().forEach(mp3Link -> {
            URL mp3URL;
            Path targetPath;
            try {
                mp3URL = new URL(mp3Link);
                String fileName = mp3Link.substring(mp3Link.lastIndexOf('/') + 1, mp3Link.length());
                targetPath = new File(localPath + File.separator + fileName).toPath();
                Files.copy(mp3URL.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                print("Save %s ", fileName);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        print("Download complete!");

    }

    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }
}
