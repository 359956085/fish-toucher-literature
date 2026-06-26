package com.fish.toucher.ui;

import javax.swing.*;
import java.awt.*;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationGuideTab {

    private final JComponent component;

    IdleCultivationGuideTab() {
        component = createContent();
    }

    JComponent getComponent() {
        return component;
    }

    void setRefreshing(boolean refreshing) {
    }

    private JComponent createContent() {
        JPanel contentPanel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        row = addGuideSection(contentPanel, gbc, row, "loop");
        row = addGuideSection(contentPanel, gbc, row, "gains");
        row = addGuideSection(contentPanel, gbc, row, "realms");
        row = addRealmDescriptionRows(contentPanel, gbc, row);
        row = addGuideSection(contentPanel, gbc, row, "travel");
        row = addGuideSection(contentPanel, gbc, row, "abode");
        row = addGuideSection(contentPanel, gbc, row, "bag");
        row = addGuideSection(contentPanel, gbc, row, "breakthrough");

        addBottomGlue(contentPanel, gbc, row);
        return createScrollableTab(contentPanel);
    }
}
