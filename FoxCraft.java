import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FoxCraft extends JFrame {

    // ============================================================
    // VERSION / LAUNCHER UPDATE
    // ============================================================

    static final String FOXCRAFT_VERSION = "1.1.0";

    // Muss zu deinem GitHub-Repository passen.
    static final String GITHUB_OWNER = "FoxyYT33344";
    static final String GITHUB_REPOSITORY = "FoxCraft-Updater";
    static final String GITHUB_BRANCH = "main";


    // ============================================================
    // BLOCKS
    // ============================================================

    static final int AIR = 0;
    static final int GRASS = 1;
    static final int DIRT = 2;
    static final int STONE = 3;
    static final int WOOD = 4;
    static final int WATER = 5;
    static final int SAND = 6;
    static final int GLASS = 7;
    static final int BRICKS = 8;
    static final int LEAVES = 9;
    static final int SNOW = 10;
    static final int COAL = 11;
    static final int GOLD = 12;
    static final int DIAMOND = 13;

    static final String[] BLOCK_NAMES = {
            "air",
            "grass",
            "dirt",
            "stone",
            "wood",
            "water",
            "sand",
            "glass",
            "bricks",
            "leaves",
            "snow",
            "coal",
            "gold",
            "diamond"
    };

    // ============================================================
    // WORLD
    // ============================================================

    static final int WORLD_WIDTH = 64;
    static final int WORLD_HEIGHT = 64;

    // ============================================================
    // CHARACTERS
    // ============================================================

    static final int CHARACTER_FOX = 0;
    static final int CHARACTER_ZIMBLIN = 1;
    static final int CHARACTER_ROBOT = 2;
    static final int CHARACTER_CAT = 3;

    static final String[] CHARACTER_NAMES = {
            "Foxy",
            "Zimblin",
            "Robot",
            "Katze"
    };

    // ============================================================
    // NETWORK
    // ============================================================

    static final int SERVER_PORT = 25575;

    // ============================================================
    // SAVE FILES
    // ============================================================

    static final File FOXCRAFT_FOLDER =
            new File(
                    System.getProperty("user.home"),
                    "FoxCraft"
            );

    static final File PLAYER_FILE =
            new File(
                    FOXCRAFT_FOLDER,
                    "player.txt"
            );

    static final File CHARACTER_FILE =
            new File(
                    FOXCRAFT_FOLDER,
                    "character.txt"
            );

    static final File DOMAIN_FILE =
            new File(
                    FOXCRAFT_FOLDER,
                    "server-domain.txt"
            );

    // ============================================================
    // PLAYER DATA
    // ============================================================

    String playerName = "Spieler";
    int selectedCharacter = CHARACTER_FOX;
    String customServerDomain = "";

    // ============================================================
    // PANELS
    // ============================================================

    JPanel currentPanel;

    LauncherPanel launcherPanel;
    MainMenuPanel mainMenu;

    GamePanel game;

    // ============================================================
    // NETWORK
    // ============================================================

    NetworkServer server;
    NetworkClient client;

    // ============================================================
    // MAIN
    // ============================================================

    private static final String LAUNCHER_TOKEN = "FOXCRAFT_LAUNCHER_START_2026";

    public static void main(String[] args) {

        boolean startedByLauncher = false;

        for (String arg : args) {
            if (LAUNCHER_TOKEN.equals(arg)) {
                startedByLauncher = true;
                break;
            }
        }

        if (!startedByLauncher) {
            JOptionPane.showMessageDialog(
                    null,
                    "Start the game with the launcher\n\n"
                            + "Bitte starte FoxCraft über FoxCraftLauncher.",
                    "FoxCraft",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

            FoxCraft foxCraft = new FoxCraft();

            foxCraft.setVisible(true);
        });
    }

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public FoxCraft() {

        setTitle("FoxCraft");

        setSize(
                1100,
                700
        );

        setMinimumSize(
                new Dimension(
                        900,
                        600
                )
        );

        setLocationRelativeTo(null);

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        loadPlayerData();

        // FoxCraft wird direkt gestartet. Der separate
        // FoxCraftLauncher startet diese Datei nach 3 Sekunden.
        showMainMenu();
    }

    // ============================================================
    // SAVE / LOAD
    // ============================================================

    private void loadPlayerData() {

        if (!FOXCRAFT_FOLDER.exists()) {
            FOXCRAFT_FOLDER.mkdirs();
        }

        if (PLAYER_FILE.exists()) {

            try {

                String name =
                        readFile(
                                PLAYER_FILE
                        ).trim();

                if (!name.isEmpty()) {

                    playerName =
                            cleanNetworkText(
                                    name
                            );
                }

            } catch (Exception ignored) {
            }
        }

        if (CHARACTER_FILE.exists()) {

            try {

                int character =
                        Integer.parseInt(
                                readFile(
                                        CHARACTER_FILE
                                ).trim()
                        );

                if (
                        character >= 0 &&
                        character <
                                CHARACTER_NAMES.length
                ) {

                    selectedCharacter =
                            character;
                }

            } catch (Exception ignored) {
            }
        }

        if (DOMAIN_FILE.exists()) {

            try {

                customServerDomain =
                        cleanDomainText(
                                readFile(DOMAIN_FILE).trim()
                        );

            } catch (Exception ignored) {
            }
        }
    }

    private void savePlayerData() {

        try {

            if (!FOXCRAFT_FOLDER.exists()) {
                FOXCRAFT_FOLDER.mkdirs();
            }

            writeFile(
                    PLAYER_FILE,
                    cleanNetworkText(
                            playerName
                    )
            );

            writeFile(
                    CHARACTER_FILE,
                    String.valueOf(
                            selectedCharacter
                    )
            );

            writeFile(
                    DOMAIN_FILE,
                    cleanDomainText(
                            customServerDomain
                    )
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Einstellungen konnten nicht gespeichert werden.\n\n"
                            + e.getMessage(),
                    "FoxCraft",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private String readFile(
            File file
    ) throws IOException {

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        new FileInputStream(
                                                file
                                        ),
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            StringBuilder result =
                    new StringBuilder();

            String line;

            while (
                    (line =
                            reader.readLine())
                            != null
            ) {

                result.append(line);
            }

            return result.toString();
        }
    }

    private void writeFile(
            File file,
            String text
    ) throws IOException {

        try (
                Writer writer =
                        new OutputStreamWriter(
                                new FileOutputStream(
                                        file
                                ),
                                StandardCharsets.UTF_8
                        )
        ) {

            writer.write(text);
        }
    }

    // ============================================================
    // LAUNCHER
    // ============================================================

    private void showLauncher() {

        launcherPanel =
                new LauncherPanel();

        setPanel(
                launcherPanel
        );
    }

    class LauncherPanel extends JPanel {

        JTextField nameField;

        JLabel selectedCharacterLabel;

        LauncherPanel() {

            setLayout(
                    new BorderLayout()
            );

            setBackground(
                    new Color(
                            18,
                            18,
                            22
                    )
            );

            // ----------------------------------------------------
            // HEADER
            // ----------------------------------------------------

            JPanel header =
                    new JPanel(
                            new BorderLayout()
                    );

            header.setOpaque(false);

            JLabel title =
                    new JLabel(
                            "FOXCRAFT"
                    );

            title.setForeground(
                    Color.WHITE
            );

            title.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            52
                    )
            );

            title.setBorder(
                    BorderFactory.createEmptyBorder(
                            25,
                            35,
                            5,
                            10
                    )
            );

            JLabel version =
                    new JLabel(
                            "Launcher"
                    );

            version.setForeground(
                    new Color(
                            150,
                            150,
                            150
                    )
            );

            version.setFont(
                    new Font(
                            "Arial",
                            Font.PLAIN,
                            18
                    )
            );

            version.setBorder(
                    BorderFactory.createEmptyBorder(
                            0,
                            40,
                            25,
                            10
                    )
            );

            JPanel titleBox =
                    new JPanel();

            titleBox.setLayout(
                    new BoxLayout(
                            titleBox,
                            BoxLayout.Y_AXIS
                    )
            );

            titleBox.setOpaque(false);

            titleBox.add(title);
            titleBox.add(version);

            header.add(
                    titleBox,
                    BorderLayout.WEST
            );

            add(
                    header,
                    BorderLayout.NORTH
            );

            // ----------------------------------------------------
            // CENTER
            // ----------------------------------------------------

            JPanel center =
                    new JPanel();

            center.setOpaque(false);

            center.setLayout(
                    new BoxLayout(
                            center,
                            BoxLayout.Y_AXIS
                    )
            );

            center.add(
                    Box.createVerticalGlue()
            );

            JLabel welcome =
                    new JLabel(
                            "Willkommen bei FoxCraft!"
                    );

            welcome.setForeground(
                    Color.WHITE
            );

            welcome.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            30
                    )
            );

            welcome.setAlignmentX(
                    Component.CENTER_ALIGNMENT
            );

            center.add(welcome);

            center.add(
                    Box.createVerticalStrut(
                            30
                    )
            );

            JLabel nameLabel =
                    new JLabel(
                            "Spielername"
                    );

            nameLabel.setForeground(
                    Color.WHITE
            );

            nameLabel.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            18
                    )
            );

            nameLabel.setAlignmentX(
                    Component.CENTER_ALIGNMENT
            );

            center.add(nameLabel);

            center.add(
                    Box.createVerticalStrut(
                            8
                    )
            );

            nameField =
                    new JTextField(
                            playerName
                    );

            nameField.setFont(
                    new Font(
                            "Arial",
                            Font.PLAIN,
                            20
                    )
            );

            nameField.setHorizontalAlignment(
                    JTextField.CENTER
            );

            nameField.setMaximumSize(
                    new Dimension(
                            420,
                            45
                    )
            );

            center.add(
                    nameField
            );

            center.add(
                    Box.createVerticalStrut(
                            20
                    )
            );

            selectedCharacterLabel =
                    new JLabel(
                            "Charakter: "
                                    + CHARACTER_NAMES[
                                    selectedCharacter
                            ]
                    );

            selectedCharacterLabel
                    .setForeground(
                            Color.WHITE
                    );

            selectedCharacterLabel
                    .setFont(
                            new Font(
                                    "Arial",
                                    Font.BOLD,
                                    18
                            )
                    );

            selectedCharacterLabel
                    .setAlignmentX(
                            Component.CENTER_ALIGNMENT
                    );

            center.add(
                    selectedCharacterLabel
            );

            center.add(
                    Box.createVerticalStrut(
                            15
                    )
            );

            JButton characterButton =
                    createButton(
                            "CHARAKTER AUSWÄHLEN"
                    );

            center.add(
                    characterButton
            );

            center.add(
                    Box.createVerticalStrut(
                            25
                    )
            );

            JButton startButton =
                    createButton(
                            "FOXCRAFT STARTEN"
                    );

            startButton.setPreferredSize(
                    new Dimension(
                            360,
                            55
                    )
            );

            startButton.setMaximumSize(
                    new Dimension(
                            360,
                            55
                    )
            );

            startButton.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            20
                    )
            );

            center.add(
                    startButton
            );

            center.add(
                    Box.createVerticalGlue()
            );

            add(
                    center,
                    BorderLayout.CENTER
            );

            // ----------------------------------------------------
            // FOOTER
            // ----------------------------------------------------

            JLabel footer =
                    new JLabel(
                            "FoxCraft • Dein Block-Abenteuer"
                    );

            footer.setForeground(
                    new Color(
                            120,
                            120,
                            120
                    )
            );

            footer.setHorizontalAlignment(
                    SwingConstants.CENTER
            );

            footer.setBorder(
                    BorderFactory.createEmptyBorder(
                            10,
                            10,
                            15,
                            10
                    )
            );

            add(
                    footer,
                    BorderLayout.SOUTH
            );

            // ----------------------------------------------------
            // BUTTONS
            // ----------------------------------------------------

            characterButton.addActionListener(
                    e -> {

                        saveNameFromLauncher();

                        showCharacterSelection(
                                true
                        );
                    }
            );

            startButton.addActionListener(
                    e -> {

                        if (
                                !saveNameFromLauncher()
                        ) {
                            return;
                        }

                        showMainMenu();
                    }
            );

            nameField.addActionListener(
                    e -> {

                        if (
                                saveNameFromLauncher()
                        ) {

                            JOptionPane.showMessageDialog(
                                    this,
                                    "Dein Name wurde gespeichert!",
                                    "FoxCraft",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    }
            );
        }

        private boolean saveNameFromLauncher() {

            String newName =
                    cleanNetworkText(
                            nameField.getText()
                    ).trim();

            if (newName.isEmpty()) {

                JOptionPane.showMessageDialog(
                        FoxCraft.this,
                        "Bitte gib einen Namen ein.",
                        "FoxCraft",
                        JOptionPane.WARNING_MESSAGE
                );

                return false;
            }

            if (
                    newName.length() > 16
            ) {

                newName =
                        newName.substring(
                                0,
                                16
                        );
            }

            playerName =
                    newName;

            savePlayerData();

            return true;
        }

        void refreshCharacter() {

            selectedCharacterLabel.setText(
                    "Charakter: "
                            + CHARACTER_NAMES[
                            selectedCharacter
                    ]
            );
        }
    }

    // ============================================================
    // MAIN MENU
    // ============================================================

    private void showMainMenu() {

        mainMenu =
                new MainMenuPanel();

        setPanel(
                mainMenu
        );
    }

    class MainMenuPanel extends JPanel {

        MainMenuPanel() {

            setLayout(
                    new BorderLayout()
            );

            setBackground(
                    new Color(
                            28,
                            28,
                            34
                    )
            );

            JPanel center =
                    new JPanel();

            center.setOpaque(false);

            center.setLayout(
                    new BoxLayout(
                            center,
                            BoxLayout.Y_AXIS
                    )
            );

            center.add(
                    Box.createVerticalGlue()
            );

            JLabel title =
                    new JLabel(
                            "FOXCRAFT"
                    );

            title.setForeground(
                    Color.WHITE
            );

            title.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            58
                    )
            );

            title.setAlignmentX(
                    Component.CENTER_ALIGNMENT
            );

            center.add(title);

            center.add(
                    Box.createVerticalStrut(
                            10
                    )
            );

            JLabel subtitle =
                    new JLabel(
                            "Deine Welt. Deine Regeln. | FoxCraft " + FOXCRAFT_VERSION
                    );

            subtitle.setForeground(
                    new Color(
                            180,
                            180,
                            180
                    )
            );

            subtitle.setFont(
                    new Font(
                            "Arial",
                            Font.PLAIN,
                            20
                    )
            );

            subtitle.setAlignmentX(
                    Component.CENTER_ALIGNMENT
            );

            center.add(
                    subtitle
            );

            center.add(
                    Box.createVerticalStrut(
                            25
                    )
            );

            JLabel player =
                    new JLabel(
                            "Angemeldet als: "
                                    + playerName
                                    + " • "
                                    + CHARACTER_NAMES[
                                    selectedCharacter
                            ]
                    );

            player.setForeground(
                    Color.WHITE
            );

            player.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            18
                    )
            );

            player.setAlignmentX(
                    Component.CENTER_ALIGNMENT
            );

            center.add(player);

            center.add(
                    Box.createVerticalStrut(
                            25
                    )
            );

            JButton host =
                    createButton(
                            "WELT HOSTEN"
                    );

            JButton join =
                    createButton(
                            "WELT BEITRETEN"
                    );

            JButton launcher =
                    createButton(
                            "LAUNCHER / NAME"
                    );

            JButton character =
                    createButton(
                            "CHARAKTER"
                    );

            JButton updateLauncher =
                    createButton(
                            "FOXCRAFT LAUNCHER UPDATE"
                    );

            JButton exit =
                    createButton(
                            "BEENDEN"
                    );

            center.add(host);

            center.add(
                    Box.createVerticalStrut(
                            10
                    )
            );

            center.add(join);

            center.add(
                    Box.createVerticalStrut(
                            10
                    )
            );

            center.add(launcher);

            center.add(
                    Box.createVerticalStrut(
                            10
                    )
            );

            center.add(character);

            center.add(
                    Box.createVerticalStrut(
                            10
                    )
            );

            center.add(updateLauncher);

            center.add(
                    Box.createVerticalStrut(
                            10
                    )
            );

            center.add(exit);

            center.add(
                    Box.createVerticalGlue()
            );

            add(
                    center,
                    BorderLayout.CENTER
            );

            host.addActionListener(
                    e -> showHostScreen()
            );

            join.addActionListener(
                    e -> showJoinScreen()
            );

            launcher.addActionListener(
                    e -> showLauncher()
            );

            character.addActionListener(
                    e -> showCharacterSelection(
                            false
                    )
            );

            updateLauncher.addActionListener(
                    e -> updateLauncherFromGame()
            );

            exit.addActionListener(
                    e -> System.exit(0)
            );
        }
    }

    private void updateLauncherFromGame() {
        int answer = JOptionPane.showConfirmDialog(
                this,
                "FoxCraft Launcher von GitHub nach Updates prüfen?\n\n"
                        + "Aktuelle FoxCraft-Version: " + FOXCRAFT_VERSION,
                "FoxCraft Launcher Update",
                JOptionPane.YES_NO_OPTION
        );

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                boolean updateAvailable = FoxCraftLauncher.hasGitHubUpdate();
                SwingUtilities.invokeLater(() -> {
                    if (updateAvailable) {
                        boolean started = FoxCraftLauncher.updateLauncherFromGitHubAndRestart(this);
                        if (started) {
                            dispose();
                        }
                    } else {
                        JOptionPane.showMessageDialog(
                                this,
                                "Der FoxCraft Launcher ist bereits aktuell.",
                                "FoxCraft Launcher Update",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        this,
                        "GitHub konnte nicht erreicht werden.\n\n" + ex.getMessage(),
                        "FoxCraft Launcher Update",
                        JOptionPane.ERROR_MESSAGE
                ));
            }
        }, "FoxCraft-Ingame-Updater");
        thread.setDaemon(true);
        thread.start();
    }

    // ============================================================
    // CHARACTER SELECTION
    // ============================================================

    private void showCharacterSelection(
            boolean fromLauncher
    ) {

        setPanel(
                new CharacterPanel(
                        fromLauncher
                )
        );
    }

    class CharacterPanel extends JPanel {

        boolean fromLauncher;

        CharacterPanel(
                boolean fromLauncher
        ) {

            this.fromLauncher =
                    fromLauncher;

            setLayout(
                    new BorderLayout()
            );

            setBackground(
                    new Color(
                            22,
                            22,
                            27
                    )
            );

            JLabel title =
                    new JLabel(
                            "CHARAKTER AUSWÄHLEN",
                            SwingConstants.CENTER
                    );

            title.setForeground(
                    Color.WHITE
            );

            title.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            34
                    )
            );

            title.setBorder(
                    BorderFactory.createEmptyBorder(
                            25,
                            10,
                            20,
                            10
                    )
            );

            add(
                    title,
                    BorderLayout.NORTH
            );

            JPanel cards =
                    new JPanel(
                            new GridLayout(
                                    1,
                                    CHARACTER_NAMES.length,
                                    20,
                                    20
                            )
                    );

            cards.setOpaque(false);

            cards.setBorder(
                    BorderFactory.createEmptyBorder(
                            25,
                            35,
                            25,
                            35
                    )
            );

            for (
                    int i = 0;
                    i < CHARACTER_NAMES.length;
                    i++
            ) {

                cards.add(
                        new CharacterCard(
                                i,
                                fromLauncher
                        )
                );
            }

            add(
                    cards,
                    BorderLayout.CENTER
            );

            JButton back =
                    createButton(
                            "ZURÜCK"
                    );

            JPanel bottom =
                    new JPanel();

            bottom.setOpaque(false);

            bottom.add(back);

            add(
                    bottom,
                    BorderLayout.SOUTH
            );

            back.addActionListener(
                    e -> {

                        if (
                                fromLauncher
                        ) {

                            showLauncher();

                        } else {

                            showMainMenu();
                        }
                    }
            );
        }
    }

    class CharacterCard extends JPanel {

        int character;
        boolean fromLauncher;

        CharacterCard(
                int character,
                boolean fromLauncher
        ) {
            this.fromLauncher = fromLauncher;

            this.character =
                    character;

            boolean selected =
                    character ==
                            selectedCharacter;

            setLayout(
                    new BorderLayout()
            );

            setBackground(
                    selected
                            ? new Color(
                            45,
                            105,
                            55
                    )
                            : new Color(
                            45,
                            45,
                            52
                    )
            );

            setBorder(
                    BorderFactory.createLineBorder(
                            selected
                                    ? Color.GREEN
                                    : new Color(
                                    90,
                                    90,
                                    90
                            ),
                            3
                    )
            );

            JLabel title =
                    new JLabel(
                            CHARACTER_NAMES[
                                    character
                            ],
                            SwingConstants.CENTER
                    );

            title.setForeground(
                    Color.WHITE
            );

            title.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            20
                    )
            );

            title.setBorder(
                    BorderFactory.createEmptyBorder(
                            15,
                            5,
                            10,
                            5
                    )
            );

            add(
                    title,
                    BorderLayout.NORTH
            );

            JPanel preview =
                    new JPanel() {

                        @Override
                        protected void paintComponent(
                                Graphics g
                        ) {

                            super.paintComponent(
                                    g
                            );

                            Graphics2D g2 =
                                    (Graphics2D)
                                            g.create();

                            g2.setRenderingHint(
                                    RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON
                            );

                            drawCharacter(
                                    g2,
                                    character,
                                    getWidth()
                                            / 2,
                                    getHeight()
                                            - 25,
                                    1.5
                            );

                            g2.dispose();
                        }
                    };

            preview.setOpaque(false);

            add(
                    preview,
                    BorderLayout.CENTER
            );

            JButton choose =
                    createButton(
                            selected
                                    ? "AUSGEWÄHLT"
                                    : "AUSWÄHLEN"
                    );

            add(
                    choose,
                    BorderLayout.SOUTH
            );

            choose.addActionListener(
                    e -> {

                        selectedCharacter =
                                character;

                        savePlayerData();

                        showCharacterSelection(
                                fromLauncher
                        );
                    }
            );
        }
    }

    // ============================================================
    // CHARACTER DRAW
    // ============================================================

    private void drawCharacter(
            Graphics2D g,
            int character,
            int center,
            int base,
            double scale
    ) {

        int bodyW =
                (int) (
                        55 * scale
                );

        int bodyH =
                (int) (
                        75 * scale
                );

        int head =
                (int) (
                        60 * scale
                );

        int x =
                center
                        - bodyW / 2;

        int bodyY =
                base
                        - bodyH;

        // Schatten
        g.setColor(
                new Color(
                        0,
                        0,
                        0,
                        70
                )
        );

        g.fillOval(
                center
                        - bodyW / 2,
                base - 5,
                bodyW,
                15
        );

        // ========================================================
        // FOXY
        // ========================================================

        if (
                character ==
                        CHARACTER_FOX
        ) {

            g.setColor(
                    new Color(
                            230,
                            120,
                            30
                    )
            );

            g.fillRoundRect(
                    x,
                    bodyY
                            + head / 2,
                    bodyW,
                    bodyH,
                    20,
                    20
            );

            g.setColor(
                    new Color(
                            245,
                            145,
                            45
                    )
            );

            g.fillOval(
                    center
                            - head / 2,
                    bodyY
                            - head / 2,
                    head,
                    head
            );

            Polygon leftEar =
                    new Polygon();

            leftEar.addPoint(
                    center - head / 3,
                    bodyY - head / 3
            );

            leftEar.addPoint(
                    center - head / 2,
                    bodyY - head
            );

            leftEar.addPoint(
                    center - 5,
                    bodyY - head / 2
            );

            g.fillPolygon(
                    leftEar
            );

            Polygon rightEar =
                    new Polygon();

            rightEar.addPoint(
                    center + head / 3,
                    bodyY - head / 3
            );

            rightEar.addPoint(
                    center + head / 2,
                    bodyY - head
            );

            rightEar.addPoint(
                    center + 5,
                    bodyY - head / 2
            );

            g.fillPolygon(
                    rightEar
            );

            g.setColor(
                    Color.BLACK
            );

            g.fillOval(
                    center - 20,
                    bodyY - 3,
                    10,
                    10
            );

            g.fillOval(
                    center + 10,
                    bodyY - 3,
                    10,
                    10
            );

        // ========================================================
        // ZIMBLIN
        // ========================================================

        } else if (
                character ==
                        CHARACTER_ZIMBLIN
        ) {

            g.setColor(
                    new Color(
                            100,
                            180,
                            100
                    )
            );

            g.fillRoundRect(
                    x,
                    bodyY
                            + head / 2,
                    bodyW,
                    bodyH,
                    15,
                    15
            );

            g.setColor(
                    new Color(
                            135,
                            210,
                            125
                    )
            );

            g.fillOval(
                    center
                            - head / 2,
                    bodyY
                            - head / 2,
                    head,
                    head
            );

            g.setColor(
                    new Color(
                            75,
                            145,
                            75
                    )
            );

            Polygon ear1 =
                    new Polygon();

            ear1.addPoint(
                    center - head / 3,
                    bodyY - head / 3
            );

            ear1.addPoint(
                    center - head / 2,
                    bodyY - head / 2
            );

            ear1.addPoint(
                    center - 5,
                    bodyY - head / 4
            );

            g.fillPolygon(
                    ear1
            );

            Polygon ear2 =
                    new Polygon();

            ear2.addPoint(
                    center + head / 3,
                    bodyY - head / 3
            );

            ear2.addPoint(
                    center + head / 2,
                    bodyY - head / 2
            );

            ear2.addPoint(
                    center + 5,
                    bodyY - head / 4
            );

            g.fillPolygon(
                    ear2
            );

            g.setColor(
                    Color.BLACK
            );

            g.fillOval(
                    center - 18,
                    bodyY - 3,
                    10,
                    10
            );

            g.fillOval(
                    center + 8,
                    bodyY - 3,
                    10,
                    10
            );

        // ========================================================
        // ROBOT
        // ========================================================

        } else if (
                character ==
                        CHARACTER_ROBOT
        ) {

            g.setColor(
                    new Color(
                            125,
                            135,
                            145
                    )
            );

            g.fillRoundRect(
                    x,
                    bodyY
                            + head / 2,
                    bodyW,
                    bodyH,
                    10,
                    10
            );

            g.setColor(
                    new Color(
                            175,
                            185,
                            195
                    )
            );

            g.fillRoundRect(
                    center
                            - head / 2,
                    bodyY
                            - head / 2,
                    head,
                    head,
                    10,
                    10
            );

            g.setColor(
                    Color.BLACK
            );

            g.fillOval(
                    center - 18,
                    bodyY,
                    10,
                    10
            );

            g.fillOval(
                    center + 8,
                    bodyY,
                    10,
                    10
            );

            g.setColor(
                    Color.RED
            );

            g.fillOval(
                    center - 5,
                    bodyY + 22,
                    10,
                    10
            );

        // ========================================================
        // KATZE
        // ========================================================

        } else if (
                character ==
                        CHARACTER_CAT
        ) {

            g.setColor(new Color(150, 150, 160));

            g.fillRoundRect(
                    x,
                    bodyY + head / 2,
                    bodyW,
                    bodyH,
                    22,
                    22
            );

            g.setColor(new Color(185, 185, 195));

            g.fillOval(
                    center - head / 2,
                    bodyY - head / 2,
                    head,
                    head
            );

            Polygon catEarLeft = new Polygon();
            catEarLeft.addPoint(center - head / 2 + 5, bodyY - 8);
            catEarLeft.addPoint(center - head / 3, bodyY - head / 2 - 20);
            catEarLeft.addPoint(center - 3, bodyY - head / 3);
            g.fillPolygon(catEarLeft);

            Polygon catEarRight = new Polygon();
            catEarRight.addPoint(center + head / 2 - 5, bodyY - 8);
            catEarRight.addPoint(center + head / 3, bodyY - head / 2 - 20);
            catEarRight.addPoint(center + 3, bodyY - head / 3);
            g.fillPolygon(catEarRight);

            g.setColor(new Color(230, 150, 170));

            Polygon innerLeft = new Polygon();
            innerLeft.addPoint(center - head / 2 + 13, bodyY - 12);
            innerLeft.addPoint(center - head / 3, bodyY - head / 2 - 8);
            innerLeft.addPoint(center - 12, bodyY - head / 3);
            g.fillPolygon(innerLeft);

            Polygon innerRight = new Polygon();
            innerRight.addPoint(center + head / 2 - 13, bodyY - 12);
            innerRight.addPoint(center + head / 3, bodyY - head / 2 - 8);
            innerRight.addPoint(center + 12, bodyY - head / 3);
            g.fillPolygon(innerRight);

            g.setColor(new Color(55, 150, 90));

            g.fillOval(center - 19, bodyY - 2, 12, 16);
            g.fillOval(center + 7, bodyY - 2, 12, 16);

            g.setColor(new Color(80, 50, 55));

            Polygon nose = new Polygon();
            nose.addPoint(center, bodyY + 16);
            nose.addPoint(center - 5, bodyY + 11);
            nose.addPoint(center + 5, bodyY + 11);
            g.fillPolygon(nose);

            g.setStroke(new BasicStroke(2f));
            g.setColor(Color.WHITE);
            g.drawLine(center - 8, bodyY + 17, center - 32, bodyY + 12);
            g.drawLine(center - 8, bodyY + 22, center - 34, bodyY + 23);
            g.drawLine(center + 8, bodyY + 17, center + 32, bodyY + 12);
            g.drawLine(center + 8, bodyY + 22, center + 34, bodyY + 23);

        // ========================================================
        // KATZE
        // ========================================================

        } else {

            g.setColor(
                    new Color(
                            75,
                            100,
                            155
                    )
            );

            g.fillRoundRect(
                    x,
                    bodyY
                            + head / 2,
                    bodyW,
                    bodyH,
                    15,
                    15
            );

            g.setColor(
                    new Color(
                            185,
                            185,
                            190
                    )
            );

            g.fillOval(
                    center
                            - head / 2,
                    bodyY
                            - head / 2,
                    head,
                    head
            );

            g.setColor(
                    new Color(
                            90,
                            90,
                            100
                    )
            );

            g.fillRect(
                    center
                            - head / 2,
                    bodyY - 3,
                    head,
                    20
            );
        }
    }

    // ============================================================
    // HOST
    // ============================================================

    private void showHostScreen() {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBackground(
                new Color(
                        25,
                        25,
                        30
                )
        );

        panel.add(
                createTitle(
                        "WELT HOSTEN"
                ),
                BorderLayout.NORTH
        );

        JPanel center =
                new JPanel();

        center.setOpaque(false);

        center.setLayout(
                new BoxLayout(
                        center,
                        BoxLayout.Y_AXIS
                )
        );

        center.add(
                Box.createVerticalGlue()
        );

        JLabel status =
                new JLabel(
                        "Internet-Server wird vorbereitet..."
                );

        status.setForeground(
                Color.WHITE
        );

        status.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        18
                )
        );

        status.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        center.add(status);

        JLabel domainTitle =
                new JLabel(
                        "Eigene Domain (optional):"
                );

        domainTitle.setForeground(Color.WHITE);
        domainTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalStrut(18));
        center.add(domainTitle);

        JTextField domainField =
                new JTextField(customServerDomain);

        domainField.setFont(
                new Font("Arial", Font.PLAIN, 18)
        );
        domainField.setHorizontalAlignment(JTextField.CENTER);
        domainField.setMaximumSize(
                new Dimension(450, 42)
        );
        domainField.setToolTipText(
                "Zum Beispiel: play.meine-domain.de"
        );

        center.add(Box.createVerticalStrut(8));
        center.add(domainField);

        JLabel domainInfo =
                new JLabel(
                        "Eigene Domain: DNS muss auf deine öffentliche IP zeigen."
                );
        domainInfo.setForeground(new Color(170, 170, 170));
        domainInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(Box.createVerticalStrut(5));
        center.add(domainInfo);

        JLabel ipLabel =
                new JLabel("");

        ipLabel.setForeground(
                Color.GREEN
        );

        ipLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        22
                )
        );

        ipLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        center.add(
                Box.createVerticalStrut(
                        15
                )
        );

        center.add(ipLabel);

        JButton copy =
                createButton(
                        "IP KOPIEREN"
                );

        copy.setEnabled(false);

        center.add(
                Box.createVerticalStrut(
                        15
                )
        );

        center.add(copy);

        JLabel info =
                new JLabel(
                        "Internet: öffentliche IP oder deine Domain + Port "
                                + SERVER_PORT
                );

        info.setForeground(
                new Color(
                        180,
                        180,
                        180
                )
        );

        info.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        center.add(
                Box.createVerticalStrut(
                        15
                )
        );

        center.add(info);

        center.add(
                Box.createVerticalGlue()
        );

        panel.add(
                center,
                BorderLayout.CENTER
        );

        JPanel bottom =
                new JPanel();

        bottom.setOpaque(false);

        JButton back =
                createButton(
                        "ZURÜCK"
                );

        JButton start =
                createButton(
                        "INTERNET-SERVER ERSTELLEN"
                );

        bottom.add(back);
        bottom.add(start);

        panel.add(
                bottom,
                BorderLayout.SOUTH
        );

        back.addActionListener(
                e -> showMainMenu()
        );

        start.addActionListener(
                e -> {

                    customServerDomain =
                            cleanDomainText(
                                    domainField.getText()
                            );

                    savePlayerData();
                    startHostGame();
                }
        );

        setPanel(panel);

        try {

            server =
                    new NetworkServer();

            server.start();

            String localIp =
                    getLocalIPv4();

            String localAddress =
                    localIp
                            + ":"
                            + SERVER_PORT;

            status.setText(
                    "Server läuft!"
            );

            ipLabel.setText(
                    "LAN: "
                            + localAddress
                            + " | Internet wird ermittelt..."
            );

            copy.setEnabled(true);

            copy.addActionListener(
                    e -> {

                        String text = ipLabel.getText();

                        Toolkit
                                .getDefaultToolkit()
                                .getSystemClipboard()
                                .setContents(
                                        new StringSelection(
                                                text
                                        ),
                                        null
                                );
                    }
            );

            Thread publicIpThread =
                    new Thread(
                            () -> {

                                String publicIp =
                                        getPublicIPv4();

                                SwingUtilities.invokeLater(
                                        () -> {

                                            if (publicIp != null) {

                                                if (!customServerDomain.isEmpty()) {
                                                    ipLabel.setText(
                                                            "Internet: "
                                                                    + customServerDomain
                                                                    + ":"
                                                                    + SERVER_PORT
                                                                    + "  |  IP: "
                                                                    + publicIp
                                                                    + ":"
                                                                    + SERVER_PORT
                                                                    + "  |  LAN: "
                                                                    + localAddress
                                                    );
                                                } else {
                                                    ipLabel.setText(
                                                            "Internet: "
                                                                    + publicIp
                                                                    + ":"
                                                                    + SERVER_PORT
                                                                    + "  |  LAN: "
                                                                    + localAddress
                                                    );
                                                }

                                            } else {

                                                ipLabel.setText(
                                                        "LAN: "
                                                                + localAddress
                                                                + "  |  Öffentliche Adresse wird weiter versucht..."
                                                );
                                            }
                                        }
                                );
                            },
                            "FoxCraft-Public-IP"
                    );

            publicIpThread.setDaemon(true);
            publicIpThread.start();

        } catch (Exception e) {

            status.setText(
                    "Server konnte nicht gestartet werden."
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Der Server konnte nicht gestartet werden.\n\n"
                            + e.getMessage(),
                    "FoxCraft",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void startHostGame() {

        if (server == null) {
            return;
        }

        game =
                new GamePanel(
                        true
                );

        setPanel(
                game
        );

        game.requestFocusInWindow();
    }

    // ============================================================
    // JOIN
    // ============================================================

    private void showJoinScreen() {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBackground(
                new Color(
                        25,
                        25,
                        30
                )
        );

        panel.add(
                createTitle(
                        "WELT BEITRETEN"
                ),
                BorderLayout.NORTH
        );

        JPanel center =
                new JPanel();

        center.setOpaque(false);

        center.setLayout(
                new BoxLayout(
                        center,
                        BoxLayout.Y_AXIS
                )
        );

        center.add(
                Box.createVerticalGlue()
        );

        JLabel label =
                new JLabel(
                        "Server-Adresse:"
                );

        label.setForeground(
                Color.WHITE
        );

        label.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        center.add(label);

        center.add(
                Box.createVerticalStrut(
                        10
                )
        );

        JTextField address =
                new JTextField();

        address.setText(
                "192.168.178.1:"
                        + SERVER_PORT
        );

        address.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        18
                )
        );

        address.setHorizontalAlignment(
                JTextField.CENTER
        );

        address.setMaximumSize(
                new Dimension(
                        450,
                        45
                )
        );

        center.add(address);

        center.add(
                Box.createVerticalStrut(
                        15
                )
        );

        JButton connect =
                createButton(
                        "VERBINDEN"
                );

        center.add(connect);

        center.add(
                Box.createVerticalGlue()
        );

        panel.add(
                center,
                BorderLayout.CENTER
        );

        JPanel bottom =
                new JPanel();

        bottom.setOpaque(false);

        JButton back =
                createButton(
                        "ZURÜCK"
                );

        bottom.add(back);

        panel.add(
                bottom,
                BorderLayout.SOUTH
        );

        back.addActionListener(
                e -> showMainMenu()
        );

        connect.addActionListener(
                e -> connectToServer(
                        address.getText()
                )
        );

        setPanel(panel);

        address.requestFocusInWindow();
    }

    private void connectToServer(
            String input
    ) {

        String address =
                input.trim();

        if (address.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Bitte gib eine Adresse ein.",
                    "FoxCraft",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        String host =
                address;

        int port =
                SERVER_PORT;

        if (
                address.contains(":")
        ) {

            try {

                int index =
                        address.lastIndexOf(
                                ':'
                        );

                host =
                        address.substring(
                                0,
                                index
                        );

                port =
                        Integer.parseInt(
                                address.substring(
                                        index + 1
                                )
                        );

            } catch (Exception e) {

                JOptionPane.showMessageDialog(
                        this,
                        "Ungültige Server-Adresse.",
                        "FoxCraft",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }
        }

        try {

            client =
                    new NetworkClient(
                            host,
                            port
                    );

            client.start();

            game =
                    new GamePanel(
                            false
                    );

            setPanel(
                    game
            );

            game.requestFocusInWindow();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Verbindung fehlgeschlagen.\n\n"
                            + e.getMessage(),
                    "FoxCraft",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ============================================================
    // GAME
    // ============================================================

    class GamePanel extends JPanel
            implements KeyListener,
            MouseListener,
            MouseMotionListener {

        boolean host;

        int[][] world =
                new int[
                        WORLD_WIDTH
                ][
                        WORLD_HEIGHT
                ];

        double playerX = 32;
        double playerY = 32;


        boolean forward;
        boolean backward;
        boolean left;
        boolean right;

        int selectedSlot = 0;

        int[] hotbar = {
                GRASS,
                DIRT,
                STONE,
                WOOD,
                SAND,
                GLASS,
                BRICKS,
                LEAVES,
                DIAMOND
        };

        Map<String, RemotePlayer>
                remotePlayers =
                new HashMap<>();

        List<String>
                chatMessages =
                new ArrayList<>();

        String chatInput = "";

        boolean chatOpen = false;

        long lastUpdate;

        // WICHTIG:
        // Kein Timer-Konflikt mehr!
        javax.swing.Timer gameTimer;

        GamePanel(
                boolean host
        ) {

            this.host = host;

            setFocusable(true);

            setBackground(
                    Color.BLACK
            );

            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);

            generateWorld();

            lastUpdate =
                    System.currentTimeMillis();

            gameTimer =
                    new javax.swing.Timer(
                            16,
                            e -> updateGame()
                    );

            gameTimer.start();

            SwingUtilities.invokeLater(
                    () ->
                            requestFocusInWindow()
            );
        }

        // ========================================================
        // WORLD
        // ========================================================

        private void generateWorld() {

            // Neue Welt bei jedem Start.
            // Kein fester Seed mehr: Die Karte wird jedes Mal zufällig erzeugt.
            Random random =
                    new Random();

            for (
                    int x = 0;
                    x < WORLD_WIDTH;
                    x++
            ) {

                for (
                        int y = 0;
                        y < WORLD_HEIGHT;
                        y++
                ) {

                    double distance =
                            Math.sqrt(
                                    Math.pow(
                                            x - 32,
                                            2
                                    )
                                            +
                                            Math.pow(
                                                    y - 32,
                                                    2
                                            )
                            );

                    if (
                            distance < 5
                    ) {

                        world[x][y] =
                                GRASS;

                    } else {

                        int randomValue =
                                random.nextInt(
                                        100
                                );

                        if (
                                randomValue < 65
                        ) {

                            world[x][y] =
                                    GRASS;

                        } else if (
                                randomValue < 78
                        ) {

                            world[x][y] =
                                    DIRT;

                        } else if (
                                randomValue < 88
                        ) {

                            world[x][y] =
                                    STONE;

                        } else if (
                                randomValue < 93
                        ) {

                            world[x][y] =
                                    SAND;

                        } else if (
                                randomValue < 96
                        ) {

                            world[x][y] =
                                    WATER;

                        } else if (
                                randomValue < 98
                        ) {

                            world[x][y] =
                                    WOOD;

                        } else {

                            world[x][y] =
                                    LEAVES;
                        }
                    }
                }
            }
        }

        // ========================================================
        // UPDATE
        // ========================================================

        private void updateGame() {

            long now =
                    System.currentTimeMillis();

            double delta =
                    (
                            now
                                    - lastUpdate
                    )
                            / 1000.0;

            lastUpdate =
                    now;

            if (
                    delta > 0.1
            ) {

                delta = 0.1;
            }

            if (!chatOpen) {

                // 2D-Bewegung: W/A/S/D bewegt den Spieler direkt
                // auf der Weltkarte. Es gibt keine Kamera-Rotation
                // und keine 3D-Perspektive mehr.
                double speed =
                        8.0 * delta;

                double dx = 0;
                double dy = 0;

                if (forward) {
                    dy -= speed;
                }

                if (backward) {
                    dy += speed;
                }

                if (left) {
                    dx -= speed;
                }

                if (right) {
                    dx += speed;
                }

                // Diagonale Bewegung nicht schneller machen.
                if (dx != 0 && dy != 0) {
                    double factor = 1.0 / Math.sqrt(2.0);
                    dx *= factor;
                    dy *= factor;
                }

                double nx =
                        playerX + dx;

                double ny =
                        playerY + dy;

                if (
                        isWalkable(
                                nx,
                                playerY
                        )
                ) {

                    playerX = nx;
                }

                if (
                        isWalkable(
                                playerX,
                                ny
                        )
                ) {

                    playerY = ny;
                }

                String positionMessage =
                        "POS|"
                                + playerName
                                + "|"
                                + selectedCharacter
                                + "|"
                                + playerX
                                + "|"
                                + playerY;

                if (
                        client != null
                ) {

                    client.send(
                            positionMessage
                    );
                }

                if (
                        server != null
                ) {

                    server.broadcast(
                            positionMessage
                    );
                }
            }

            repaint();
        }

        private boolean isWalkable(
                double x,
                double y
        ) {

            int ix =
                    (int)
                            Math.floor(
                                    x
                            );

            int iy =
                    (int)
                            Math.floor(
                                    y
                            );

            if (
                    ix < 0 ||
                    iy < 0 ||
                    ix >= WORLD_WIDTH ||
                    iy >= WORLD_HEIGHT
            ) {

                return false;
            }

            return world[ix][iy]
                    != WATER;
        }

        // ========================================================
        // RENDER
        // ========================================================

        @Override
        protected void paintComponent(
                Graphics g
        ) {

            super.paintComponent(g);

            Graphics2D g2 =
                    (Graphics2D)
                            g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            drawSky(g2);
            drawWorld(g2);
            drawCrosshair(g2);
            drawHotbar(g2);

            // IMMER SICHTBAR
            drawOwnNametag(g2);

            drawRemoteNametags(g2);

            if (chatOpen) {
                drawChat(g2);
            }

            g2.dispose();
        }

        private static final int TILE_SIZE = 42;

        private void drawSky(
                Graphics2D g
        ) {
            // In der 2D-Version gibt es keinen Himmel mehr.
            // Der Hintergrund wird von der Weltkarte bedeckt.
            g.setColor(new Color(35, 35, 40));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        private void drawWorld(
                Graphics2D g
        ) {
            double cameraX =
                    playerX * TILE_SIZE
                            - getWidth() / 2.0;

            double cameraY =
                    playerY * TILE_SIZE
                            - getHeight() / 2.0;

            int startX =
                    Math.max(
                            0,
                            (int) Math.floor(cameraX / TILE_SIZE) - 1
                    );

            int startY =
                    Math.max(
                            0,
                            (int) Math.floor(cameraY / TILE_SIZE) - 1
                    );

            int endX =
                    Math.min(
                            WORLD_WIDTH - 1,
                            startX + getWidth() / TILE_SIZE + 3
                    );

            int endY =
                    Math.min(
                            WORLD_HEIGHT - 1,
                            startY + getHeight() / TILE_SIZE + 3
                    );

            for (int wy = startY; wy <= endY; wy++) {
                for (int wx = startX; wx <= endX; wx++) {

                    int sx =
                            (int) Math.round(
                                    wx * TILE_SIZE - cameraX
                            );

                    int sy =
                            (int) Math.round(
                                    wy * TILE_SIZE - cameraY
                            );

                    int block = world[wx][wy];

                    g.setColor(getBlockColor(block));
                    g.fillRect(
                            sx,
                            sy,
                            TILE_SIZE + 1,
                            TILE_SIZE + 1
                    );

                    // Dezente Block-Gitterlinien für den echten 2D-Look.
                    g.setColor(new Color(0, 0, 0, 35));
                    g.drawRect(
                            sx,
                            sy,
                            TILE_SIZE,
                            TILE_SIZE
                    );

                    drawBlockDecoration(
                            g,
                            block,
                            sx,
                            sy
                    );
                }
            }

            // Andere Spieler werden direkt auf der 2D-Karte angezeigt.
            for (RemotePlayer remote : remotePlayers.values()) {

                int sx =
                        (int) Math.round(
                                remote.x * TILE_SIZE - cameraX
                                        + TILE_SIZE / 2.0
                        );

                int sy =
                        (int) Math.round(
                                remote.y * TILE_SIZE - cameraY
                                        + TILE_SIZE / 2.0
                        );

                drawPlayer(
                        g,
                        sx,
                        sy,
                        remote.character,
                        remote.name,
                        false
                );
            }

            int playerScreenX = getWidth() / 2;
            int playerScreenY = getHeight() / 2;

            drawPlayer(
                    g,
                    playerScreenX,
                    playerScreenY,
                    selectedCharacter,
                    playerName,
                    true
            );
        }

        private void drawBlockDecoration(
                Graphics2D g,
                int block,
                int x,
                int y
        ) {
            int cx = x + TILE_SIZE / 2;
            int cy = y + TILE_SIZE / 2;

            if (block == WATER) {
                g.setColor(new Color(255, 255, 255, 55));
                g.drawLine(x + 5, cy - 7, x + TILE_SIZE - 6, cy - 7);
                g.drawLine(x + 9, cy + 6, x + TILE_SIZE - 10, cy + 6);
            } else if (block == GRASS) {
                g.setColor(new Color(35, 125, 35, 90));
                g.drawLine(cx - 7, cy + 8, cx - 2, cy - 5);
                g.drawLine(cx, cy + 8, cx + 4, cy - 7);
                g.drawLine(cx + 7, cy + 8, cx + 9, cy - 3);
            } else if (block == STONE) {
                g.setColor(new Color(255, 255, 255, 35));
                g.fillOval(x + 8, y + 8, 5, 5);
                g.fillOval(x + 27, y + 20, 6, 5);
            } else if (block == WOOD) {
                g.setColor(new Color(70, 40, 20, 100));
                g.drawLine(cx - 6, y + 4, cx - 6, y + TILE_SIZE - 4);
                g.drawLine(cx + 6, y + 4, cx + 6, y + TILE_SIZE - 4);
            } else if (block == LEAVES) {
                g.setColor(new Color(20, 90, 30, 100));
                g.fillOval(x + 5, y + 5, 14, 14);
                g.fillOval(x + 20, y + 13, 17, 16);
            } else if (block == DIAMOND) {
                g.setColor(new Color(255, 255, 255, 120));
                Polygon diamond = new Polygon();
                diamond.addPoint(cx, cy - 12);
                diamond.addPoint(cx + 9, cy);
                diamond.addPoint(cx, cy + 12);
                diamond.addPoint(cx - 9, cy);
                g.fillPolygon(diamond);
            } else if (block == GOLD) {
                g.setColor(new Color(255, 245, 120, 150));
                g.fillOval(cx - 7, cy - 7, 14, 14);
            } else if (block == COAL) {
                g.setColor(new Color(0, 0, 0, 120));
                g.fillOval(cx - 7, cy - 5, 12, 10);
            }
        }

        private void drawPlayer(
                Graphics2D g,
                int x,
                int y,
                int character,
                String name,
                boolean ownPlayer
        ) {
            int safeCharacter =
                    Math.max(
                            0,
                            Math.min(
                                    CHARACTER_NAMES.length - 1,
                                    character
                            )
                    );

            int radius = ownPlayer ? 15 : 13;

            Color body;
            Color detail;

            switch (safeCharacter) {
                case CHARACTER_ZIMBLIN:
                    body = new Color(120, 75, 170);
                    detail = new Color(215, 180, 245);
                    break;
                case CHARACTER_ROBOT:
                    body = new Color(115, 125, 135);
                    detail = new Color(70, 210, 230);
                    break;
                case CHARACTER_CAT:
                    body = new Color(165, 165, 175);
                    detail = new Color(235, 150, 175);
                    break;
                default:
                    body = new Color(220, 125, 45);
                    detail = new Color(255, 185, 80);
                    break;
            }

            g.setColor(new Color(0, 0, 0, 70));
            g.fillOval(
                    x - radius - 2,
                    y - radius + 4,
                    radius * 2 + 4,
                    radius * 2 + 4
            );

            g.setColor(body);
            g.fillOval(
                    x - radius,
                    y - radius,
                    radius * 2,
                    radius * 2
            );

            g.setColor(detail);
            g.fillOval(
                    x - radius / 2,
                    y - radius / 2 - 2,
                    radius,
                    radius
            );

            g.setColor(Color.BLACK);
            g.fillOval(x - 6, y - 4, 4, 5);
            g.fillOval(x + 2, y - 4, 4, 5);

            if (ownPlayer) {
                g.setStroke(new BasicStroke(2f));
                g.setColor(Color.WHITE);
                g.drawOval(
                        x - radius - 2,
                        y - radius - 2,
                        radius * 2 + 4,
                        radius * 2 + 4
                );
            }

            drawPlayerName(g, x, y - radius - 8, name, ownPlayer);
        }

        private void drawPlayerName(
                Graphics2D g,
                int centerX,
                int baselineY,
                String name,
                boolean ownPlayer
        ) {
            String text = name;

            g.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            ownPlayer ? 14 : 12
                    )
            );

            FontMetrics metrics = g.getFontMetrics();
            int width = metrics.stringWidth(text);
            int x = centerX - width / 2;

            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(
                    x - 5,
                    baselineY - metrics.getAscent(),
                    width + 10,
                    metrics.getHeight(),
                    8,
                    8
            );

            g.setColor(Color.WHITE);
            g.drawString(text, x, baselineY);
        }

        private Color getBlockColor(
                int block
        ) {
            switch (block) {
                case GRASS:
                    return new Color(80, 180, 70);
                case DIRT:
                    return new Color(130, 80, 40);
                case STONE:
                    return new Color(120, 120, 125);
                case WOOD:
                    return new Color(130, 80, 35);
                case WATER:
                    return new Color(40, 120, 220);
                case SAND:
                    return new Color(220, 200, 120);
                case GLASS:
                    return new Color(160, 220, 240);
                case BRICKS:
                    return new Color(170, 70, 55);
                case LEAVES:
                    return new Color(40, 130, 50);
                case SNOW:
                    return Color.WHITE;
                case COAL:
                    return new Color(45, 45, 45);
                case GOLD:
                    return new Color(240, 190, 30);
                case DIAMOND:
                    return new Color(50, 220, 230);
                default:
                    return Color.GRAY;
            }
        }

        // ========================================================
        // CROSSHAIR
        // ========================================================

        private void drawCrosshair(
                Graphics2D g
        ) {
            if (chatOpen) {
                return;
            }

            // 2D-Mausauswahl: markiert den Block unter dem Mauszeiger.
            if (mouseWorldX >= 0 && mouseWorldY >= 0
                    && mouseWorldX < WORLD_WIDTH
                    && mouseWorldY < WORLD_HEIGHT) {

                double cameraX =
                        playerX * TILE_SIZE - getWidth() / 2.0;
                double cameraY =
                        playerY * TILE_SIZE - getHeight() / 2.0;

                int sx =
                        (int) Math.round(mouseWorldX * TILE_SIZE - cameraX);
                int sy =
                        (int) Math.round(mouseWorldY * TILE_SIZE - cameraY);

                g.setColor(new Color(255, 255, 255, 210));
                g.setStroke(new BasicStroke(2f));
                g.drawRect(
                        sx + 1,
                        sy + 1,
                        TILE_SIZE - 2,
                        TILE_SIZE - 2
                );
            }
        }

        // ========================================================
        // HOTBAR
        // ========================================================

        private void drawHotbar(
                Graphics2D g
        ) {

            int slotSize = 55;

            int total =
                    slotSize
                            * hotbar.length;

            int start =
                    (
                            getWidth()
                                    - total
                    )
                            / 2;

            int y =
                    getHeight()
                            - 75;

            for (
                    int i = 0;
                    i < hotbar.length;
                    i++
            ) {

                if (
                        i ==
                                selectedSlot
                ) {

                    g.setColor(
                            Color.YELLOW
                    );

                } else {

                    g.setColor(
                            new Color(
                                    40,
                                    40,
                                    40,
                                    220
                            )
                    );
                }

                g.fillRect(
                        start
                                + i
                                * slotSize,
                        y,
                        slotSize - 3,
                        slotSize - 3
                );

                g.setColor(
                        Color.WHITE
                );

                g.drawRect(
                        start
                                + i
                                * slotSize,
                        y,
                        slotSize - 3,
                        slotSize - 3
                );

                g.setColor(
                        getBlockColor(
                                hotbar[i]
                        )
                );

                g.fillRect(
                        start
                                + i
                                * slotSize
                                + 13,
                        y + 13,
                        27,
                        27
                );

                g.setColor(
                        Color.WHITE
                );

                g.setFont(
                        new Font(
                                "Arial",
                                Font.BOLD,
                                12
                        )
                );

                g.drawString(
                        String.valueOf(
                                i + 1
                        ),
                        start
                                + i
                                * slotSize
                                + 4,
                        y + 15
                );
            }
        }

        // ========================================================
        // OWN NAMETAG
        // ========================================================

        private void drawOwnNametag(
                Graphics2D g
        ) {
            // Der eigene Name wird direkt über der Spielfigur angezeigt.
        }

        // ========================================================
        // REMOTE NAMETAGS
        // ========================================================

        private void drawRemoteNametags(
                Graphics2D g
        ) {
            // Die Namen werden direkt über den 2D-Spielerfiguren gezeichnet.
        }

        // ========================================================
        // CHAT
        // ========================================================

        private void drawChat(
                Graphics2D g
        ) {

            int x = 20;

            int y =
                    getHeight()
                            - 260;

            g.setColor(
                    new Color(
                            0,
                            0,
                            0,
                            180
                    )
            );

            g.fillRoundRect(
                    x,
                    y,
                    520,
                    220,
                    12,
                    12
            );

            g.setFont(
                    new Font(
                            "Arial",
                            Font.PLAIN,
                            15
                    )
            );

            int lineY =
                    y + 25;

            int start =
                    Math.max(
                            0,
                            chatMessages.size()
                                    - 8
                    );

            for (
                    int i = start;
                    i < chatMessages.size();
                    i++
            ) {

                g.setColor(
                        Color.WHITE
                );

                g.drawString(
                        chatMessages.get(i),
                        x + 12,
                        lineY
                );

                lineY += 22;
            }

            g.setColor(
                    Color.WHITE
            );

            g.fillRoundRect(
                    x + 10,
                    y + 185,
                    490,
                    25,
                    5,
                    5
            );

            g.setColor(
                    Color.BLACK
            );

            g.drawString(
                    "> "
                            + chatInput,
                    x + 18,
                    y + 203
            );
        }

        void addChat(
                String message
        ) {

            chatMessages.add(
                    message
            );

            while (
                    chatMessages.size()
                            > 30
            ) {

                chatMessages.remove(
                        0
                );
            }
        }

        // ========================================================
        // CHAT / COMMANDS
        // ========================================================

        private void handleChat(
                String text
        ) {

            if (
                    text.startsWith(
                            "/"
                    )
            ) {

                executeCommand(
                        text
                );

                return;
            }

            String message =
                    playerName
                            + ": "
                            + text;

            addChat(
                    message
            );

            if (
                    server != null
            ) {

                server.broadcast(
                        "CHAT|"
                                + message
                );
            }

            if (
                    client != null
            ) {

                client.send(
                        "CHAT|"
                                + message
                );
            }
        }

        private void executeCommand(
                String command
        ) {

            String[] parts =
                    command
                            .trim()
                            .split(
                                    "\\s+"
                            );

            if (
                    parts.length == 0
            ) {
                return;
            }

            String cmd =
                    parts[0]
                            .toLowerCase();

            if (
                    cmd.equals(
                            "/help"
                    )
            ) {

                addChat(
                        "FoxCraft Befehle:"
                );

                addChat(
                        "/help"
                );

                addChat(
                        "/give <block> <anzahl>"
                );

                addChat(
                        "/setblock <block>"
                );

                addChat(
                        "/pos"
                );

                addChat(
                        "/time"
                );

                return;
            }

            if (
                    cmd.equals(
                            "/pos"
                    )
            ) {

                addChat(
                        "Position: "
                                + String.format(
                                "%.2f",
                                playerX
                        )
                                + ", "
                                + String.format(
                                "%.2f",
                                playerY
                        )
                );

                return;
            }

            if (
                    cmd.equals(
                            "/time"
                    )
            ) {

                addChat(
                        "FoxCraft-Zeit: "
                                + System.currentTimeMillis()
                );

                return;
            }

            if (
                    cmd.equals(
                            "/give"
                    )
            ) {

                if (
                        parts.length < 2
                ) {

                    addChat(
                            "Benutzung: /give <block> <anzahl>"
                    );

                    return;
                }

                int block =
                        getBlockByName(
                                parts[1]
                        );

                if (
                        block == AIR
                ) {

                    addChat(
                            "Unbekannter Block: "
                                    + parts[1]
                    );

                    return;
                }

                int amount = 1;

                if (
                        parts.length >= 3
                ) {

                    try {

                        amount =
                                Integer.parseInt(
                                        parts[2]
                                );

                    } catch (
                            Exception e
                    ) {

                        addChat(
                                "Ungültige Anzahl."
                        );

                        return;
                    }
                }

                addChat(
                        "Du bekommst "
                                + amount
                                + "x "
                                + BLOCK_NAMES[
                                block
                        ]
                );

                return;
            }

            if (
                    cmd.equals(
                            "/setblock"
                    )
            ) {

                if (
                        parts.length < 2
                ) {

                    addChat(
                            "Benutzung: /setblock <block>"
                    );

                    return;
                }

                int block =
                        getBlockByName(
                                parts[1]
                        );

                if (
                        block == AIR
                ) {

                    addChat(
                            "Unbekannter Block."
                    );

                    return;
                }

                int x =
                        (int)
                                playerX;

                int y =
                        (int)
                                playerY;

                if (
                        x >= 0 &&
                        y >= 0 &&
                        x < WORLD_WIDTH &&
                        y < WORLD_HEIGHT
                ) {

                    world[x][y] =
                            block;

                    broadcastBlock(
                            x,
                            y,
                            block
                    );

                    addChat(
                            "Block gesetzt: "
                                    + BLOCK_NAMES[
                                    block
                            ]
                    );
                }

                return;
            }

            addChat(
                    "Unbekannter Befehl. /help"
            );
        }

        private int getBlockByName(
                String name
        ) {

            for (
                    int i = 1;
                    i < BLOCK_NAMES.length;
                    i++
            ) {

                if (
                        BLOCK_NAMES[i]
                                .equalsIgnoreCase(
                                        name
                                )
                ) {

                    return i;
                }
            }

            return AIR;
        }

        private void broadcastBlock(
                int x,
                int y,
                int block
        ) {

            String message =
                    "BLOCK|"
                            + x
                            + "|"
                            + y
                            + "|"
                            + block;

            if (
                    server != null
            ) {

                server.broadcast(
                        message
                );
            }

            if (
                    client != null
            ) {

                client.send(
                        message
                );
            }
        }

        // ========================================================
        // COMMAND SUGGESTIONS
        // ========================================================

        private List<String>
        getSuggestions(
                String text
        ) {

            List<String> result =
                    new ArrayList<>();

            if (
                    !text.startsWith(
                            "/"
                    )
            ) {

                return result;
            }

            String[] commands = {
                    "/help",
                    "/give",
                    "/setblock",
                    "/pos",
                    "/time"
            };

            if (
                    !text.contains(
                            " "
                    )
            ) {

                for (
                        String command :
                        commands
                ) {

                    if (
                            command.startsWith(
                                    text
                            )
                    ) {

                        result.add(
                                command
                        );
                    }
                }

                return result;
            }

            if (
                    text.startsWith(
                            "/give "
                    )
            ) {

                String part =
                        text.substring(
                                6
                        ).toLowerCase();

                for (
                        int i = 1;
                        i < BLOCK_NAMES.length;
                        i++
                ) {

                    if (
                            BLOCK_NAMES[i]
                                    .startsWith(
                                            part
                                    )
                    ) {

                        result.add(
                                "/give "
                                        + BLOCK_NAMES[i]
                        );
                    }
                }
            }

            if (
                    text.startsWith(
                            "/setblock "
                    )
            ) {

                String part =
                        text.substring(
                                10
                        ).toLowerCase();

                for (
                        int i = 1;
                        i < BLOCK_NAMES.length;
                        i++
                ) {

                    if (
                            BLOCK_NAMES[i]
                                    .startsWith(
                                            part
                                    )
                    ) {

                        result.add(
                                "/setblock "
                                        + BLOCK_NAMES[i]
                        );
                    }
                }
            }

            return result;
        }

        // ========================================================
        // KEYBOARD
        // ========================================================

        @Override
        public void keyPressed(
                KeyEvent e
        ) {

            int key =
                    e.getKeyCode();

            if (
                    chatOpen
            ) {

                if (
                        key ==
                                KeyEvent.VK_ESCAPE
                ) {

                    chatOpen =
                            false;

                    chatInput =
                            "";

                    repaint();

                    return;
                }

                if (
                        key ==
                                KeyEvent.VK_ENTER
                ) {

                    String text =
                            chatInput.trim();

                    if (
                            !text.isEmpty()
                    ) {

                        handleChat(
                                text
                        );
                    }

                    chatInput =
                            "";

                    chatOpen =
                            false;

                    repaint();

                    return;
                }

                if (
                        key ==
                                KeyEvent.VK_BACK_SPACE
                ) {

                    if (
                            !chatInput.isEmpty()
                    ) {

                        chatInput =
                                chatInput.substring(
                                        0,
                                        chatInput.length()
                                                - 1
                                );
                    }

                    repaint();

                    return;
                }

                if (
                        key ==
                                KeyEvent.VK_TAB
                ) {

                    List<String>
                            suggestions =
                            getSuggestions(
                                    chatInput
                            );

                    if (
                            !suggestions.isEmpty()
                    ) {

                        chatInput =
                                suggestions.get(
                                        0
                                );
                    }

                    repaint();

                    return;
                }

                return;
            }

            if (
                    key ==
                            KeyEvent.VK_T
            ) {

                chatOpen =
                        true;

                repaint();

                return;
            }

            if (
                    key ==
                            KeyEvent.VK_SLASH
            ) {

                chatOpen =
                        true;

                chatInput =
                        "/";

                repaint();

                return;
            }

            if (
                    key ==
                            KeyEvent.VK_W
            ) {

                forward =
                        true;
            }

            if (
                    key ==
                            KeyEvent.VK_S
            ) {

                backward =
                        true;
            }

            if (
                    key ==
                            KeyEvent.VK_A
            ) {

                left =
                        true;
            }

            if (
                    key ==
                            KeyEvent.VK_D
            ) {

                right =
                        true;
            }

            if (
                    key >=
                            KeyEvent.VK_1
                            &&
                            key <=
                                    KeyEvent.VK_9
            ) {

                selectedSlot =
                        key
                                - KeyEvent.VK_1;
            }

            repaint();
        }

        @Override
        public void keyReleased(
                KeyEvent e
        ) {

            switch (
                    e.getKeyCode()
            ) {

                case KeyEvent.VK_W:
                    forward = false;
                    break;

                case KeyEvent.VK_S:
                    backward = false;
                    break;

                case KeyEvent.VK_A:
                    left = false;
                    break;

                case KeyEvent.VK_D:
                    right = false;
                    break;
            }
        }

        @Override
        public void keyTyped(
                KeyEvent e
        ) {

            if (
                    !chatOpen
            ) {

                return;
            }

            char c =
                    e.getKeyChar();

            if (
                    !Character.isISOControl(
                            c
                    )
            ) {

                if (
                        chatInput.length()
                                < 100
                ) {

                    chatInput +=
                            c;
                }

                repaint();
            }
        }

        // ========================================================
        // MOUSE
        // ========================================================

        int mouseWorldX = -1;
        int mouseWorldY = -1;

        @Override
        public void mouseMoved(
                MouseEvent e
        ) {
            updateMouseWorldPosition(e.getX(), e.getY());
            repaint();
        }

        @Override
        public void mouseDragged(
                MouseEvent e
        ) {
            updateMouseWorldPosition(e.getX(), e.getY());
            repaint();
        }

        private void updateMouseWorldPosition(
                int mouseX,
                int mouseY
        ) {
            double cameraX =
                    playerX * TILE_SIZE - getWidth() / 2.0;
            double cameraY =
                    playerY * TILE_SIZE - getHeight() / 2.0;

            mouseWorldX =
                    (int) Math.floor(
                            (mouseX + cameraX) / TILE_SIZE
                    );

            mouseWorldY =
                    (int) Math.floor(
                            (mouseY + cameraY) / TILE_SIZE
                    );
        }

        @Override
        public void mousePressed(
                MouseEvent e
        ) {
            if (chatOpen) {
                return;
            }

            updateMouseWorldPosition(e.getX(), e.getY());

            if (e.getButton() == MouseEvent.BUTTON1) {
                breakBlock();
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                placeBlock();
            }

            repaint();
        }

        private void breakBlock() {
            int x = mouseWorldX;
            int y = mouseWorldY;

            if (x < 0 || y < 0 || x >= WORLD_WIDTH || y >= WORLD_HEIGHT) {
                return;
            }

            int old = world[x][y];

            if (old != AIR) {
                world[x][y] = AIR;
                broadcastBlock(x, y, AIR);
            }
        }

        private void placeBlock() {
            int x = mouseWorldX;
            int y = mouseWorldY;

            if (x < 0 || y < 0 || x >= WORLD_WIDTH || y >= WORLD_HEIGHT) {
                return;
            }

            // Nicht direkt unter dem Spieler platzieren.
            if (Math.floor(playerX) == x && Math.floor(playerY) == y) {
                return;
            }

            int block = hotbar[selectedSlot];
            world[x][y] = block;
            broadcastBlock(x, y, block);
        }

        @Override
        public void mouseClicked(
                MouseEvent e
        ) {
        }

        @Override
        public void mouseReleased(
                MouseEvent e
        ) {
        }

        @Override
        public void mouseEntered(
                MouseEvent e
        ) {
        }

        @Override
        public void mouseExited(
                MouseEvent e
        ) {
            mouseWorldX = -1;
            mouseWorldY = -1;
            repaint();
        }
    }

    // ============================================================
    // REMOTE PLAYER
    // ============================================================

    static class RemotePlayer {

        String name;

        int character;

        double x;
        double y;

        RemotePlayer(
                String name,
                int character,
                double x,
                double y
        ) {

            this.name =
                    name;

            this.character =
                    character;

            this.x =
                    x;

            this.y =
                    y;
        }
    }

    // ============================================================
    // SERVER
    // ============================================================

    class NetworkServer {

        ServerSocket serverSocket;

        List<ClientConnection>
                connections =
                new ArrayList<>();

        Thread serverThread;

        NetworkServer()
                throws IOException {

            serverSocket =
                    new ServerSocket(
                            SERVER_PORT
                    );
        }

        void start() {

            serverThread =
                    new Thread(
                            () -> {

                                while (
                                        !serverSocket
                                                .isClosed()
                                ) {

                                    try {

                                        Socket socket =
                                                serverSocket
                                                        .accept();

                                        ClientConnection
                                                connection =
                                                new ClientConnection(
                                                        socket
                                                );

                                        synchronized (
                                                connections
                                        ) {

                                            connections.add(
                                                    connection
                                            );
                                        }

                                        connection.start();

                                    } catch (
                                            IOException e
                                    ) {

                                        break;
                                    }
                                }

                            },
                            "FoxCraft-Server"
                    );

            serverThread.start();
        }

        void broadcast(
                String message
        ) {

            synchronized (
                    connections
            ) {

                for (
                        ClientConnection connection :
                        connections
                ) {

                    connection.send(
                            message
                    );
                }
            }
        }

        void remove(
                ClientConnection connection
        ) {

            synchronized (
                    connections
            ) {

                connections.remove(
                        connection
                );
            }
        }

        void stop() {

            try {

                for (
                        ClientConnection connection :
                        connections
                ) {

                    connection.close();
                }

                connections.clear();

                if (
                        serverSocket != null
                ) {

                    serverSocket.close();
                }

            } catch (
                    IOException ignored
            ) {
            }
        }
    }

    // ============================================================
    // CLIENT CONNECTION
    // ============================================================

    class ClientConnection {

        Socket socket;

        BufferedReader reader;

        PrintWriter writer;

        Thread thread;

        String name =
                "Spieler";

        int character =
                CHARACTER_FOX;

        ClientConnection(
                Socket socket
        )
                throws IOException {

            this.socket =
                    socket;

            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    );

            writer =
                    new PrintWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream(),
                                    StandardCharsets.UTF_8
                            ),
                            true
                    );
        }

        void start() {

            thread =
                    new Thread(
                            this::run,
                            "FoxCraft-Client"
                    );

            thread.start();
        }

        void run() {

            try {

                String line;

                while (
                        (
                                line =
                                        reader.readLine()
                        )
                                != null
                ) {

                    process(
                            line
                    );
                }

            } catch (
                    IOException ignored
            ) {

            } finally {

                close();
            }
        }

        void process(
                String message
        ) {

            String[] parts =
                    message.split(
                            "\\|"
                    );

            if (
                    parts.length == 0
            ) {

                return;
            }

            if (
                    parts[0]
                            .equals(
                                    "HELLO"
                            )
            ) {

                if (
                        parts.length >= 3
                ) {

                    name =
                            cleanNetworkText(
                                    parts[1]
                            );

                    character =
                            parseIntSafe(
                                    parts[2],
                                    CHARACTER_FOX
                            );

                    send(
                            "WELCOME|"
                                    + playerName
                                    + "|"
                                    + selectedCharacter
                    );

                    server.broadcast(
                            "JOIN|"
                                    + name
                                    + "|"
                                    + character
                    );
                }

                return;
            }

            if (
                    parts[0]
                            .equals(
                                    "CHAT"
                            )
            ) {

                server.broadcast(
                        message
                );

                if (
                        game != null
                ) {

                    SwingUtilities.invokeLater(
                            () ->
                                    game.addChat(
                                            parts.length > 1
                                                    ? parts[1]
                                                    : ""
                                    )
                    );
                }

                return;
            }

            if (
                    parts[0]
                            .equals(
                                    "POS"
                            )
            ) {

                server.broadcast(
                        message
                );

                return;
            }

            if (
                    parts[0]
                            .equals(
                                    "BLOCK"
                            )
            ) {

                server.broadcast(
                        message
                );
            }
        }

        void send(
                String message
        ) {

            writer.println(
                    message
            );
        }

        void close() {

            try {

                socket.close();

            } catch (
                    IOException ignored
            ) {
            }

            if (
                    server != null
            ) {

                server.remove(
                        this
                );
            }
        }
    }

    // ============================================================
    // NETWORK CLIENT
    // ============================================================

    class NetworkClient {

        Socket socket;

        BufferedReader reader;

        PrintWriter writer;

        Thread thread;

        NetworkClient(
                String host,
                int port
        )
                throws IOException {

            socket =
                    new Socket(
                            host,
                            port
                    );

            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    );

            writer =
                    new PrintWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream(),
                                    StandardCharsets.UTF_8
                            ),
                            true
                    );
        }

        void start() {

            send(
                    "HELLO|"
                            + cleanNetworkText(
                            playerName
                    )
                            + "|"
                            + selectedCharacter
            );

            thread =
                    new Thread(
                            this::listen,
                            "FoxCraft-Network"
                    );

            thread.start();
        }

        void listen() {

            try {

                String line;

                while (
                        (
                                line =
                                        reader.readLine()
                        )
                                != null
                ) {

                    process(
                            line
                    );
                }

            } catch (
                    IOException e
            ) {

                SwingUtilities.invokeLater(
                        () -> {

                            if (
                                    game != null
                            ) {

                                game.addChat(
                                        "Verbindung zum Server verloren."
                                );
                            }
                        }
                );
            }
        }

        void process(
                String message
        ) {

            String[] parts =
                    message.split(
                            "\\|"
                    );

            if (
                    parts.length == 0
            ) {

                return;
            }

            if (
                    parts[0]
                            .equals(
                                    "CHAT"
                            )
            ) {

                if (
                        game != null
                ) {

                    SwingUtilities.invokeLater(
                            () ->
                                    game.addChat(
                                            parts.length > 1
                                                    ? parts[1]
                                                    : ""
                                    )
                    );
                }

                return;
            }

            if (
                    parts[0]
                            .equals(
                                    "JOIN"
                            )
            ) {

                if (
                        parts.length >= 3 &&
                        game != null
                ) {

                    String name =
                            cleanNetworkText(
                                    parts[1]
                            );

                    int character =
                            parseIntSafe(
                                    parts[2],
                                    CHARACTER_FOX
                            );

                    if (
                            !name.equals(
                                    playerName
                            )
                    ) {

                        game.remotePlayers.put(
                                name,
                                new RemotePlayer(
                                        name,
                                        character,
                                        32,
                                        32
                                )
                        );

                        game.addChat(
                                name
                                        + " ist der Welt beigetreten."
                        );
                    }
                }

                return;
            }

            if (
                    parts[0]
                            .equals(
                                    "POS"
                            )
            ) {

                if (
                        parts.length >= 5 &&
                        game != null
                ) {

                    String name =
                            cleanNetworkText(
                                    parts[1]
                            );

                    if (
                            name.equals(
                                    playerName
                            )
                    ) {

                        return;
                    }

                    int character =
                            parseIntSafe(
                                    parts[2],
                                    CHARACTER_FOX
                            );

                    double x =
                            parseDoubleSafe(
                                    parts[3],
                                    32
                            );

                    double y =
                            parseDoubleSafe(
                                    parts[4],
                                    32
                            );

                    RemotePlayer remote =
                            game.remotePlayers.get(
                                    name
                            );

                    if (
                            remote == null
                    ) {

                        remote =
                                new RemotePlayer(
                                        name,
                                        character,
                                        x,
                                        y
                                );

                        game.remotePlayers.put(
                                name,
                                remote
                        );

                    } else {

                        remote.x =
                                x;

                        remote.y =
                                y;

                        remote.character =
                                character;
                    }
                }

                return;
            }

            if (
                    parts[0]
                            .equals(
                                    "BLOCK"
                            )
            ) {

                if (
                        parts.length >= 4 &&
                        game != null
                ) {

                    int x =
                            parseIntSafe(
                                    parts[1],
                                    0
                            );

                    int y =
                            parseIntSafe(
                                    parts[2],
                                    0
                            );

                    int block =
                            parseIntSafe(
                                    parts[3],
                                    AIR
                            );

                    if (
                            x >= 0 &&
                            y >= 0 &&
                            x < WORLD_WIDTH &&
                            y < WORLD_HEIGHT
                    ) {

                        game.world[x][y] =
                                block;
                    }
                }
            }
        }

        void send(
                String message
        ) {

            if (
                    writer != null
            ) {

                writer.println(
                        message
                );
            }
        }

        void stop() {

            try {

                if (
                        socket != null
                ) {

                    socket.close();
                }

            } catch (
                    IOException ignored
            ) {
            }
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private JButton createButton(
            String text
    ) {

        JButton button =
                new JButton(
                        text
                );

        button.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        16
                )
        );

        button.setFocusPainted(
                false
        );

        button.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        button.setPreferredSize(
                new Dimension(
                        320,
                        45
                )
        );

        button.setMaximumSize(
                new Dimension(
                        320,
                        45
                )
        );

        return button;
    }

    private JLabel createTitle(
            String text
    ) {

        JLabel title =
                new JLabel(
                        text,
                        SwingConstants.CENTER
                );

        title.setForeground(
                Color.WHITE
        );

        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        35
                )
        );

        title.setBorder(
                BorderFactory.createEmptyBorder(
                        25,
                        10,
                        25,
                        10
                )
        );

        return title;
    }

    private void setPanel(
            JPanel panel
    ) {

        currentPanel =
                panel;

        setContentPane(
                panel
        );

        revalidate();
        repaint();

        SwingUtilities.invokeLater(
                () ->
                        panel.requestFocusInWindow()
        );
    }

    private void stopNetworking() {

        if (
                game != null &&
                game.gameTimer != null
        ) {

            game.gameTimer.stop();
        }

        if (
                server != null
        ) {

            server.stop();

            server =
                    null;
        }

        if (
                client != null
        ) {

            client.stop();

            client =
                    null;
        }
    }

    private static String cleanDomainText(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String value = text.trim();

        value = value.replaceFirst(
                "^https?://",
                ""
        );

        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }

        value = value.replaceAll("\\s+", "");

        return value;
    }

    static String cleanNetworkText(
            String text
    ) {

        if (
                text == null
        ) {

            return "";
        }

        return text
                .replace(
                        "|",
                        ""
                )
                .replace(
                        "\n",
                        ""
                )
                .replace(
                        "\r",
                        ""
                )
                .trim();
    }

    private static int parseIntSafe(
            String value,
            int fallback
    ) {

        try {

            return Integer.parseInt(
                    value
            );

        } catch (
                Exception e
        ) {

            return fallback;
        }
    }

    private static double parseDoubleSafe(
            String value,
            double fallback
    ) {

        try {

            return Double.parseDouble(
                    value
            );

        } catch (
                Exception e
        ) {

            return fallback;
        }
    }

    private static String getLocalIPv4() {

        try {

            java.util.Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

            while (interfaces != null && interfaces.hasMoreElements()) {

                NetworkInterface networkInterface =
                        interfaces.nextElement();

                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                java.util.Enumeration<InetAddress> addresses =
                        networkInterface.getInetAddresses();

                while (addresses.hasMoreElements()) {

                    InetAddress address =
                            addresses.nextElement();

                    String ip =
                            address.getHostAddress();

                    if (address instanceof Inet4Address
                            && ip != null
                            && !ip.startsWith("127.")) {

                        return ip;
                    }
                }
            }

        } catch (Exception ignored) {
        }

        return "127.0.0.1";
    }

    /**
     * Ermittelt die öffentliche IPv4-Adresse des Hosts.
     * Diese Adresse kann von Spielern außerhalb des LANs verwendet werden,
     * wenn der Router den FoxCraft-Port 25575/TCP an diesen PC weiterleitet.
     */
    private static String getPublicIPv4() {

        // STUN benötigt keinen Login und funktioniert auch dann,
        // wenn ein normaler HTTP-IP-Dienst nicht erreichbar ist.
        String stunIp = getPublicIPv4ViaStun();
        if (stunIp != null) {
            return stunIp;
        }

        HttpURLConnection connection = null;

        try {

            URL url = new URL("https://api.ipify.org");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                String ip = reader.readLine();
                if (ip != null && ip.trim().matches("\\d{1,3}(?:\\.\\d{1,3}){3}")) {
                    return ip.trim();
                }
            }

        } catch (Exception ignored) {
        } finally {
            if (connection != null) connection.disconnect();
        }

        return null;
    }

    /**
     * Erkennt die öffentliche IPv4-Adresse über einen STUN-Server.
     * Dadurch ist FoxCraft nicht von einem einzelnen HTTP-IP-Dienst abhängig.
     */
    private static String getPublicIPv4ViaStun() {

        String[] servers = {
                "stun.l.google.com",
                "stun.cloudflare.com"
        };

        for (String server : servers) {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(3000);

                InetAddress stunAddress = InetAddress.getByName(server);
                byte[] request = new byte[20];
                request[0] = 0x00;
                request[1] = 0x01; // Binding Request
                request[4] = 0x21;
                request[5] = 0x12;
                request[6] = (byte) 0xA4;
                request[7] = 0x42;

                new Random().nextBytes(java.util.Arrays.copyOfRange(request, 8, 20));
                // Transaktions-ID muss mit dem gesetzten Teil wirklich gefüllt werden.
                byte[] txid = new byte[12];
                new Random().nextBytes(txid);
                System.arraycopy(txid, 0, request, 8, 12);

                DatagramPacket packet = new DatagramPacket(
                        request, request.length, stunAddress, 19302);
                socket.send(packet);

                byte[] buffer = new byte[2048];
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket.receive(response);

                if (response.getLength() < 20) continue;
                int messageLength = ((buffer[2] & 0xff) << 8) | (buffer[3] & 0xff);
                int pos = 20;
                int endPos = Math.min(response.getLength(), 20 + messageLength);

                while (pos + 4 <= endPos) {
                    int type = ((buffer[pos] & 0xff) << 8) | (buffer[pos + 1] & 0xff);
                    int len = ((buffer[pos + 2] & 0xff) << 8) | (buffer[pos + 3] & 0xff);
                    int value = pos + 4;
                    if (value + len > endPos) break;

                    // XOR-MAPPED-ADDRESS (0x0020)
                    if (type == 0x0020 && len >= 8) {
                        int family = buffer[value + 1] & 0xff;
                        int xport = ((buffer[value + 2] & 0xff) << 8) | (buffer[value + 3] & 0xff);
                        if (family == 0x01 && len >= 8) {
                            byte[] ip = new byte[4];
                            int magic = 0x2112A442;
                            ip[0] = (byte) ((buffer[value + 4] & 0xff) ^ ((magic >>> 24) & 0xff));
                            ip[1] = (byte) ((buffer[value + 5] & 0xff) ^ ((magic >>> 16) & 0xff));
                            ip[2] = (byte) ((buffer[value + 6] & 0xff) ^ ((magic >>> 8) & 0xff));
                            ip[3] = (byte) ((buffer[value + 7] & 0xff) ^ (magic & 0xff));
                            String ipText = InetAddress.getByAddress(ip).getHostAddress();
                            if (!ipText.startsWith("10.") && !ipText.startsWith("192.168.")
                                    && !ipText.startsWith("172.16.") && !ipText.startsWith("172.17.")
                                    && !ipText.startsWith("172.18.") && !ipText.startsWith("172.19.")
                                    && !ipText.startsWith("172.20.") && !ipText.startsWith("172.21.")
                                    && !ipText.startsWith("172.22.") && !ipText.startsWith("172.23.")
                                    && !ipText.startsWith("172.24.") && !ipText.startsWith("172.25.")
                                    && !ipText.startsWith("172.26.") && !ipText.startsWith("172.27.")
                                    && !ipText.startsWith("172.28.") && !ipText.startsWith("172.29.")
                                    && !ipText.startsWith("172.30.") && !ipText.startsWith("172.31.")) {
                                return ipText;
                            }
                        }
                    }

                    pos += 4 + ((len + 3) & ~3);
                }

            } catch (Exception ignored) {
            } finally {
                if (socket != null) socket.close();
            }
        }

        return null;
    }

    // ============================================================
    // EXIT
    // ============================================================

    @Override
    public void dispose() {

        stopNetworking();

        super.dispose();
    }
}
