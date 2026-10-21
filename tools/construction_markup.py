#!/usr/bin/env python3
"""Writes the construction markup of a GeckoLib model from its Blockbench file.

The sections come from the Build Sections plugin (tools/blockbench/axiomata_sections.js): the model's
``axiomata_sections`` list gives their order, each cube's ``axiomata_section`` its section. A group
may carry ``axiomata_section`` too: a bone without cubes, such as a rope anchor, is then listed in
that section and stands once it is built.

    python construction_markup.py trebuchet.bbmodel data/example/construction/trebuchet.json \\
        --model example:geo/trebuchet.geo.json

Parts are written in entity-local blocks, the way GeckoLib draws the model: turned half a revolution
around Y. Cube indices count the exported cubes of each bone in outliner order, as in the geo file.
"""
import argparse
import json
import math
import sys

SECTIONS_PROPERTY = "axiomata_sections"
SECTION_PROPERTY = "axiomata_section"


def rotation(axis, degrees):
    a = math.radians(degrees)
    c, s = math.cos(a), math.sin(a)
    if axis == 0:
        return [[1, 0, 0], [0, c, -s], [0, s, c]]
    if axis == 1:
        return [[c, 0, s], [0, 1, 0], [-s, 0, c]]
    return [[c, -s, 0], [s, c, 0], [0, 0, 1]]


def multiply(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def apply(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def euler(angles):
    # Blockbench turns about X, then Y, then Z.
    m = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for axis in range(3):
        if angles[axis]:
            m = multiply(rotation(axis, angles[axis]), m)
    return m


def about(m, point, pivot):
    return [a + b for a, b in zip(apply(m, [p - q for p, q in zip(point, pivot)]), pivot)]


def clean(value):
    value = round(value, 4)
    return 0 if value == 0 else (int(value) if value == int(value) else value)


class Model:
    def __init__(self, data):
        self.data = data
        self.elements = {e["uuid"]: e for e in data.get("elements", [])}
        self.groups = {g["uuid"]: g for g in data.get("groups", [])}
        self.cubes = []  # (bone, index in bone, element, groups from the root down)
        self.bones = {}  # bone name -> group, for every group
        self.walk(data.get("outliner", []), [])

    def group(self, node):
        # Format 5 keeps groups apart from the outliner tree; older files nest them.
        return node if "name" in node else {**self.groups.get(node["uuid"], {}), **node}

    def walk(self, nodes, chain):
        index = 0
        for node in nodes:
            if isinstance(node, str):
                element = self.elements.get(node)
                if element is None or element.get("type", "cube") != "cube" or element.get("export") is False:
                    continue
                if not chain:
                    sys.exit("cube %s is outside every bone; GeckoLib cannot export it" % element.get("name"))
                self.cubes.append((chain[-1]["name"], index, element, list(chain)))
                index += 1
            else:
                group = self.group(node)
                if group.get("export") is False:
                    continue
                self.bones[group["name"]] = group
                self.walk(node.get("children", []), chain + [group])


def part(element, chain):
    inflate = element.get("inflate", 0) or 0
    low = [v - inflate for v in element["from"]]
    high = [v + inflate for v in element["to"]]
    center = [(a + b) / 2 for a, b in zip(low, high)]
    half = [(b - a) / 2 for a, b in zip(low, high)]
    turn = euler(element.get("rotation", [0, 0, 0]))
    center = about(turn, center, element.get("origin", [0, 0, 0]))
    for group in reversed(chain):
        group_turn = euler(group.get("rotation", [0, 0, 0]))
        center = about(group_turn, center, group.get("origin", [0, 0, 0]))
        turn = multiply(group_turn, turn)
    # Half a revolution around Y: X and Z point the other way in the entity.
    flip = [[-1, 0, 0], [0, 1, 0], [0, 0, -1]]
    center = apply(flip, center)
    turn = multiply(flip, turn)
    return {
        "center": [clean(v / 16) for v in center],
        "halfExtent": [clean(v / 16) for v in half],
        "rotation": [clean(v) for row in turn for v in row],
    }


def markup(data, model_id):
    model = Model(data)
    names = [s["name"] if isinstance(s, dict) else s for s in data.get(SECTIONS_PROPERTY, [])]
    if not names:
        sys.exit("the model has no build sections; assign them with the Build Sections plugin")
    known = set(names)
    bone_cubes = {}
    for bone, _, _, _ in model.cubes:
        bone_cubes[bone] = bone_cubes.get(bone, 0) + 1
    sections = []
    for name in names:
        bones, parts, low, high = {}, [], None, None
        for bone, index, element, chain in model.cubes:
            if element.get(SECTION_PROPERTY) != name:
                continue
            bones.setdefault(bone, []).append(index)
            parts.append(part(element, chain))
            low = element["from"] if low is None else [min(a, b) for a, b in zip(low, element["from"])]
            high = element["to"] if high is None else [max(a, b) for a, b in zip(high, element["to"])]
        for bone, group in model.bones.items():
            if group.get(SECTION_PROPERTY) == name and bone not in bones:
                bones[bone] = []
        if not parts and not bones:
            print("warning: section %s has nothing assigned" % name, file=sys.stderr)
        section = {"name": name, "bones": bones}
        if low is not None:
            section["box"] = [clean(v) for v in list(low) + list(high)]
            section["parts"] = parts
        sections.append(section)
    for bone, index, element, _ in model.cubes:
        assigned = element.get(SECTION_PROPERTY)
        if assigned and assigned not in known:
            sys.exit("cube %d of bone %s is in section %s, which the model does not list" % (index, bone, assigned))
    return {"model": model_id, "boneCubes": bone_cubes, "sections": sections}


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("bbmodel")
    parser.add_argument("output")
    parser.add_argument("--model", required=True, help="the geo resource, e.g. example:geo/cart.geo.json")
    args = parser.parse_args()
    with open(args.bbmodel, encoding="utf-8") as source:
        data = json.load(source)
    with open(args.output, "w", encoding="utf-8", newline="\n") as target:
        json.dump(markup(data, args.model), target, indent=2)
        target.write("\n")


if __name__ == "__main__":
    main()
