package com.fish.toucher.ui;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** 数据相同时保留模型；持久化选择未变时，保留用户正在编辑的选择。 */
final class StableComboOptions {
    private static final Object SAVED_KEY = new Object();
    private record Saved(Object key) {}

    private StableComboOptions() {}

    static <T, K> void update(JComboBox<T> combo, List<T> options, Function<T, K> key, K savedKey) {
        Saved previous = (Saved) combo.getClientProperty(SAVED_KEY);
        K selectedKey = savedKey;
        int selected = combo.getSelectedIndex();
        if (previous != null && Objects.equals(previous.key(), savedKey) && selected >= 0) {
            selectedKey = key.apply(combo.getItemAt(selected));
        }
        boolean changed = combo.getItemCount() != options.size();
        if (!changed) {
            for (int index = 0; index < options.size(); index++) {
                if (!Objects.equals(combo.getItemAt(index), options.get(index))) {
                    changed = true;
                    break;
                }
            }
        }
        T target = find(options, key, selectedKey);
        if (target == null) target = find(options, key, savedKey);
        if (target == null && !options.isEmpty()) target = options.get(0);
        combo.putClientProperty(SAVED_KEY, new Saved(savedKey));
        if (changed) {
            DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
            model.addAll(options);
            model.setSelectedItem(target);
            combo.setModel(model);
        } else if (!Objects.equals(combo.getSelectedItem(), target)) {
            combo.setSelectedItem(target);
        }
    }

    private static <T, K> T find(List<T> options, Function<T, K> key, K target) {
        for (T option : options) {
            if (Objects.equals(key.apply(option), target)) return option;
        }
        return null;
    }
}
