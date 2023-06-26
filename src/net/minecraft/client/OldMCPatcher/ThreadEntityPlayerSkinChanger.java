package net.minecraft.client.OldMCPatcher;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import static net.minecraft.client.OldMCPatcher.ReflectionHelper.*;

public class ThreadEntityPlayerSkinChanger extends Thread{
    public Object minecraft;
    private boolean run;
    private Field fieldInMc;
    private final ArrayList<String> loaded;
    private final HashMap<String, String> uuidMap;

    public ThreadEntityPlayerSkinChanger(Object mc) {
        this.run = true;
        this.minecraft = mc;
        this.loaded = new ArrayList<>();
        this.uuidMap = new HashMap<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                for (File f : Objects.requireNonNull(new File("resources/skin/").listFiles())) {
                    try {
                        Files.delete(f.toPath());
                    }catch (Exception ignore) {}
                }
            }catch (Exception ignore) {}
        }));
    }

    @Override
    public void run() {
        try {
            String updateString = null;
            {
                System.out.println("[OldMCPatcher] Checking updates...");
                String raw = get("https://raw.githubusercontent.com/kusaanko/OldMCPatcher/master/sum.json");
                if (raw != null) {
                    Matcher matcher = Pattern.compile("\"([^\"]*)\"").matcher(raw);
                    if (matcher.find()) {
                        String version = matcher.group(1);
                        if (!version.equals(Main.version)) {
                            updateString = "[OldMCPatcher] Update " + version + " available. Update from MCAddToJar or https://github.com/kusaanko/OldMCPatcher/releases";
                            System.out.println(updateString);
                        }
                    }
                } else {
                    System.out.println("[OldMCPatcher] Can't get updates info");
                }
            }
            ArrayList<Field> mcFields = getAllDeclaredFields(this.minecraft.getClass());
            URLClassLoader loader = (URLClassLoader) getClass().getClassLoader();
            Method addURL = getDeclaredMethod(URLClassLoader.class, "addURL", URL.class);
            while (run) {
                //playersList
                for (Field fi : mcFields) {
                    try {
                        fi.setAccessible(true);
                        Object objInMc;
                        if (fieldInMc == null) objInMc = fi.get(this.minecraft);
                        else objInMc = fieldInMc.get(this.minecraft);
                        if (Main.registerTexture == null) {
                            if (objInMc != null && !objInMc.getClass().getName().contains("."))
                                for (Method method : objInMc.getClass().getMethods()) {
                                    if (method.getReturnType() == int.class && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == String.class) {
                                        Main.registerTexture = method;
                                        Main.renderEngine = objInMc;
                                    }
                                }
                        }
                        if (objInMc != null) for (Field fie : getAllDeclaredFields(fi.getType())) {
                            fie.setAccessible(true);
                            if (fie.getType() == List.class) {
                                try {
                                    Object playersList = fie.get(objInMc);
                                    if (playersList == null) continue;
                                    List<Object> playerList = new ArrayList<>((List<?>) playersList);
                                    //skinUrl
                                    try {
                                        for (Object player : playerList) {
                                            if (player != null) {
                                                try {
                                                    Class<?> superclass = player.getClass();
                                                    while((superclass = superclass.getSuperclass()) != null) {
                                                        ArrayList<Field> fields = getAllDeclaredFields(superclass);
                                                        boolean isPlayer = false;
                                                        Field skinField = null;
                                                        for (Field fiel : fields) {
                                                            if (fiel.getType() == String.class) {
                                                                fiel.setAccessible(true);
                                                                String url = (String) fiel.get(player);
                                                                if (url != null && url.equals("/mob/char.png")) {
                                                                    skinField = fiel;
                                                                    isPlayer = true;
                                                                    if (fieldInMc == null) {
                                                                        fieldInMc = fi;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (isPlayer && updateString != null) {
                                                            for (Method method : superclass.getDeclaredMethods()) {
                                                                if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == String.class) {
                                                                    method.setAccessible(true);
                                                                    method.invoke(player, updateString);
                                                                }
                                                            }
                                                        }
                                                        if (isPlayer) for (Field fiel : fields) {
                                                            String userName = null;
                                                            if (fiel.getType() == String.class) {
                                                                fiel.setAccessible(true);
                                                                String data = (String) fiel.get(player);
                                                                if (data != null && !data.equals("/mob/char.png") && !data.equals("humanoid")
                                                                        && !data.startsWith("http://skins.minecraft.net/Minecraft")) {
                                                                    userName = data;
                                                                }
                                                                if (data != null && data.startsWith("http://skins.minecraft.net/Minecraft")) {
                                                                    fiel.set(player, "");
                                                                }
                                                                if (data != null && data.startsWith("http://s3.amazonaws.com/Minecraft")) {
                                                                    fiel.set(player, "");
                                                                }
                                                            }
                                                            if (userName != null) {
                                                                String uuid;
                                                                if ((uuid = uuidMap.get(userName)) == null) {
                                                                    uuid = get("https://api.mojang.com/users/profiles/minecraft/" + userName);
                                                                    if(uuid != null) {
                                                                        Matcher matcher = Pattern.compile("\"id\"[^:]*:[^\"]*\"([^\"]*)").matcher(uuid);
                                                                        if (matcher.find()) {
                                                                            uuid = matcher.group(1);
                                                                        }
                                                                        uuidMap.put(userName, uuid);
                                                                    }else {
                                                                        break;
                                                                    }
                                                                }
                                                                if (!this.loaded.contains(uuid)) {
                                                                    System.out.println("[OldMCPatcher] Downloading skin from https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
                                                                    String profile = get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
                                                                    if (profile != null) {
                                                                        Matcher matcher = Pattern.compile("\"value\"[^:]*:[^\"]*\"([^\"]*)").matcher(profile);
                                                                        if (matcher.find()) {
                                                                            String textureData = new String(Base64.getDecoder().decode(matcher.group(1)));
                                                                            matcher = Pattern.compile("\"url\"[^:]*:[^\"]*\"([^\"]*)").matcher(textureData);
                                                                            if (matcher.find()) {
                                                                                String textureUrl = matcher.group(1);
                                                                                File out = new File("resources/skin/" + uuid + ".zip");
                                                                                if (out.getParentFile().mkdirs()) {
                                                                                }
                                                                                compress("mob/" + uuid + ".png", getByte(textureUrl), out);
                                                                            }
                                                                        }
                                                                    }
                                                                    if (new File("resources/skin/" + uuid + ".zip").exists()) {
                                                                        addURL.invoke(loader, new File("resources/skin/" + uuid + ".zip").toURI().toURL());
                                                                        if (Main.needRegisterTexture) {
                                                                            addURL.invoke(Main.registerTexture.getDeclaringClass().getClassLoader(), new File("resources/skin/" + uuid + ".zip").toURI().toURL());
                                                                            Main.loadTexture = "/mob/" + uuid + ".png";
                                                                        }
                                                                        this.loaded.add(uuid);
                                                                        while (Main.loadTexture != null) {
                                                                            try {
                                                                                Thread.sleep(10);
                                                                            } catch (Exception ignore) {
                                                                            }
                                                                        }
                                                                    }else {
                                                                        System.out.println("[OldMCPatcher] Failed to download skin file of " + uuid);
                                                                        break;
                                                                    }
                                                                }
                                                                System.out.println("[OldMCPatcher] Setting skin of " + userName + "(" + uuid + ")");
                                                                skinField.set(player, "/mob/" + uuid + ".png");
                                                            }
                                                        }
                                                    }
                                                } catch (NullPointerException ignore) {}
                                            }
                                        }
                                    } catch (ConcurrentModificationException ignore) {}
                                } catch (IllegalArgumentException ignore) {}
                            }
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void Stop() {
        this.run = false;
    }

    private String get(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpsURLConnection http = (HttpsURLConnection)url.openConnection();
        http.setRequestMethod("GET");
        http.connect();

        if(http.getResponseCode()==200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String xml = "", line;
            while ((line = reader.readLine()) != null)
                xml += line;
            reader.close();
            return xml;
        }
        return null;
    }

    private byte[] getByte(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod("GET");
        http.connect();

        InputStream inputStream = http.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream w = new DataOutputStream(baos);

        int len;
        byte[] bytes = new byte[4096];
        while((len = inputStream.read(bytes, 0, bytes.length)) > 0) {
            w.write(bytes, 0, len);
        }
        return baos.toByteArray();
    }

    private static void compress(String name, byte[] input, File output) throws IOException {
        ZipOutputStream append = new ZipOutputStream(new FileOutputStream(output));
        try {
            append.putNextEntry(new ZipEntry(name));
            append.write(input, 0, input.length);
            append.closeEntry();
        } catch (ZipException ignore) {
        }

        append.close();
    }
}
