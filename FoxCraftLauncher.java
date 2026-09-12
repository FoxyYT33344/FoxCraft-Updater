import javax.swing.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * FoxCraft Launcher
 *
 * Funktionen:
 * - Starten / Beenden
 * - Installieren, wenn FoxCraft noch nicht installiert ist
 * - Deinstallieren, wenn FoxCraft installiert ist
 * - Deinstallation verlangt zwei Bestätigungen
 * - Installation lädt die aktuelle FoxCraft.java direkt von GitHub
 * - Beim Launcher-Start wird automatisch nach Spiel- und Launcher-Updates gesucht
 * - Launcher kann aus FoxCraft heraus aktualisiert werden
 * - FoxCraft selbst kann nur über diesen Launcher gestartet werden
 */
public class FoxCraftLauncher extends JFrame {

    // ============================================================
    // LIVE LAUNCHER VERSION
    // ============================================================

    public static final String LAUNCHER_VERSION = "1.1.0";

    // ============================================================
    // GITHUB
    // ============================================================

    public static final String GITHUB_OWNER = "FoxyYT33344";
    public static final String GITHUB_REPOSITORY = "FoxCraft-Updater";
    public static final String GITHUB_BRANCH = "main";

    public static final String GAME_SOURCE_URL =
            "https://raw.githubusercontent.com/" + GITHUB_OWNER + "/"
                    + GITHUB_REPOSITORY + "/" + GITHUB_BRANCH + "/FoxCraft.java";

    public static final String LAUNCHER_SOURCE_URL =
            "https://raw.githubusercontent.com/" + GITHUB_OWNER + "/"
                    + GITHUB_REPOSITORY + "/" + GITHUB_BRANCH + "/FoxCraftLauncher.java";

    // ============================================================
    // START TOKEN / UPDATE TOKEN
    // ============================================================

    public static final String GAME_LAUNCHER_TOKEN = "FOXCRAFT_LAUNCHER_START_2026";
    private static final String UPDATED_LAUNCHER_FLAG = "--foxcraft-updated-launcher";

    // ============================================================
    // INSTALLATION
    // ============================================================

    private static final Path INSTALL_DIR =
            Paths.get(System.getProperty("user.home"), "FoxCraft");

    private static final Path INSTALLED_GAME_SOURCE = INSTALL_DIR.resolve("FoxCraft.java");
    private static final Path INSTALLED_GAME_CLASS = INSTALL_DIR.resolve("FoxCraft.class");
    private static final Path INSTALLED_LAUNCHER_SOURCE = INSTALL_DIR.resolve("FoxCraftLauncher.java");
    private static final Path INSTALL_MARKER = INSTALL_DIR.resolve("installed.flag");

