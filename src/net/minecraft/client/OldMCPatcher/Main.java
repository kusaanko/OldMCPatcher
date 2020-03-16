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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static net.minecraft.client.OldMCPatcher.ReflectionHelper.*;

public class Main extends Frame {
    private static Launcher launcher;
    public static String assetsDir;
    public static boolean registerTexture = false;
    public static ArrayList<String> loadTexture = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("OldMCPatcher 1.1.1 https://github.com/kusaanko/OldMCPatcher/releases");
        try {
            Main.class.getClassLoader().loadClass("cpw.mods.fml.common.ITickHandler");
            registerTexture = true;
            new Thread(() -> {
                AtomicBoolean run = new AtomicBoolean(true);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    run.set(false);
                }));
                while (run.get()) {
                    try {
                        Class fmlRelauncher = Main.class.getClassLoader().loadClass("cpw.mods.fml.relauncher.FMLRelauncher");
                        Method instance = getDeclaredMethod(fmlRelauncher, "instance");
                        Field loaderField = getDeclaredField(fmlRelauncher, "classLoader");
                        Object fmlRelauncherInstance = waitAndInvoke(instance, null);
                        URLClassLoader loader = (URLClassLoader) waitAndGet(loaderField, fmlRelauncherInstance);
                        loader.loadClass("com.google.common.collect.Queues");
                        try {
                            Class sideClass = loader.loadClass("cpw.mods.fml.common.Side");
                            Method registerTickHandler = loader.loadClass("cpw.mods.fml.common.registry.TickRegistry").getMethod("registerTickHandler", loader.loadClass("cpw.mods.fml.common.ITickHandler"), sideClass);
                            Enum sideClient = null;
                            for(Object enu : sideClass.getEnumConstants()) {
                                if(enu.toString().equals("CLIENT")) sideClient = (Enum) enu;
                            }
                            defineClass(loader, "net.minecraft.client.OldMCPatcher.OldMCPatcherTickEvent", "/net/minecraft/client/OldMCPatcher/OldMCPatcherTickEvent.class");
                            if(sideClient!=null) {
                                registerTickHandler.invoke(null,
                                        loader.loadClass(OldMCPatcherTickEvent.class.getName()).newInstance(),
                                        sideClient);
                            }
                        }catch (ClassNotFoundException ignore){}
                        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException | InstantiationException e) {
                            e.printStackTrace();
                        }
                        break;
                    } catch (ClassNotFoundException ignore){}
                    catch(NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (ClassNotFoundException ignore) {
            try {
                defineClass(Main.class.getClassLoader(), "cpw.mods.fml.common.ITickHandler", "net/minecraft/client/OldMCPatcher/ITickHandler.class");
            } catch (NoSuchMethodException | IOException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
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
                    if (name.startsWith("META-INF/")&&(name.endsWith(".DF")&&name.endsWith(".SF")&&name.endsWith(".MF")&&name.endsWith(".DSA"))) {
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
