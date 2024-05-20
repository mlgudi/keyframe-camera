package com.keyframecamera.panel;

import com.formdev.flatlaf.icons.FlatFileViewDirectoryIcon;
import com.formdev.flatlaf.icons.FlatFileViewFloppyDriveIcon;
import com.keyframecamera.*;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private static final ImageIcon OVERWRITE_ICON;
    private static final ImageIcon DUPLICATE_ICON;
    private static final ImageIcon UP_ICON;
    private static final ImageIcon DOWN_ICON;
    private static final ImageIcon DELETE_ICON;

    static {
        PLAY_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "play.png"));
        PAUSE_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "pause.png"));
        STOP_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "stop.png"));
        OVERWRITE_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "overwrite.png"));
        DUPLICATE_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "duplicate.png"));
        UP_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "up.png"));
        DOWN_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "down.png"));
        DELETE_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "delete.png"));
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

        // Create a separate panel for the headers
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        headerPanel.setOpaque(false);

        GridBagConstraints headerConstraints = new GridBagConstraints();
        headerConstraints.fill = GridBagConstraints.HORIZONTAL;
        headerConstraints.gridx = 0;
        headerConstraints.gridy = 0;
        headerConstraints.weightx = 0.2;
        headerConstraints.weighty = 0;
        headerConstraints.insets = new Insets(0, 0, 3, 0);

        JLabel indexHeader = new JLabel("#");
        indexHeader.setForeground(Color.WHITE);
        indexHeader.setFont(FontManager.getRunescapeSmallFont());
        headerPanel.add(indexHeader, headerConstraints);

        headerConstraints.gridx++;
        headerConstraints.weightx = 0.5;

        JLabel durationHeader = new JLabel("Duration");
        durationHeader.setForeground(Color.WHITE);
        durationHeader.setFont(FontManager.getRunescapeSmallFont());
        headerPanel.add(durationHeader, headerConstraints);

        headerConstraints.gridx++;
        headerConstraints.weightx = 0.3;

        JLabel easingHeader = new JLabel("Easing");
        easingHeader.setForeground(Color.WHITE);
        easingHeader.setFont(FontManager.getRunescapeSmallFont());
        headerPanel.add(easingHeader, headerConstraints);

        // Create a separate panel for the keyframes
        JPanel keyframePanel = new JPanel(new GridBagLayout());
        keyframePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        keyframePanel.setOpaque(false);

        GridBagConstraints kc = new GridBagConstraints();
        kc.fill = GridBagConstraints.HORIZONTAL;
        kc.gridx = 0;
        kc.gridy = 0;
        kc.weightx = 1;
        kc.insets = new Insets(0, 0, 3, 0);

        for (int i = 0; i < sequence.getKeyframes().size(); i++) {
            Keyframe keyframe = sequence.getKeyframe(i);

            JPanel headerWrapper = new JPanel(new BorderLayout());
            headerWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            headerWrapper.setBorder(new EmptyBorder(0, 5, 0, 2));
            keyframesPanel.add(headerWrapper, kc);

            JPanel actionPanel = new JPanel(new FlowLayout());
            actionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            actionPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

            int finalI = i;
            JLabel overwrite = createActionLabel(OVERWRITE_ICON, "Overwrite with current camera position", new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    sequence.overwriteKeyframe(finalI);
                }
            });
            JLabel moveUp = createActionLabel(UP_ICON, "Move keyframe up", new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    sequence.moveKeyframe(true, finalI);
                }
            });
            JLabel moveDown = createActionLabel(DOWN_ICON, "Move keyframe down", new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    sequence.moveKeyframe(false, finalI);
                }
            });
            JLabel duplicate = createActionLabel(DUPLICATE_ICON, "Duplicate keyframe", new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    sequence.duplicateKeyframe(finalI);
                }
            });
            JLabel delete = createActionLabel(DELETE_ICON, "Delete keyframe", new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    sequence.deleteKeyframe(finalI);
                }
            });

            if (finalI == 0) {
                moveUp.setEnabled(false);
            }
            if (finalI == sequence.getKeyframes().size() - 1) {
                moveDown.setEnabled(false);
            }

            actionPanel.add(overwrite);
            actionPanel.add(duplicate);
            actionPanel.add(moveUp);
            actionPanel.add(moveDown);
            actionPanel.add(delete);

            //headerWrapper.add(actionPanel, BorderLayout.EAST);
            kc.gridy++;

            GridBagConstraints dc = new GridBagConstraints();
            dc.gridx = 0;
            dc.gridy = 0;
            dc.weightx = 0.2;
            dc.insets = new Insets(0, 0, 3, 0);

            JPanel detailsPanel = new JPanel(new GridBagLayout());
            detailsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
            detailsPanel.setOpaque(false);

            JLabel indexLabel = new JLabel(String.valueOf(i + 1));
            indexLabel.setForeground(Color.WHITE);
            indexLabel.setFont(FontManager.getRunescapeSmallFont());
            detailsPanel.add(indexLabel, dc);
            dc.gridx++;
            dc.weightx = 0.5;

            JSpinner durationSpinner = new JSpinner();
            durationSpinner.setFont(FontManager.getRunescapeSmallFont());
            Component editor = durationSpinner.getEditor();
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setColumns(8);

            durationSpinner.addChangeListener(e -> {
                keyframe.setDuration(((Number) durationSpinner.getValue()).longValue());
            });
            detailsPanel.add(durationSpinner, dc);

            dc.gridx++;
            dc.weightx = 0.3;

            JComboBox<EaseType> easeTypeComboBox = new JComboBox<>(EaseType.values());
            easeTypeComboBox.setFont(FontManager.getRunescapeSmallFont());
            easeTypeComboBox.setSelectedItem(keyframe.getEase());
            easeTypeComboBox.addActionListener(e -> keyframe.setEase((EaseType) easeTypeComboBox.getSelectedItem()));
            detailsPanel.add(easeTypeComboBox, dc);

            keyframePanel.add(detailsPanel, kc);
        }

        GridBagConstraints panelConstraints = new GridBagConstraints();
        panelConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelConstraints.gridx = 0;
        panelConstraints.gridy = 0;
        panelConstraints.weightx = 1;
        panelConstraints.insets = new Insets(0, 0, 3, 0);

        keyframesPanel.add(headerPanel, panelConstraints);
        panelConstraints.gridy++;
        keyframesPanel.add(keyframePanel, panelConstraints);

        panel.add(keyframesPanel, this.c);
        this.c.gridy++;
    }

    private JLabel createActionLabel(ImageIcon icon, String tooltip, MouseAdapter mouseAdapter) {
        JLabel label = new JLabel(icon);
        label.setToolTipText(tooltip);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseAdapter.mousePressed(e);
                updateUI();
            }
        });
        return label;
    }

    private boolean loggedIn() {
        return client.getGameState().getState() == GameState.LOGGED_IN.getState();
    }
}
