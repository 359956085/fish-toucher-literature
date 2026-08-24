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
    private final IdleCultivationSectTab sectTab;
    private final IdleCultivationGuideTab guideTab;
    private final JTabbedPane tabs;
    private final Runnable changeListener;
    private Boolean lastAscendedTabState;

    public IdleCultivationPanel() {
        LOG.info("IdleCultivationPanel: initializing");
        setLayout(new BorderLayout());

        add(createTopBar(), BorderLayout.NORTH);

        trainingTab = new IdleCultivationTrainingTab(this);
        bagTab = new IdleCultivationBagTab();
        travelTab = new IdleCultivationTravelTab();
        abodeTab = new IdleCultivationAbodeTab();
        challengeTab = new IdleCultivationChallengeTab();
        sectTab = new IdleCultivationSectTab();
        guideTab = new IdleCultivationGuideTab();

        tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addTab(FishToucherBundle.message("cultivation.tab.training"), trainingTab.getComponent());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.bag"), bagTab.getComponent());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.travel"), travelTab.getComponent());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.abode"), abodeTab.getComponent());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.challenge"), challengeTab.getComponent());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.sect"), sectTab.getComponent());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.guide"), guideTab.getComponent());
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
        preserveOuterScrollPositions(this, this::refreshContentWithoutScrollJump);
    }

    private void refreshContentWithoutScrollJump() {
        setTabsRefreshing(true);
        try {
            IdleCultivationManager manager = IdleCultivationManager.getInstance();
            NovelReaderSettings settings = NovelReaderSettings.getInstance();
            syncAscendedTabs(settings.isCultivationAscended());
            guideTab.updateGuideState(settings.isCultivationAscended());
            trainingTab.updateTrainingState(manager);
            bagTab.reloadBagOptions(manager, settings);
            travelTab.reloadTravelOptions(manager);
            travelTab.updateActiveTravel(manager);
            abodeTab.reloadAbodeFacilities(manager);
            challengeTab.reloadCultivatorOptions(manager);
            challengeTab.updateBattleState(manager);
            sectTab.reloadSectState(manager);
        } finally {
            setTabsRefreshing(false);
        }
        updateSelectionDescriptions();
    }

    private void setTabsRefreshing(boolean refreshing) {
        bagTab.setRefreshing(refreshing);
        travelTab.setRefreshing(refreshing);
        challengeTab.setRefreshing(refreshing);
        sectTab.setRefreshing(refreshing);
    }

    private void updateSelectionDescriptions() {
        bagTab.updateSelectionDescriptions();
        travelTab.updateSelectionDescriptions();
        challengeTab.updateSelectionDescriptions();
    }

    private void syncAscendedTabs(boolean ascended) {
        if (lastAscendedTabState != null && lastAscendedTabState == ascended) {
            return;
        }
        lastAscendedTabState = ascended;

        int abodeIndex = tabs.indexOfComponent(abodeTab.getComponent());
        if (ascended) {
            if (abodeIndex >= 0) {
                boolean selectedAbode = tabs.getSelectedIndex() == abodeIndex;
                tabs.removeTabAt(abodeIndex);
                if (selectedAbode) {
                    int sectIndex = tabs.indexOfComponent(sectTab.getComponent());
                    tabs.setSelectedIndex(sectIndex >= 0 ? sectIndex : 0);
                }
            }
            return;
        }

        if (abodeIndex < 0) {
            // 未飞升时恢复洞府页签，位置保持在游历与挑战之间。
            int challengeIndex = tabs.indexOfComponent(challengeTab.getComponent());
            int insertIndex = challengeIndex >= 0 ? challengeIndex : Math.min(3, tabs.getTabCount());
            tabs.insertTab(
                    FishToucherBundle.message("cultivation.tab.abode"),
                    null,
                    abodeTab.getComponent(),
                    null,
                    insertIndex
            );
        }
    }

    @Override
    public void dispose() {
        IdleCultivationManager.getInstance().removeChangeListener(changeListener);
    }
}
