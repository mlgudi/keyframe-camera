package com.keyframecamera.panel;

import com.formdev.flatlaf.icons.*;
import com.keyframecamera.KeyframeCameraPlugin;
import com.keyframecamera.CameraSequence;
import com.keyframecamera.EaseType;
import com.keyframecamera.Keyframe;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class KeyframePanel extends JPanel
{
    private final Keyframe keyframe;
    private final CameraSequence sequence;
    private final int index;
    private final CameraControlPanel parent;
    private final ImageIcon OVERWRITE_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "overwrite.png"));

    public KeyframePanel(CameraControlPanel parent, CameraSequence sequence, int index, int totalKeyframes) {
        this.parent = parent;
        this.sequence = sequence;
        this.index = index;
        this.keyframe = sequence.getKeyframe(index);

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sequence.setCameraToKeyframe(keyframe);
            }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 3, 0);

        addHeader(c, totalKeyframes);
        c.gridy++;
        addEasing(c);
        c.gridy++;
        addDuration(c);
    }

    private void addHeader(GridBagConstraints c, int totalKeyframes) {
        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        headerWrapper.setBorder(new EmptyBorder(0, 5, 0, 2));
        add(headerWrapper, c);

        JLabel indexLabel = createLabel("Keyframe " + (index + 1));
        headerWrapper.add(indexLabel, BorderLayout.CENTER);

        JPanel actionPanel = createActionPanel(totalKeyframes);
        headerWrapper.add(actionPanel, BorderLayout.EAST);
    }

    private JPanel createActionPanel(int totalKeyframes) {
        JPanel actionPanel = new JPanel(new FlowLayout());
        actionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        actionPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

        JLabel overwrite = createActionLabel(OVERWRITE_ICON, "Overwrite with current camera position", new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sequence.overwriteKeyframe(index);
            }
        });
        JLabel moveUp = createActionLabel(new FlatAscendingSortIcon(), "Move keyframe up", new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sequence.moveKeyframe(true, index);
            }
        });
        JLabel moveDown = createActionLabel(new FlatDescendingSortIcon(), "Move keyframe down", new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sequence.moveKeyframe(false, index);
            }
        });
        JLabel duplicate = createActionLabel(new FlatInternalFrameRestoreIcon(), "Duplicate keyframe", new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sequence.duplicateKeyframe(index);
            }
        });
        JLabel delete = createActionLabel(new FlatTabbedPaneCloseIcon(), "Delete keyframe", new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sequence.deleteKeyframe(index);
            }
        });

        if (index == 0) {
            moveUp.setEnabled(false);
        }
        if (index == totalKeyframes - 1) {
            moveDown.setEnabled(false);
        }

        actionPanel.add(overwrite);
        actionPanel.add(duplicate);
        actionPanel.add(moveUp);
        actionPanel.add(moveDown);
        actionPanel.add(delete);

        return actionPanel;
    }

    private void addEasing(GridBagConstraints c) {
        JPanel easingPanel = new JPanel(new GridBagLayout());
        easingPanel.setBackground(getBackground());
        easingPanel.setBorder(new EmptyBorder(4, 8, 0, 8));
        add(easingPanel, c);

        GridBagConstraints easingConstraints = new GridBagConstraints();
        easingConstraints.fill = GridBagConstraints.HORIZONTAL;
        easingConstraints.weightx = 0.6;
        easingConstraints.insets = new Insets(0, 0, 5, 0);

        JLabel easingLabel = createLabel("Ease Type");
        easingPanel.add(easingLabel, easingConstraints);

        easingConstraints.gridx = 1;
        easingConstraints.weightx = 0.4;
        easingConstraints.insets = new Insets(0, 5, 5, 0);

        JComboBox<EaseType> easeTypeComboBox = new JComboBox<>(EaseType.values());
        easeTypeComboBox.setSelectedItem(keyframe.getEase());
        easeTypeComboBox.addActionListener(e -> keyframe.setEase((EaseType) easeTypeComboBox.getSelectedItem()));
        easingPanel.add(easeTypeComboBox, easingConstraints);
    }

    private void addDuration(GridBagConstraints c) {
        JPanel durationPanel = new JPanel(new GridBagLayout());
        durationPanel.setBackground(getBackground());
        durationPanel.setBorder(new EmptyBorder(0, 8, 0, 8));
        add(durationPanel, c);

        GridBagConstraints durationConstraints = new GridBagConstraints();
        durationConstraints.gridwidth = 2;
        durationConstraints.weightx = 1;
        durationConstraints.anchor = GridBagConstraints.WEST;
        durationConstraints.insets = new Insets(0, 0, 5, 0);

        JLabel durationLabel = createLabel("Duration (ms)");
        durationLabel.setToolTipText("The time it takes for this keyframe to transition to the next");
        durationLabel.setBorder(new EmptyBorder(0, 0, 0, 0));
        durationPanel.add(durationLabel, durationConstraints);
        durationConstraints.gridx++;

        SpinnerNumberModel durationModel = new SpinnerNumberModel(keyframe.getDuration(), 1, Long.MAX_VALUE, 1);
        JSpinner durationSpinner = new JSpinner(durationModel);
        durationSpinner.addChangeListener(e -> {
            keyframe.setDuration(((Number) durationSpinner.getValue()).longValue());
            sequence.calcKeyframeStartTimes();
        });
        durationPanel.add(durationSpinner, durationConstraints);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        return label;
    }

    private JLabel createActionLabel(FlatAbstractIcon icon, String tooltip, MouseAdapter mouseAdapter) {
        JLabel label = new JLabel(icon);
        label.setToolTipText(tooltip);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseAdapter.mousePressed(e);
                parent.updateUI();
            }
        });
        return label;
    }

    private JLabel createActionLabel(ImageIcon icon, String tooltip, MouseAdapter mouseAdapter) {
        JLabel label = new JLabel(icon);
        label.setToolTipText(tooltip);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseAdapter.mousePressed(e);
                parent.updateUI();
            }
        });
        return label;
    }
}
