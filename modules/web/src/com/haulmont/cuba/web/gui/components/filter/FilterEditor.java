/*
 * Copyright (c) 2009 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 14.10.2009 17:19:08
 *
 * $Id$
 */
package com.haulmont.cuba.web.gui.components.filter;

import com.haulmont.cuba.core.entity.CategorizedEntity;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.global.UserSessionProvider;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.UserSessionClient;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.filter.*;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.security.entity.FilterEntity;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.cuba.web.gui.components.WebFilter;
import com.vaadin.data.Property;
import com.vaadin.event.Action;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.BaseTheme;
import org.apache.commons.lang.BooleanUtils;
import org.dom4j.Element;

import java.util.*;

import static org.apache.commons.lang.BooleanUtils.isTrue;

public class FilterEditor extends AbstractFilterEditor {

    private AbstractOrderedLayout layout;
    private TextField nameField;
    private Table table;
    private Select addSelect;
    private CheckBox defaultCb;
    private CheckBox applyDefaultCb;

    private static final String EDITOR_WIDTH = "640px";
    private static final String TABLE_WIDTH = "600px";
    private CheckBox globalCb;
    private Button saveBtn;

    private Button upBtn;
    private Button downBtn;

    public FilterEditor(final WebFilter webFilter, FilterEntity filterEntity,
                        Element filterDescriptor, List<String> existingNames) {
        super(webFilter, filterEntity, filterDescriptor, existingNames);
    }

    public void init() {
        layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true, false, false, false);
        layout.setWidth(EDITOR_WIDTH);

        GridLayout topGrid = new GridLayout(2, 1);
        topGrid.setWidth("100%");
        topGrid.setSpacing(true);

        GridLayout bottomGrid = new GridLayout(2, 3);
        bottomGrid.setWidth("100%");
        bottomGrid.setSpacing(true);

        HorizontalLayout controlLayout = new HorizontalLayout();
        controlLayout.setSpacing(true);

