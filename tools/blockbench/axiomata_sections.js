(function () {
    const SECTIONS_PROPERTY = "axiomata_sections";
    const CUBE_PROPERTY = "axiomata_section";
    const PANEL_ID = "axiomata_sections_panel_v2";
    const MIRROR_SETTING = "axiomata_mirror_colours";
    const MAX_SHORTCUTS = 8;
    const TRANSLATION_PREFIX = "plugin.axiomata_sections.";

    Language.addTranslations("en", {
        [`${TRANSLATION_PREFIX}title`]: "Axiomata: Build Sections",
        [`${TRANSLATION_PREFIX}description`]: "Assign model cubes to construction stages.",
        [`${TRANSLATION_PREFIX}section_name`]: "Section name",
        [`${TRANSLATION_PREFIX}new_name`]: "New name",
        [`${TRANSLATION_PREFIX}select_cubes_first`]: "Select cubes first",
        [`${TRANSLATION_PREFIX}empty_section`]: "This section has no cubes",
        [`${TRANSLATION_PREFIX}all_assigned`]: "All cubes are assigned",
        [`${TRANSLATION_PREFIX}assign_selected`]: "Assign selected cubes",
        [`${TRANSLATION_PREFIX}isolate_section`]: "Show only this section",
        [`${TRANSLATION_PREFIX}move_up`]: "Move up",
        [`${TRANSLATION_PREFIX}move_down`]: "Move down",
        [`${TRANSLATION_PREFIX}rename`]: "Rename",
        [`${TRANSLATION_PREFIX}delete_section`]: "Delete section",
        [`${TRANSLATION_PREFIX}setting_name`]: "Color cubes by section",
        [`${TRANSLATION_PREFIX}setting_description`]: "Mirror section numbers to marker colors. Disable this if marker colors are used for something else.",
        [`${TRANSLATION_PREFIX}panel_name`]: "Build Sections",
        [`${TRANSLATION_PREFIX}new_section`]: "New section",
        [`${TRANSLATION_PREFIX}show_all`]: "Show all",
        [`${TRANSLATION_PREFIX}section_hint`]: "Click to select this section's cubes; right-click for more actions",
        [`${TRANSLATION_PREFIX}assign_here`]: "Assign selected cubes here",
        [`${TRANSLATION_PREFIX}mirror_label`]: "color cubes by section",
        [`${TRANSLATION_PREFIX}selected`]: "Selected",
        [`${TRANSLATION_PREFIX}unassigned`]: "unassigned",
        [`${TRANSLATION_PREFIX}select`]: "select",
        [`${TRANSLATION_PREFIX}assigned_all`]: "all assigned",
        [`${TRANSLATION_PREFIX}shortcut_name`]: "Assign selected cubes to section %0",
        [`${TRANSLATION_PREFIX}shortcut_description`]: "Axiomata build sections",
        [`${TRANSLATION_PREFIX}undo_add`]: "Add build section",
        [`${TRANSLATION_PREFIX}undo_rename`]: "Rename build section",
        [`${TRANSLATION_PREFIX}undo_remove`]: "Remove build section",
        [`${TRANSLATION_PREFIX}undo_reorder`]: "Reorder build sections",
        [`${TRANSLATION_PREFIX}undo_assign`]: "Assign cubes to build section",
        [`${TRANSLATION_PREFIX}undo_isolate`]: "Isolate build section",
        [`${TRANSLATION_PREFIX}undo_show_all`]: "Show all cubes",
        [`${TRANSLATION_PREFIX}undo_repaint`]: "Repaint build sections"
    });
    Language.addTranslations("ru", {
        [`${TRANSLATION_PREFIX}title`]: "Axiomata: секции сборки",
        [`${TRANSLATION_PREFIX}description`]: "Распределяет кубы модели по этапам строительства.",
        [`${TRANSLATION_PREFIX}section_name`]: "Название секции",
        [`${TRANSLATION_PREFIX}new_name`]: "Новое название",
        [`${TRANSLATION_PREFIX}select_cubes_first`]: "Сначала выделите кубы",
        [`${TRANSLATION_PREFIX}empty_section`]: "В секции пока нет кубов",
        [`${TRANSLATION_PREFIX}all_assigned`]: "Все кубы распределены",
        [`${TRANSLATION_PREFIX}assign_selected`]: "Отнести выделенное",
        [`${TRANSLATION_PREFIX}isolate_section`]: "Показать только эту секцию",
        [`${TRANSLATION_PREFIX}move_up`]: "Выше",
        [`${TRANSLATION_PREFIX}move_down`]: "Ниже",
        [`${TRANSLATION_PREFIX}rename`]: "Переименовать",
        [`${TRANSLATION_PREFIX}delete_section`]: "Удалить секцию",
        [`${TRANSLATION_PREFIX}setting_name`]: "Красить кубы по секциям",
        [`${TRANSLATION_PREFIX}setting_description`]: "Повторяет номера секций маркерными цветами. Отключите, если маркерные цвета используются для другого.",
        [`${TRANSLATION_PREFIX}panel_name`]: "Секции сборки",
        [`${TRANSLATION_PREFIX}new_section`]: "Новая секция",
        [`${TRANSLATION_PREFIX}show_all`]: "Показать всё",
        [`${TRANSLATION_PREFIX}section_hint`]: "Клик — выделить кубы секции; правый клик — остальные действия",
        [`${TRANSLATION_PREFIX}assign_here`]: "Отнести выделенные кубы сюда",
        [`${TRANSLATION_PREFIX}mirror_label`]: "красить кубы по секциям",
        [`${TRANSLATION_PREFIX}selected`]: "Выделено",
        [`${TRANSLATION_PREFIX}unassigned`]: "вне секций",
        [`${TRANSLATION_PREFIX}select`]: "выделить",
        [`${TRANSLATION_PREFIX}assigned_all`]: "размечено всё",
        [`${TRANSLATION_PREFIX}shortcut_name`]: "Отнести выделенное в секцию %0",
        [`${TRANSLATION_PREFIX}shortcut_description`]: "Секции сборки Axiomata",
        [`${TRANSLATION_PREFIX}undo_add`]: "Добавить секцию сборки",
        [`${TRANSLATION_PREFIX}undo_rename`]: "Переименовать секцию сборки",
        [`${TRANSLATION_PREFIX}undo_remove`]: "Удалить секцию сборки",
        [`${TRANSLATION_PREFIX}undo_reorder`]: "Изменить порядок секций сборки",
        [`${TRANSLATION_PREFIX}undo_assign`]: "Распределить кубы по секции сборки",
        [`${TRANSLATION_PREFIX}undo_isolate`]: "Изолировать секцию сборки",
        [`${TRANSLATION_PREFIX}undo_show_all`]: "Показать все кубы",
        [`${TRANSLATION_PREFIX}undo_repaint`]: "Перекрасить секции сборки"
    });

    function t(key, ...values) {
        let text = tl(`${TRANSLATION_PREFIX}${key}`);
        values.forEach((value, index) => {
            text = text.replace(`%${index}`, String(value));
        });
        return text;
    }

    let registered = [];
    let actions = [];
    let panel;
    let styles;
    let setting;
    let mirror = true;

    function sections() {
        if (!Project) return [];
        if (!Array.isArray(Project[SECTIONS_PROPERTY])) Project[SECTIONS_PROPERTY] = [];
        return Project[SECTIONS_PROPERTY];
    }

    function cubes() {
        return Cube.all ?? [];
    }

    function sectionIndex(name) {
        return sections().findIndex(section => section.name === name);
    }

    function mirrorsColours() {
        return mirror;
    }

    function setMirror(value) {
        mirror = Boolean(value);
        const stored = typeof settings !== "undefined" ? settings[MIRROR_SETTING] : null;
        if (stored) {
            stored.value = mirror;
            if (typeof Settings !== "undefined") {
                if (typeof Settings.saveLocalStorages === "function") Settings.saveLocalStorages();
                else if (typeof Settings.save === "function") Settings.save();
            }
        }
    }

    function paint(cube, name) {
        if (!mirrorsColours()) return;
        const index = sectionIndex(name);
        if (index >= 0 && typeof markerColors !== "undefined" && markerColors.length > 0) {
            cube.color = index % markerColors.length;
        }
    }

    function repaintAll() {
        for (const cube of cubes()) {
            const name = cube[CUBE_PROPERTY];
            if (name) paint(cube, name);
        }
    }

    function countsByName() {
        const counts = {};
        let unassigned = 0;
        for (const cube of cubes()) {
            const name = cube[CUBE_PROPERTY];
            if (name && sectionIndex(name) >= 0) {
                counts[name] = (counts[name] ?? 0) + 1;
            } else {
                unassigned++;
            }
        }
        return { counts, unassigned };
    }

    function refresh() {
        if (!panel || !panel.inside_vue) return;
        const { counts, unassigned } = countsByName();
        panel.inside_vue.sections = sections().map(section => ({
            name: section.name,
            cubes: counts[section.name] ?? 0
        }));
        panel.inside_vue.unassigned = unassigned;
        panel.inside_vue.selected = Cube.selected ? Cube.selected.length : 0;
        panel.inside_vue.mirror = mirrorsColours();
    }

    function edit(description, action) {
        Undo.initEdit({ elements: cubes(), outliner: true });
        action();
        Undo.finishEdit(description);
        refresh();
        Canvas.updateAll();
    }

    function addSection() {
        Blockbench.textPrompt(t("section_name"), "", name => {
            const trimmed = (name ?? "").trim();
            if (!trimmed || sectionIndex(trimmed) >= 0) return;
            edit(t("undo_add"), () => sections().push({ name: trimmed }));
        });
    }

    function renameSection(name) {
        Blockbench.textPrompt(t("new_name"), name, next => {
            const trimmed = (next ?? "").trim();
            if (!trimmed || trimmed === name || sectionIndex(trimmed) >= 0) return;
            edit(t("undo_rename"), () => {
                sections()[sectionIndex(name)].name = trimmed;
                for (const cube of cubes()) {
                    if (cube[CUBE_PROPERTY] === name) cube[CUBE_PROPERTY] = trimmed;
                }
            });
        });
    }

    function removeSection(name) {
        edit(t("undo_remove"), () => {
            const index = sectionIndex(name);
            if (index < 0) return;
            sections().splice(index, 1);
            for (const cube of cubes()) {
                if (cube[CUBE_PROPERTY] === name) cube[CUBE_PROPERTY] = "";
            }
            repaintAll();
        });
    }

    function moveSection(name, offset) {
        const index = sectionIndex(name);
        const target = index + offset;
        if (index < 0 || target < 0 || target >= sections().length) return;
        edit(t("undo_reorder"), () => {
            const list = sections();
            const [moved] = list.splice(index, 1);
            list.splice(target, 0, moved);
            repaintAll();
        });
    }

    function assignSelection(name) {
        if (!Cube.selected || Cube.selected.length === 0) {
            Blockbench.showQuickMessage(t("select_cubes_first"));
            return;
        }
        edit(t("undo_assign"), () => {
            for (const cube of Cube.selected) {
                cube[CUBE_PROPERTY] = name;
                paint(cube, name);
            }
        });
    }

    function applySelection(list) {
        for (const cube of cubes()) cube.selected = false;

        const target = (typeof Outliner !== "undefined" && Array.isArray(Outliner.selected))
            ? Outliner.selected
            : (Project ? Project.selected_elements : null);
        if (Array.isArray(target)) {
            target.length = 0;
            for (const cube of list) target.push(cube);
        }
        for (const cube of list) cube.selected = true;

        if (typeof updateSelection === "function") updateSelection();
        if (typeof Canvas !== "undefined" && typeof Canvas.updateAll === "function") Canvas.updateAll();
        refresh();
    }

    function selectSection(name) {
        const matching = cubes().filter(cube => cube[CUBE_PROPERTY] === name);
        if (matching.length === 0) {
            Blockbench.showQuickMessage(t("empty_section"));
            return;
        }
        applySelection(matching);
    }

    function selectUnassigned() {
        const matching = cubes().filter(cube => !cube[CUBE_PROPERTY] || sectionIndex(cube[CUBE_PROPERTY]) < 0);
        if (matching.length === 0) {
            Blockbench.showQuickMessage(t("all_assigned"));
            return;
        }
        applySelection(matching);
    }

    function rowMenu(event, name) {
        new Menu([
            { name: t("assign_selected"), icon: "add", click: () => assignSelection(name) },
            { name: t("isolate_section"), icon: "visibility", click: () => isolateSection(name) },
            "_",
            { name: t("move_up"), icon: "keyboard_arrow_up", click: () => moveSection(name, -1) },
            { name: t("move_down"), icon: "keyboard_arrow_down", click: () => moveSection(name, 1) },
            "_",
            { name: t("rename"), icon: "edit", click: () => renameSection(name) },
            { name: t("delete_section"), icon: "delete", click: () => removeSection(name) }
        ]).open(event);
    }

    function isolateSection(name) {
        edit(t("undo_isolate"), () => {
            for (const cube of cubes()) {
                cube.visibility = cube[CUBE_PROPERTY] === name;
            }
        });
    }

    function showEverything() {
        edit(t("undo_show_all"), () => {
            for (const cube of cubes()) cube.visibility = true;
        });
    }

    Plugin.register("axiomata_sections", {
        title: t("title"),
        author: "mss1r",
        icon: "construction",
        description: t("description"),
        version: "1.1.0",
        variant: "both",
        onload() {
            registered = [
                new Property(ModelProject, "array", SECTIONS_PROPERTY, { default: [] }),
                new Property(Cube, "string", CUBE_PROPERTY, { default: "" })
            ];

            setting = new Setting(MIRROR_SETTING, {
                name: t("setting_name"),
                description: t("setting_description"),
                category: "edit",
                type: "boolean",
                value: true,
                onChange(value) {
                    mirror = Boolean(value);
                    refresh();
                }
            });
            mirror = setting.value !== false;

            panel = new Panel(PANEL_ID, {
                name: t("panel_name"),
                growable: true,
                condition: { modes: ["edit"] },
                default_position: { slot: "right_bar", float_position: [0, 0], float_size: [300, 320], height: 260 },
                component: {
                    data() {
                        return { sections: [], unassigned: 0, selected: 0, mirror: true };
                    },
                    methods: {
                        add: addSection,
                        assign: assignSelection,
                        pick: selectSection,
                        menu: rowMenu,
                        pickUnassigned: selectUnassigned,
                        showAll: showEverything,
                        toggleMirror() {
                            setMirror(!mirrorsColours());
                            this.mirror = mirrorsColours();
                            if (this.mirror) {
                                edit(t("undo_repaint"), repaintAll);
                            } else {
                                refresh();
                            }
                        }
                    },
                    template: `
                        <div class="axiomata-sections">
                            <div class="sw-toolbar">
                                <button @click="add()">${t("new_section")}</button>
                                <button @click="showAll()">${t("show_all")}</button>
                            </div>
                            <ul class="sw-list">
                                <li v-for="(section, index) in sections" :key="section.name"
                                    class="sw-row"
                                    @click="pick(section.name)"
                                    @contextmenu.prevent="menu($event, section.name)"
                                    title="${t("section_hint")}">
                                    <span class="sw-index">{{ index + 1 }}</span>
                                    <span class="sw-name">{{ section.name }}</span>
                                    <span class="sw-count">{{ section.cubes }}</span>
                                    <button class="sw-assign" @click.stop="assign(section.name)"
                                            title="${t("assign_here")}">+</button>
                                </li>
                            </ul>
                            <label class="sw-mirror" @click.prevent="toggleMirror()">
                                <input type="checkbox" :checked="mirror">
                                ${t("mirror_label")}
                            </label>
                            <div class="sw-footer">
                                <span>${t("selected")}: {{ selected }}</span>
                                <span v-if="unassigned > 0" class="sw-unassigned">
                                    ${t("unassigned")} {{ unassigned }}
                                    <button @click="pickUnassigned()">${t("select")}</button>
                                </span>
                                <span v-else>${t("assigned_all")}</span>
                            </div>
                        </div>`
                }
            });

            styles = Blockbench.addCSS(`
                .axiomata-sections { display: flex; flex-direction: column; height: 100%; padding: 4px; }
                .axiomata-sections .sw-toolbar { display: flex; gap: 4px; margin-bottom: 4px; }
                .axiomata-sections .sw-toolbar button { flex: 1 1 0; }
                .axiomata-sections .sw-list { flex: 1 1 auto; overflow-y: auto; overflow-x: hidden; }
                .axiomata-sections .sw-row { display: flex; align-items: center; gap: 6px;
                    padding: 3px 4px; cursor: pointer; }
                .axiomata-sections .sw-row:hover { background-color: var(--color-selected); }
                .axiomata-sections .sw-index { opacity: 0.5; min-width: 14px; }
                .axiomata-sections .sw-name { flex: 1 1 auto; overflow: hidden;
                    text-overflow: ellipsis; white-space: nowrap; }
                .axiomata-sections .sw-count { opacity: 0.6; }
                .axiomata-sections .sw-assign { min-width: 22px; }
                .axiomata-sections .sw-mirror { display: flex; align-items: center; gap: 4px;
                    padding-top: 4px; cursor: pointer; opacity: 0.85; }
                .axiomata-sections .sw-footer { display: flex; justify-content: space-between;
                    gap: 6px; padding-top: 4px; opacity: 0.85; }
                .axiomata-sections .sw-unassigned { color: var(--color-accent); }
            `);

            actions = [];
            for (let slot = 0; slot < MAX_SHORTCUTS; slot++) {
                const action = new Action(`axiomata_assign_${slot + 1}`, {
                    name: t("shortcut_name", slot + 1),
                    description: t("shortcut_description"),
                    icon: "playlist_add",
                    category: "edit",
                    keybind: new Keybind({ key: 49 + slot, alt: true }),
                    condition: () => Boolean(Project) && sections().length > slot,
                    click() {
                        const section = sections()[slot];
                        if (section) assignSelection(section.name);
                    }
                });
                actions.push(action);
            }

            for (const event of ["update_selection", "select_project", "add_cube", "finish_edit", "undo", "redo"]) {
                Blockbench.on(event, refresh);
            }
            refresh();
        },
        onunload() {
            for (const event of ["update_selection", "select_project", "add_cube", "finish_edit", "undo", "redo"]) {
                Blockbench.removeListener(event, refresh);
            }
            if (panel) panel.delete();
            if (styles) styles.delete();
            if (setting) setting.delete();
            for (const action of actions) action.delete();
            actions = [];
            for (const property of registered) property.delete();
            registered = [];
        }
    });
})();
