package base.util;

import base.annotation.Entity;
import base.annotation.Key;
import base.cache.RunTimeCache;
import base.exception.CxyJPAException;
import config.DataSourceConstant;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

/**
 * @author Wonder Chen
 */
public class PackageScan {
    private static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] dirfiles = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".java"));
            }
        });
        for (File file : dirfiles) {
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive);
            } else {
                String fileName = file.getName().substring(0, file.getName().length() - 5);
                String className = packageName.substring(1) + "." + fileName;
                System.out.println(className + "is been scanning!");
                try {
                    Class clazz = Class.forName(className);
                    Entity entity = (Entity) clazz.getAnnotation(Entity.class);
                    if (entity != null && !entity.tableName().isEmpty()) {
                        boolean hasKey = Arrays.asList(clazz.getDeclaredFields()).stream().anyMatch(x -> x.getAnnotation(Key.class) == null );
                        if (hasKey) {
                            injectIfAnnotatedByEntity(fileName, className);
                        }else {
                            throw new CxyJPAException("Entity must have a key!");
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (CxyJPAException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void injectIfAnnotatedByEntity(String fileName, String path){
        Object backCall = RunTimeCache.ENTITY_CACHE.put(fileName, path);
        if (backCall != null){
            try {
                throw new CxyJPAException("Entities can not use same name!");
            } catch (CxyJPAException e) {
                e.printStackTrace();
            }
        }else {
            System.out.println(fileName + " " + path + " has injected!");
        }
    }

    public static void initialize() {
        findAndAddClassesInPackageByFile("", DataSourceConstant.SCAN_PATH,true);
    }
}
