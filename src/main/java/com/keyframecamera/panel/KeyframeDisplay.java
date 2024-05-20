package com.keyframecamera.panel;

import com.keyframecamera.CameraSequence;
import com.keyframecamera.EaseType;
import com.keyframecamera.Keyframe;
import com.keyframecamera.KeyframeCameraPlugin;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class KeyframeDisplay extends JPanel
{

    private static final ImageIcon VIEW_ICON;
    private static final ImageIcon OVERWRITE_ICON;
    private static final ImageIcon DUPLICATE_ICON;
    private static final ImageIcon UP_ICON;
    private static final ImageIcon DOWN_ICON;
    private static final ImageIcon DELETE_ICON;

    static {
        VIEW_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "view.png"));
        OVERWRITE_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "overwrite.png"));
        DUPLICATE_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "duplicate.png"));
        UP_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "up.png"));
        DOWN_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "down.png"));
        DELETE_ICON = new ImageIcon(ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "delete.png"));

    }

    GridBagConstraints c = new GridBagConstraints();

    KeyframePanel parent;
    CameraSequence sequence;
    Keyframe keyframe;

    @Getter
    @Setter
    int index;

    @Getter
    @Setter
    boolean showControls = false;

    public KeyframeDisplay(KeyframePanel parent, CameraSequence sequence, int index)
    {
        super();
        this.parent = parent;
        this.sequence = sequence;
        this.index = index;
        keyframe = sequence.getKeyframe(index);

        setLayout(new GridBagLayout());
        setOpaque(true);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, bgColor().brighter()));

        drawPanel();
    }

    private void drawPanel()
    {
        setBackground(bgColor());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createLineBorder(bgColor(), 3));
        panel.setOpaque(false);
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                toggleShowDetails();
                updateKeyframe();
            }
        });

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        panel.add(details, c);

        c.weightx = 0.2;

        JLabel indexLabel = new JLabel(String.valueOf(index + 1));
        indexLabel.setOpaque(true);
        indexLabel.setBackground(bgColor());
        indexLabel.setVerticalAlignment(SwingConstants.CENTER);
        indexLabel.setVerticalTextPosition(SwingConstants.CENTER);
        indexLabel.setPreferredSize(new Dimension(24, 24));
        indexLabel.setForeground(Color.WHITE);
        indexLabel.setFont(FontManager.getRunescapeSmallFont());

        details.add(indexLabel, c);

        c.gridx++;
        c.weightx = 0.4;
        c.fill = GridBagConstraints.NONE;

        JSpinner durationSpinner = new JSpinner();
        durationSpinner.setFont(FontManager.getRunescapeSmallFont());
        Component spinnerEditor = durationSpinner.getEditor();
        JFormattedTextField tf = ((JSpinner.DefaultEditor) spinnerEditor).getTextField();
        tf.setColumns(8);
        durationSpinner.setValue(keyframe.getDuration());

        durationSpinner.addChangeListener(e -> keyframe.setDuration(((Number) durationSpinner.getValue()).longValue()));
        details.add(durationSpinner, c);

        c.gridx++;
        c.weightx = 0.4;

        JComboBox<EaseType> easeTypeComboBox = new JComboBox<>(EaseType.values());
        easeTypeComboBox.setPrototypeDisplayValue(EaseType.LINEAR);
        easeTypeComboBox.setFont(FontManager.getRunescapeSmallFont());
        easeTypeComboBox.setSelectedItem(keyframe.getEase());
        easeTypeComboBox.addActionListener(e -> keyframe.setEase((EaseType) easeTypeComboBox.getSelectedItem()));
        details.add(easeTypeComboBox, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;

        if (showControls)
        {
            addControls(panel);
            add(panel, c);
        } else {
            add(panel, c);
        }
    }

    private Color bgColor()
    {
        Color bgColor = showControls ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARK_GRAY_COLOR;
        return bgColor;
    }

    private void addControls(JPanel panel)
    {
        JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);
        controls.setBorder(new EmptyBorder(2, 0, 2, 0));
        panel.add(controls, c);

        JPanel actionPanel = createActionPanel();

        controls.add(actionPanel, BorderLayout.CENTER);
        add(panel, c);
    }

    private JLabel createActionLabel(ImageIcon icon, String tooltip, MouseAdapter mouseAdapter) {
        JLabel label = new JLabel(icon);
        setOpaque(true);
        setBackground(bgColor());
        label.setPreferredSize(new Dimension(24, 24));
        label.setToolTipText(tooltip);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!sequence.isPlaying()) mouseAdapter.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!sequence.isPlaying()) {
                    label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    label.setBackground(bgColor().darker());
                    revalidate();
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                label.setBackground(bgColor());
                revalidate();
                repaint();
            }
        });
        return label;
    }

    private JPanel createActionPanel()
    {
        JPanel actionPanel = new JPanel(new FlowLayout());
        actionPanel.setOpaque(false);
        actionPanel.setBorder(new EmptyBorder(5, 2, 5, 2));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints cc = new GridBagConstraints();
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.gridx = 0;
        cc.gridy = 0;
        cc.weightx = 0.16666;
        actionPanel.add(content);

        JLabel view = createActionLabel(VIEW_ICON, "Move the camera to this keyframe", new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sequence.setCameraToKeyframe(keyframe);
            }
        });

        JLabel overwrite = createActionLabel(OVERWRITE_ICON, "Overwrite with current camera state", new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sequence.overwriteKeyframe(index);
                updateKeyframe();
            }
        });
        JLabel moveUp = createActionLabel(UP_ICON, "Move keyframe up", new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                parent.moveKeyframe(true, index);
            }
        });
        JLabel moveDown = createActionLabel(DOWN_ICON, "Move keyframe down", new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                parent.moveKeyframe(false, index);
            }
        });
        JLabel duplicate = createActionLabel(DUPLICATE_ICON, "Duplicate keyframe", new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sequence.duplicateKeyframe(index);
                parent.addKeyframe(sequence.getKeyframes().size() - 1);
                parent.updateKeyframe(sequence.getKeyframes().size() - 1);
            }
        });
        JLabel delete = createActionLabel(DELETE_ICON, "Delete keyframe", new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                parent.deleteKeyframe(index);
            }
        });

        if (index == 0) {
            moveUp.setEnabled(false);
        }

        if (index == sequence.getKeyframes().size() - 1) {
            moveDown.setEnabled(false);
        }

        content.add(view, cc);
        cc.gridx++;
        content.add(overwrite, cc);
        cc.gridx++;
        content.add(duplicate, cc);
        cc.gridx++;
        content.add(moveUp, cc);
        cc.gridx++;
        content.add(moveDown, cc);
        cc.gridx++;
        content.add(delete, cc);
        cc.gridx++;

        return content;
    }

    private void toggleShowDetails()
    {
        showControls = !showControls;
    }

    public void updateKeyframe()
    {
        this.keyframe = sequence.getKeyframe(index);
        removeAll();
        drawPanel();
        revalidate();
        repaint();
    }

}