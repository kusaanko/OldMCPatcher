package net.minecraft.client.OldMCPatcher;

import net.minecraft.Launcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main extends Frame {
    private static Launcher launcher;
    public static String assetsDir;

    public static void main(String[] args) {
        System.out.println("OldMCPatcher 1.1.0 https://github.com/kusaanko/OldMCPatcher/releases");
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--assetsDir")) {
                assetsDir = args[i + 1];
            }
        }
        try{
            final File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            if(jarFile.isFile()) {
                final JarFile jar = new JarFile(jarFile);
                final Enumeration<JarEntry> entries = jar.entries();
                while(entries.hasMoreElements()) {
                    final String name = entries.nextElement().getName();
                    if (name.startsWith("META-INF/")) {
                        JOptionPane.showMessageDialog(null, "Please delete META-INF!", "Minecraft", JOptionPane.ERROR_MESSAGE);
                        throw new IllegalStateException("Please delete META-INF!");
                    }
                }
                jar.close();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        try{
            for(Field f : Main.class.getClassLoader().loadClass("net.minecraft.client.Minecraft").getDeclaredFields()) {
                if (f.getType() == File.class&&Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    f.set(null, new File("./"));
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        new Main(args);
    }

    public Main(String[] args) {
        launcher = new Launcher();
        launcher.setPreferredSize(new Dimension(854, 480));
        launcher.init(args);
        this.add(launcher);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.setTitle("Minecraft");
        try {
            File icon = new File(assetsDir, "icons/icon_32x32.png");
            if(!icon.exists()) {
                icon = new File(assetsDir.replace("pre-1.6", "legacy"), "icons/icon_32x32.png");
            }
            this.setIconImage(ImageIO.read(icon));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent var1) {
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            Thread.sleep(30000L);
                        } catch (InterruptedException var2) {
                            var2.printStackTrace();
                        }

                        System.out.println("FORCING EXIT!");
                        System.exit(0);
                    }
                }.start();
                launcher.stop();
                launcher.destroy();

                System.exit(0);
            }
        });
    }
}
