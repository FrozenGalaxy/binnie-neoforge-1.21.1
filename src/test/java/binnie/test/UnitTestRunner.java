package binnie.test;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Minimal deterministic Jupiter-annotation runner used because the sealed offline cache lacks platform-launcher. */
public final class UnitTestRunner {
    private UnitTestRunner() {
    }

    public static void main(String[] args) throws Exception {
        if (net.neoforged.fml.loading.LoadingModList.get() == null) {
            net.neoforged.fml.loading.LoadingModList.of(
                    java.util.List.of(), java.util.List.of(), java.util.List.of(),
                    java.util.List.of(), java.util.Map.of());
        }
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path classes = locateTestClasses();
        List<String> names = new ArrayList<>();
        try (var paths = Files.walk(classes)) {
            paths.filter(path -> path.toString().endsWith(".class"))
                    .filter(path -> !path.getFileName().toString().contains("$"))
                    .map(classes::relativize)
                    .map(Path::toString)
                    .map(name -> name.substring(0, name.length() - 6).replace(File.separatorChar, '.'))
                    .filter(name -> !name.equals(UnitTestRunner.class.getName()))
                    .sorted()
                    .forEach(names::add);
        }

        int passed = 0;
        List<String> failures = new ArrayList<>();
        for (String name : names) {
            Class<?> type = Class.forName(name);
            List<Method> tests = List.of(type.getDeclaredMethods()).stream()
                    .filter(method -> method.isAnnotationPresent(Test.class))
                    .sorted(Comparator.comparing(Method::getName))
                    .toList();
            if (tests.isEmpty()) continue;
            Object instance = null;
            for (Method method : tests) {
                try {
                    if (!Modifier.isStatic(method.getModifiers())) {
                        if (instance == null) instance = construct(type);
                    }
                    method.setAccessible(true);
                    method.invoke(Modifier.isStatic(method.getModifiers()) ? null : instance);
                    passed++;
                    System.out.println("[PASS] " + name + '#' + method.getName());
                } catch (InvocationTargetException exception) {
                    Throwable cause = exception.getCause();
                    failures.add(name + '#' + method.getName() + ": " + cause);
                    cause.printStackTrace(System.err);
                } catch (Throwable throwable) {
                    failures.add(name + '#' + method.getName() + ": " + throwable);
                    throwable.printStackTrace(System.err);
                }
            }
        }
        System.out.println("[UNIT TEST SUMMARY] passed=" + passed + " failed=" + failures.size());
        if (passed == 0) throw new AssertionError("No @Test methods were discovered under " + classes);
        if (!failures.isEmpty()) throw new AssertionError("Unit test failures:\n" + String.join("\n", failures));
    }

    private static Object construct(Class<?> type) throws ReflectiveOperationException {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static Path locateTestClasses() throws Exception {
        URI uri = UnitTestRunner.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        Path path = Path.of(uri);
        if (!Files.isDirectory(path)) throw new IllegalStateException("Test classes location is not a directory: " + path);
        return path;
    }
}
