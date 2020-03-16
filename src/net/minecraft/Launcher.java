package net.minecraft;

import net.minecraft.client.OldMCPatcher.ReflectionHelper;
import net.minecraft.client.OldMCPatcher.ThreadEntityPlayerSkinChanger;

import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class Launcher extends Applet implements AppletStub {
    private HashMap<String, String> parameter = new HashMap<>();
    private Applet applet;
    private boolean active;
    private ThreadEntityPlayerSkinChanger threadEntityPlayerSkinChanger;
    private Field mcf;

    public void init(String[] args) {
        parameter.put("username", args[0]);
        parameter.put("sessionid", args[1]);
        parameter.put("stand-alone", "true");
        Thread thread1 = new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    applet = createApplet();
                    add(applet);
                    applet.setStub(Launcher.this);
                    applet.setSize(getWidth(), getHeight());
                    setLayout(new BorderLayout());
                    add(applet, "Center");
                    applet.init();
                    for(Field f : applet.getClass().getDeclaredFields()) {
                        if(f.getType().getName().equals("net.minecraft.client.Minecraft")) {
                            mcf = f;
                        }
                    }
                    if(mcf!=null) {
                        mcf.setAccessible(true);
                        new Thread(() -> {
                            super.run();
                            try {
                                Object mc = ReflectionHelper.waitAndGet(mcf, applet);

                                threadEntityPlayerSkinChanger = new ThreadEntityPlayerSkinChanger(mc);
                                threadEntityPlayerSkinChanger.start();
                                Runtime.getRuntime().addShutdownHook(new Thread(() -> threadEntityPlayerSkinChanger.Stop()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                    active = true;
                    applet.start();
                    Launcher.this.validate();
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                    e.printStackTrace();
                }
            }
        };
        thread1.start();
        this.setBackground(Color.BLACK);
        this.setVisible(true);
    }

    public Applet createApplet() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (Applet) getClass().getClassLoader().loadClass("net.minecraft.client.MinecraftApplet").newInstance();
    }

    @Override
    public void appletResize(int width, int height) {

    }

    @Override
    public String getParameter(String name) {
        String result = this.parameter.get(name);
        if(result!=null) {
            return result;
        }
        try {
            return super.getParameter(name);
        } catch (Exception e) {
            this.parameter.put(name, null);
            return null;
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void stop() {
        if (this.applet != null) {
            this.active = false;
            this.applet.stop();
        }

    }

    public void destroy() {
        if (this.applet != null) {
            this.applet.destroy();
        }

    }

    @Override
    public URL getDocumentBase() {
        try {
            return new URL("http://www.minecraft.net/game/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void replace(Applet applet) {
        this.applet = applet;
        applet.setStub(this);
        applet.setSize(this.getWidth(), this.getHeight());
        this.setLayout(new BorderLayout());
        this.add(applet, "Center");
        applet.init();
        this.active = true;
        applet.start();
        this.validate();
    }
}
