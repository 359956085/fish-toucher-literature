package com.fish.toucher.ui;

import com.fish.toucher.settings.NovelReaderSettings;

import javax.swing.*;
import java.awt.*;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationGuideTab {

    private final JComponent component;
    private final JPanel contentPanel;
    private Boolean lastAscendedState;

    IdleCultivationGuideTab() {
        contentPanel = createFormPanel();
        component = createScrollableTab(contentPanel);
        updateGuideState(NovelReaderSettings.getInstance().isCultivationAscended());
    }

    JComponent getComponent() {
        return component;
    }

    void updateGuideState(boolean ascended) {
        if (lastAscendedState != null && lastAscendedState == ascended) {
            return;
        }
        lastAscendedState = ascended;
        rebuildContent(ascended);
    }

    private void rebuildContent(boolean ascended) {
        contentPanel.removeAll();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        if (ascended) {
            row = addGuideSection(contentPanel, gbc, row, "spirit.loop");
            row = addGuideSection(contentPanel, gbc, row, "spirit.realms");
            row = addRealmDescriptionRows(contentPanel, gbc, row, 9, 14);
            row = addGuideSection(contentPanel, gbc, row, "spirit.travel");
            row = addGuideSection(contentPanel, gbc, row, "spirit.ownSect");
            row = addGuideSection(contentPanel, gbc, row, "spirit.buildings");
            row = addGuideSection(contentPanel, gbc, row, "spirit.promotion");
            row = addGuideSection(contentPanel, gbc, row, "spirit.resources");
        } else {
            row = addGuideSection(contentPanel, gbc, row, "loop");
            row = addGuideSection(contentPanel, gbc, row, "gains");
            row = addGuideSection(contentPanel, gbc, row, "realms");
            row = addRealmDescriptionRows(contentPanel, gbc, row, 0, 8);
            row = addGuideSection(contentPanel, gbc, row, "travel");
            row = addGuideSection(contentPanel, gbc, row, "abode");
            row = addGuideSection(contentPanel, gbc, row, "bag");
            row = addGuideSection(contentPanel, gbc, row, "breakthrough");
        }

        addBottomGlue(contentPanel, gbc, row);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}
