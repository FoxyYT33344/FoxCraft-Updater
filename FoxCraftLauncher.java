import javax.swing.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.TimerTask;

/**
 * FoxCraft Launcher
 *
 * Der Launcher prüft beim Start eine GitHub-version.txt. Wenn die
 * GitHub-Version höher ist, lädt er FoxCraft.java und FoxCraftLauncher.java
 * herunter, kompiliert die Dateien und startet die aktualisierte Version.
 *
 * GitHub vorbereiten:
 *   1. version.txt ins Repository legen, z.B. 1.1.0
 *   2. FoxCraft.java hochladen
 *   3. FoxCraftLauncher.java hochladen
 *   4. GITHUB_OWNER / GITHUB_REPOSITORY / GITHUB_BRANCH anpassen
 */
public class FoxCraftLauncher extends JFrame {

    public static final String LAUNCHER_VERSION = "1.0.0";

    // ============================================================
    // GITHUB UPDATE KONFIGURATION
    // ============================================================

    public static final String GITHUB_OWNER = "FoxyYT33344";
    public static final String GITHUB_REPOSITORY = "FoxCraft";
    public static final String GITHUB_BRANCH = "main";

    private static final String RAW_BASE =
            "https://raw.githubusercontent.com/"
                    + GITHUB_OWNER + "/"
                    + GITHUB_REPOSITORY + "/"
                    + GITHUB_BRANCH + "/";

    private static final String VERSION_URL = RAW_BASE + "version.txt";
    private static final String GAME_SOURCE_URL = RAW_BASE + "FoxCraft.java";
    private static final String LAUNCHER_SOURCE_URL = RAW_BASE + "FoxCraftLauncher.java";

    private static final String UPDATED_LAUNCHER_FLAG = "--foxcraft-updated-launcher";
    private static final String GAME_LAUNCHER_TOKEN = "FOXCRAFT_LAUNCHER_START_2026";

