package uk.ac.cam.cl.dtg.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionsUtil {
  public static List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException,
      URISyntaxException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    String path = packageName.replace('.', '/');
    Enumeration<URL> resources = classLoader.getResources(path);
    List<File> dirs = new ArrayList<>();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      URI uri = new URI(resource.toString());
      dirs.add(new File(uri.getPath()));
    }
    List<Class<?>> classes = new ArrayList<>();
    for (File directory : dirs) {
      classes.addAll(findClasses(directory, packageName));
    }

    return classes;
  }

  private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
    List<Class<?>> classes = new ArrayList<>();
    if (!directory.exists()) {
      return classes;
    }
    File[] files = directory.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        classes.addAll(findClasses(file, packageName + "." + file.getName()));
      } else if (file.getName().endsWith(".class")) {
        classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
      }
    }
    return classes;
  }

  @SuppressWarnings("unchecked")
  public static <T> Set<Class<? extends T>> getSubTypes(List<Class<?>> classes, Class<T> parentClass) {
    return classes.stream().filter(parentClass::isAssignableFrom).map(c -> (Class<T>) c).collect(Collectors.toSet());
  }
}
