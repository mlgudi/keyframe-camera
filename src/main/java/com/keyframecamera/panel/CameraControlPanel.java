package com.keyframecamera.panel;

import com.formdev.flatlaf.icons.FlatFileViewDirectoryIcon;
import com.formdev.flatlaf.icons.FlatFileViewFileIcon;
import com.formdev.flatlaf.icons.FlatFileViewFloppyDriveIcon;
import com.keyframecamera.KeyframeCameraConfig;
import com.keyframecamera.KeyframeCameraPlugin;
import com.keyframecamera.Playback;
import com.keyframecamera.Sequence;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Paths;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

public class CameraControlPanel extends PluginPanel
{

	private final KeyframeCameraPlugin plugin;
	private final Playback playback;
	private final Client client;
	private Sequence sequence;
	private final KeyframeCameraConfig config;

	private final GridBagConstraints c = new GridBagConstraints();

	private final JPanel contentPanel = new JPanel();

	private final JPanel controlsPanel = new JPanel();
	private final JButton playButton = new JButton();
	private final JButton pauseButton = new JButton();
	private final JButton stopButton = new JButton();
	private final JButton newButton = new JButton();
	private final JButton saveButton = new JButton();
	private final JButton loadButton = new JButton();
	private final JButton addKeyframeButton = new JButton();
	private final JButton cameraModeButton = new JButton();

	private final KeyframePanel keyframesPanel;
	private static final ImageIcon PLAY_ICON;
	private static final ImageIcon PAUSE_ICON;
	private static final ImageIcon STOP_ICON;

	static
	{
		PLAY_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "play.png"));
		PAUSE_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "pause.png"));
		STOP_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "stop.png"));
	}

	public CameraControlPanel(KeyframeCameraPlugin plugin, Playback playback, Client client, KeyframeCameraConfig config)
	{
		super();
		this.plugin = plugin;
		this.playback = playback;
		this.client = client;
		this.config = config;
		this.sequence = plugin.getSequence();

		setOpaque(true);
		setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.insets = new Insets(0, 0, 3, 0);

		contentPanel.setOpaque(true);
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.setLayout(new GridBagLayout());
		add(contentPanel, BorderLayout.CENTER);

		keyframesPanel = new KeyframePanel(plugin, playback);

		addControls();
		contentPanel.add(keyframesPanel, c);
	}

	private void addControls()
	{
		controlsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		controlsPanel.setOpaque(false);
		controlsPanel.setLayout(new GridBagLayout());
		controlsPanel.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1));

		cameraModeButton.setText(client.getCameraMode() == 0 ? "Enable Free Cam" : "Disable Free Cam");
		cameraModeButton.addActionListener(e -> {
			client.setCameraMode(client.getCameraMode() == 0 ? 1 : 0);
			cameraModeButton.setText(client.getCameraMode() == 0 ? "Enable Free Cam" : "Disable Free Cam");
			cameraModeButton.revalidate();
			cameraModeButton.repaint();
		});

		controlsPanel.add(cameraModeButton, c);
		c.gridy++;

		JPanel playbackControls = new JPanel();
		playbackControls.setBackground(ColorScheme.DARK_GRAY_COLOR);
		playbackControls.setOpaque(false);
		playbackControls.setLayout(new FlowLayout());
		playbackControls.setBorder(new EmptyBorder(1, 1, 1, 1));

		addPlayButton(playbackControls);
		addPauseButton(playbackControls);
		addStopButton(playbackControls);
		addNewButton(playbackControls);
		addSaveButton(playbackControls);
		addLoadButton(playbackControls);

		controlsPanel.add(playbackControls, c);
		c.gridy++;

		addKeyframeButton.setText("Add Keyframe");
		addKeyframeButton.addActionListener(e -> {
			int newKeyframeIndex = plugin.add();
			if (newKeyframeIndex == -1)
			{
				return;
			}
			keyframesPanel.addKeyframe(newKeyframeIndex);
			updatePanel();
		});
		addKeyframeButton.setEnabled(!config.playing());

		controlsPanel.add(addKeyframeButton, c);
		c.gridy++;

		contentPanel.add(controlsPanel, c);
		c.gridy++;
	}

	private void addPlayButton(JPanel panel)
	{
		playButton.setIcon(PLAY_ICON);
		playButton.addActionListener(e -> {
			if (config.paused())
			{
				playback.togglePause();
			}
			else
			{
				playback.play();
			}
		});
		playButton.setEnabled((!config.playing() || config.paused()) && sequence.getKeyframes().size() >= 2 && loggedIn());
		panel.add(playButton);
	}

	private void addPauseButton(JPanel panel)
	{
		pauseButton.setIcon(PAUSE_ICON);
		pauseButton.addActionListener(e -> playback.togglePause());
		pauseButton.setEnabled(config.playing() && !config.paused() && loggedIn());
		panel.add(pauseButton);
	}

	private void addStopButton(JPanel panel)
	{
		stopButton.setIcon(STOP_ICON);
		stopButton.addActionListener(e -> playback.stop());
		stopButton.setEnabled(config.playing() && loggedIn());
		panel.add(stopButton);
	}

	private void addNewButton(JPanel panel)
	{
		newButton.setIcon(new FlatFileViewFileIcon());
		newButton.addActionListener(e -> {
			plugin.wipe();
			keyframesPanel.redrawKeyframes();
		});
		panel.add(newButton);
	}

	private void addSaveButton(JPanel panel)
	{
		saveButton.setIcon(new FlatFileViewFloppyDriveIcon());
		saveButton.addActionListener(e -> plugin.save());
		panel.add(saveButton);
	}

	private void addLoadButton(JPanel panel)
	{
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
						plugin.sendChatMessage("Failed to load sequence: file not found.");
						return;
					}
				}
				plugin.load(Paths.get(selectedFile.getPath()).getFileName().toString());
			}
		});

		panel.add(loadButton);
	}

	public void updatePanel()
	{
		this.sequence = plugin.getSequence();
		keyframesPanel.redrawKeyframes();
		updateControls();
		revalidate();
		repaint();
	}

	public void updateControls()
	{
		playButton.setEnabled((!config.playing() || config.paused()) && sequence.getKeyframes().size() >= 2 && loggedIn());
		pauseButton.setEnabled(config.playing() && !config.paused() && loggedIn());
		newButton.setEnabled(!config.playing());
		stopButton.setEnabled(config.playing() && loggedIn());
		saveButton.setEnabled(!config.playing() && !sequence.getKeyframes().isEmpty());
		loadButton.setEnabled(!config.playing());
		addKeyframeButton.setEnabled(!config.playing() && loggedIn());
		cameraModeButton.setText(client.getCameraMode() == 0 && loggedIn() ? "Enable Free Cam" : "Disable Free Cam");
	}

	private boolean loggedIn()
	{
		return client.getGameState() == GameState.LOGGED_IN;
	}
}
