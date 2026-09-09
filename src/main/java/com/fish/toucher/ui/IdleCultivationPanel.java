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
    private final VisibleTabRefresher<Component> refresher;
    private boolean syncingTabs;
    private boolean disposed;

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

        refresher = new VisibleTabRefresher<>(this::isShowing, tabs::getSelectedComponent);
        for (int index = 0; index < tabs.getTabCount(); index++) {
            JComponent tab = (JComponent) tabs.getComponentAt(index);
            refresher.register(tab, () -> preserveOuterScrollPositions(tab, () -> refreshTab(tab)));
        }
        tabs.addChangeListener(event -> {
            if (!syncingTabs && !disposed) refresher.refreshSelected();
        });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && !disposed) {
                refresher.refreshSelected();
            }
        });
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
        if (disposed) return;
        syncingTabs = true;
        try {
            syncAscendedTabs(NovelReaderSettings.getInstance().isCultivationAscended());
        } finally {
            syncingTabs = false;
        }
        refresher.invalidate();
        refresher.refreshSelected();
    }

    private void refreshTab(JComponent tab) {
        if (disposed) return;
        setTabsRefreshing(true);
        try {
            IdleCultivationManager manager = IdleCultivationManager.getInstance();
            NovelReaderSettings settings = NovelReaderSettings.getInstance();
            if (tab == trainingTab.getComponent()) trainingTab.updateTrainingState(manager);
            else if (tab == bagTab.getComponent()) bagTab.reloadBagOptions(manager, settings);
            else if (tab == travelTab.getComponent()) {
                travelTab.reloadTravelOptions(manager);
                travelTab.updateActiveTravel(manager);
            } else if (tab == abodeTab.getComponent()) abodeTab.reloadAbodeFacilities(manager);
            else if (tab == challengeTab.getComponent()) {
                challengeTab.reloadCultivatorOptions(manager);
                challengeTab.updateBattleState(manager);
            } else if (tab == sectTab.getComponent()) sectTab.reloadSectState(manager);
            else if (tab == guideTab.getComponent()) guideTab.updateGuideState(settings.isCultivationAscended());
        } finally {
            setTabsRefreshing(false);
        }
        if (tab == bagTab.getComponent()) bagTab.updateSelectionDescriptions();
        else if (tab == travelTab.getComponent()) travelTab.updateSelectionDescriptions();
        else if (tab == challengeTab.getComponent()) challengeTab.updateSelectionDescriptions();
    }

    private void setTabsRefreshing(boolean refreshing) {
        bagTab.setRefreshing(refreshing);
        travelTab.setRefreshing(refreshing);
        challengeTab.setRefreshing(refreshing);
        sectTab.setRefreshing(refreshing);
    }

    private void syncAscendedTabs(boolean ascended) {
        if (lastAscendedTabState != null && lastAscendedTabState == ascended) {
            return;
        }
        lastAscendedTabState = ascended;

        int abodeIndex = tabs.indexOfComponent(abodeTab.getComponent());
        int challengeIndex = tabs.indexOfComponent(challengeTab.getComponent());
        if (ascended) {
            boolean selectedRemovedTab = (abodeIndex >= 0 && tabs.getSelectedIndex() == abodeIndex)
                    || (challengeIndex >= 0 && tabs.getSelectedIndex() == challengeIndex);
            if (challengeIndex >= 0) {
                tabs.removeTabAt(challengeIndex);
            }
            abodeIndex = tabs.indexOfComponent(abodeTab.getComponent());
            if (abodeIndex >= 0) {
                tabs.removeTabAt(abodeIndex);
            }
            if (selectedRemovedTab) {
                int sectIndex = tabs.indexOfComponent(sectTab.getComponent());
                tabs.setSelectedIndex(sectIndex >= 0 ? sectIndex : 0);
            }
            return;
        }

        if (abodeIndex < 0) {
            // 未飞升时恢复洞府页签，位置保持在游历与挑战之间。
            challengeIndex = tabs.indexOfComponent(challengeTab.getComponent());
            int insertIndex = challengeIndex >= 0 ? challengeIndex : Math.min(3, tabs.getTabCount());
            tabs.insertTab(
                    FishToucherBundle.message("cultivation.tab.abode"),
                    null,
                    abodeTab.getComponent(),
                    null,
                    insertIndex
            );
        }
        if (tabs.indexOfComponent(challengeTab.getComponent()) < 0) {
            // 未飞升时恢复挑战页签，位置保持在洞府与宗门之间。
            int sectIndex = tabs.indexOfComponent(sectTab.getComponent());
            int insertIndex = sectIndex >= 0 ? sectIndex : Math.min(4, tabs.getTabCount());
            tabs.insertTab(
                    FishToucherBundle.message("cultivation.tab.challenge"),
                    null,
                    challengeTab.getComponent(),
                    null,
                    insertIndex
            );
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        refresher.close();
        IdleCultivationManager.getInstance().removeChangeListener(changeListener);
    }
}
