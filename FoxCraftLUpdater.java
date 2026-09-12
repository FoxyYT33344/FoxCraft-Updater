import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * FoxCraftLUpdater
 *
 * Der "L" steht fuer Launcher.
 * Dieser Prozess wird vom FoxCraft Launcher gestartet, wenn eine
 * neue Launcher-Version gefunden wurde. Der Launcher beendet sich,
 * danach ersetzt dieser Prozess die Launcher-Datei, kompiliert sie
 * und startet den aktualisierten Launcher.
 */
public class FoxCraftLUpdater {

    private static final String GITHUB_LAUNCHER_URL =
            "https://raw.githubusercontent.com/FoxyYT33344/FoxCraft-Updater/main/FoxCraftLauncher.java";

    private static final String LAUNCHER_CLASS = "FoxCraftLauncher";
    private static final String RESTART_FLAG = "--skip-startup-update";

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                throw new IllegalArgumentException(
                        "Verwendung: FoxCraftLUpdater <Launcher-PID> <src-Ordner> [update-id] [ziel-version]");
            }

            long parentPid = Long.parseLong(args[0]);
            Path installDir = Paths.get(args[1]).toAbsolutePath().normalize();
            Files.createDirectories(installDir);

            waitForLauncherToExit(parentPid);

            String newLauncherSource = downloadText(GITHUB_LAUNCHER_URL);
            String remoteVersion = extractVersion(newLauncherSource, "LAUNCHER_VERSION");

            Path tempSource = installDir.resolve("FoxCraftLauncher.java.update");
            Path launcherSource = installDir.resolve("FoxCraftLauncher.java");
            Path launcherClass = installDir.resolve("FoxCraftLauncher.class");

            Files.writeString(
                    tempSource,
                    newLauncherSource,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            replaceFileWithRetry(tempSource, launcherSource, 20, 250);
            compileLauncher(launcherSource, installDir, launcherClass);

            Files.deleteIfExists(installDir.resolve("FoxCraftLauncher.java.update"));

            ProcessBuilder restart = new ProcessBuilder(
                    getJavaExecutable(),
                    "-cp",
                    installDir.toString(),
                    LAUNCHER_CLASS,
                    RESTART_FLAG
            );
            restart.directory(installDir.toFile());
            restart.inheritIO();
            restart.start();

            System.out.println("FoxCraft Launcher wurde auf Version " + remoteVersion + " aktualisiert.");
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void waitForLauncherToExit(long pid) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (!ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
                return;
            }
            Thread.sleep(100);
        }
        // Falls der Prozess nach rund 10 Sekunden noch gesehen wird, noch etwas warten.
        Thread.sleep(1000);
    }

    private static String downloadText(String urlString) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) URI.create(urlString).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "FoxCraftLUpdater");

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("GitHub antwortete mit HTTP " + status + ".");
        }

        try (var input = connection.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    private static String extractVersion(String source, String constantName) throws IOException {
        String marker = constantName + " = \"";
        int index = source.indexOf(marker);
        if (index < 0) {
            throw new IOException("Die Version " + constantName + " wurde nicht gefunden.");
        }
        int start = index + marker.length();
        int end = source.indexOf('"', start);
        if (end < 0) {
            throw new IOException("Ungültige Version.");
        }
        return source.substring(start, end).trim();
    }

    private static void replaceFileWithRetry(
            Path temporary,
            Path destination,
            int attempts,
            long delayMillis
    ) throws Exception {
        Exception last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
                return;
            } catch (Exception ex) {
                last = ex;
                Thread.sleep(delayMillis);
            }
        }
        throw last == null
                ? new IOException("Launcher-Datei konnte nicht ersetzt werden.")
                : last;
    }

    private static void compileLauncher(
            Path source,
            Path outputDir,
            Path expectedClass
    ) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Kein Java-Compiler gefunden. Bitte ein JDK verwenden.");
        }

        int result = compiler.run(
                null,
                null,
                null,
                "-encoding", "UTF-8",
                "-d", outputDir.toString(),
                source.toString()
        );

        if (result != 0 || !Files.exists(expectedClass)) {
            throw new IllegalStateException("Der aktualisierte FoxCraftLauncher konnte nicht kompiliert werden.");
        }
    }

    private static String getJavaExecutable() {
        String executable =
                System.getProperty("os.name", "")
                        .toLowerCase()
                        .contains("win")
                        ? "java.exe"
                        : "java";
        return Paths.get(
                System.getProperty("java.home"),
                "bin",
                executable
        ).toString();
    }
}
