package com.keyframecamera.panel;

import com.formdev.flatlaf.icons.FlatFileViewDirectoryIcon;
import com.formdev.flatlaf.icons.FlatFileViewFloppyDriveIcon;
import com.keyframecamera.KeyframeCameraConfig;
import com.keyframecamera.KeyframeCameraPlugin;
import com.keyframecamera.CameraSequence;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;

public class CameraControlPanel extends PluginPanel {

    private final KeyframeCameraPlugin plugin;
    private final Client client;
    private CameraSequence sequence;
    private final KeyframeCameraConfig config;
    private final boolean initialised;

    private final GridBagConstraints c;
    private final JPanel keyframesPanel = new JPanel();

    private static final ImageIcon PLAY_ICON;
    private static final ImageIcon PAUSE_ICON;
    private static final ImageIcon STOP_ICON;

    static {
        PLAY_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "play.png"));
        PAUSE_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "pause.png"));
        STOP_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "stop.png"));
    }

    public CameraControlPanel(KeyframeCameraPlugin plugin, Client client, KeyframeCameraConfig config) {
        super();
        this.plugin = plugin;
        this.client = client;
        this.config = config;
        this.sequence = plugin.getSequence();

        setOpaque(true);
        setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.insets = new Insets(0, 0, 3, 0);

        initialised = true;
        updateUI();
    }

    public void updateUI() {
        if (!initialised) return;
        this.sequence = plugin.getSequence();

        removeAll();
        c.gridy = 0;

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(true);
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        contentPanel.setLayout(new GridBagLayout());

        if (client.getGameState().getState() < GameState.LOADING.getState()) {
            addLoginLabel(contentPanel);
        } else {
            addControlsPanel(contentPanel);
            addKeyframes(contentPanel);
        }

        add(contentPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void addLoginLabel(JPanel panel) {
        JLabel loginLabel = new JLabel("Please log in");
        loginLabel.setForeground(Color.WHITE);
        loginLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        loginLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(loginLabel, c);
    }

    private void addControlsPanel(JPanel panel) {
        JPanel controlsPanel = new JPanel();
        controlsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        controlsPanel.setOpaque(false);
        controlsPanel.setLayout(new GridBagLayout());
        controlsPanel.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1));

        addCameraModeButton(controlsPanel);
        c.gridy++;
        addPlaybackControls(controlsPanel);
        addKeyframeButton(controlsPanel);

        panel.add(controlsPanel, c);
        c.gridy++;
    }

    private void addCameraModeButton(JPanel panel) {
        JButton cameraModeButton = new JButton(client.getCameraMode() == 0 ? "Enable Free Cam" : "Disable Free Cam");
        cameraModeButton.addActionListener(e -> {
            client.setCameraMode(client.getCameraMode() == 0 ? 1 : 0);
            updateUI();
        });
        panel.add(cameraModeButton, c);
    }

    private void addPlaybackControls(JPanel panel) {
        JPanel playbackControls = new JPanel();
        playbackControls.setBackground(ColorScheme.DARK_GRAY_COLOR);
        playbackControls.setOpaque(false);
        playbackControls.setLayout(new FlowLayout());
        playbackControls.setBorder(new EmptyBorder(1, 1, 1, 1));

        addPlayButton(playbackControls);
        addPauseButton(playbackControls);
        addStopButton(playbackControls);
        addSaveButton(playbackControls);
        addLoadButton(playbackControls);

        panel.add(playbackControls, c);
        c.gridy++;
    }

    private void addPlayButton(JPanel panel) {
        JButton playButton = new JButton();
        playButton.setIcon(PLAY_ICON);
        playButton.addActionListener(e -> {
            if (config.paused())
            {
                sequence.togglePause();
            } else {
                sequence.play();
            }
            updateUI();
        });
        playButton.setEnabled((!config.playing() || config.paused()) && sequence.getKeyframes().size() >= 2 && loggedIn());
        panel.add(playButton);
    }

    private void addPauseButton(JPanel panel) {
        JButton pauseButton = new JButton();
        pauseButton.setIcon(PAUSE_ICON);
        pauseButton.addActionListener(e -> {
            sequence.togglePause();
            updateUI();
        });
        pauseButton.setEnabled(config.playing() && !config.paused() && loggedIn());
        panel.add(pauseButton);
    }

    private void addStopButton(JPanel panel) {
        JButton stopButton = new JButton();
        stopButton.setIcon(STOP_ICON);
        stopButton.addActionListener(e -> {
            sequence.stop();
            updateUI();
        });
        stopButton.setEnabled(config.playing() && loggedIn());
        panel.add(stopButton);
    }

    private void addSaveButton(JPanel panel) {
        JButton saveButton = new JButton();
        saveButton.setIcon(new FlatFileViewFloppyDriveIcon());
        saveButton.addActionListener(e -> {
            sequence.save();
            updateUI();
        });
        panel.add(saveButton);
    }

    private void addLoadButton(JPanel panel) {
        JButton loadButton = new JButton();
        loadButton.setIcon(new FlatFileViewDirectoryIcon());
        loadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(KeyframeCameraPlugin.SEQUENCE_DIR.toFile());
            fileChooser.setDialogTitle("Choose a keyframe sequence to load");

            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION)
            {
                File selectedFile = fileChooser.getSelectedFile();
                if (!selectedFile.exists())
                {
                    selectedFile = new File(selectedFile.getPath() + ".txt");
                    if (!selectedFile.exists())
                    {
                        return;
                    }
                }
                plugin.loadSequence(Paths.get(selectedFile.getPath()).getFileName().toString());
            }
        });
        panel.add(loadButton);
    }

    private void addKeyframeButton(JPanel panel) {
        JButton addKeyframeButton = new JButton("Add Keyframe");
        addKeyframeButton.addActionListener(e -> {
            sequence.addKeyframe();
            updateUI();
        });
        addKeyframeButton.setEnabled(!config.playing());
        panel.add(addKeyframeButton, c);
    }

    private void addKeyframes(JPanel panel) {
        keyframesPanel.removeAll();

        keyframesPanel.setLayout(new GridBagLayout());
        keyframesPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        keyframesPanel.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.insets = new Insets(0, 0, 3, 0);

        for (int i = 0; i < sequence.getKeyframes().size(); i++) {
            KeyframePanel keyframePanel = new KeyframePanel(this, sequence, i, sequence.getKeyframes().size());
            keyframesPanel.add(keyframePanel, c);
            c.gridy++;
        }

        panel.add(keyframesPanel, this.c);
        this.c.gridy++;
    }

    private boolean loggedIn() {
        return client.getGameState().getState() == GameState.LOGGED_IN.getState();
    }
}
