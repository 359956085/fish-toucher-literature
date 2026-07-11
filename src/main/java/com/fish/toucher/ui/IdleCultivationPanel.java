package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

import static com.fish.toucher.ui.IdleCultivationUiSupport.preserveOuterScrollPositions;

public class IdleCultivationPanel extends JPanel implements Disposable {

    private static final Logger LOG = Logger.getInstance(IdleCultivationPanel.class);

    private final IdleCultivationTrainingTab trainingTab;
    private final IdleCultivationBagTab bagTab;
    private final IdleCultivationTravelTab travelTab;
    private final IdleCultivationAbodeTab abodeTab;
    private final IdleCultivationChallengeTab challengeTab;
    private final IdleCultivationGuideTab guideTab;
    private final JTabbedPane tabs;
    private final boolean[] dirtyTabs = {true, true, true, true, true, true};
    private final Runnable changeListener;

    public IdleCultivationPanel() {
        LOG.info("IdleCultivationPanel: initializing");
        setLayout(new BorderLayout());

        add(createTopBar(), BorderLayout.NORTH);

        trainingTab = new IdleCultivationTrainingTab(this);
        bagTab = new IdleCultivationBagTab();
        travelTab = new IdleCultivationTravelTab();
        abodeTab = new IdleCultivationAbodeTab();
        challengeTab = new IdleCultivationChallengeTab();
        guideTab = new IdleCultivationGuideTab();

        tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addTab(FishToucherBundle.message("cultivation.tab.training"), trainingTab.getComponent());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.bag"), bagTab.getComponent());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.travel"), travelTab.getComponent());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.abode"), abodeTab.getComponent());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.challenge"), challengeTab.getComponent());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.guide"), guideTab.getComponent());
        tabs.addChangeListener(event -> refreshActiveTab());
        add(tabs, BorderLayout.CENTER);

        changeListener = this::refreshContent;
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        manager.addChangeListener(changeListener);
        manager.start();
        refreshContent();
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()));
        JComboBox<String> modeCombo = PluginModeSelector.createCombo(NovelReaderSettings.MODE_CULTIVATION);
        modeCombo.addActionListener(e -> {
            String selectedMode = PluginModeSelector.getSelectedMode(modeCombo);
            if (!NovelReaderSettings.MODE_CULTIVATION.equals(selectedMode)) {
                NovelReaderToolWindowFactory.switchMode(selectedMode);
            }
        });
        topBar.add(modeCombo);
        JComboBox<String> languageCombo = LanguageSelector.createCombo(NovelReaderSettings.getInstance().getUiLanguage());
        languageCombo.addActionListener(e -> {
            String selectedLanguage = LanguageSelector.getSelectedLanguage(languageCombo);
            if (!selectedLanguage.equals(NovelReaderSettings.getInstance().getUiLanguage())) {
                NovelReaderToolWindowFactory.switchLanguage(selectedLanguage);
            }
        });
        topBar.add(languageCombo);
        return topBar;
    }

    private void refreshContent() {
        java.util.Arrays.fill(dirtyTabs, true);
        refreshActiveTab();
    }

    private void refreshActiveTab() {
        int index = tabs.getSelectedIndex();
        if (index < 0 || !dirtyTabs[index]) {
            return;
        }
        preserveOuterScrollPositions(this, () -> refreshTab(index));
        dirtyTabs[index] = false;
    }

    private void refreshTab(int index) {
        setTabsRefreshing(true);
        try {
            IdleCultivationManager manager = IdleCultivationManager.getInstance();
            NovelReaderSettings settings = NovelReaderSettings.getInstance();
            switch (index) {
                case 0 -> trainingTab.updateTrainingState(manager);
                case 1 -> {
                    bagTab.reloadBagOptions(manager, settings);
                    bagTab.updateSelectionDescriptions();
                }
                case 2 -> {
                    travelTab.reloadTravelOptions(manager);
                    travelTab.updateActiveTravel(manager);
                    travelTab.updateSelectionDescriptions();
                }
                case 3 -> abodeTab.reloadAbodeFacilities(manager);
                case 4 -> {
                    challengeTab.reloadCultivatorOptions(manager);
                    challengeTab.updateBattleState(manager);
                    challengeTab.updateSelectionDescriptions();
                }
                default -> {
                    // Guide tab is static.
                }
            }
        } finally {
            setTabsRefreshing(false);
        }
    }

    private void setTabsRefreshing(boolean refreshing) {
        bagTab.setRefreshing(refreshing);
        travelTab.setRefreshing(refreshing);
        challengeTab.setRefreshing(refreshing);
    }

    @Override
    public void dispose() {
        IdleCultivationManager.getInstance().removeChangeListener(changeListener);
    }
}
