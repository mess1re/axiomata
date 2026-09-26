# Axiomata Build Sections

`axiomata_sections.js` is a Blockbench plugin for assigning model cubes to construction stages.

## Install

Open **File → Plugins**, choose **Load Plugin from File**, and select
`axiomata_sections.js`.

## Use

Open the **Build Sections** panel in Edit mode, create the required sections, select cubes in the
outliner, and assign them with the `+` button. `Alt+1` through `Alt+8` assign the selection to the
corresponding section.

Section data is stored in the model as `axiomata_sections` and `axiomata_section` properties. The
plugin does not export construction resources by itself; an exporter or integration must read
those properties.

Marker colors can mirror section numbers for visual reference. This can be disabled in the panel
or Blockbench settings.
