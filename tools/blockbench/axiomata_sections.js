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
        [`${TRANSLATION_PREFIX}export_markup`]: "Construction Markup",
        [`${TRANSLATION_PREFIX}export_markup_description`]: "Export the build sections as data/<namespace>/construction/<model>.json",
        [`${TRANSLATION_PREFIX}export_no_sections`]: "The model has no build sections to export",
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
        [`${TRANSLATION_PREFIX}export_markup`]: "Разметка стройки",
        [`${TRANSLATION_PREFIX}export_markup_description`]: "Выгрузить секции сборки в data/<namespace>/construction/<model>.json",
        [`${TRANSLATION_PREFIX}export_no_sections`]: "В модели нет секций сборки",
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

    function groups() {
        return Group.all ?? [];
    }

    // A selected group stands for a bone without cubes, such as a rope anchor.
    function selectedGroups() {
        if (Array.isArray(Group.multi_selected)) return Group.multi_selected;
        return Group.selected ? [Group.selected] : [];
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
                for (const node of [...cubes(), ...groups()]) {
                    if (node[CUBE_PROPERTY] === name) node[CUBE_PROPERTY] = trimmed;
                }
            });
        });
    }

    function removeSection(name) {
        edit(t("undo_remove"), () => {
            const index = sectionIndex(name);
            if (index < 0) return;
            sections().splice(index, 1);
            for (const node of [...cubes(), ...groups()]) {
                if (node[CUBE_PROPERTY] === name) node[CUBE_PROPERTY] = "";
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
        const selectedCubes = Cube.selected ?? [];
        const anchors = selectedCubes.length === 0 ? selectedGroups() : [];
        if (selectedCubes.length === 0 && anchors.length === 0) {
            Blockbench.showQuickMessage(t("select_cubes_first"));
            return;
        }
        edit(t("undo_assign"), () => {
            for (const cube of selectedCubes) {
                cube[CUBE_PROPERTY] = name;
                paint(cube, name);
            }
            for (const group of anchors) group[CUBE_PROPERTY] = name;
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

    // The construction markup the game reads to raise the model stage by stage. Cube indices count each bone's
    // exported cubes in outliner order, as GeckoLib's geo file does. Parts are in entity-local blocks, the way GeckoLib
    // draws the model: turned half a revolution around Y. Nodes are plain objects, see liveNodes.
    const markup = (() => {
        function rotation(axis, degrees) {
            const a = degrees * Math.PI / 180, c = Math.cos(a), s = Math.sin(a);
            if (axis === 0) return [[1, 0, 0], [0, c, -s], [0, s, c]];
            if (axis === 1) return [[c, 0, s], [0, 1, 0], [-s, 0, c]];
            return [[c, -s, 0], [s, c, 0], [0, 0, 1]];
        }

        function multiply(a, b) {
            return a.map(row => [0, 1, 2].map(j => row[0] * b[0][j] + row[1] * b[1][j] + row[2] * b[2][j]));
        }

        function apply(m, v) {
            return m.map(row => row[0] * v[0] + row[1] * v[1] + row[2] * v[2]);
        }

        // Blockbench turns about X, then Y, then Z.
        function euler(angles) {
            let m = [[1, 0, 0], [0, 1, 0], [0, 0, 1]];
            for (let axis = 0; axis < 3; axis++) {
                if (angles[axis]) m = multiply(rotation(axis, angles[axis]), m);
            }
            return m;
        }

        function about(m, point, pivot) {
            return apply(m, point.map((v, i) => v - pivot[i])).map((v, i) => v + pivot[i]);
        }

        function part(cube, chain) {
            const low = cube.from.map(v => v - cube.inflate), high = cube.to.map(v => v + cube.inflate);
            let centre = low.map((v, i) => (v + high[i]) / 2);
            const half = low.map((v, i) => (high[i] - v) / 2);
            let turn = euler(cube.rotation);
            centre = about(turn, centre, cube.origin);
            for (let depth = chain.length - 1; depth >= 0; depth--) {
                const groupTurn = euler(chain[depth].rotation);
                centre = about(groupTurn, centre, chain[depth].origin);
                turn = multiply(groupTurn, turn);
            }
            const flip = [[-1, 0, 0], [0, 1, 0], [0, 0, -1]];
            centre = apply(flip, centre);
            turn = multiply(flip, turn);
            return { center: centre.map(v => v / 16), halfExtent: half.map(v => v / 16), rotation: [].concat(...turn) };
        }

        // Bones in the order GeckoLib exports them, a parent before the groups inside it, and every exported cube.
        function walk(nodes, chain, bones, cubes) {
            let index = 0;
            for (const node of nodes) {
                if (!node.export) continue;
                if (node.group) {
                    if (!bones.has(node.name)) bones.set(node.name, node);
                    walk(node.children, chain.concat([node]), bones, cubes);
                } else {
                    if (!chain.length) throw new Error("A cube is outside every bone; GeckoLib cannot export it");
                    cubes.push({ bone: chain[chain.length - 1].name, index: index++, cube: node, chain });
                }
            }
        }

        function number(value) {
            const rounded = Number(value.toFixed(4));
            return Object.is(rounded, -0) ? "0" : String(rounded);
        }

        // Objects across lines with two-space indents, number lists on one line.
        function write(value, indent) {
            if (typeof value === "number") return number(value);
            if (typeof value === "string") return JSON.stringify(value);
            const inner = indent + "  ";
            if (Array.isArray(value)) {
                if (value.every(v => typeof v === "number")) return "[" + value.map(number).join(", ") + "]";
                if (!value.length) return "[]";
                return "[\n" + value.map(v => inner + write(v, inner)).join(",\n") + "\n" + indent + "]";
            }
            const entries = value instanceof Map ? [...value.entries()] : Object.entries(value);
            if (!entries.length) return "{}";
            return "{\n" + entries.map(([k, v]) => inner + JSON.stringify(k) + ": " + write(v, inner)).join(",\n")
                + "\n" + indent + "}";
        }

        function build(nodes, names, modelId) {
            const bones = new Map(), cubes = [];
            walk(nodes, [], bones, cubes);
            const known = new Set(names);
            const counts = new Map();
            for (const entry of cubes) {
                counts.set(entry.bone, (counts.get(entry.bone) || 0) + 1);
                if (entry.cube.section && !known.has(entry.cube.section)) {
                    throw new Error(`Cube ${entry.index} of bone ${entry.bone} is in section ${entry.cube.section}, which the model does not list`);
                }
            }
            const boneCubes = new Map([...bones.keys()].filter(b => counts.has(b)).map(b => [b, counts.get(b)]));
            const sections = names.map(name => {
                const found = new Map(), parts = new Map();
                let low = null, high = null;
                for (const entry of cubes) {
                    if (entry.cube.section !== name) continue;
                    if (!found.has(entry.bone)) found.set(entry.bone, []);
                    found.get(entry.bone).push(entry.index);
                    parts.set(entry.bone + "#" + entry.index, part(entry.cube, entry.chain));
                    low = low ? low.map((v, i) => Math.min(v, entry.cube.from[i])) : entry.cube.from.slice();
                    high = high ? high.map((v, i) => Math.max(v, entry.cube.to[i])) : entry.cube.to.slice();
                }
                for (const [bone, group] of bones) {
                    if (group.section === name && !found.has(bone)) found.set(bone, []);
                }
                const ordered = new Map([...bones.keys()].filter(b => found.has(b)).map(b => [b, found.get(b)]));
                const section = new Map([["name", name], ["bones", ordered]]);
                if (low) {
                    // Model pixels with X the way GeckoLib exports it, used for hits when a file has no parts.
                    section.set("box", [-high[0], low[1], low[2], -low[0], high[1], high[2]]);
                    section.set("parts", [...ordered].flatMap(([bone, indices]) =>
                        indices.map(index => parts.get(bone + "#" + index))));
                }
                return section;
            });
            return write(new Map([["model", modelId], ["boneCubes", boneCubes], ["sections", sections]]), "") + "\n";
        }

        return { build };
    })();

    if (typeof AXIOMATA_MARKUP_TEST === "function") AXIOMATA_MARKUP_TEST(markup);

    function liveNodes(list) {
        return list.filter(node => node instanceof Group || node instanceof Cube).map(node => node instanceof Group
            ? {
                group: true, name: node.name, origin: node.origin, rotation: node.rotation,
                export: node.export !== false, section: node[CUBE_PROPERTY] || "", children: liveNodes(node.children)
            }
            : {
                group: false, from: node.from, to: node.to, origin: node.origin, rotation: node.rotation,
                inflate: node.inflate || 0, export: node.export !== false, section: node[CUBE_PROPERTY] || ""
            });
    }

    function exportMarkup() {
        const names = sections().map(section => section.name);
        if (!names.length) {
            Blockbench.showQuickMessage(t("export_no_sections"));
            return;
        }
        const name = Project.name || "model";
        const namespace = Project.geckolib_modid || "minecraft";
        Blockbench.export({
            type: "JSON",
            extensions: ["json"],
            name,
            content: markup.build(liveNodes(Outliner.root), names, `${namespace}:geo/${name}.geo.json`),
            savetype: "text"
        });
    }

    Plugin.register("axiomata_sections", {
        title: t("title"),
        author: "mss1r",
        icon: "construction",
        description: t("description"),
        version: "1.3.0",
        variant: "both",
        onload() {
            registered = [
                new Property(ModelProject, "array", SECTIONS_PROPERTY, { default: [] }),
                new Property(Cube, "string", CUBE_PROPERTY, { default: "" }),
                new Property(Group, "string", CUBE_PROPERTY, { default: "" })
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
            const exportAction = new Action("axiomata_export_markup", {
                name: t("export_markup"),
                description: t("export_markup_description"),
                icon: "construction",
                category: "file",
                condition: () => Boolean(Project) && sections().length > 0,
                click: exportMarkup
            });
            MenuBar.addAction(exportAction, "file.export");
            actions.push(exportAction);
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