    private int seconds = 3;
    private final JLabel statusLabel = new JLabel("FoxCraft wird geprüft ...");
    private final JLabel versionLabel = new JLabel("Version " + LAUNCHER_VERSION);
    private Timer countdownTimer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FoxCraftLauncher launcher = new FoxCraftLauncher();
            launcher.setVisible(true);
            launcher.checkForUpdatesThenStart(args);
        });
    }

    public FoxCraftLauncher() {
        setTitle("FoxCraft Launcher");
        setSize(700, 470);
        setMinimumSize(new Dimension(620, 380));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(20, 20));
        root.setBorder(BorderFactory.createEmptyBorder(35, 35, 25, 35));
        root.setBackground(new Color(18, 18, 22));

        JLabel title = new JLabel("FOXCRAFT");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 54));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitle = new JLabel("Launcher");
        subtitle.setForeground(new Color(170, 170, 170));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 20));
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setOpaque(false);
        top.add(title, BorderLayout.CENTER);
        top.add(subtitle, BorderLayout.SOUTH);

        JLabel fox = new JLabel("🦊");
        fox.setHorizontalAlignment(SwingConstants.CENTER);
        fox.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 70));

        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 22));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        versionLabel.setForeground(new Color(145, 145, 145));
        versionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.setOpaque(false);
        bottom.add(statusLabel, BorderLayout.CENTER);
        bottom.add(versionLabel, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(fox, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
    }

    /** Prüft GitHub und startet danach das Spiel. */
    public void checkForUpdatesThenStart(String[] args) {
        boolean alreadyUpdated = false;
        for (String arg : args) {
            if (UPDATED_LAUNCHER_FLAG.equals(arg)) {
                alreadyUpdated = true;
                break;
            }
        }

        if (alreadyUpdated) {
            startCountdown();
            return;
        }

        statusLabel.setText("Suche nach FoxCraft-Updates ...");

        Thread t = new Thread(() -> {
            try {
                String remoteVersion = downloadText(VERSION_URL).trim();
                if (remoteVersion.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Keine Versionsdatei gefunden.");
                        startCountdown();
                    });
                    return;
                }

                int cmp = compareVersions(remoteVersion, LAUNCHER_VERSION);
                if (cmp > 0) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText(
                            "Update gefunden: " + LAUNCHER_VERSION + " → " + remoteVersion));
                    performUpdateAndRestart(remoteVersion);
                } else {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("FoxCraft ist aktuell.");
                        startCountdown();
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Update-Prüfung nicht möglich – FoxCraft startet trotzdem.");
                    startCountdown();
                });
            }
        }, "FoxCraft-Update-Check");
        t.setDaemon(true);
        t.start();
    }

    /** Lädt die beiden Java-Dateien, kompiliert sie und startet den neuen Launcher. */
    private void performUpdateAndRestart(String remoteVersion) {
        Thread updater = new Thread(() -> {
            try {
                Path appDir = new File(System.getProperty("user.dir")).toPath();
                Path updateDir = appDir.resolve("FoxCraftUpdate");
                Path updateClasses = updateDir.resolve("classes");
                Files.createDirectories(updateClasses);

                Path launcherSource = updateDir.resolve("FoxCraftLauncher.java");
                Path gameSource = updateDir.resolve("FoxCraft.java");
                Path remoteVersionFile = updateDir.resolve("version.txt");

                downloadToFile(LAUNCHER_SOURCE_URL, launcherSource);
                downloadToFile(GAME_SOURCE_URL, gameSource);
                Files.writeString(remoteVersionFile, remoteVersion, StandardCharsets.UTF_8);

                SwingUtilities.invokeLater(() -> statusLabel.setText("Update wird kompiliert ..."));

                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                if (compiler == null) {
                    throw new IllegalStateException(
                            "Kein Java-Compiler gefunden. Bitte eine JDK-Version verwenden.");
                }

                int result = compiler.run(
                        null,
                        null,
                        null,
                        "-encoding", "UTF-8",
                        "-d", updateClasses.toString(),
                        launcherSource.toString(),
                        gameSource.toString()
                );

                if (result != 0) {
                    throw new IllegalStateException("Das Update konnte nicht kompiliert werden.");
                }

                // Die Versionsinformation lokal speichern. Die aktualisierten
                // Quellen bleiben zusätzlich im FoxCraftUpdate-Ordner erhalten.
                Files.copy(
                        remoteVersionFile,
                        appDir.resolve("version.txt"),
                        StandardCopyOption.REPLACE_EXISTING
                );

                SwingUtilities.invokeLater(() -> statusLabel.setText("Update fertig – Launcher wird neu gestartet ..."));

                String javaExecutable = getJavaExecutable();
                ProcessBuilder builder = new ProcessBuilder(
                        javaExecutable,
                        "-cp",
                        updateClasses.toString(),
                        "FoxCraftLauncher",
                        UPDATED_LAUNCHER_FLAG
                );
                builder.directory(appDir.toFile());
                builder.inheritIO();
                builder.start();

                Thread.sleep(250);
                System.exit(0);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Update fehlgeschlagen.");
                    JOptionPane.showMessageDialog(
                            this,
                            "Das FoxCraft-Update konnte nicht installiert werden.\n\n"
                                    + ex.getMessage(),
                            "FoxCraft Launcher",
                            JOptionPane.ERROR_MESSAGE
                    );
                    startCountdown();
                });
            }
        }, "FoxCraft-Updater");
        updater.setDaemon(true);
        updater.start();
    }

    private void startCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        seconds = 3;
        statusLabel.setText("FoxCraft startet in 3 Sekunden ...");

        countdownTimer = new Timer(1000, null);
        countdownTimer.addActionListener(e -> {
            seconds--;
            if (seconds > 0) {
                statusLabel.setText("FoxCraft startet in " + seconds + " Sekunden ...");
            } else {
                countdownTimer.stop();
                statusLabel.setText("FoxCraft wird gestartet ...");
                startFoxCraft();
            }
        });
        countdownTimer.setInitialDelay(1000);
        countdownTimer.start();
    }

    private void startFoxCraft() {
        try {
            String javaExecutable = getJavaExecutable();
            String classPath = System.getProperty("java.class.path");

            ProcessBuilder builder = new ProcessBuilder(
                    javaExecutable,
                    "-cp",
                    classPath,
                    "FoxCraft",
                    GAME_LAUNCHER_TOKEN
            );

            builder.directory(new File(System.getProperty("user.dir")));
            builder.inheritIO();
            builder.start();
            dispose();
        } catch (Exception ex) {
            statusLabel.setText("FoxCraft konnte nicht gestartet werden.");
            JOptionPane.showMessageDialog(
                    this,
                    "FoxCraft konnte nicht gestartet werden.\n\n" + ex.getMessage(),
                    "FoxCraft Launcher",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static boolean updateLauncherFromGitHubAndRestart(Component parent) {
        try {
            Path appDir = new File(System.getProperty("user.dir")).toPath();
            Path updateDir = appDir.resolve("FoxCraftUpdate");
            Path updateClasses = updateDir.resolve("classes");
            Files.createDirectories(updateClasses);

            Path launcherSource = updateDir.resolve("FoxCraftLauncher.java");
            Path gameSource = updateDir.resolve("FoxCraft.java");

            String remoteVersion = downloadTextStatic(VERSION_URL).trim();
            if (remoteVersion.isEmpty()) {
                throw new IOException("GitHub version.txt ist leer.");
            }

            downloadToFileStatic(LAUNCHER_SOURCE_URL, launcherSource);
            downloadToFileStatic(GAME_SOURCE_URL, gameSource);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException(
                        "Kein Java-Compiler gefunden. Für den Launcher-Update brauchst du ein JDK.");
            }

            int result = compiler.run(
                    null,
                    null,
                    null,
                    "-encoding", "UTF-8",
                    "-d", updateClasses.toString(),
                    launcherSource.toString(),
                    gameSource.toString()
            );

            if (result != 0) {
                throw new IllegalStateException("Die neue FoxCraft-Version konnte nicht kompiliert werden.");
            }

            Files.writeString(
                    appDir.resolve("version.txt"),
                    remoteVersion,
                    StandardCharsets.UTF_8
            );

            ProcessBuilder builder = new ProcessBuilder(
                    getJavaExecutableStatic(),
                    "-cp",
                    updateClasses.toString(),
                    "FoxCraftLauncher",
                    UPDATED_LAUNCHER_FLAG
            );
            builder.directory(appDir.toFile());
            builder.inheritIO();
            builder.start();
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Der Launcher konnte nicht aktualisiert werden.\n\n" + ex.getMessage(),
                    "FoxCraft Launcher Update",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    public static boolean hasGitHubUpdate() throws IOException {
        String remote = downloadTextStatic(VERSION_URL).trim();
        return compareVersions(remote, LAUNCHER_VERSION) > 0;
    }

    private static String downloadTextStatic(String urlString) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        connection.setRequestProperty("User-Agent", "FoxCraftLauncher/" + LAUNCHER_VERSION);
        int code = connection.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new IOException("GitHub antwortet mit HTTP " + code);
        }
        try (InputStream in = connection.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    private String downloadText(String urlString) throws IOException {
        return downloadTextStatic(urlString);
    }

    private static void downloadToFileStatic(String urlString, Path target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", "FoxCraftLauncher/" + LAUNCHER_VERSION);
        int code = connection.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new IOException("Download fehlgeschlagen (HTTP " + code + ").");
        }
        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }
    }

    private void downloadToFile(String urlString, Path target) throws IOException {
        downloadToFileStatic(urlString, target);
    }

    private static String getJavaExecutableStatic() {
        return System.getProperty("java.home")
                + File.separator
                + "bin"
                + File.separator
                + "java";
    }

    private String getJavaExecutable() {
        return getJavaExecutableStatic();
    }

    private static int compareVersions(String a, String b) {
        String[] pa = a.trim().split("\\.");
        String[] pb = b.trim().split("\\.");
        int max = Math.max(pa.length, pb.length);
        for (int i = 0; i < max; i++) {
            int va = i < pa.length ? parseVersionPart(pa[i]) : 0;
            int vb = i < pb.length ? parseVersionPart(pb[i]) : 0;
            if (va != vb) {
                return Integer.compare(va, vb);
            }
        }
        return 0;
    }

    private static int parseVersionPart(String value) {
        String digits = value.replaceAll("[^0-9].*", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
