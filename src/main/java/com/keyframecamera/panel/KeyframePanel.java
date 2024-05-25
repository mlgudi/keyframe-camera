package com.keyframecamera.panel;

import com.keyframecamera.KeyframeCameraPlugin;
import com.keyframecamera.Playback;
import com.keyframecamera.Sequence;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;

@Slf4j
public class KeyframePanel extends JPanel {

    KeyframeCameraPlugin plugin;
    Playback playback;
    Sequence sequence;
    GridBagConstraints c = new GridBagConstraints();
    JPanel keyframes = new JPanel(new GridBagLayout());

    HashSet<String> showingControls = new HashSet<>();

    public KeyframePanel(KeyframeCameraPlugin plugin, Playback playback)
    {
        super();

        this.plugin = plugin;
        this.playback = playback;
        this.sequence = plugin.getSequence();

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setOpaque(false);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 3, 0);

        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        headerPanel.setOpaque(false);

        c.weightx = 0.2;

        JLabel indexHeader = new JLabel("#");
        indexHeader.setForeground(Color.WHITE);
        indexHeader.setFont(FontManager.getRunescapeSmallFont());
        headerPanel.add(indexHeader, c);

        c.gridx++;
        c.weightx = 0.5;

        JLabel durationHeader = new JLabel("Duration (ms)");
        durationHeader.setForeground(Color.WHITE);
        durationHeader.setFont(FontManager.getRunescapeSmallFont());
        headerPanel.add(durationHeader, c);

        c.gridx++;
        c.weightx = 0.3;

        JLabel easingHeader = new JLabel("Easing");
        easingHeader.setForeground(Color.WHITE);
        easingHeader.setFont(FontManager.getRunescapeSmallFont());
        headerPanel.add(easingHeader, c);

        add(headerPanel, c);

        c.gridy++;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);

        keyframes.setBackground(ColorScheme.DARK_GRAY_COLOR);
        keyframes.setOpaque(false);

        for (int i = 0; i < sequence.size(); i++)
        {
            KeyframeDisplay keyframeDisplay = new KeyframeDisplay(this, plugin, sequence.get(i));
            keyframes.add(keyframeDisplay, c);
            c.gridy++;
        }

        add(keyframes, c);
    }

    public void toggleShowControls(String id)
    {
        if (showingControls.contains(id))
        {
            showingControls.remove(id);
        }
        else
        {
            showingControls.add(id);
        }
    }

    public void addKeyframe(int index)
    {
        c.gridy = index + 2;
        KeyframeDisplay keyframeDisplay = new KeyframeDisplay(this, plugin, sequence.get(index));
        keyframes.add(keyframeDisplay, c);
    }

    public void redrawKeyframes()
    {
        this.sequence = plugin.getSequence();
        keyframes.removeAll();
        c.gridy = 1;

        for (int i = 0; i < sequence.size(); i++)
        {
            addKeyframe(i);
        }
        revalidate();
        repaint();
    }

}
