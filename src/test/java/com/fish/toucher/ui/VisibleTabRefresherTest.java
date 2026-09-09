package com.fish.toucher.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class VisibleTabRefresherTest {
    @Test
    void 隐藏时合并更新切入时只刷新当前页签() {
        AtomicBoolean showing = new AtomicBoolean();
        AtomicReference<String> selected = new AtomicReference<>("修炼");
        AtomicInteger training = new AtomicInteger();
        AtomicInteger bag = new AtomicInteger();
        VisibleTabRefresher<String> refresher = new VisibleTabRefresher<>(showing::get, selected::get);
        refresher.register("修炼", training::incrementAndGet);
        refresher.register("背包", bag::incrementAndGet);
        for (int index = 0; index < 100; index++) {
            refresher.invalidate(); refresher.refreshSelected();
        }
        assertEquals(0, training.get());
        showing.set(true); refresher.refreshSelected();
        assertEquals(1, training.get());
        assertEquals(0, bag.get());
        selected.set("背包"); refresher.refreshSelected(); refresher.refreshSelected();
        assertEquals(1, bag.get());
        refresher.invalidate(); refresher.refreshSelected();
        assertEquals(2, bag.get());
        assertEquals(1, training.get());
        refresher.close(); refresher.invalidate(); refresher.refreshSelected();
        assertEquals(2, bag.get());
    }

    private record Option(String id, int count) {}

    @Test
    void 同样数据保留模型和未保存选择而真实配置变更能够同步() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JComboBox<Option> combo = new JComboBox<>();
            List<Option> options = List.of(new Option("a", 1), new Option("b", 2));
            StableComboOptions.update(combo, options, Option::id, "a");
            var model = combo.getModel();
            combo.setSelectedIndex(1);
            StableComboOptions.update(combo, options, Option::id, "a");
            assertSame(model, combo.getModel());
            assertEquals("b", ((Option) combo.getSelectedItem()).id());
            List<Option> changed = List.of(new Option("a", 2), new Option("b", 3));
            StableComboOptions.update(combo, changed, Option::id, "a");
            assertNotSame(model, combo.getModel());
            assertEquals(new Option("b", 3), combo.getSelectedItem());
            StableComboOptions.update(combo, changed, Option::id, "b");
            combo.setSelectedIndex(0);
            StableComboOptions.update(combo, changed, Option::id, "b");
            assertEquals("a", ((Option) combo.getSelectedItem()).id());
            StableComboOptions.update(combo, List.of(new Option("b", 4)), Option::id, "b");
            assertEquals(new Option("b", 4), combo.getSelectedItem());
        });
    }

    @Test
    void 外部配置更新时选择新配置且空列表可以恢复() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JComboBox<String> combo = new JComboBox<>();
            StableComboOptions.update(combo, List.of("a", "b", "c"), value -> value, "a");
            combo.setSelectedItem("b");
            StableComboOptions.update(combo, List.of("a", "b", "c"), value -> value, "c");
            assertEquals("c", combo.getSelectedItem());
            StableComboOptions.update(combo, List.of(), value -> value, "c");
            assertNull(combo.getSelectedItem());
            StableComboOptions.update(combo, List.of("a", "c"), value -> value, "c");
            assertEquals("c", combo.getSelectedItem());
        });
    }
}