        // Move up button
        upBtn = WebComponentsHelper.createButton("icons/up.png");
        upBtn.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                Object item = table.getValue();
                if (item != table.getNullSelectionItemId()) {
                    AbstractCondition condition = (AbstractCondition) item;
                    int index = conditions.indexOf(condition);
                    if (index > 0) {
                        AbstractCondition next = conditions.get(index - 1);
                        conditions.set(index - 1, condition);
                        conditions.set(index, next);
                        updateTable();
                    }
                }
            }
        });
        upBtn.setEnabled(true);

        // Move down button
        downBtn = WebComponentsHelper.createButton("icons/down.png");
        downBtn.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                Object item = table.getValue();
                if (item != table.getNullSelectionItemId()) {
                    AbstractCondition condition = (AbstractCondition) item;
                    int index = conditions.indexOf(condition);
                    int count = conditions.size();
                    if (index < count - 1) {
                        AbstractCondition next = conditions.get(index + 1);
                        conditions.set(index + 1, condition);
                        conditions.set(index, next);
                        updateTable();
                    }
                }
            }
        });
        downBtn.setEnabled(true);

        // Save button
        saveBtn = WebComponentsHelper.createButton("icons/ok.png");
        saveBtn.setCaption(MessageProvider.getMessage(AppConfig.getInstance().getMessagesPack(), "actions.Ok"));
        saveBtn.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                if (commit())
                    ((WebFilter) filter).editorCommitted();
            }
        });
        if (filterEntity.getCode() != null)
            saveBtn.setEnabled(false);
        controlLayout.addComponent(saveBtn);

        // Cancel button
        Button cancelBtn = WebComponentsHelper.createButton("icons/cancel.png");
        cancelBtn.setCaption(MessageProvider.getMessage(AppConfig.getInstance().getMessagesPack(), "actions.Cancel"));
        cancelBtn.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                ((WebFilter) filter).editorCancelled();
            }
        });
        controlLayout.addComponent(cancelBtn);

        bottomGrid.addComponent(controlLayout, 0, 2);

        globalCb = new CheckBox();
        globalCb.setCaption(getMessage("FilterEditor.global"));
        globalCb.setValue(filterEntity.getUser() == null);
        globalCb.setEnabled(UserSessionClient.getUserSession().isSpecificPermitted("cuba.gui.filter.global"));
        controlLayout.addComponent(globalCb);

        bottomGrid.addComponent(globalCb, 1, 0);
        bottomGrid.setComponentAlignment(globalCb, Alignment.MIDDLE_RIGHT);

        defaultCb = new CheckBox();
        defaultCb.setCaption(getMessage("FilterEditor.isDefault"));
        defaultCb.setImmediate(true);

        defaultCb.addListener(new Property.ValueChangeListener() {

            public void valueChange(Property.ValueChangeEvent event) {
                if ((Boolean) defaultCb.getValue()) {
                    applyDefaultCb.setEnabled(true);
                } else {
                    applyDefaultCb.setEnabled(false);
                    applyDefaultCb.setValue(false);
                }
                if (filterEntity != null) {
                    filterEntity.setIsDefault(isTrue((Boolean) defaultCb.getValue()));

                }
            }
        });
        bottomGrid.addComponent(defaultCb, 1, 1);
        bottomGrid.setComponentAlignment(defaultCb, Alignment.MIDDLE_RIGHT);

        applyDefaultCb = new CheckBox();
        applyDefaultCb.setCaption(getMessage("FilterEditor.applyDefault"));
        applyDefaultCb.setImmediate(true);
        applyDefaultCb.setEnabled(false);
        applyDefaultCb.addListener(new Property.ValueChangeListener() {
            public void valueChange(Property.ValueChangeEvent event) {
                if (filterEntity != null) {
                    filterEntity.setApplyDefault(isTrue((Boolean) applyDefaultCb.getValue()));
                }
            }
        });

        bottomGrid.addComponent(applyDefaultCb, 1, 2);
        bottomGrid.setComponentAlignment(applyDefaultCb, Alignment.MIDDLE_RIGHT);

        HorizontalLayout nameLayout = new HorizontalLayout();
        nameLayout.setSpacing(true);

        Label label = new Label(getMessage("FilterEditor.nameLab"));
        nameLayout.addComponent(label);

        nameField = new TextField();
        nameField.setValue(filterEntity.getName());
        nameField.setWidth("200px");
        nameLayout.addComponent(nameField);

        topGrid.addComponent(nameLayout, 0, 0);
        topGrid.setWidth(TABLE_WIDTH);

        HorizontalLayout addLayout = new HorizontalLayout();
        addLayout.setSpacing(true);
        initAddSelect(addLayout);
        topGrid.addComponent(addLayout, 1, 0);
        topGrid.setComponentAlignment(addLayout, Alignment.MIDDLE_RIGHT);

        HorizontalLayout hlayLayout = new HorizontalLayout();
        hlayLayout.setSpacing(true);

        VerticalLayout controlsAndtable = new VerticalLayout();
        controlsAndtable.addComponent(topGrid);
        controlsAndtable.setSpacing(true);
        initTable(controlsAndtable);

        hlayLayout.addComponent(controlsAndtable);

        VerticalLayout upDownLayout = new VerticalLayout();
        upDownLayout.setSpacing(true);
        upDownLayout.addComponent(upBtn);
        upDownLayout.addComponent(downBtn);
        hlayLayout.addComponent(upDownLayout);
        hlayLayout.setComponentAlignment(upDownLayout, Alignment.MIDDLE_CENTER);
        layout.addComponent(hlayLayout);
        layout.addComponent(bottomGrid);

        updateControls();
    }

    public Button getSaveButton() {
        return saveBtn;
    }

    private void initAddSelect(AbstractLayout layout) {
        Label label = new Label(getMessage("FilterEditor.addCondition"));
        layout.addComponent(label);

        addSelect = new Select();
        addSelect.setImmediate(true);
        addSelect.setNullSelectionAllowed(true);
        addSelect.setFilteringMode(Select.FILTERINGMODE_CONTAINS);
        addSelect.setWidth("100px");
        for (AbstractConditionDescriptor descriptor : descriptors) {
            addSelect.addItem(descriptor);
            addSelect.setItemCaption(descriptor, descriptor.getLocCaption());
        }

        if (UserSessionProvider.getUserSession().isSpecificPermitted("cuba.gui.filter.customConditions")) {
            ConditionCreator conditionCreator = new ConditionCreator(filterComponentName, datasource);
            addSelect.addItem(conditionCreator);
            addSelect.setItemCaption(conditionCreator, conditionCreator.getLocCaption());
        }

        if (CategorizedEntity.class.isAssignableFrom(metaClass.getJavaClass())) {
            RuntimePropConditionCreator runtimePropCreator = new RuntimePropConditionCreator(filterComponentName, datasource);
            addSelect.addItem(runtimePropCreator);
            addSelect.setItemCaption(runtimePropCreator, runtimePropCreator.getLocCaption());
        }
        addSelect.addListener(new Property.ValueChangeListener() {
            public void valueChange(Property.ValueChangeEvent event) {
                if (addSelect.getValue() != null) {
                    addCondition((AbstractConditionDescriptor) addSelect.getValue());
                    addSelect.select(null);
                }
            }
        });
        layout.addComponent(addSelect);
    }

    private void initTable(AbstractLayout layout) {
        table = new com.haulmont.cuba.web.toolkit.ui.Table();
        table.setImmediate(true);
        table.setSelectable(true);
        table.setPageLength(0);
        table.setWidth(TABLE_WIDTH);
        table.setHeight("200px");
        table.setStyleName("filter-conditions");

        String nameCol = getMessage("FilterEditor.column.name");
        String opCol = getMessage("FilterEditor.column.op");
        String paramCol = getMessage("FilterEditor.column.param");
        String hiddenCol = getMessage("FilterEditor.column.hidden");
        String cntrCol = getMessage("FilterEditor.column.control");

        table.addContainerProperty(nameCol, NameEditor.class, null);
        table.setColumnWidth(nameCol, 160);

        table.addContainerProperty(opCol, OperationEditor.Editor.class, null);
        table.setColumnWidth(opCol, 100);

        table.addContainerProperty(paramCol, ParamEditor.class, null);
        table.setColumnWidth(paramCol, 160);

        table.addContainerProperty(hiddenCol, CheckBox.class, null);
        table.setColumnWidth(cntrCol, 50);

        table.addContainerProperty(cntrCol, Button.class, null);
        table.setColumnWidth(cntrCol, 30);

        for (final AbstractCondition condition : this.conditions) {
            NameEditor nameEditor = new NameEditor(condition);
            AbstractOperationEditor operationEditor = condition.createOperationEditor();
            ParamEditor paramEditor = new ParamEditor(condition, false);

            table.addItem(new Object[]{
                    nameEditor,
                    operationEditor.getImpl(),
                    paramEditor,
                    createHiddenCheckbox(condition),
                    createDeleteConditionBtn(condition)
            },
                    condition
            );
        }

        final Action showNameAction = new Action(MessageProvider.getMessage(MESSAGES_PACK, "FilterEditor.showNameAction"));
        table.addActionHandler(
                new Action.Handler() {
                    public Action[] getActions(Object target, Object sender) {
                        return new Action[]{showNameAction};
                    }

                    public void handleAction(Action action, Object sender, Object target) {
                        if (action.equals(showNameAction)) {
                            App.getInstance().getWindowManager().showMessageDialog(
                                    MessageProvider.getMessage(MESSAGES_PACK, "FilterEditor.showNameTitle"),
                                    ((AbstractCondition) target).getParam().getName(),
                                    IFrame.MessageType.CONFIRMATION
                            );
                        }
                    }
                }
        );

        layout.addComponent(table);
    }

    private void addCondition(AbstractConditionDescriptor descriptor) {
        AbstractCondition condition = descriptor.createCondition();
        conditions.add(condition);

        NameEditor nameEditor = new NameEditor(condition);
        AbstractOperationEditor operationEditor = condition.createOperationEditor();
        ParamEditor paramEditor = new ParamEditor(condition, false);

        table.addItem(new Object[]{
                nameEditor,
                operationEditor.getImpl(),
                paramEditor,
                createHiddenCheckbox(condition),
                createDeleteConditionBtn(condition)
        },
                condition
        );

        updateControls();
        if (operationEditor instanceof HasAction) {
            ((HasAction) operationEditor).doAction();
        }
    }

    private void deleteCondition(AbstractCondition condition) {
        conditions.remove(condition);
        table.removeItem(condition);
        updateControls();
    }

    private void updateControls() {
        if (filterEntity.getCode() == null)
            saveBtn.setEnabled(!conditions.isEmpty());
        else
            saveBtn.setEnabled(false);
        defaultCb.setVisible(filterEntity.getFolder() == null);
        defaultCb.setValue(isTrue(filterEntity.getIsDefault()));
        applyDefaultCb.setVisible(defaultCb.isVisible() && manualApplyRequired);
        applyDefaultCb.setValue(BooleanUtils.isTrue(filterEntity.getApplyDefault()));
    }

    private void updateTable() {
        table.removeAllItems();
        for (final AbstractCondition condition : this.conditions) {
            NameEditor nameEditor = new NameEditor(condition);
            AbstractOperationEditor operationEditor = condition.createOperationEditor();
            ParamEditor paramEditor = new ParamEditor(condition, false);

            table.addItem(new Object[]{
                    nameEditor,
                    operationEditor.getImpl(),
                    paramEditor,
                    createHiddenCheckbox(condition),
                    createDeleteConditionBtn(condition)
            },
                    condition
            );
        }
        updateControls();
    }

    private Button createDeleteConditionBtn(final AbstractCondition condition) {
        Button delBtn = WebComponentsHelper.createButton("icons/tab-remove.png");
        delBtn.setStyleName(BaseTheme.BUTTON_LINK);
        delBtn.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                deleteCondition(condition);
            }
        });
        return delBtn;
    }

    private CheckBox createHiddenCheckbox(final AbstractCondition condition) {
        final CheckBox checkBox = new CheckBox();
        checkBox.setValue(condition.isHidden());
        checkBox.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                boolean hidden = BooleanUtils.isTrue((Boolean) checkBox.getValue());
                condition.setHidden(hidden);
            }
        });
        return checkBox;
    }

    @Override
    protected AbstractFilterParser createFilterParser(String xml, String messagesPack, String filterComponentName,
                                                      CollectionDatasource datasource) {
        return new FilterParser(xml, messagesPack, filterComponentName, datasource);
    }

    @Override
    protected AbstractPropertyConditionDescriptor createPropertyConditionDescriptor(
            Element element, String messagesPack, String filterComponentName, CollectionDatasource datasource) {
        return new PropertyConditionDescriptor(element, messagesPack, filterComponentName, datasource);
    }

    @Override
    protected AbstractPropertyConditionDescriptor createPropertyConditionDescriptor(
            String name, String caption, String messagesPack, String filterComponentName, CollectionDatasource datasource) {
        return new PropertyConditionDescriptor(name, caption, messagesPack, filterComponentName, datasource);
    }

    @Override
    protected AbstractCustomConditionDescriptor createCustomConditionDescriptor(
            Element element, String messagesPack, String filterComponentName, CollectionDatasource datasource) {
        return new CustomConditionDescriptor(element, messagesPack, filterComponentName, datasource);
    }

    @Override
    protected AbstractFilterParser createFilterParser(List<AbstractCondition> conditions, String messagesPack,
                                                      String filterComponentName, Datasource datasource) {
        return new FilterParser(conditions, messagesPack, filterComponentName, datasource);
    }

    @Override
    protected String getName() {
        return (String) nameField.getValue();
    }

    @Override
    protected boolean isGlobal() {
        return (Boolean) globalCb.getValue();
    }

    @Override
    protected void showNotification(String caption, String description) {
        App.getInstance().getAppWindow().showNotification(caption, description, Window.Notification.TYPE_HUMANIZED_MESSAGE);
    }

    public AbstractOrderedLayout getLayout() {
        return layout;
    }

    public FilterEntity getFilterEntity() {
        return filterEntity;
    }

    public List<AbstractCondition> getConditions() {
        return conditions;
    }
}
