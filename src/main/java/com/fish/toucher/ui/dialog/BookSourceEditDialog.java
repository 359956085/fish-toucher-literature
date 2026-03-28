package com.fish.toucher.ui.dialog;

import com.fish.toucher.model.BookSource;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class BookSourceEditDialog extends DialogWrapper {

    private static final Logger LOG = Logger.getInstance(BookSourceEditDialog.class);

    // Basic info
    private final JTextField nameField = new JTextField();
    private final JTextField urlField = new JTextField();
    private final JCheckBox enabledCheckBox = new JCheckBox("\u542f\u7528", true);
    private final JTextField headersField = new JTextField();

    // Search rule
    private final JTextField searchUrlField = new JTextField();
    private final JComboBox<String> searchMethodCombo = new JComboBox<>(new String[]{"GET", "POST"});
    private final JComboBox<String> searchTypeCombo = new JComboBox<>(new String[]{"html", "json"});
    private final JTextField searchListField = new JTextField();
    private final JTextField searchNameField = new JTextField();
    private final JTextField searchAuthorField = new JTextField();
    private final JTextField searchBookUrlField = new JTextField();
    private final JTextField searchCoverUrlField = new JTextField();

    // Chapter rule
    private final JTextField chapterUrlField = new JTextField();
    private final JComboBox<String> chapterTypeCombo = new JComboBox<>(new String[]{"html", "json"});
    private final JTextField chapterListField = new JTextField();
    private final JTextField chapterNameField = new JTextField();
    private final JTextField chapterChapterUrlField = new JTextField();

    // Content rule
    private final JTextField contentUrlField = new JTextField();
    private final JComboBox<String> contentTypeCombo = new JComboBox<>(new String[]{"html", "json"});
    private final JTextField contentSelectorField = new JTextField();
    private final JTextField contentPurifyField = new JTextField();

    private BookSource result;

    public BookSourceEditDialog(@Nullable BookSource existing) {
        super((Project) null, true);
        setTitle(existing != null ? "\u7f16\u8f91\u4e66\u6e90" : "\u65b0\u5efa\u4e66\u6e90");
        init();
        if (existing != null) {
            populateFrom(existing);
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(3);
        gbc.weightx = 1.0;

        int row = 0;

        // ── Basic Info ──────────────────────────────────────────────────────────
        row = addSectionTitle(form, gbc, row, "\u57fa\u672c\u4fe1\u606f");
        row = addField(form, gbc, row, "\u540d\u79f0:", nameField, null);
        row = addField(form, gbc, row, "\u57fa\u7840URL:", urlField, null);

        // enabled checkbox spans both columns
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        form.add(enabledCheckBox, gbc);
        gbc.gridwidth = 1;
        row++;

        headersField.setToolTipText("\u683c\u5f0f: Key1:Value1, Key2:Value2");
        row = addField(form, gbc, row, "\u8bf7\u6c42\u5934:", headersField, "\u683c\u5f0f: Key1:Value1, Key2:Value2");

        // ── Search Rule ─────────────────────────────────────────────────────────
        row = addSectionTitle(form, gbc, row, "\u641c\u7d22\u89c4\u5219");
        row = addField(form, gbc, row, "\u641c\u7d22URL:", searchUrlField, "\u4f7f\u7528 {{keyword}} \u4f5c\u4e3a\u5173\u952e\u8bcd\u5360\u4f4d\u7b26");
        row = addField(form, gbc, row, "\u65b9\u6cd5:", searchMethodCombo, null);
        row = addField(form, gbc, row, "\u7c7b\u578b:", searchTypeCombo, null);
        row = addField(form, gbc, row, "\u5217\u8868\u9009\u62e9\u5668:", searchListField, "CSS\u9009\u62e9\u5668(html) \u6216 JSONPath(json)");
        row = addField(form, gbc, row, "\u4e66\u540d:", searchNameField, "\u683c\u5f0f: selector@text \u6216 @text");
        row = addField(form, gbc, row, "\u4f5c\u8005:", searchAuthorField, null);
        row = addField(form, gbc, row, "\u4e66\u7c4dURL:", searchBookUrlField, "\u683c\u5f0f: a@href");
        row = addField(form, gbc, row, "\u5c01\u9762URL:", searchCoverUrlField, null);

        // ── Chapter Rule ────────────────────────────────────────────────────────
        row = addSectionTitle(form, gbc, row, "\u7ae0\u8282\u89c4\u5219");
        row = addField(form, gbc, row, "\u7ae0\u8282\u9875URL:", chapterUrlField, "\u4f7f\u7528 {{bookUrl}} \u4f5c\u4e3a\u5360\u4f4d\u7b26");
        row = addField(form, gbc, row, "\u7c7b\u578b:", chapterTypeCombo, null);
        row = addField(form, gbc, row, "\u5217\u8868\u9009\u62e9\u5668:", chapterListField, null);
        row = addField(form, gbc, row, "\u7ae0\u8282\u540d:", chapterNameField, null);
        row = addField(form, gbc, row, "\u7ae0\u8282URL:", chapterChapterUrlField, null);

        // ── Content Rule ────────────────────────────────────────────────────────
        row = addSectionTitle(form, gbc, row, "\u6b63\u6587\u89c4\u5219");
        row = addField(form, gbc, row, "\u6b63\u6587URL:", contentUrlField, "\u4f7f\u7528 {{chapterUrl}} \u4f5c\u4e3a\u5360\u4f4d\u7b26");
        row = addField(form, gbc, row, "\u7c7b\u578b:", contentTypeCombo, null);
        row = addField(form, gbc, row, "\u6b63\u6587\u9009\u62e9\u5668:", contentSelectorField, null);
        row = addField(form, gbc, row, "\u8fc7\u6ee4\u9009\u62e9\u5668:", contentPurifyField,
                "\u9017\u53f7\u5206\u9694\u7684CSS\u9009\u62e9\u5668\uff0c\u5982: script, div.ad");

        // vertical glue so everything stays top-aligned
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(Box.createVerticalGlue(), gbc);

        JScrollPane scrollPane = new JScrollPane(form);
        scrollPane.setPreferredSize(new Dimension(520, 620));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    // ── Helper: section title ────────────────────────────────────────────────────

    private int addSectionTitle(JPanel panel, GridBagConstraints gbc, int row, String title) {
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD));

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = JBUI.insets(8, 3, 2, 3);
        panel.add(label, gbc);

        gbc.gridwidth = 1;
        gbc.insets = JBUI.insets(3);
        return row + 1;
    }

    // ── Helper: label + component row ───────────────────────────────────────────

    private int addField(JPanel panel, GridBagConstraints gbc, int row, String labelText,
                         JComponent field, @Nullable String tooltip) {
        JLabel label = new JLabel(labelText);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(label, gbc);

        if (tooltip != null) {
            field.setToolTipText(tooltip);
        }

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);

        return row + 1;
    }

    // ── Populate from existing BookSource ────────────────────────────────────────

    private void populateFrom(BookSource source) {
        nameField.setText(nullToEmpty(source.getName()));
        urlField.setText(nullToEmpty(source.getUrl()));
        enabledCheckBox.setSelected(source.isEnabled());

        if (source.getHeader() != null && !source.getHeader().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : source.getHeader().entrySet()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(entry.getKey()).append(":").append(entry.getValue());
            }
            headersField.setText(sb.toString());
        }

        BookSource.SearchRule sr = source.getSearchRule();
        if (sr != null) {
            searchUrlField.setText(nullToEmpty(sr.getUrl()));
            setComboSelection(searchMethodCombo, sr.getMethod());
            setComboSelection(searchTypeCombo, sr.getType());
            searchListField.setText(nullToEmpty(sr.getList()));
            searchNameField.setText(nullToEmpty(sr.getName()));
            searchAuthorField.setText(nullToEmpty(sr.getAuthor()));
            searchBookUrlField.setText(nullToEmpty(sr.getBookUrl()));
            searchCoverUrlField.setText(nullToEmpty(sr.getCoverUrl()));
        }

        BookSource.ChapterRule cr = source.getChapterRule();
        if (cr != null) {
            chapterUrlField.setText(nullToEmpty(cr.getUrl()));
            setComboSelection(chapterTypeCombo, cr.getType());
            chapterListField.setText(nullToEmpty(cr.getList()));
            chapterNameField.setText(nullToEmpty(cr.getName()));
            chapterChapterUrlField.setText(nullToEmpty(cr.getChapterUrl()));
        }

        BookSource.ContentRule cont = source.getContentRule();
        if (cont != null) {
            contentUrlField.setText(nullToEmpty(cont.getUrl()));
            setComboSelection(contentTypeCombo, cont.getType());
            contentSelectorField.setText(nullToEmpty(cont.getSelector()));
            if (cont.getPurify() != null) {
                contentPurifyField.setText(String.join(", ", cont.getPurify()));
            }
        }
    }

    // ── doOKAction ───────────────────────────────────────────────────────────────

    @Override
    protected void doOKAction() {
        try {
            result = buildBookSource();
        } catch (Exception e) {
            LOG.warn("Failed to build BookSource from form", e);
        }
        super.doOKAction();
    }

    private BookSource buildBookSource() {
        BookSource source = new BookSource();
        source.setName(nameField.getText().trim());
        source.setUrl(urlField.getText().trim());
        source.setEnabled(enabledCheckBox.isSelected());

        // Parse headers
        String headersText = headersField.getText().trim();
        if (!headersText.isEmpty()) {
            Map<String, String> headers = new LinkedHashMap<>();
            for (String pair : headersText.split(",")) {
                int idx = pair.indexOf(':');
                if (idx > 0) {
                    String key = pair.substring(0, idx).trim();
                    String value = pair.substring(idx + 1).trim();
                    if (!key.isEmpty()) {
                        headers.put(key, value);
                    }
                }
            }
            source.setHeader(headers);
        }

        // Search rule
        BookSource.SearchRule sr = new BookSource.SearchRule();
        sr.setUrl(searchUrlField.getText().trim());
        sr.setMethod(Objects.requireNonNull((String) searchMethodCombo.getSelectedItem()));
        sr.setType(Objects.requireNonNull((String) searchTypeCombo.getSelectedItem()));
        sr.setList(searchListField.getText().trim());
        sr.setName(searchNameField.getText().trim());
        sr.setAuthor(searchAuthorField.getText().trim());
        sr.setBookUrl(searchBookUrlField.getText().trim());
        sr.setCoverUrl(searchCoverUrlField.getText().trim());
        source.setSearchRule(sr);

        // Chapter rule
        BookSource.ChapterRule cr = new BookSource.ChapterRule();
        cr.setUrl(chapterUrlField.getText().trim());
        cr.setType(Objects.requireNonNull((String) chapterTypeCombo.getSelectedItem()));
        cr.setList(chapterListField.getText().trim());
        cr.setName(chapterNameField.getText().trim());
        cr.setChapterUrl(chapterChapterUrlField.getText().trim());
        source.setChapterRule(cr);

        // Content rule
        BookSource.ContentRule cont = new BookSource.ContentRule();
        cont.setUrl(contentUrlField.getText().trim());
        cont.setType(Objects.requireNonNull((String) contentTypeCombo.getSelectedItem()));
        cont.setSelector(contentSelectorField.getText().trim());

        String purifyText = contentPurifyField.getText().trim();
        if (!purifyText.isEmpty()) {
            List<String> purifyList = new ArrayList<>();
            for (String s : purifyText.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) purifyList.add(t);
            }
            cont.setPurify(purifyList);
        }
        source.setContentRule(cont);

        return source;
    }

    // ── Public accessor ───────────────────────────────────────────────────────────

    @Nullable
    public BookSource getResult() {
        return result;
    }

    // ── Private utilities ─────────────────────────────────────────────────────────

    private static String nullToEmpty(@Nullable String s) {
        return s != null ? s : "";
    }

    private static void setComboSelection(JComboBox<String> combo, @Nullable String value) {
        if (value == null) return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (value.equals(combo.getItemAt(i))) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }
}