    private final JLabel statusLabel = new JLabel("FoxCraft Launcher wird gestartet ...");
    private final JLabel versionLabel = new JLabel("Launcher " + LAUNCHER_VERSION);
    private JButton startButton;
    private JButton installUninstallButton;
    private JButton updateButton;
    private JButton exitButton;
    private boolean busy;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FoxCraftLauncher launcher = new FoxCraftLauncher();
            launcher.setVisible(true);
            launcher.startupUpdateCheck(args);
        });
    }

    public FoxCraftLauncher() {
        setTitle("FoxCraft Launcher");
        setSize(760, 520);
        setMinimumSize(new Dimension(680, 460));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBorder(BorderFactory.createEmptyBorder(28, 32, 26, 32));
        root.setBackground(new Color(18, 18, 22));

        JLabel title = new JLabel("FOXCRAFT");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 54));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitle = new JLabel("Launcher");
        subtitle.setForeground(new Color(175, 175, 175));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 21));
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.setOpaque(false);
        top.add(title, BorderLayout.CENTER);
        top.add(subtitle, BorderLayout.SOUTH);

        JLabel fox = new JLabel("🦊");
        fox.setHorizontalAlignment(SwingConstants.CENTER);
        fox.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 76));

        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        versionLabel.setForeground(new Color(145, 145, 145));
        versionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.setOpaque(false);
        bottom.add(statusLabel, BorderLayout.CENTER);
        bottom.add(versionLabel, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new GridLayout(1, 0, 10, 0));
        buttons.setOpaque(false);

        startButton = createButton("STARTEN");
        installUninstallButton = createButton("INSTALLIEREN");
        updateButton = createButton("NACH UPDATE SUCHEN");
        exitButton = createButton("BEENDEN");

        startButton.addActionListener(e -> startGame());
        installUninstallButton.addActionListener(e -> installOrUninstall());
        updateButton.addActionListener(e -> manualUpdateCheck());
        exitButton.addActionListener(e -> System.exit(0));

        buttons.add(startButton);
        buttons.add(installUninstallButton);
        buttons.add(updateButton);
        buttons.add(exitButton);

        root.add(top, BorderLayout.NORTH);
        root.add(fox, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        JPanel wrapper = new JPanel(new BorderLayout(0, 15));
        wrapper.setOpaque(false);
        wrapper.add(root, BorderLayout.CENTER);
        wrapper.add(bottom, BorderLayout.SOUTH);
        setContentPane(wrapper);

        refreshButtons();
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 15));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(160, 46));
        return button;
    }

    // ============================================================
    // STARTUP UPDATE
    // ============================================================

    private void startupUpdateCheck(String[] args) {
        refreshButtons();
        setBusy(true);
        status("Suche automatisch nach Updates ...");

        Thread thread = new Thread(() -> {
            try {
                Files.createDirectories(INSTALL_DIR);

                String remoteGameSource = downloadText(GAME_SOURCE_URL);
                String remoteLauncherSource = downloadText(LAUNCHER_SOURCE_URL);

                String remoteGameVersion = extractVersion(remoteGameSource, "FOXCRAFT_VERSION");
                String remoteLauncherVersion = extractVersion(remoteLauncherSource, "LAUNCHER_VERSION");

                boolean installed = isInstalled();
                String localGameVersion = installed
                        ? getLocalGameVersion()
                        : "0.0.0";

                boolean gameUpdate = !installed || compareVersions(remoteGameVersion, localGameVersion) > 0;
                boolean launcherUpdate = compareVersions(remoteLauncherVersion, LAUNCHER_VERSION) > 0;

                // Bei einem echten Launcher-Update zuerst den neuen Launcher vorbereiten.
                if (launcherUpdate && !containsArgument(args, UPDATED_LAUNCHER_FLAG)) {
                    status("Neuer Launcher gefunden: " + LAUNCHER_VERSION + " → " + remoteLauncherVersion);
                    performLauncherUpdateAndRestart(remoteLauncherSource, remoteGameSource);
                    return;
                }

                // Spiel installieren/aktualisieren.
                if (gameUpdate) {
                    status(installed
                            ? "Neues FoxCraft-Update gefunden ..."
                            : "FoxCraft ist noch nicht installiert ...");
                    installOrUpdateGame(remoteGameSource, remoteGameVersion);
                }

                SwingUtilities.invokeLater(() -> {
                    setBusy(false);
                    refreshButtons();
                    if (gameUpdate) {
                        status("FoxCraft " + remoteGameVersion + " ist bereit.");
                    } else {
                        status("FoxCraft ist aktuell.");
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setBusy(false);
                    refreshButtons();
                    status("Update-Prüfung fehlgeschlagen. Lokales Spiel bleibt verfügbar.");
                    JOptionPane.showMessageDialog(
                            this,
                            "GitHub konnte nicht geprüft werden.\n\n" + ex.getMessage()
                                    + "\n\nDu kannst ein bereits installiertes FoxCraft trotzdem starten.",
                            "FoxCraft Launcher",
                            JOptionPane.WARNING_MESSAGE
                    );
                });
            }
        }, "FoxCraft-Startup-Update-Check");
        thread.setDaemon(true);
        thread.start();
    }

    private void manualUpdateCheck() {
        if (busy) return;
        setBusy(true);
        status("Suche auf GitHub nach Updates ...");

        Thread thread = new Thread(() -> {
            try {
                String remoteGameSource = downloadText(GAME_SOURCE_URL);
                String remoteLauncherSource = downloadText(LAUNCHER_SOURCE_URL);
                String remoteGameVersion = extractVersion(remoteGameSource, "FOXCRAFT_VERSION");
                String remoteLauncherVersion = extractVersion(remoteLauncherSource, "LAUNCHER_VERSION");

                boolean gameUpdate = !isInstalled()
                        || compareVersions(remoteGameVersion, getLocalGameVersion()) > 0;
                boolean launcherUpdate = compareVersions(remoteLauncherVersion, LAUNCHER_VERSION) > 0;

                if (launcherUpdate) {
                    status("Launcher-Update gefunden: " + remoteLauncherVersion);
                    performLauncherUpdateAndRestart(remoteLauncherSource, remoteGameSource);
                    return;
                }

                if (gameUpdate) {
                    status("FoxCraft-Update wird installiert ...");
                    installOrUpdateGame(remoteGameSource, remoteGameVersion);
                    SwingUtilities.invokeLater(() -> {
                        setBusy(false);
                        refreshButtons();
                        status("FoxCraft " + remoteGameVersion + " wurde aktualisiert.");
                        JOptionPane.showMessageDialog(
                                this,
                                "FoxCraft wurde auf Version " + remoteGameVersion + " aktualisiert.",
                                "Update fertig",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    });
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    setBusy(false);
                    refreshButtons();
                    status("Keine neuen Updates gefunden.");
                    JOptionPane.showMessageDialog(
                            this,
                            "FoxCraft und der Launcher sind aktuell.",
                            "Kein Update",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setBusy(false);
                    refreshButtons();
                    status("Update-Prüfung fehlgeschlagen.");
                    JOptionPane.showMessageDialog(
                            this,
                            "Die Updates konnten nicht geprüft werden.\n\n" + ex.getMessage(),
                            "FoxCraft Launcher",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
            }
        }, "FoxCraft-Manual-Update-Check");
        thread.setDaemon(true);
        thread.start();
    }

    // ============================================================
    // INSTALL / UNINSTALL
    // ============================================================

    private void installOrUninstall() {
        if (busy) return;

        if (isInstalled()) {
            uninstallGame();
        } else {
            installGameFromGitHub();
        }
    }

    private void installGameFromGitHub() {
        int answer = JOptionPane.showConfirmDialog(
                this,
                "FoxCraft wird aus deinem GitHub-Repository heruntergeladen und installiert.\n\n"
                        + "Installationsordner:\n" + INSTALL_DIR
                        + "\n\nFortfahren?",
                "FoxCraft installieren",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (answer != JOptionPane.YES_OPTION) return;

        setBusy(true);
        status("Aktuelle FoxCraft.java wird von GitHub geladen ...");

        Thread thread = new Thread(() -> {
            try {
                String remoteGameSource = downloadText(GAME_SOURCE_URL);
                String remoteVersion = extractVersion(remoteGameSource, "FOXCRAFT_VERSION");
                installOrUpdateGame(remoteGameSource, remoteVersion);

                SwingUtilities.invokeLater(() -> {
                    setBusy(false);
                    refreshButtons();
                    status("FoxCraft " + remoteVersion + " wurde installiert.");
                    JOptionPane.showMessageDialog(
                            this,
                            "FoxCraft " + remoteVersion + " wurde installiert.\n\nDu kannst das Spiel jetzt starten.",
                            "Installation abgeschlossen",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setBusy(false);
                    refreshButtons();
                    status("Installation fehlgeschlagen.");
                    JOptionPane.showMessageDialog(
                            this,
                            "FoxCraft konnte nicht installiert werden.\n\n" + ex.getMessage(),
                            "Installation fehlgeschlagen",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
            }
        }, "FoxCraft-Installer");
        thread.setDaemon(true);
        thread.start();
    }

    private void installOrUpdateGame(String remoteGameSource, String remoteVersion) throws Exception {
        Files.createDirectories(INSTALL_DIR);
        Path temporary = INSTALL_DIR.resolve("FoxCraft.java.download");
        Files.writeString(temporary, remoteGameSource, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Java-Code direkt als FoxCraft.java installieren.
        Files.move(temporary, INSTALLED_GAME_SOURCE,
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        compileGame();
        Files.writeString(INSTALL_MARKER, remoteVersion, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Auch eine Kopie der aktuellen Launcher-Datei ablegen, damit die Installation
        // alle benötigten Dateien im FoxCraft-Ordner enthält.
        try {
            String launcherSource = downloadText(LAUNCHER_SOURCE_URL);
            Files.writeString(INSTALLED_LAUNCHER_SOURCE, launcherSource, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {
            // Für das Spiel selbst ist die Launcher-Kopie nicht notwendig.
        }
    }

    private void compileGame() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "Kein Java-Compiler gefunden. Der Launcher benötigt ein JDK, damit FoxCraft installiert/aktualisiert werden kann.");
        }

        int result = compiler.run(
                null, null, null,
                "-encoding", "UTF-8",
                "-d", INSTALL_DIR.toString(),
                INSTALLED_GAME_SOURCE.toString()
        );

        if (result != 0 || !Files.exists(INSTALLED_GAME_CLASS)) {
            throw new IllegalStateException("FoxCraft.java konnte nicht erfolgreich kompiliert werden.");
        }
    }

    private void uninstallGame() {
        // 1. Bestätigung
        int first = JOptionPane.showConfirmDialog(
                this,
                "Möchtest du FoxCraft wirklich deinstallieren?\n\n"
                        + "Dabei werden die installierte FoxCraft.java und die kompilierten Spieldateien entfernt.",
                "FoxCraft deinstallieren",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (first != JOptionPane.YES_OPTION) return;

        // 2. Bestätigung
        int second = JOptionPane.showConfirmDialog(
                this,
                "LETZTE BESTÄTIGUNG\n\n"
                        + "FoxCraft wird jetzt vom Computer entfernt.\n\n"
                        + "Möchtest du wirklich fortfahren?",
                "FoxCraft wirklich deinstallieren?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (second != JOptionPane.YES_OPTION) return;

        setBusy(true);
        status("FoxCraft wird deinstalliert ...");

        Thread thread = new Thread(() -> {
            try {
                deleteInstalledGameFiles();
                SwingUtilities.invokeLater(() -> {
                    setBusy(false);
                    refreshButtons();
                    status("FoxCraft wurde deinstalliert.");
                    JOptionPane.showMessageDialog(
                            this,
                            "FoxCraft wurde deinstalliert.\n\nDer Launcher bleibt erhalten.",
                            "Deinstallation abgeschlossen",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setBusy(false);
                    refreshButtons();
                    status("Deinstallation fehlgeschlagen.");
                    JOptionPane.showMessageDialog(
                            this,
                            "FoxCraft konnte nicht vollständig deinstalliert werden.\n\n" + ex.getMessage(),
                            "Deinstallation fehlgeschlagen",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
            }
        }, "FoxCraft-Uninstaller");
        thread.setDaemon(true);
        thread.start();
    }

    private void deleteInstalledGameFiles() throws IOException {
        String[] classNames = {
                "FoxCraft.class", "FoxCraft$GamePanel.class", "FoxCraft$NetworkServer.class",
                "FoxCraft$NetworkClient.class", "FoxCraft$LauncherPanel.class", "FoxCraft$MainMenuPanel.class"
        };

        Files.deleteIfExists(INSTALLED_GAME_SOURCE);
        Files.deleteIfExists(INSTALLED_LAUNCHER_SOURCE);
        Files.deleteIfExists(INSTALL_MARKER);
        Files.deleteIfExists(INSTALLED_GAME_CLASS);
        for (String name : classNames) {
            Files.deleteIfExists(INSTALL_DIR.resolve(name));
        }

        // Entfernt weitere FoxCraft.class-Innendateien, ohne den gesamten Benutzerordner zu löschen.
        if (Files.isDirectory(INSTALL_DIR)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(INSTALL_DIR, "FoxCraft$*.class")) {
                for (Path path : stream) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    // ============================================================
    // START GAME
    // ============================================================

    private void startGame() {
        if (!isInstalled()) {
            JOptionPane.showMessageDialog(
                    this,
                    "FoxCraft ist noch nicht installiert.\n\nDrücke zuerst auf INSTALLIEREN.",
                    "FoxCraft",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        try {
            if (!Files.exists(INSTALLED_GAME_CLASS)) {
                compileGame();
            }

            String javaExecutable = getJavaExecutable();
            ProcessBuilder builder = new ProcessBuilder(
                    javaExecutable,
                    "-cp",
                    INSTALL_DIR.toString(),
                    "FoxCraft",
                    GAME_LAUNCHER_TOKEN
            );
            builder.directory(INSTALL_DIR.toFile());
            builder.inheritIO();
            builder.start();
            status("FoxCraft wird gestartet ...");
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "FoxCraft konnte nicht gestartet werden.\n\n" + ex.getMessage(),
                    "FoxCraft Startfehler",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ============================================================
    // INGAME LAUNCHER UPDATE API
    // Diese Methoden werden von FoxCraft.java verwendet.
    // ============================================================

    public static boolean hasGitHubUpdate() throws IOException {
        String remoteSource = downloadTextStatic(LAUNCHER_SOURCE_URL);
        String remoteVersion = extractVersion(remoteSource, "LAUNCHER_VERSION");
        return compareVersions(remoteVersion, LAUNCHER_VERSION) > 0;
    }

    public static String getGitHubGameVersion() throws IOException {
        String remoteSource = downloadTextStatic(GAME_SOURCE_URL);
        return extractVersion(remoteSource, "FOXCRAFT_VERSION");
    }

    public static boolean updateLauncherFromGitHubAndRestart(Component parent) {
        try {
            String remoteLauncherSource = downloadTextStatic(LAUNCHER_SOURCE_URL);
            String remoteGameSource = downloadTextStatic(GAME_SOURCE_URL);
            String remoteLauncherVersion = extractVersion(remoteLauncherSource, "LAUNCHER_VERSION");

            if (compareVersions(remoteLauncherVersion, LAUNCHER_VERSION) <= 0) {
                JOptionPane.showMessageDialog(
                        parent,
                        "Der FoxCraft Launcher ist bereits aktuell.\n\nLauncher-Version: "
                                + LAUNCHER_VERSION,
                        "FoxCraft Launcher Update",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return false;
            }

            return startPreparedLauncherUpdate(parent, remoteLauncherSource, remoteGameSource);
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

    private void performLauncherUpdateAndRestart(String remoteLauncherSource, String remoteGameSource) {
        try {
            startPreparedLauncherUpdate(this, remoteLauncherSource, remoteGameSource);
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> {
                setBusy(false);
                refreshButtons();
                status("Launcher-Update fehlgeschlagen.");
                JOptionPane.showMessageDialog(
                        this,
                        "Der Launcher konnte nicht aktualisiert werden.\n\n" + ex.getMessage(),
                        "Launcher Update",
                        JOptionPane.ERROR_MESSAGE
                );
            });
        }
    }

    private static boolean startPreparedLauncherUpdate(
            Component parent,
            String remoteLauncherSource,
            String remoteGameSource) throws Exception {

        Path updateDir = INSTALL_DIR.resolve("FoxCraftLauncherUpdate");
        Path updateClasses = updateDir.resolve("classes");
        Files.createDirectories(updateClasses);

        Path launcherSource = updateDir.resolve("FoxCraftLauncher.java");
        Path gameSource = updateDir.resolve("FoxCraft.java");
        Files.writeString(launcherSource, remoteLauncherSource, StandardCharsets.UTF_8);
        Files.writeString(gameSource, remoteGameSource, StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Kein Java-Compiler gefunden. Bitte ein JDK verwenden.");
        }

        int result = compiler.run(
                null, null, null,
                "-encoding", "UTF-8",
                "-d", updateClasses.toString(),
                launcherSource.toString(),
                gameSource.toString()
        );

        if (result != 0) {
            throw new IllegalStateException("Das neue Launcher-Paket konnte nicht kompiliert werden.");
        }

        ProcessBuilder builder = new ProcessBuilder(
                getJavaExecutableStatic(),
                "-cp",
                updateClasses.toString(),
                "FoxCraftLauncher",
                UPDATED_LAUNCHER_FLAG
        );
        builder.directory(INSTALL_DIR.toFile());
        builder.inheritIO();
        builder.start();

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    parent,
                    "Der aktualisierte FoxCraft Launcher wurde vorbereitet.\n\n"
                            + "Der Launcher startet jetzt neu.",
                    "Launcher Update",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        Thread.sleep(500);
        System.exit(0);
        return true;
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private boolean isInstalled() {
        try {
            return Files.exists(INSTALL_MARKER)
                    && Files.exists(INSTALLED_GAME_SOURCE)
                    && Files.exists(INSTALLED_GAME_CLASS);
        } catch (Exception ex) {
            return false;
        }
    }

    private String getLocalGameVersion() throws IOException {
        if (!Files.exists(INSTALLED_GAME_SOURCE)) {
            return "0.0.0";
        }
        return extractVersion(
                Files.readString(INSTALLED_GAME_SOURCE, StandardCharsets.UTF_8),
                "FOXCRAFT_VERSION"
        );
    }

    private static String downloadTextStatic(String urlString) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", "FoxCraftLauncher/" + LAUNCHER_VERSION);
        int code = connection.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new IOException("GitHub antwortet mit HTTP " + code + ".");
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

    private static String extractVersion(String source, String constantName) throws IOException {
        String marker = constantName + " = \"";
        int index = source.indexOf(marker);
        if (index < 0) {
            throw new IOException("Die Version " + constantName + " wurde im GitHub-Code nicht gefunden.");
        }
        int start = index + marker.length();
        int end = source.indexOf('"', start);
        if (end < 0) {
            throw new IOException("Ungültige Version für " + constantName + ".");
        }
        String version = source.substring(start, end).trim();
        if (version.isEmpty()) {
            throw new IOException("Version " + constantName + " ist leer.");
        }
        return version;
    }

    private static int compareVersions(String a, String b) {
        String[] pa = a.trim().split("\\.");
        String[] pb = b.trim().split("\\.");
        int max = Math.max(pa.length, pb.length);
        for (int i = 0; i < max; i++) {
            int va = i < pa.length ? parseVersionPart(pa[i]) : 0;
            int vb = i < pb.length ? parseVersionPart(pb[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    public static int compareVersionsPublic(String a, String b) {
        return compareVersions(a, b);
    }

    private static int parseVersionPart(String value) {
        String digits = value.replaceAll("[^0-9].*", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String getJavaExecutable() {
        return getJavaExecutableStatic();
    }

    private static String getJavaExecutableStatic() {
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
        return Paths.get(System.getProperty("java.home"), "bin", executable).toString();
    }

    private static boolean containsArgument(String[] args, String wanted) {
        if (args == null) return false;
        for (String arg : args) {
            if (wanted.equals(arg)) return true;
        }
        return false;
    }

    private void refreshButtons() {
        boolean installed = isInstalled();
        if (startButton != null) startButton.setEnabled(installed && !busy);
        if (installUninstallButton != null) {
            installUninstallButton.setText(installed ? "DEINSTALLIEREN" : "INSTALLIEREN");
            installUninstallButton.setEnabled(!busy);
        }
        if (updateButton != null) updateButton.setEnabled(!busy);
        if (exitButton != null) exitButton.setEnabled(true);
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        SwingUtilities.invokeLater(this::refreshButtons);
    }

    private void status(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }
}
