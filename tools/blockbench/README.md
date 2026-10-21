# Axiomata Build Sections

`axiomata_sections.js` is a Blockbench plugin for assigning model cubes to construction stages.

## Install

Open **File → Plugins**, choose **Load Plugin from File**, and select
`axiomata_sections.js`.

## Use

Open the **Build Sections** panel in Edit mode, create the required sections, select cubes in the
outliner, and assign them with the `+` button. `Alt+1` through `Alt+8` assign the selection to the
corresponding section.

With no cubes selected, the selected group is assigned instead. Use this for bones without cubes,
such as rope anchors: they stand once their section is built.

Section data is stored in the model as `axiomata_sections` and `axiomata_section` properties.
`tools/construction_markup.py` turns them into the construction markup file:

```
python tools/construction_markup.py cart.bbmodel data/example/construction/cart.json --model example:geo/cart.geo.json
```

Marker colors can mirror section numbers for visual reference. This can be disabled in the panel
or Blockbench settings.
