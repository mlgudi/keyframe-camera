package com.keyframecamera.panel;

import com.keyframecamera.CameraSequence;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

@Slf4j
public class KeyframePanel extends JPanel {

    CameraSequence sequence;
    GridBagConstraints headerConstraints = new GridBagConstraints();
    GridBagConstraints c = new GridBagConstraints();
    JPanel keyframes = new JPanel(new GridBagLayout());
    HashMap<Integer, KeyframeDisplay> displayedKeyframes = new HashMap<>();

    public KeyframePanel(CameraSequence sequence)
    {
        super();

        this.sequence = sequence;

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

        headerConstraints.fill = GridBagConstraints.HORIZONTAL;
        headerConstraints.gridx = 0;
        headerConstraints.gridy = 0;
        headerConstraints.weightx = 0.2;
        headerConstraints.insets = new Insets(0, 0, 3, 0);

        JLabel indexHeader = new JLabel("#");
        indexHeader.setForeground(Color.WHITE);
        indexHeader.setFont(FontManager.getRunescapeSmallFont());
        headerPanel.add(indexHeader, headerConstraints);

        headerConstraints.gridx++;
        headerConstraints.weightx = 0.5;

        JLabel durationHeader = new JLabel("Duration (ms)");
        durationHeader.setForeground(Color.WHITE);
        durationHeader.setFont(FontManager.getRunescapeSmallFont());
        headerPanel.add(durationHeader, headerConstraints);

        headerConstraints.gridx++;
        headerConstraints.weightx = 0.3;

        JLabel easingHeader = new JLabel("Easing");
        easingHeader.setForeground(Color.WHITE);
        easingHeader.setFont(FontManager.getRunescapeSmallFont());
        headerPanel.add(easingHeader, headerConstraints);

        add(headerPanel, c);

        c.gridy++;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);

        keyframes.setBackground(ColorScheme.DARK_GRAY_COLOR);
        keyframes.setOpaque(false);

        for (int i = 0; i < sequence.getKeyframes().size(); i++)
        {
            KeyframeDisplay keyframeDisplay = new KeyframeDisplay(this, sequence, i);
            keyframes.add(keyframeDisplay, c);
            displayedKeyframes.put(i, keyframeDisplay);
            c.gridy++;
        }

        add(keyframes, c);
    }

    public void addKeyframe(int index)
    {
        addKeyframe(index, false);
    }

    public void addKeyframe(int index, boolean showControls)
    {
        c.gridy = index + 2;
        KeyframeDisplay keyframeDisplay = new KeyframeDisplay(this, sequence, index);
        keyframeDisplay.setShowControls(showControls);
        displayedKeyframes.put(index, keyframeDisplay);
        keyframes.add(keyframeDisplay, c);
    }

    public void updateKeyframe(int index)
    {
        displayedKeyframes.get(index).updateKeyframe();
    }

    public void updateKeyframesFrom(int index)
    {
        for (int i = index; i < sequence.getKeyframes().size(); i++)
        {
            updateKeyframe(i);
        }
    }

    public void redrawKeyframes()
    {
        this.sequence = sequence.getPlugin().getSequence();
        keyframes.removeAll();
        displayedKeyframes.clear();
        c.gridy = 1;

        for (int i = 0; i < sequence.getKeyframes().size(); i++)
        {
            addKeyframe(i);
        }
        revalidate();
        repaint();
    }

    void moveKeyframe(boolean up, int index)
    {
        if (!sequence.moveKeyframe(up, index)) return;

        int neighbourIndex = up ? index - 1 : index + 1;
        KeyframeDisplay keyframe = displayedKeyframes.get(index);
        KeyframeDisplay neighbour = displayedKeyframes.get(neighbourIndex);

        keyframes.remove(keyframe);
        keyframes.remove(neighbour);

        displayedKeyframes.remove(neighbourIndex);
        displayedKeyframes.remove(index);

        addKeyframe(neighbourIndex, keyframe.isShowControls());
        addKeyframe(index, neighbour.isShowControls());

        updateKeyframe(neighbourIndex);
        updateKeyframe(index);
    }

    void deleteKeyframe(int index)
    {
        sequence.deleteKeyframe(index);
        keyframes.remove(displayedKeyframes.get(index));

        for (int i = index + 1; i < displayedKeyframes.size(); i++)
        {
            displayedKeyframes.get(i).setIndex(i - 1);
            displayedKeyframes.put(i - 1, displayedKeyframes.get(i));
        }

        displayedKeyframes.remove(displayedKeyframes.size() - 1);

        if (index != displayedKeyframes.size() - 1) {
            updateKeyframesFrom(index);
        }

        keyframes.revalidate();
        keyframes.repaint();
    }

}
